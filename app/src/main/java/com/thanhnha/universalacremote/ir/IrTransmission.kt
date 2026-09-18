package com.thanhnha.universalacremote.ir

/** Raw IR carrier and alternating mark/space durations, with all durations in microseconds. */
data class IrTransmission(
    val carrierFrequencyHz: Int,
    val timingsMicros: List<Int>,
) {
    fun validate() {
        require(carrierFrequencyHz > 0) {
            "Carrier frequency must be greater than 0 Hz."
        }
        require(timingsMicros.isNotEmpty()) {
            "IR timing sequence must not be empty."
        }
        require(timingsMicros.size % 2 == 0) {
            "Malformed IR signal: expected complete mark/space pairs."
        }
        require(timingsMicros.all { it > 0 }) {
            "Malformed IR signal: every mark and space duration must be greater than 0 µs."
        }
    }
}
