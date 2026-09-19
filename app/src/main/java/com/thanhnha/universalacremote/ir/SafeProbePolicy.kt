package com.thanhnha.universalacremote.ir

/** User-facing description and state for the first, non-destructive scanner probe. */
data class SafeProbe(
    val state: AcState,
    val description: String,
    val warning: String? = null,
    val transmission: IrTransmission? = null,
)

/**
 * Generic scanner policy. A first probe must leave the A/C on whenever the
 * profile can express an on/stateful command. The policy is deliberately
 * independent of a brand or model so scanner behavior stays consistent.
 */
object SafeProbePolicy {
    fun forCandidate(candidate: RemoteCandidate): SafeProbe? {
        val controls = RemoteControls.from(candidate)
        val definition = CatalogTransmitter.protocol(candidate)
        if (candidate.encodingType.equals("RAW_PROFILE", true) && CatalogTransmitter.explicitPowerOn(candidate) != null) {
            return SafeProbe(
                state = AcState(true, 24, AcMode.COOL, AcFan.AUTO),
                description = "Thử bật máy",
            )
        }
        val mode = preferredMode(controls, definition)
        val fan = preferredFan(controls, definition)
        val range = controls.temperatureRange
            ?: definition?.let { it.minTemperatureCelsius..it.maxTemperatureCelsius }
            ?: return null
        val temperature = range.clamp(24)
        val state = AcState(
            power = true,
            temperatureCelsius = temperature,
            mode = mode ?: return null,
            fan = fan ?: return null,
        )
        val description = if (mode == AcMode.COOL) "Thử làm lạnh ${temperature}°C" else "Thử bật máy"
        val warning = if (candidate.encodingType.equals("RAW_PROFILE", true) &&
            hasToggleOnlyRawProfile(candidate)) {
            "Hồ sơ này chỉ có lệnh bật/tắt. Máy có thể đổi trạng thái khi thử."
        } else null
        return SafeProbe(state, description, warning)
    }

    private fun preferredMode(controls: RemoteControls, definition: ProtocolDefinition?): AcMode? =
        listOf(AcMode.COOL, AcMode.AUTO, AcMode.DRY, AcMode.FAN, AcMode.HEAT)
            .firstOrNull { mode ->
                val supportedByControls = controls.modes.any { CatalogTransmitter.mode(it) == mode }
                val supportedByProtocol = definition?.modes?.contains(mode) ?: true
                supportedByControls && supportedByProtocol
            }
            ?: definition?.modes?.firstOrNull()

    private fun preferredFan(controls: RemoteControls, definition: ProtocolDefinition?): AcFan? =
        listOf(AcFan.AUTO, AcFan.MIN, AcFan.MEDIUM, AcFan.HIGH)
            .firstOrNull { fan ->
                val supportedByControls = controls.fanModes.any { CatalogTransmitter.fan(it) == fan }
                val supportedByProtocol = definition?.fanSpeeds?.contains(fan) ?: true
                supportedByControls && supportedByProtocol
            }
            ?: definition?.fanSpeeds?.firstOrNull()

    private fun hasToggleOnlyRawProfile(candidate: RemoteCandidate): Boolean =
        candidate.encodingType.equals("RAW_PROFILE", true) &&
            candidate.rawCommandsJson?.contains("\"off\"") == true &&
            candidate.operationModes.isEmpty() && candidate.fanModes.isEmpty()

    private fun IntRange.clamp(value: Int): Int = value.coerceIn(first, last)
}
