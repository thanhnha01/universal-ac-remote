package com.thanhnha.universalacremote.ir

/** Protocol-independent desired A/C state passed through the native IRac adapter. */
data class AcState(
    val power: Boolean,
    val temperatureCelsius: Int,
    val mode: AcMode,
    val fan: AcFan,
    val swingVertical: Boolean = false,
    val swingHorizontal: Boolean = false,
)

enum class AcMode(val nativeValue: Int) { COOL(0), HEAT(1), DRY(2), FAN(3), AUTO(4) }
enum class AcFan(val nativeValue: Int) { AUTO(0), MIN(1), MEDIUM(2), HIGH(3) }
