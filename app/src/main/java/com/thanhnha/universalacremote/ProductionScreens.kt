package com.thanhnha.universalacremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thanhnha.universalacremote.ir.AcFan
import com.thanhnha.universalacremote.ir.AcMode
import com.thanhnha.universalacremote.ir.AcState
import com.thanhnha.universalacremote.ir.AndroidIrTransmitter
import com.thanhnha.universalacremote.ir.CatalogTransmitter
import com.thanhnha.universalacremote.ir.IrHardwareDiagnostics
import com.thanhnha.universalacremote.ir.RemoteCandidate
import com.thanhnha.universalacremote.ir.RemoteControls
import com.thanhnha.universalacremote.ir.RemoteQuery
import com.thanhnha.universalacremote.ir.ScanResult
import com.thanhnha.universalacremote.ir.ScanState
import com.thanhnha.universalacremote.ir.UniversalAcScanner
import com.thanhnha.universalacremote.ir.VerificationCheck
import com.thanhnha.universalacremote.ir.displayModelLabel
import com.thanhnha.universalacremote.ir.compactDisplayModelLabel
import com.thanhnha.universalacremote.update.UpdatePanel
import java.util.UUID

@Composable
fun ProductionHomeScreen(
    state: HomeUiState,
    diagnostics: IrHardwareDiagnostics,
    store: SavedRemotesViewModel,
    onTab: (String) -> Unit,
    navigate: (String) -> Unit,
) {
    val favorites = remember(state.remotes) { favoriteRemotes(state.remotes) }
    val recent = remember(state.remotes) { recentRemotes(state.remotes) }
    val rooms = remember(state.remotes) { roomSummaries(state.remotes) }

    AppScaffold("home", onTab) { padding ->
        PageColumn(padding) {
            BrandHeader {
                HeaderIconButton(Icons.Filled.Search, "Thêm máy lạnh") { navigate("add") }
            }

            SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        if (state.remotes.isEmpty()) "Ngôi nhà mát hơn bắt đầu ở đây"
                        else "Điều khiển mọi phòng trong một chạm",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.navy,
                    )
                    Text(
                        if (state.remotes.isEmpty()) "Thêm máy lạnh đầu tiên bằng model, dò mã hoặc file .ir."
                        else state.remotes.size.toString() + " thiết bị • " + rooms.size + " khu vực",
                        color = AppColors.navySoft,
                    )
                    if (state.remotes.isEmpty()) {
                        PrimaryButton(
                            "Thêm máy lạnh",
                            Modifier.fillMaxWidth(),
                            Icons.Filled.Add,
                        ) { navigate("add") }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryButton(
                                "Mở thiết bị",
                                Modifier.weight(1f),
                                Icons.Filled.AcUnit,
                            ) { onTab("remote") }
                            SecondaryButton(
                                "Thêm mới",
                                Modifier.weight(1f),
                                Icons.Filled.Add,
                            ) { navigate("add") }
                        }
                    }
                }
            }

            if (!diagnostics.hasIrEmitter) {
                InfoBanner(
                    "Bộ phát IR chưa sẵn sàng. Bạn vẫn có thể sắp xếp và quản lý thiết bị.",
                    Icons.Filled.ErrorOutline,
                    AppColors.danger,
                    AppColors.paleDanger,
                )
            }

            when {
                state.loading -> SurfaceCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(22.dp))
                        Text("Đang tải thiết bị…", color = AppColors.navySoft)
                    }
                }

                state.remotes.isEmpty() -> EmptyState(
                    "Chưa có máy lạnh",
                    "Sau khi thêm thiết bị, các phòng, mục yêu thích và thiết bị gần đây sẽ xuất hiện ở đây.",
                    Icons.Outlined.AcUnit,
                )

                else -> {
                    if (favorites.isNotEmpty()) {
                        SectionTitle("Yêu thích")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(favorites, key = SavedRemote::id) { remote ->
                                HomeQuickRemoteCard(
                                    remote = remote,
                                    icon = Icons.Filled.Star,
                                    onClick = { navigate("remote/" + remote.id) },
                                )
                            }
                        }
                    }

                    if (recent.isNotEmpty()) {
                        SectionTitle("Gần đây")
                        recent.forEach { remote ->
                            HomeRemoteRow(
                                remote = remote,
                                store = store,
                                onClick = { navigate("remote/" + remote.id) },
                            )
                        }
                    }

                    SectionTitle("Phòng")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(rooms, key = RoomSummary::name) { room ->
                            Surface(
                                modifier = Modifier
                                    .width(158.dp)
                                    .clickable { onTab("remote") },
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, AppColors.line),
                            ) {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    IconBubble(Icons.Filled.Home, size = 44)
                                    Text(
                                        room.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppColors.navy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        room.count.toString() + " thiết bị",
                                        color = AppColors.navySoft,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    SecondaryButton(
                        "Xem tất cả thiết bị",
                        Modifier.fillMaxWidth(),
                        Icons.Filled.ArrowForward,
                    ) { onTab("remote") }
                }
            }
        }
    }
}

@Composable
fun ProductionDevicesScreen(
    state: HomeUiState,
    store: SavedRemotesViewModel,
    onTab: (String) -> Unit,
    navigate: (String) -> Unit,
) {
    var selectedRoom by remember { mutableStateOf<String?>(null) }
    val rooms = remember(state.remotes) { roomSummaries(state.remotes) }
    val visibleRemotes = remember(state.remotes, selectedRoom) {
        selectedRoom?.let { room -> state.remotes.filter { it.roomLabel() == room } } ?: state.remotes
    }

    AppScaffold("remote", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar(
                title = "Thiết bị",
                subtitle = if (state.loading) "Đang tải…" else state.remotes.size.toString() + " máy lạnh đã lưu",
                actions = {
                    HeaderIconButton(Icons.Filled.Add, "Thêm máy lạnh") { navigate("add") }
                },
            )

            if (rooms.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        DeviceRoomFilter(
                            label = "Tất cả",
                            count = state.remotes.size,
                            selected = selectedRoom == null,
                        ) { selectedRoom = null }
                    }
                    items(rooms, key = RoomSummary::name) { room ->
                        DeviceRoomFilter(
                            label = room.name,
                            count = room.count,
                            selected = selectedRoom == room.name,
                        ) { selectedRoom = room.name }
                    }
                }
            }

            when {
                state.loading -> SurfaceCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(22.dp))
                        Text("Đang tải thiết bị…", color = AppColors.navySoft)
                    }
                }

                state.remotes.isEmpty() -> {
                    EmptyState(
                        "Chưa có thiết bị",
                        "Thêm máy lạnh để bắt đầu tạo các phòng và mục yêu thích.",
                        Icons.Outlined.AcUnit,
                    )
                    PrimaryButton(
                        "Thêm máy lạnh",
                        Modifier.fillMaxWidth(),
                        Icons.Filled.Add,
                    ) { navigate("add") }
                }

                visibleRemotes.isEmpty() -> EmptyState(
                    "Phòng này chưa có thiết bị",
                    "Bạn có thể đổi phòng từ màn Chi tiết thiết bị.",
                    Icons.Filled.Home,
                )

                else -> visibleRemotes
                    .sortedWith(
                        compareByDescending<SavedRemote> { it.favorite }
                            .thenByDescending { it.lastUsedAtEpochMs }
                            .thenBy { it.displayName.lowercase() },
                    )
                    .forEach { remote ->
                        HomeRemoteRow(
                            remote = remote,
                            store = store,
                            showRoom = true,
                            onClick = { navigate("remote/" + remote.id) },
                        )
                    }
            }
        }
    }
}

@Composable
private fun DeviceRoomFilter(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AppColors.blue else Color.White,
        border = BorderStroke(1.dp, if (selected) AppColors.blue else AppColors.line),
    ) {
        Text(
            label + " · " + count,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = if (selected) Color.White else AppColors.navy,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HomeQuickRemoteCard(
    remote: SavedRemote,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(190.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconBubble(icon, tint = AppColors.warning, background = AppColors.paleWarning, size = 42)
                Text(
                    remote.roomLabel(),
                    color = AppColors.navySoft,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                remote.displayName,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.navy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                remote.brand,
                color = AppColors.navySoft,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeRemoteRow(
    remote: SavedRemote,
    store: SavedRemotesViewModel,
    showRoom: Boolean = true,
    onClick: () -> Unit,
) {
    val profile = store.profileFor(remote.catalogProfileId)
    val imported = remote.importedCommandsJson.isNotBlank()
    val verification = remote.verificationState(profile)

    SurfaceCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            AcWallUnitArt(remote.brand, Modifier.width(98.dp).height(66.dp))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        remote.displayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.navy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (remote.favorite) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Yêu thích",
                            tint = AppColors.warning,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Text(
                    buildList {
                        if (showRoom) add(remote.roomLabel())
                        add(remote.brand)
                        remote.acModel?.takeIf(String::isNotBlank)?.let(::add)
                    }.joinToString(" • "),
                    color = AppColors.navySoft,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(
                    when {
                        imported -> "File IR"
                        verification == SavedVerificationState.FULL -> "Đã xác minh"
                        verification == SavedVerificationState.PARTIAL -> "Xác minh một phần"
                        else -> "Chưa xác minh"
                    },
                    when {
                        imported || verification == SavedVerificationState.FULL -> Icons.Filled.CheckCircle
                        else -> Icons.Filled.Info
                    },
                    when {
                        imported -> AppColors.blue
                        verification == SavedVerificationState.FULL -> AppColors.mint
                        else -> AppColors.warning
                    },
                    when {
                        imported -> AppColors.paleBlue
                        verification == SavedVerificationState.FULL -> AppColors.paleMint
                        else -> AppColors.paleWarning
                    },
                )
            }
            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = AppColors.navySoft)
        }
    }
}

@Composable
fun ProductionAddScreen(
    store: SavedRemotesViewModel,
    catalog: CatalogUiState,
    onTab: (String) -> Unit,
    navigate: (String) -> Unit,
) {
    val popular by store.popularBrands.collectAsState()
    val allBrands by store.allBrands.collectAsState()
    val brandProfiles by store.brandProfiles.collectAsState()
    val search by store.search.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var selectedLetter by remember(allBrands) {
        mutableStateOf(allBrands.firstOrNull()?.firstOrNull()?.uppercaseChar() ?: 'A')
    }

    fun searchNow(value: String) {
        query = value
        selectedBrand = null
        store.clearBrandBrowse()
        store.updateSearchText(value)
    }

    fun chooseBrand(brand: String) {
        query = ""
        selectedBrand = brand
        store.browseBrand(brand)
    }

    val visibleResults = search.results
    val sections = remember(allBrands) { brandSections(allBrands) }
    val visibleBrands = remember(sections, selectedLetter) {
        sections.firstOrNull { it.letter == selectedLetter }?.brands.orEmpty()
    }
    val seriesGroups = remember(brandProfiles) { modelSeriesGroups(brandProfiles) }

    AppScaffold("home", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar(
                "Thêm máy lạnh",
                selectedBrand ?: "Tìm hãng, model hoặc remote",
                onBack = {
                    if (selectedBrand != null) {
                        selectedBrand = null
                        store.clearBrandBrowse()
                    } else {
                        onTab("home")
                    }
                },
            )

            SearchField(query, ::searchNow, "Tìm hãng, model máy hoặc model remote")

            if (catalog.loading) {
                InfoBanner("Đang tải thư viện điều khiển…", Icons.Filled.Refresh, AppColors.blue, AppColors.paleBlue)
            }
            catalog.error?.let {
                InfoBanner("Không thể tải thư viện điều khiển.", Icons.Filled.ErrorOutline, AppColors.danger, AppColors.paleDanger)
            }

            when {
                query.isNotBlank() -> {
                    if (search.loading) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    } else {
                        SectionTitle("Kết quả phù hợp")
                        if (visibleResults.isEmpty()) {
                            EmptyState(
                                "Không tìm thấy mã phù hợp",
                                "Thử tên hãng, model máy hoặc model remote khác.",
                                Icons.Filled.Search,
                            )
                        } else {
                            visibleResults.take(30).forEach { candidate ->
                                val canTransmit = store.canTransmit(candidate)
                                UserProfileCard(candidate, canTransmit) {
                                    if (canTransmit) {
                                        store.beginScan(selected = candidate)
                                        navigate("scan")
                                    }
                                }
                            }
                        }
                    }
                }

                selectedBrand != null -> {
                    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
                        Row(
                            Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            AcWallUnitArt(selectedBrand.orEmpty(), Modifier.width(112.dp).height(74.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    selectedBrand.orEmpty(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.navy,
                                )
                                Text(
                                    buildString {
                                        val usable = brandProfiles.count(store::canTransmit)
                                        append(usable)
                                        append(" có thể phát")
                                        val references = brandProfiles.size - usable
                                        if (references > 0) append(" • ").append(references).append(" có trong thư viện")
                                    },
                                    color = AppColors.navySoft,
                                )
                            }
                        }
                    }

                    SectionTitle("Series / Model")
                    if (seriesGroups.isEmpty()) {
                        EmptyState(
                            "Chưa có profile có thể phát",
                            "Bạn vẫn có thể thử dò remote theo hãng này.",
                            Icons.Filled.Info,
                        )
                    } else {
                        seriesGroups.forEach { group ->
                            SurfaceCard(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppColors.navy,
                                    )
                                    group.profiles.take(8).forEach { candidate ->
                                        val canTransmit = store.canTransmit(candidate)
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .clickable(enabled = canTransmit) {
                                                    store.beginScan(selected = candidate)
                                                    navigate("scan")
                                                }
                                                .padding(horizontal = 12.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            IconBubble(Icons.Filled.AcUnit, size = 42)
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    candidate.compactDisplayModelLabel().ifBlank { "Model chưa xác định" },
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppColors.navy,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    buildList {
                                                        candidate.remoteModel?.let { add("Remote " + it) }
                                                        sourceDisplayLabel(candidate).takeIf(String::isNotBlank)?.let(::add)
                                                        candidate.protocolId?.takeIf(String::isNotBlank)?.let(::add)
                                                        if (!canTransmit) add("Chưa hỗ trợ phát")
                                                    }.joinToString(" • ").ifBlank { "Profile điều khiển" },
                                                    color = AppColors.navySoft,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Icon(Icons.Filled.ArrowForward, null, tint = AppColors.navySoft)
                                        }
                                    }
                                    if (group.profiles.size > 8) {
                                        Text(
                                            "+" + (group.profiles.size - 8) + " profile khác",
                                            color = AppColors.navySoft,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val hasTransmittable = brandProfiles.any(store::canTransmit)
                    PrimaryButton(
                        "Dò tất cả mã " + selectedBrand,
                        Modifier.fillMaxWidth(),
                        Icons.Filled.PlayArrow,
                        enabled = hasTransmittable,
                    ) {
                        store.beginScan(RemoteQuery(brand = selectedBrand))
                        navigate("scan")
                    }
                }

                else -> {
                    SectionTitle("Hãng phổ biến")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(popular.take(12)) { brand ->
                            BrandChoiceChip(brand, false) { chooseBrand(brand) }
                        }
                    }

                    SectionTitle("Tất cả hãng A–Z")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(sections, key = BrandSection::letter) { section ->
                            Surface(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable { selectedLetter = section.letter },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selectedLetter == section.letter) AppColors.blue else Color.White,
                                border = BorderStroke(1.dp, if (selectedLetter == section.letter) AppColors.blue else AppColors.line),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        section.letter.toString(),
                                        color = if (selectedLetter == section.letter) Color.White else AppColors.navy,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                            }
                        }
                    }

                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                selectedLetter.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.blue,
                            )
                            visibleBrands.forEach { brand ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { chooseBrand(brand) }
                                        .padding(horizontal = 12.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    IconBubble(Icons.Filled.AcUnit, size = 42)
                                    Text(
                                        brand,
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.navy,
                                    )
                                    Icon(Icons.Filled.ArrowForward, null, tint = AppColors.navySoft)
                                }
                            }
                        }
                    }

                    SurfaceCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { navigate("import") }
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconBubble(Icons.Filled.FileDownload, tint = AppColors.purple, background = AppColors.palePurple)
                            Column(Modifier.weight(1f)) {
                                Text("Nhập file .ir", fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                                Text("Dùng file IR có sẵn trên điện thoại", color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Filled.ArrowForward, null, tint = AppColors.navySoft)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionScannerScreen(
    store: SavedRemotesViewModel,
    onTab: (String) -> Unit,
    onDone: () -> Unit,
    onChangeBrand: () -> Unit,
    onImport: () -> Unit,
) {
    val context = LocalContext.current
    val sessionStore = remember(context) { ScannerSessionStore(context) }
    val candidates by store.scanCandidates.collectAsState()
    val scannerBrands by store.scannerBrands.collectAsState()
    val candidateKey = candidates.joinToString("|") { it.id }
    val persistedSession = remember(candidateKey) {
        sessionStore.load(candidates.map(RemoteCandidate::id).toSet())
    }
    var refresh by remember { mutableIntStateOf(0) }
    var scanStarted by remember(candidateKey) { mutableStateOf(persistedSession != null && candidates.isNotEmpty()) }
    var pendingCheck by remember { mutableStateOf<VerificationCheck?>(null) }
    var machineName by remember { mutableStateOf("") }
    var entryBrand by remember(candidateKey) { mutableStateOf<String?>(null) }
    var brandFilter by remember(candidateKey) { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    @Suppress("UNUSED_VARIABLE") val stateRefresh = refresh

    val scanner = remember(candidateKey) {
        UniversalAcScanner(candidates) { candidate ->
            AndroidIrTransmitter.from(context).transmit(CatalogTransmitter.encodeSafeProbe(candidate))
        }.also { active ->
            persistedSession?.second?.let(active::restore)
        }
    }

    val current = scanner.selected ?: candidates.getOrNull(scanner.cursor)
    val controls = current?.let(RemoteControls::from)
    val requirements = current?.let { candidate ->
        controls?.verificationOrder().orEmpty().filter { CatalogTransmitter.verificationState(candidate, it) != null }
    }.orEmpty()
    val nextCheck = if (scanner.state == ScanState.VERIFYING) scanner.nextVerificationCheck() else null
    val safeProbe = current?.let { CatalogTransmitter.safeProbe(it) }

    AppScaffold("scan", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar(
                "Dò remote 1000-in-1",
                current?.brand ?: "Chọn hãng để bắt đầu",
                onBack = onDone,
            )

            if (candidates.isEmpty()) {
                val resumableBrand = sessionStore.load(emptySet())?.first
                val matchingBrands = scannerBrands.filter {
                    brandFilter.isBlank() || it.contains(brandFilter.trim(), ignoreCase = true)
                }
                val visibleBrands = matchingBrands.take(24)

                resumableBrand?.let { brand ->
                    InfoBanner(
                        "Có phiên dò $brand chưa hoàn tất.",
                        Icons.Filled.Refresh,
                        AppColors.blue,
                        AppColors.paleBlue,
                    )
                    PrimaryButton(
                        "Tiếp tục dò $brand",
                        Modifier.fillMaxWidth(),
                        Icons.Filled.PlayArrow,
                    ) {
                        store.beginScan(RemoteQuery(brand = brand))
                    }
                }

                SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconBubble(Icons.Outlined.Radio, size = 54)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Chọn hãng máy lạnh",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.navy,
                                )
                                Text(
                                    if (scannerBrands.isEmpty())
                                        "Thư viện mã điều khiển chưa sẵn sàng."
                                    else
                                        "${scannerBrands.size} hãng có mã điều khiển có thể phát.",
                                    color = AppColors.navySoft,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        if (scannerBrands.isEmpty()) {
                            InfoBanner(
                                "Thư viện hãng chưa sẵn sàng.",
                                Icons.Filled.Info,
                                AppColors.warning,
                                AppColors.paleWarning,
                            )
                        } else {
                            SearchField(
                                brandFilter,
                                { value ->
                                    brandFilter = value
                                    scannerBrands.firstOrNull { it.equals(value.trim(), true) }?.let {
                                        entryBrand = it
                                    }
                                },
                                "Tìm hãng, ví dụ: Daikin, Casper, LG…",
                            )

                            if (visibleBrands.isEmpty()) {
                                EmptyState(
                                    "Không tìm thấy hãng",
                                    "Thử nhập tên hãng khác.",
                                    Icons.Filled.Search,
                                )
                            } else {
                                visibleBrands.chunked(3).forEach { rowBrands ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        rowBrands.forEach { brand ->
                                            BrandTile(
                                                brand = brand,
                                                selected = entryBrand.equals(brand, true),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                entryBrand = brand
                                                brandFilter = brand
                                            }
                                        }
                                        repeat(3 - rowBrands.size) { Box(Modifier.weight(1f)) }
                                    }
                                }
                                if (matchingBrands.size > visibleBrands.size) {
                                    Text(
                                        "Đang hiển thị ${visibleBrands.size}/${matchingBrands.size} hãng. Nhập tên hãng để lọc nhanh.",
                                        color = AppColors.navySoft,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }

                        entryBrand?.let { brand ->
                            PrimaryButton(
                                "Bắt đầu dò $brand",
                                Modifier.fillMaxWidth(),
                                Icons.Filled.PlayArrow,
                            ) {
                                sessionStore.clear()
                                store.beginScan(RemoteQuery(brand = brand))
                            }
                        }
                    }
                }

                SecondaryButton(
                    "Nhập file .ir",
                    Modifier.fillMaxWidth(),
                    Icons.Filled.FileDownload,
                    onClick = onImport,
                )
                return@PageColumn
            }

            ScannerBrandHero(
                brand = current?.brand.orEmpty(),
                model = current?.compactDisplayModelLabel().orEmpty(),
                onChangeBrand = {
                    sessionStore.clear()
                    onChangeBrand()
                },
            )

            if (!scanStarted && scanner.state == ScanState.READY) {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Chuẩn bị trước khi dò", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        ScannerInstruction("1", "Bật máy lạnh bằng remote gốc hoặc nút trên máy.")
                        ScannerInstruction("2", "Hướng đầu phát IR của điện thoại về máy lạnh.")
                        ScannerInstruction("3", "Mỗi lần app chỉ thử một mã điều khiển và chờ bạn xác nhận.")
                        InfoBanner(
                            "Lần thử đầu tiên ưu tiên bật máy và không gửi lệnh tắt để quá trình dò không bị gián đoạn.",
                            Icons.Filled.Info,
                            AppColors.blue,
                            AppColors.paleBlue,
                        )
                        PrimaryButton("Bắt đầu dò", Modifier.fillMaxWidth(), Icons.Filled.PlayArrow) {
                            scanStarted = true
                            sessionStore.save(current?.brand, scanner.snapshot())
                        }
                    }
                }
            }

            if (scanStarted) {
                val progress = ((scanner.cursor + 1).toFloat() / candidates.size).coerceIn(0f, 1f)
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Mã ${scanner.cursor + 1} / ${candidates.size}",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.navy,
                            )
                            Text("${(progress * 100).toInt()}%", color = AppColors.blue, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = AppColors.blue,
                            trackColor = AppColors.paleBlueStrong,
                        )
                    }
                }

                if (scanner.state == ScanState.READY && current != null) {
                    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                AcWallUnitArt(current.brand, Modifier.width(98.dp).height(66.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        current.brand,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppColors.navy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        current.compactDisplayModelLabel().ifBlank { "Model chưa xác định" },
                                        color = AppColors.navySoft,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            safeProbe?.warning?.let {
                                InfoBanner(it, Icons.Filled.Info, AppColors.warning, AppColors.paleWarning)
                            }
                            PrimaryButton(
                                safeProbe?.description ?: "Phát thử",
                                Modifier.fillMaxWidth(),
                                Icons.Filled.PlayArrow,
                            ) {
                                message = runCatching {
                                    scanner.tryCurrent(System.currentTimeMillis())
                                    "Đã phát tín hiệu. Chờ 2–3 giây rồi xác nhận phản ứng của máy."
                                }.getOrElse {
                                    "Không thể phát tín hiệu này. Hãy kiểm tra IR hoặc thử mã khác."
                                }
                                refresh++
                            }
                        }
                    }
                }

                if (scanner.state == ScanState.AWAITING_FEEDBACK) {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Máy lạnh có phản ứng không?", fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                            Text("Beep, bật máy hoặc thay đổi trạng thái đều được xem là có phản ứng.", color = AppColors.navySoft)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PrimaryButton("Có phản ứng", Modifier.weight(1f), Icons.Filled.Check) {
                                    scanner.reportReaction()
                                    sessionStore.save(current?.brand, scanner.snapshot())
                                    pendingCheck = null
                                    message = "Đã giữ mã này. Tiếp tục kiểm tra từng chức năng."
                                    refresh++
                                }
                                SecondaryButton("Không", Modifier.weight(1f), Icons.Filled.Close) {
                                    scanner.reportNoReaction()
                                    if (scanner.state == ScanState.COMPLETE) {
                                        sessionStore.clear()
                                    } else {
                                        sessionStore.save(current?.brand, scanner.snapshot())
                                    }
                                    message = if (scanner.state == ScanState.COMPLETE) "Đã thử hết mã điều khiển." else "Chuyển sang mã tiếp theo."
                                    refresh++
                                }
                            }
                        }
                    }
                }

                if (scanner.state == ScanState.VERIFYING && current != null) {
                    if (nextCheck != null) {
                        VerificationWizardCard(
                            check = nextCheck,
                            step = requirements.indexOf(nextCheck) + 1,
                            total = requirements.size,
                            waitingForAnswer = pendingCheck == nextCheck,
                            onSend = {
                                val selected = scanner.selected ?: return@VerificationWizardCard
                                message = runCatching {
                                    AndroidIrTransmitter.from(context).transmit(
                                        CatalogTransmitter.encode(
                                            selected,
                                            CatalogTransmitter.verificationState(selected, nextCheck)
                                                ?: error("No encodable verification state."),
                                        )
                                    )
                                    pendingCheck = nextCheck
                                    "Đã gửi lệnh kiểm tra. Hãy quan sát máy lạnh."
                                }.getOrElse {
                                    "Không thể phát lệnh kiểm tra này."
                                }
                                refresh++
                            },
                            onPass = {
                                val selected = scanner.selected
                                scanner.recordVerification(nextCheck, supported = true)
                                sessionStore.save(current?.brand, scanner.snapshot())
                                pendingCheck = null
                                if (nextCheck == VerificationCheck.POWER && selected != null) {
                                    val restored = runCatching {
                                        AndroidIrTransmitter.from(context).transmit(CatalogTransmitter.encodeSafeProbe(selected))
                                    }.isSuccess
                                    message = if (restored) {
                                        "Đã xác nhận nguồn. App đã gửi lệnh bật lại máy."
                                    } else {
                                        "Đã xác nhận nguồn nhưng không thể gửi lệnh bật lại máy."
                                    }
                                }
                                refresh++
                            },
                            onFail = {
                                scanner.recordVerification(nextCheck, supported = false)
                                sessionStore.save(current?.brand, scanner.snapshot())
                                pendingCheck = null
                                refresh++
                            },
                            onSkip = {
                                scanner.skipVerification(nextCheck)
                                sessionStore.save(current?.brand, scanner.snapshot())
                                pendingCheck = null
                                refresh++
                            },
                        )
                    } else {
                        PrimaryButton("Xem kết quả", Modifier.fillMaxWidth(), Icons.Filled.CheckCircle) {
                            scanner.finishVerification()
                            sessionStore.save(current?.brand, scanner.snapshot())
                            refresh++
                        }
                    }
                }

                if (scanner.state == ScanState.COMPLETE) {
                    val scanResult = scanner.result
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val title = when (scanResult) {
                                ScanResult.FULL_MATCH -> "Mã phù hợp"
                                ScanResult.PARTIAL_MATCH -> "Mã hoạt động một phần"
                                else -> "Chưa tìm thấy remote phù hợp"
                            }
                            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            when (scanResult) {
                                ScanResult.FULL_MATCH, ScanResult.PARTIAL_MATCH -> {
                                    OutlinedField("Tên máy lạnh", machineName, { machineName = it })
                                    PrimaryButton(
                                        "Lưu remote",
                                        Modifier.fillMaxWidth(),
                                        Icons.Filled.CheckCircle,
                                        enabled = machineName.isNotBlank() && scanner.selected != null,
                                    ) {
                                        val candidate = scanner.selected ?: return@PrimaryButton
                                        store.save(
                                            SavedRemote(
                                                id = UUID.randomUUID().toString(),
                                                displayName = machineName.trim(),
                                                catalogProfileId = candidate.id,
                                                brand = candidate.brand,
                                                acModel = candidate.acModel,
                                                remoteModel = candidate.remoteModel,
                                                protocolId = candidate.protocolId,
                                                protocolModel = candidate.protocolModel,
                                                verifiedCapabilities = scanner.verifiedCapabilities.map { it.name },
                                            )
                                        )
                                        sessionStore.clear()
                                        onDone()
                                    }
                                    if (scanner.cursor + 1 < candidates.size) {
                                        SecondaryButton("Thử mã khác", Modifier.fillMaxWidth(), Icons.Filled.Refresh) {
                                            scanner.continueAfterResult()
                                            sessionStore.save(current?.brand, scanner.snapshot())
                                            pendingCheck = null
                                            message = ""
                                            refresh++
                                        }
                                    }
                                }
                                else -> {
                                    SecondaryButton("Chọn hãng khác", Modifier.fillMaxWidth(), Icons.Filled.Refresh) {
                                        sessionStore.clear()
                                        onChangeBrand()
                                    }
                                }
                            }
                        }
                    }
                }

                if (message.isNotBlank()) {
                    InfoBanner(
                        message,
                        if (message.startsWith("Không")) Icons.Filled.ErrorOutline else Icons.Filled.Info,
                        if (message.startsWith("Không")) AppColors.danger else AppColors.blue,
                        if (message.startsWith("Không")) AppColors.paleDanger else AppColors.paleBlue,
                    )
                }

                if (scanner.state != ScanState.COMPLETE && scanner.state != ScanState.STOPPED) {
                    TextButton(
                        onClick = {
                            scanner.stop()
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Stop, null, tint = AppColors.danger)
                        Text("  Dừng dò", color = AppColors.danger)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionSettingsScreen(
    diagnostics: IrHardwareDiagnostics,
    catalog: CatalogUiState,
    store: SavedRemotesViewModel,
    onTab: (String) -> Unit,
    openDiagnostics: () -> Unit,
    openImport: () -> Unit,
) {
    AppScaffold("settings", onTab) { padding ->
        PageColumn(padding) {
            SettingsHero(diagnostics)

            SectionTitle("Ứng dụng")
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    SourceInfoRow("Phiên bản ứng dụng", BuildConfig.VERSION_NAME, Icons.Filled.Build)
                    SourceInfoRow(
                        "Thư viện điều khiển",
                        if (catalog.loading) "Đang tải…" else "${catalog.profileCount} mã điều khiển",
                        Icons.Filled.FilterAlt,
                    )
                    SourceInfoRow(
                        "Bộ phát IR",
                        if (diagnostics.hasIrEmitter) "Sẵn sàng" else "Không khả dụng",
                        Icons.Filled.SignalCellularAlt,
                        if (diagnostics.hasIrEmitter) AppColors.mint else AppColors.danger,
                    )
                }
            }

            SectionTitle("Cập nhật")
            SurfaceCard(
                Modifier.fillMaxWidth(),
                Brush.linearGradient(listOf(Color(0xFFE8F5FF), Color(0xFFF4FAFF))),
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        IconBubble(Icons.Filled.CloudDownload, size = 62, background = Color.White, tint = AppColors.blue)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Cập nhật ứng dụng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text("Kiểm tra bản mới và thư viện IR đi kèm.", color = AppColors.navySoft)
                        }
                    }
                    UpdatePanel()
                }
            }

            SectionTitle("Sao lưu dữ liệu")
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Remote, phòng, yêu thích và trạng thái lần cuối đã gửi",
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.navy,
                    )
                    Text(
                        "Backup được lưu thành file JSON trên thiết bị. Khôi phục sẽ thay thế danh sách remote hiện tại.",
                        color = AppColors.navySoft,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    BackupPanel(store)
                }
            }

            SectionTitle("Công cụ")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToolCard(
                    title = "Phần cứng IR",
                    subtitle = "Kiểm tra thiết bị",
                    icon = Icons.Filled.SignalCellularAlt,
                    tint = AppColors.mint,
                    background = AppColors.paleMint,
                    modifier = Modifier.weight(1f),
                    onClick = openDiagnostics,
                )
                SettingsToolCard(
                    title = "Nhập file .ir",
                    subtitle = "Dùng file có sẵn",
                    icon = Icons.Filled.FileDownload,
                    tint = AppColors.purple,
                    background = Color(0xFFF4F0FF),
                    modifier = Modifier.weight(1f),
                    onClick = openImport,
                )
                SettingsToolCard(
                    title = "Dò remote",
                    subtitle = "1000-in-1",
                    icon = Icons.Outlined.Radio,
                    tint = AppColors.blue,
                    background = AppColors.paleBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onTab("scan") },
                )
            }

            SectionTitle("Thông tin nguồn")
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    ProductionSourceRow("IRremoteESP8266", "Thư viện mã điều khiển máy lạnh")
                    ProductionSourceRow("SmartIR", "Dữ liệu điều khiển cộng đồng")
                    ProductionSourceRow("Flipper IRDB", "Nguồn file IR tương thích")
                    ProductionSourceRow("irplus", "Dữ liệu tương thích bổ sung")
                }
            }
        }
    }
}

@Composable
fun AcWallUnitArt(brand: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF7FBFF),
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 9.dp)
            ) {
                val bodyTop = size.height * 0.14f
                val bodyHeight = size.height * 0.50f
                val bodyLeft = size.width * 0.08f
                val bodyWidth = size.width * 0.84f

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx()),
                )
                drawRoundRect(
                    color = Color(0xFFE8F0F7),
                    topLeft = Offset(bodyLeft + bodyWidth * 0.06f, bodyTop + bodyHeight * 0.66f),
                    size = Size(bodyWidth * 0.88f, bodyHeight * 0.15f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
                drawLine(
                    color = Color(0xFF9CB3C8),
                    start = Offset(bodyLeft + bodyWidth * 0.12f, bodyTop + bodyHeight * 0.86f),
                    end = Offset(bodyLeft + bodyWidth * 0.88f, bodyTop + bodyHeight * 0.86f),
                    strokeWidth = 1.dp.toPx(),
                )

                val airflowStartY = bodyTop + bodyHeight * 1.02f
                val airflowEndY = size.height * 0.96f
                listOf(0.28f, 0.43f, 0.58f, 0.73f).forEachIndexed { index, fraction ->
                    val x = bodyLeft + bodyWidth * fraction
                    drawLine(
                        color = AppColors.cyan.copy(alpha = 0.46f - index * 0.06f),
                        start = Offset(x, airflowStartY),
                        end = Offset(x + size.width * 0.055f, airflowEndY),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }

            Text(
                brand.ifBlank { "A/C" },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 17.dp, top = 13.dp),
                color = AppColors.blue,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SetupFlowHeader(
    number: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    background: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = background,
            border = BorderStroke(1.dp, tint.copy(alpha = 0.18f)),
        ) {
            Text(
                number,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = tint,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        IconBubble(icon, tint = tint, background = background, size = 48)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
            Text(subtitle, color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BrandTile(
    brand: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AppColors.paleBlue else Color.White,
        border = BorderStroke(1.dp, if (selected) AppColors.blue else AppColors.line),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.AcUnit,
                null,
                tint = AppColors.blue,
                modifier = Modifier.size(24.dp),
            )
            Text(
                brand,
                modifier = Modifier.padding(top = 6.dp),
                color = AppColors.navy,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BrandChoiceChip(brand: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AppColors.blue else Color.White,
        border = BorderStroke(1.dp, if (selected) AppColors.blue else AppColors.line),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                Icons.Filled.AcUnit,
                null,
                tint = if (selected) Color.White else AppColors.blue,
                modifier = Modifier.size(18.dp),
            )
            Text(
                brand,
                color = if (selected) Color.White else AppColors.navy,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun UserProfileCard(candidate: RemoteCandidate, enabled: Boolean = true, onClick: () -> Unit) {
    SurfaceCard(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(Icons.Filled.AcUnit)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    candidate.brand,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.navy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    candidate.compactDisplayModelLabel().ifBlank { "Model chưa xác định" },
                    color = AppColors.navySoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildList {
                        candidate.remoteModel?.takeIf(String::isNotBlank)?.let { add("Remote $it") }
                        sourceDisplayLabel(candidate).takeIf(String::isNotBlank)?.let(::add)
                        candidate.protocolId?.takeIf(String::isNotBlank)?.let { add(it) }
                    }.joinToString(" • "),
                    color = AppColors.navySoft,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(
                    when {
                        enabled && candidate.source.equals("irremoteesp8266", true) -> "IRremoteESP8266 · Sẵn sàng thử"
                        enabled -> "Sẵn sàng thử"
                        candidate.source.equals("irremoteesp8266", true) -> "IRremoteESP8266 · Chưa hỗ trợ phát"
                        else -> "Chưa hỗ trợ phát"
                    },
                    if (enabled) Icons.Filled.SignalCellularAlt else Icons.Filled.Info,
                )
            }
            Icon(Icons.Filled.ArrowForward, null, tint = AppColors.navySoft)
        }
    }
}

@Composable
private fun ScannerBrandHero(brand: String, model: String, onChangeBrand: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AcWallUnitArt(brand, Modifier.width(98.dp).height(66.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        brand.ifBlank { "Máy lạnh" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.navy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        model.ifBlank { "Không rõ model" },
                        color = AppColors.navySoft,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(
                onClick = onChangeBrand,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Đổi hãng")
            }
        }
    }
}

@Composable
private fun ScannerInstruction(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(shape = RoundedCornerShape(999.dp), color = AppColors.paleBlue) {
            Text(
                number,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                color = AppColors.blue,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(text, modifier = Modifier.weight(1f), color = AppColors.navy)
    }
}

@Composable
private fun VerificationWizardCard(
    check: VerificationCheck,
    step: Int,
    total: Int,
    waitingForAnswer: Boolean,
    onSend: () -> Unit,
    onPass: () -> Unit,
    onFail: () -> Unit,
    onSkip: () -> Unit,
) {
    val (title, instruction, action) = when (check) {
        VerificationCheck.TEMPERATURE_CHANGED -> Triple("Nhiệt độ", "Quan sát máy có nhận thay đổi nhiệt độ không.", "Gửi thử nhiệt độ")
        VerificationCheck.MODE -> Triple("Chế độ", "Quan sát máy có đổi chế độ hoạt động không.", "Gửi thử chế độ")
        VerificationCheck.FAN -> Triple("Quạt", "Quan sát tốc độ quạt có thay đổi không.", "Gửi thử quạt")
        VerificationCheck.SWING_VERTICAL -> Triple("Đảo gió dọc", "Quan sát cánh gió lên/xuống.", "Gửi thử đảo gió dọc")
        VerificationCheck.SWING_HORIZONTAL -> Triple("Đảo gió ngang", "Quan sát cánh gió trái/phải.", "Gửi thử đảo gió ngang")
        VerificationCheck.POWER -> Triple("Bật/Tắt nguồn", "Nguồn được kiểm tra cuối để không làm gián đoạn các bước trước.", "Thử tắt máy")
    }

    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusChip("Bước $step / $total", Icons.Filled.CheckCircle)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(instruction, color = AppColors.navySoft)
            PrimaryButton(action, Modifier.fillMaxWidth(), Icons.Filled.Send, enabled = !waitingForAnswer, onClick = onSend)
            if (waitingForAnswer) {
                Text("Kết quả trên máy lạnh?", fontWeight = FontWeight.Bold, color = AppColors.navy)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Đúng", Modifier.weight(1f), Icons.Filled.Check, onClick = onPass)
                    SecondaryButton("Không đúng", Modifier.weight(1f), Icons.Filled.Close, onClick = onFail)
                }
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Bỏ qua bước này") }
            }
        }
    }
}

@Composable
private fun SettingsHero(diagnostics: IrHardwareDiagnostics) {
    SurfaceCard(
        Modifier.fillMaxWidth(),
        Brush.linearGradient(listOf(Color(0xFFE8F5FF), Color.White, Color(0xFFF7FBFF))),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBubble(
                    Icons.Filled.Settings,
                    size = 62,
                    background = Color(0xFF1687EE),
                    tint = Color.White,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "Cài đặt",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.navy,
                    )
                    Text(
                        "Universal A/C Remote",
                        color = AppColors.navy,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Thiết bị, dữ liệu và cập nhật",
                        color = AppColors.navySoft,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                AcWallUnitArt("A/C", Modifier.width(118.dp).height(72.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(
                    "v${BuildConfig.VERSION_NAME}",
                    Icons.Filled.Build,
                    AppColors.blue,
                    AppColors.paleBlue,
                )
                StatusChip(
                    if (diagnostics.hasIrEmitter) "IR sẵn sàng" else "IR chưa sẵn sàng",
                    Icons.Filled.SignalCellularAlt,
                    if (diagnostics.hasIrEmitter) AppColors.mint else AppColors.danger,
                    if (diagnostics.hasIrEmitter) AppColors.paleMint else AppColors.paleDanger,
                )
            }
        }
    }
}

@Composable
private fun SettingsToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(142.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = background,
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconBubble(icon, tint = tint, background = Color.White, size = 46)
            Text(
                title,
                modifier = Modifier.padding(top = 9.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.navy,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                subtitle,
                textAlign = TextAlign.Center,
                color = AppColors.navySoft,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ProductionSourceRow(name: String, detail: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBubble(Icons.Filled.FilterAlt, size = 38)
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, color = AppColors.navy)
            Text(detail, color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall)
        }
    }
}
