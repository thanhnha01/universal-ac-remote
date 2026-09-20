package com.thanhnha.universalacremote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Transaction
import com.thanhnha.universalacremote.ir.CatalogTransmitter
import com.thanhnha.universalacremote.ir.RemoteCandidate
import com.thanhnha.universalacremote.ir.RemoteQuery
import com.thanhnha.universalacremote.ir.RemoteResolver
import com.thanhnha.universalacremote.ir.IrTransmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "saved_remotes")
data class SavedRemote(
    @PrimaryKey val id: String,
    val displayName: String,
    val catalogProfileId: String,
    val brand: String,
    val acModel: String?,
    val remoteModel: String?,
    val protocolId: String?,
    val protocolModel: String?,
    val verifiedCapabilities: List<String>,
    @ColumnInfo(defaultValue = "''") val importedCommandsJson: String = "",
    @ColumnInfo(defaultValue = "''") val roomName: String = "",
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val lastUsedAtEpochMs: Long = 0L,
    @ColumnInfo(defaultValue = "''") val lastSentStateJson: String = "",
)

class SavedRemoteConverters {
    @TypeConverter fun encodeCapabilities(value: List<String>): String = JSONArray(value).toString()
    @TypeConverter fun decodeCapabilities(value: String): List<String> = runCatching {
        JSONArray(value).let { array -> (0 until array.length()).map(array::getString) }
    }.getOrDefault(emptyList())

    @TypeConverter fun encodeImportedCommands(commands: List<ImportedRawCommand>): String = JSONArray().apply {
        commands.forEach { command -> put(JSONObject().put("name", command.name)
            .put("frequency", command.transmission.carrierFrequencyHz)
            .put("timings", JSONArray(command.transmission.timingsMicros))) }
    }.toString()
    @TypeConverter fun decodeImportedCommands(value: String): List<ImportedRawCommand> = runCatching {
        val array = JSONArray(value)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val timings = item.getJSONArray("timings").let { values -> (0 until values.length()).map(values::getInt) }
            val transmission = IrTransmission(item.getInt("frequency"), timings).also { it.validate() }
            ImportedRawCommand(item.getString("name"), transmission)
        }
    }.getOrDefault(emptyList())
}

data class ImportedRawCommand(val name: String, val transmission: IrTransmission)

@Dao
interface SavedRemoteDao {
    @Query("SELECT * FROM saved_remotes ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<SavedRemote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(remote: SavedRemote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(remotes: List<SavedRemote>)

    @Query("UPDATE saved_remotes SET displayName = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE saved_remotes SET roomName = :roomName WHERE id = :id")
    suspend fun updateRoom(id: String, roomName: String)

    @Query("UPDATE saved_remotes SET favorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: String, favorite: Boolean)

    @Query("UPDATE saved_remotes SET lastUsedAtEpochMs = :lastUsedAtEpochMs WHERE id = :id")
    suspend fun markUsed(id: String, lastUsedAtEpochMs: Long)

    @Query("UPDATE saved_remotes SET lastSentStateJson = :stateJson WHERE id = :id")
    suspend fun updateLastSentState(id: String, stateJson: String)

    @Query("DELETE FROM saved_remotes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM saved_remotes")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(remotes: List<SavedRemote>) {
        deleteAll()
        saveAll(remotes)
    }
}

@Database(entities = [SavedRemote::class], version = 4, exportSchema = false)
@TypeConverters(SavedRemoteConverters::class)
abstract class SavedRemoteDatabase : RoomDatabase() {
    abstract fun savedRemoteDao(): SavedRemoteDao

    companion object {
        @Volatile private var instance: SavedRemoteDatabase? = null
        fun get(application: Application): SavedRemoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(application, SavedRemoteDatabase::class.java, "saved-remotes.db")
                .addMigrations(
                    object : androidx.room.migration.Migration(1, 2) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE saved_remotes ADD COLUMN importedCommandsJson TEXT NOT NULL DEFAULT ''")
                        }
                    },
                    object : androidx.room.migration.Migration(2, 3) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE saved_remotes ADD COLUMN roomName TEXT NOT NULL DEFAULT ''")
                            db.execSQL("ALTER TABLE saved_remotes ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE saved_remotes ADD COLUMN lastUsedAtEpochMs INTEGER NOT NULL DEFAULT 0")
                        }
                    },
                    object : androidx.room.migration.Migration(3, 4) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE saved_remotes ADD COLUMN lastSentStateJson TEXT NOT NULL DEFAULT ''")
                        }
                    },
                )
                .build().also { instance = it }
        }
    }
}

data class HomeUiState(val remotes: List<SavedRemote> = emptyList(), val loading: Boolean = true)
data class CatalogUiState(val loading: Boolean = true, val error: String? = null, val profileCount: Int = 0)
data class CatalogSearchState(val loading: Boolean = false, val results: List<RemoteCandidate> = emptyList())

private data class LoadedCatalog(
    val resolver: RemoteResolver,
    val popularBrands: List<String>,
    val scannerBrands: List<String>,
    val scannerCapableIds: Set<String>,
    val allBrands: List<String>,
)

class SavedRemotesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = SavedRemoteDatabase.get(application).savedRemoteDao()
    private val mutableState = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()
    private val mutableCatalog = MutableStateFlow(CatalogUiState())
    val catalog: StateFlow<CatalogUiState> = mutableCatalog.asStateFlow()
    private val mutablePopularBrands = MutableStateFlow<List<String>>(emptyList())
    val popularBrands: StateFlow<List<String>> = mutablePopularBrands.asStateFlow()
    private val mutableAllBrands = MutableStateFlow<List<String>>(emptyList())
    val allBrands: StateFlow<List<String>> = mutableAllBrands.asStateFlow()
    private val mutableBrandProfiles = MutableStateFlow<List<RemoteCandidate>>(emptyList())
    val brandProfiles: StateFlow<List<RemoteCandidate>> = mutableBrandProfiles.asStateFlow()
    private val mutableScannerBrands = MutableStateFlow<List<String>>(emptyList())
    val scannerBrands: StateFlow<List<String>> = mutableScannerBrands.asStateFlow()
    private val mutableSearch = MutableStateFlow(CatalogSearchState())
    val search: StateFlow<CatalogSearchState> = mutableSearch.asStateFlow()
    private val mutableScanCandidates = MutableStateFlow<List<RemoteCandidate>>(emptyList())
    val scanCandidates: StateFlow<List<RemoteCandidate>> = mutableScanCandidates.asStateFlow()
    @Volatile private var resolver: RemoteResolver? = null
    @Volatile private var scannerCapableIds: Set<String> = emptySet()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { dao.observeAll().collect { mutableState.value = HomeUiState(it, false) } }
        viewModelScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().assets.open("catalog-index.json").bufferedReader().use { it.readText() }
                }
                withContext(Dispatchers.Default) {
                    val loaded = RemoteResolver.fromCatalogIndex(json)
                    val capableIds = loaded.resolve(RemoteQuery())
                        .asSequence()
                        .filter(CatalogTransmitter::supports)
                        .map(RemoteCandidate::id)
                        .toSet()
                    LoadedCatalog(
                        resolver = loaded,
                        popularBrands = loaded.popularBrands(),
                        scannerBrands = loaded.scannerBrands { it.id in capableIds },
                        scannerCapableIds = capableIds,
                        allBrands = loaded.allBrands(),
                    )
                }
            }.onSuccess { loaded ->
                resolver = loaded.resolver
                scannerCapableIds = loaded.scannerCapableIds
                mutableCatalog.value = CatalogUiState(loading = false, profileCount = loaded.resolver.profileCount)
                mutablePopularBrands.value = loaded.popularBrands
                mutableAllBrands.value = loaded.allBrands
                mutableScannerBrands.value = loaded.scannerBrands
            }.onFailure { error ->
                mutableCatalog.value = CatalogUiState(loading = false, error = error.message ?: "Không thể tải danh mục máy lạnh.")
            }
        }
    }

    fun updateSearch(query: RemoteQuery) {
        searchJob?.cancel()
        if (resolver == null) return
        mutableSearch.value = CatalogSearchState(loading = true)
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(120)
            val result = resolver?.resolve(query).orEmpty().take(30)
            mutableSearch.value = CatalogSearchState(loading = false, results = result)
        }
    }

    /**
     * Search the user-facing catalog from one text field. The previous UI sent
     * every query as a brand query even though the hint promised brand/model/
     * remote search, so model searches silently returned nothing.
     */
    fun updateSearchText(text: String) {
        searchJob?.cancel()
        val activeResolver = resolver ?: return
        if (text.isBlank()) {
            mutableSearch.value = CatalogSearchState()
            return
        }
        mutableSearch.value = CatalogSearchState(loading = true)
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(120)
            val result = buildList {
                addAll(activeResolver.resolve(RemoteQuery(brand = text)))
                addAll(activeResolver.resolve(RemoteQuery(acModel = text)))
                addAll(activeResolver.resolve(RemoteQuery(remoteModel = text)))
            }.distinctBy(RemoteCandidate::id)
                .sortedWith(compareBy<RemoteCandidate> { if (it.id in scannerCapableIds) 0 else 1 }
                    .thenBy(RemoteCandidate::priority)
                    .thenBy(RemoteCandidate::brand)
                    .thenBy(RemoteCandidate::id))
                .take(40)
            mutableSearch.value = CatalogSearchState(loading = false, results = result)
        }
    }

    fun canTransmit(candidate: RemoteCandidate): Boolean = candidate.id in scannerCapableIds

    fun browseBrand(brand: String) {
        mutableBrandProfiles.value = resolver?.brandCandidates(brand)
            .orEmpty()
            .filter(::canTransmit)
    }

    fun clearBrandBrowse() {
        mutableBrandProfiles.value = emptyList()
    }

    fun beginScan(query: RemoteQuery = RemoteQuery(), selected: RemoteCandidate? = null) {
        val canTransmit: (RemoteCandidate) -> Boolean = ::canTransmit
        val previouslySuccessful = mutableState.value.remotes
            .asSequence()
            .filter { it.verifiedCapabilities.isNotEmpty() }
            .map { it.catalogProfileId }
            .toSet()
        mutableScanCandidates.value = if (selected != null) {
            listOf(selected).filter(canTransmit)
        } else {
            resolver?.scannerCandidates(query, canTransmit)
                .orEmpty()
                .sortedWith(
                    compareBy<RemoteCandidate> { if (it.id in previouslySuccessful) 0 else 1 }
                        .thenBy(RemoteCandidate::priority)
                        .thenByDescending { it.verifiedScannerCapabilityCount() }
                        .thenBy { it.id },
                )
        }
    }

    fun clearScan() {
        mutableScanCandidates.value = emptyList()
    }

    fun profileFor(id: String): RemoteCandidate? = resolver?.findById(id)

    fun save(remote: SavedRemote) = viewModelScope.launch { dao.save(remote) }
    fun rename(id: String, name: String) = viewModelScope.launch { dao.rename(id, name.trim()) }
    fun updateRoom(id: String, roomName: String) = viewModelScope.launch {
        dao.updateRoom(id, roomName.trim())
    }
    fun setFavorite(id: String, favorite: Boolean) = viewModelScope.launch {
        dao.updateFavorite(id, favorite)
    }
    fun markUsed(id: String, atEpochMs: Long = System.currentTimeMillis()) = viewModelScope.launch {
        dao.markUsed(id, atEpochMs)
    }
    fun updateLastSentState(id: String, state: com.thanhnha.universalacremote.ir.AcState) = viewModelScope.launch {
        dao.updateLastSentState(id, encodeLastSentState(state))
    }
    fun restoreBackup(bundle: BackupBundle) = viewModelScope.launch {
        dao.replaceAll(bundle.remotes)
    }
    fun delete(id: String) = viewModelScope.launch { dao.delete(id) }
}

private fun RemoteCandidate.verifiedScannerCapabilityCount(): Int =
    runCatching { com.thanhnha.universalacremote.ir.RemoteControls.from(this).verificationRequirements().size }
        .getOrDefault(0)


fun encodeLastSentState(state: com.thanhnha.universalacremote.ir.AcState): String =
    JSONObject()
        .put("power", state.power)
        .put("temperatureCelsius", state.temperatureCelsius)
        .put("mode", state.mode.name)
        .put("fan", state.fan.name)
        .put("swingVertical", state.swingVertical)
        .put("swingHorizontal", state.swingHorizontal)
        .toString()

fun decodeLastSentState(value: String): com.thanhnha.universalacremote.ir.AcState? = runCatching {
    if (value.isBlank()) return@runCatching null
    val json = JSONObject(value)
    com.thanhnha.universalacremote.ir.AcState(
        power = json.getBoolean("power"),
        temperatureCelsius = json.getInt("temperatureCelsius"),
        mode = com.thanhnha.universalacremote.ir.AcMode.valueOf(json.getString("mode")),
        fan = com.thanhnha.universalacremote.ir.AcFan.valueOf(json.getString("fan")),
        swingVertical = json.optBoolean("swingVertical", false),
        swingHorizontal = json.optBoolean("swingHorizontal", false),
    )
}.getOrNull()
