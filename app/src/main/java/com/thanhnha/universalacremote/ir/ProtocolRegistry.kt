package com.thanhnha.universalacremote.ir

/** Stable app IDs and reviewed metadata for the pinned upstream build. */
object ProtocolRegistry {
    private val commonModes = AcMode.entries.toSet()
    private val commonFans = AcFan.entries.toSet()
    val protocols: Map<String, ProtocolDefinition> = listOf(
        ProtocolDefinition("lg", "LG", "LG", "LG", "GE6711AR2853M", setOf("GE6711AR2853M"), 16, 30, commonModes, commonFans, true, true),
        ProtocolDefinition("gree", "GREE", "GREE", "Gree", "YAW1F", setOf("YAW1F", "YBOFB", "YX1FSF"), 16, 30, commonModes, commonFans, true, true),
        ProtocolDefinition("panasonic_ac", "PANASONIC_AC", "PANASONIC_AC", "Panasonic", "NKE", setOf("LKE", "NKE", "DKE", "JKE", "CKP", "RKR"), 16, 30, commonModes, commonFans, true, true),
        ProtocolDefinition("daikin", "DAIKIN", "DAIKIN", "Daikin", null, emptySet(), 10, 32, commonModes, commonFans, true, true),
        ProtocolDefinition("fujitsu_ac", "FUJITSU_AC", "FUJITSU_AC", "Fujitsu", "ARRAH2E", setOf("ARDB1", "ARJW2", "ARRAH2E", "ARREB1E", "ARREW4E", "ARRY4"), 16, 30, commonModes, commonFans, true, true),
        ProtocolDefinition("midea", "MIDEA", "MIDEA", "Midea", null, emptySet(), 17, 30, commonModes, commonFans, true, false),
        ProtocolDefinition("mitsubishi_ac", "MITSUBISHI_AC", "MITSUBISHI_AC", "Mitsubishi Electric", null, emptySet(), 16, 31, commonModes, commonFans, true, true),
        ProtocolDefinition("samsung_ac", "SAMSUNG_AC", "SAMSUNG_AC", "Samsung", null, emptySet(), 16, 30, commonModes, commonFans, true, true),
    ).associateBy(ProtocolDefinition::id)

    fun requireSupported(id: String, state: AcState): ProtocolDefinition {
        val definition = protocols[id] ?: throw IllegalArgumentException("Unsupported protocol ID: $id")
        require(state.temperatureCelsius in definition.minTemperatureCelsius..definition.maxTemperatureCelsius) {
            "$id temperature must be between ${definition.minTemperatureCelsius} and ${definition.maxTemperatureCelsius} Celsius."
        }
        require(state.mode in definition.modes) { "$id does not support ${state.mode} mode." }
        require(state.fan in definition.fanSpeeds) { "$id does not support ${state.fan} fan speed." }
        require(!state.swingVertical || definition.verticalSwing) { "$id does not support vertical swing." }
        require(!state.swingHorizontal || definition.horizontalSwing) { "$id does not support horizontal swing." }
        return definition
    }

    fun modelId(definition: ProtocolDefinition, model: String?): String? {
        val selected = model ?: definition.defaultModel
        require(selected == null || selected in definition.modelIds) {
            "Unsupported ${definition.id} model/variant: $selected"
        }
        return selected
    }
}

data class ProtocolDefinition(
    val id: String,
    val upstreamProtocol: String,
    val protocolName: String,
    val manufacturer: String,
    val defaultModel: String?,
    val modelIds: Set<String>,
    val minTemperatureCelsius: Int,
    val maxTemperatureCelsius: Int,
    val modes: Set<AcMode>,
    val fanSpeeds: Set<AcFan>,
    val verticalSwing: Boolean,
    val horizontalSwing: Boolean,
    val detailedAcSupport: Boolean = true,
    /** Carrier is sampled from upstream IRsend::enableIROut for each encode. */
    val carrierFrequencyHz: Int? = null,
) {
    val capabilities: Set<String>
        get() = buildSet {
            add("power")
            addAll(modes.map { "mode:${it.name.lowercase()}" })
            addAll(fanSpeeds.map { "fan:${it.name.lowercase()}" })
            if (verticalSwing) add("swing:vertical")
            if (horizontalSwing) add("swing:horizontal")
        }
}
