package com.thanhnha.universalacremote.ir

/**
 * Protocol-independent desired A/C state.
 *
 * Extended fields are intentionally represented here before every encoder can
 * emit them. Encoders must reject unsupported extended values rather than
 * silently dropping a user action.
 */
data class AcState(
    val power: Boolean,
    val temperatureCelsius: Int,
    val mode: AcMode,
    val fan: AcFan,
    val swingVertical: Boolean = false,
    val swingHorizontal: Boolean = false,
    val swingVerticalPosition: AcSwingPosition? = null,
    val swingHorizontalPosition: AcSwingPosition? = null,
    val turbo: Boolean = false,
    val quiet: Boolean = false,
    val eco: Boolean = false,
    val sleep: Boolean = false,
    val light: Boolean = false,
    val clean: Boolean = false,
) {
    fun requiresExtendedEncoder(): Boolean =
        swingVerticalPosition != null ||
            swingHorizontalPosition != null ||
            turbo || quiet || eco || sleep || light || clean
}

enum class AcMode(val nativeValue: Int) { COOL(0), HEAT(1), DRY(2), FAN(3), AUTO(4) }
enum class AcFan(val nativeValue: Int) { AUTO(0), MIN(1), MEDIUM(2), HIGH(3) }

enum class AcSwingPosition {
    AUTO,
    HIGHEST,
    HIGH,
    MIDDLE,
    LOW,
    LOWEST,
    LEFT_MAX,
    LEFT,
    MIDDLE_LEFT,
    MIDDLE_RIGHT,
    RIGHT,
    RIGHT_MAX,
    WIDE,
}
