package com.thanhnha.universalacremote.ir

import org.json.JSONObject

data class RemoteQuery(val brand: String? = null, val acModel: String? = null, val remoteModel: String? = null)
data class SwingCapability(val type: String = "NONE", val positions: List<String> = emptyList())
data class RemoteCandidate(
    val id: String, val brand: String, val acModel: String?, val remoteModel: String?,
    val protocolId: String?, val protocolModel: String?, val encodingType: String,
    val capabilities: Set<String>, val evidence: List<String>, val priority: Int,
    val aliases: List<String> = emptyList(),
    val minimumTemperatureCelsius: Int? = null,
    val maximumTemperatureCelsius: Int? = null,
    val operationModes: List<String> = emptyList(),
    val fanModes: List<String> = emptyList(),
    val verticalSwing: SwingCapability = SwingCapability(),
    val horizontalSwing: SwingCapability = SwingCapability(),
    val specialCapabilities: List<String> = emptyList(),
    val source: String? = null,
    val sourceCommitSha: String? = null,
    val sourcePath: String? = null,
    val sourceProfileId: String? = null,
    val verificationStatus: String? = null,
    val rawCommandsJson: String? = null,
    val sourceMetadataJson: String? = null,
)

/** Read-only view of the generated Unified IR Catalog. */
class RemoteResolver(private val candidates: List<RemoteCandidate>) {
    val profileCount: Int get() = candidates.size

    fun popularBrands(limit: Int = 8): List<String> = candidates.asSequence()
        .filter { it.brand.isNotBlank() }.groupingBy { it.brand }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(limit).map { it.key }

    fun allBrands(): List<String> = candidates.asSequence()
        .map { it.brand.trim() }
        .filter(String::isNotBlank)
        .distinctBy(::normalizeSearchText)
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .toList()

    fun brandCandidates(brand: String): List<RemoteCandidate> =
        resolve(RemoteQuery(brand = brand))

    fun scannerBrands(canTransmit: (RemoteCandidate) -> Boolean): List<String> =
        candidates.asSequence()
            .filter(canTransmit)
            .map { it.brand.trim() }
            .filter(String::isNotBlank)
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .distinctBy(::normalizeSearchText)
            .toList()

    fun findById(id: String): RemoteCandidate? = candidates.firstOrNull { it.id == id }

    fun scannerCandidates(query: RemoteQuery = RemoteQuery(), canTransmit: (RemoteCandidate) -> Boolean): List<RemoteCandidate> =
        resolve(query).filter(canTransmit)

    fun resolve(query: RemoteQuery): List<RemoteCandidate> = candidates.mapNotNull { candidate ->
        val reasons = mutableListOf<String>()
        val remoteScore = query.remoteModel.matchScore(candidate.remoteModel)
        val acScore = query.acModel.matchScore(candidate.acModel)
        val brandScore = ((listOf(candidate.brand) + candidate.aliases).mapNotNull { query.brand.matchScore(it) } +
            candidate.evidence.filter { it.startsWith("oem:", true) }.mapNotNull { query.brand.matchScore(it.substringAfter('=')) }).minOrNull()
        val remoteExact = remoteScore == 0
        val acExact = acScore == 0
        val sameBrand = brandScore == 0
        val brandMatch = brandScore != null
        val acMatch = acScore != null
        val remoteMatch = remoteScore != null
        if (query.brand != null && brandMatch) reasons += if (sameBrand) "Same brand match: ${candidate.brand}" else "Brand alias match"
        if (query.acModel != null && acMatch) reasons += "A/C model match: ${candidate.acModel}"
        if (query.remoteModel != null && remoteMatch) reasons += "Remote model match: ${candidate.remoteModel}"
        if (remoteExact) reasons += "Exact remote model match: ${candidate.remoteModel}"
        if (acExact) reasons += "Exact A/C model match: ${candidate.acModel}"
        if (candidate.id.startsWith("known-compatible:") && (brandMatch || remoteMatch || acMatch)) reasons += "Catalog marks profile as known-compatible"
        if (!candidate.protocolId.isNullOrBlank() && query.remoteModel != null && candidate.protocolModel?.containsNormalized(query.remoteModel) == true) reasons += "Same protocol/model family: ${candidate.protocolId}/${candidate.protocolModel}"
        val oem = candidate.evidence.filter { it.startsWith("oem:", true) }
        if (oem.isNotEmpty() && (brandMatch || remoteMatch || acMatch)) reasons += oem
        val hasQuery = listOf(query.brand, query.acModel, query.remoteModel).any { !it.isNullOrBlank() }
        if ((query.brand.isNullOrBlank().not() && !brandMatch) ||
            (query.acModel.isNullOrBlank().not() && !acMatch) ||
            (query.remoteModel.isNullOrBlank().not() && !remoteMatch)) return@mapNotNull null
        if (reasons.isEmpty() && !hasQuery) reasons += "Catalog profile"
        if (reasons.isEmpty()) return@mapNotNull null
        val priority = when {
            remoteExact -> 0
            remoteScore == 1 -> 1
            acExact -> 2
            acScore == 1 -> 3
            reasons.any { it.startsWith("Catalog marks") } -> 4
            oem.isNotEmpty() -> 4
            reasons.any { it.startsWith("Same protocol") } -> 5
            sameBrand -> 7
            brandMatch -> 8
            else -> 9
        }
        candidate.copy(evidence = reasons.distinct(), priority = priority)
        }.sortedWith(compareBy<RemoteCandidate> { if (it.isTransmittableByCatalogStatus()) 0 else 1 }
            .thenBy { it.priority }
            .thenBy { if (it.source.equals("irremoteesp8266", true)) 0 else 1 }
            .thenBy { it.id })

    companion object {
        fun fromCatalog(json: String): RemoteResolver = fromCatalogDocument(json)

        fun fromCatalogIndex(json: String): RemoteResolver = fromCatalogDocument(json)

        private fun fromCatalogDocument(json: String): RemoteResolver {
            val profiles = JSONObject(json).getJSONArray("profiles")
            val parsed = (0 until profiles.length()).map { i ->
                val p = profiles.getJSONObject(i)
                val aliases = p.optJSONArray("aliases")
                val metadata = p.optJSONObject("sourceMetadata")
                val evidence = buildList {
                    if (metadata != null) {
                        val keys = metadata.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            if (key.contains("oem", true) || key.contains("rebrand", true) || key.contains("compatible", true)) add("oem:$key=${metadata.opt(key)}")
                        }
                    }
                }
                val vertical = p.swing("verticalSwingCapabilities")
                val horizontal = p.swing("horizontalSwingCapabilities")
                RemoteCandidate(
                    id = p.getString("id"), brand = p.optString("brand"), acModel = p.nullable("acModel"),
                    remoteModel = p.nullable("remoteModel"), protocolId = p.nullable("protocolId"),
                    protocolModel = p.nullable("protocolModel"), encodingType = p.optString("encodingType"),
                    capabilities = p.getJSONArray("capabilities").strings().toMutableSet(),
                    evidence = evidence, priority = 9,
                    aliases = aliases?.strings().orEmpty(),
                    minimumTemperatureCelsius = p.optJSONObject("temperatureRange")?.nullableInt("minC"),
                    maximumTemperatureCelsius = p.optJSONObject("temperatureRange")?.nullableInt("maxC"),
                    operationModes = p.optJSONArray("operationModes")?.strings().orEmpty(),
                    fanModes = p.optJSONArray("fanModes")?.strings().orEmpty(),
                    verticalSwing = vertical, horizontalSwing = horizontal,
                    specialCapabilities = p.optJSONArray("specialCapabilities")?.strings().orEmpty(),
                    source = p.nullable("source"), sourceCommitSha = p.nullable("sourceCommitSha"), sourcePath = p.nullable("sourcePath"),
                    sourceProfileId = p.nullable("sourceProfileId"), verificationStatus = p.nullable("verificationStatus"),
                    rawCommandsJson = p.optJSONObject("rawCommands")?.toString(),
                    sourceMetadataJson = metadata?.toString(),
                )
            }
            return RemoteResolver(parsed)
        }

        private fun JSONObject.nullable(key: String): String? = if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
        private fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else optDouble(key).takeIf { it.isFinite() }?.toInt()
        private fun JSONObject.swing(key: String): SwingCapability = optJSONObject(key)?.let { obj ->
            SwingCapability(obj.optString("type", "NONE"), obj.optJSONArray("positions")?.strings().orEmpty())
        } ?: SwingCapability()
        private fun org.json.JSONArray.strings(): List<String> = (0 until length()).map { optString(it) }.filter(String::isNotBlank)
        private fun String?.matchScore(value: String?): Int? {
            if (this.isNullOrBlank() || value.isNullOrBlank()) return null
            val query = normalizeSearchText(this)
            val target = normalizeSearchText(value)
            if (query.isEmpty() || target.isEmpty() || !target.contains(query)) return null
            return if (query == target) 0 else 1
        }
        private fun String.containsNormalized(query: String): Boolean = normalizeSearchText(this).contains(normalizeSearchText(query))
    }
}

fun RemoteCandidate.isTransmittableByCatalogStatus(): Boolean =
    encodingType.equals("PROTOCOL", true) || verificationStatus.equals("transmittable", true)

fun RemoteCandidate.displayModelLabel(): String = when {
    !acModel.isNullOrBlank() && !acModel.equals("Unknown", true) -> acModel
    !remoteModel.isNullOrBlank() -> "Remote $remoteModel"
    source.equals("smartir", true) -> "Model chưa xác định · SmartIR #${sourceProfileId ?: id.substringAfterLast(':')}"
    source.equals("irremoteesp8266", true) -> protocolId?.let { "Protocol $it" }.orEmpty()
    else -> acModel.orEmpty()
}

fun RemoteCandidate.compactDisplayModelLabel(maxModels: Int = 2): String {
    val label = displayModelLabel()
    val models = label.split(',').map { it.trim() }.filter { it.isNotBlank() }
    if (models.size <= maxModels) return label
    return models.take(maxModels).joinToString(", ") + "  +${models.size - maxModels} model"
}

fun normalizeSearchText(value: String): String = value.trim().lowercase().filter(Char::isLetterOrDigit)
