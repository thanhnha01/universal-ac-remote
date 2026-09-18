package com.thanhnha.universalacremote.ir

data class SwingControl(val type: String, val positions: List<String>) {
    val visible: Boolean get() = type != "NONE"
    val toggleOnly: Boolean get() = type == "ON_OFF"
}

data class RemoteControls(
    val power: Boolean,
    val temperatureRange: IntRange?,
    val modes: List<String>,
    val fanModes: List<String>,
    val verticalSwing: SwingControl,
    val horizontalSwing: SwingControl,
    val specialCapabilities: List<String>,
) {
    fun verificationRequirements(): Set<VerificationCheck> = buildSet {
        if (power) add(VerificationCheck.POWER)
        if (temperatureRange != null && temperatureRange.first < temperatureRange.last) add(VerificationCheck.TEMPERATURE_CHANGED)
        if (modes.distinct().size > 1) add(VerificationCheck.MODE)
        if (fanModes.distinct().size > 1) add(VerificationCheck.FAN)
        if (verticalSwing.visible) add(VerificationCheck.SWING_VERTICAL)
        if (horizontalSwing.visible) add(VerificationCheck.SWING_HORIZONTAL)
    }

    fun verificationResult(verified: Set<VerificationCheck>): ScanResult {
        if (verified.isEmpty()) return ScanResult.NO_MATCH
        val required = verificationRequirements()
        return if (required.size > 1 && verified.containsAll(required)) ScanResult.FULL_MATCH
        else ScanResult.PARTIAL_MATCH
    }

    companion object {
        fun from(candidate: RemoteCandidate): RemoteControls {
            val modes = candidate.operationModes.filter { "mode:${it.lowercase()}" in candidate.capabilities }
            val fans = candidate.fanModes.filter { "fan:${it.lowercase()}" in candidate.capabilities }
            val min = candidate.minimumTemperatureCelsius
            val max = candidate.maximumTemperatureCelsius
            val range = if (min != null && max != null && min <= max) min..max else null
            val specials = candidate.specialCapabilities.filter { "special:${it.lowercase()}" in candidate.capabilities }
            return RemoteControls(
                power = "power" in candidate.capabilities,
                temperatureRange = range,
                modes = modes,
                fanModes = fans,
                verticalSwing = swing(candidate.verticalSwing),
                horizontalSwing = swing(candidate.horizontalSwing),
                specialCapabilities = specials,
            )
        }

        private fun swing(capability: SwingCapability): SwingControl {
            val type = capability.type.uppercase()
            val sourcePositions = capability.positions.distinct()
            val positions = when (type) {
                "POSITIONS" -> sourcePositions
                "AUTO_AND_POSITIONS" -> (listOf("AUTO") + sourcePositions.filterNot { it.equals("AUTO", true) }).distinct()
                else -> emptyList()
            }
            return SwingControl(type, positions)
        }
    }
}

fun modeLabel(value: String): String = when (value.lowercase()) {
    "auto" -> "Tự động"
    "cool" -> "Làm mát"
    "dry" -> "Hút ẩm"
    "fan" -> "Quạt"
    "heat" -> "Sưởi ấm"
    else -> value.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

fun fanLabel(value: String): String = when (value.lowercase()) {
    "auto" -> "Tự động"
    "min", "low" -> "Thấp"
    "medium", "mid" -> "Trung bình"
    "high" -> "Cao"
    "quiet" -> "Yên tĩnh"
    "turbo" -> "Turbo"
    else -> value.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

fun swingPositionLabel(value: String): String = when (value.uppercase()) {
    "HIGHEST", "HIGH_MAX", "TOP" -> "Cao nhất"
    "HIGH" -> "Cao"
    "MIDDLE", "MID" -> "Giữa"
    "LOW" -> "Thấp"
    "LOWEST", "LOW_MAX", "BOTTOM" -> "Thấp nhất"
    "LEFTMAX", "LEFT_MAX" -> "Trái xa nhất"
    "LEFT" -> "Trái"
    "MIDDLELEFT", "MIDDLE_LEFT" -> "Giữa trái"
    "MID_LEFT" -> "Giữa trái"
    "MID_RIGHT", "MIDDLERIGHT", "MIDDLE_RIGHT" -> "Giữa phải"
    "RIGHT" -> "Phải"
    "RIGHTMAX", "RIGHT_MAX" -> "Phải xa nhất"
    "WIDE" -> "Rộng"
    "AUTO" -> "Tự động"
    else -> value.replace('_', ' ').replace('-', ' ').replaceFirstChar(Char::uppercase)
}

fun specialCapabilityLabel(value: String): String = when (value.lowercase()) {
    "turbo" -> "Turbo"
    "quiet" -> "Yên tĩnh"
    "eco" -> "Tiết kiệm"
    "sleep" -> "Ngủ"
    "powerful" -> "Công suất cao"
    "light", "led" -> "Đèn"
    "timer" -> "Hẹn giờ"
    "clean" -> "Tự làm sạch"
    "health" -> "Sức khỏe"
    else -> value.replace('_', ' ').replace('-', ' ').replaceFirstChar(Char::uppercase)
}
