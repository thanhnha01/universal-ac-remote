package com.thanhnha.universalacremote.ir

/** Raw IR carrier and alternating mark/space durations, with all durations in microseconds. */
data class IrTransmission(
    val carrierFrequencyHz: Int,
    val timingsMicros: List<Int>,
    val protocolId: String? = null,
    val modelId: String? = null,
    val supportedCapabilities: Set<String> = emptySet(),
) {
    fun validate() {
        require(carrierFrequencyHz in 1..500_000) {
            "Carrier frequency is outside the supported 1..500000 Hz range."
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
        require(timingsMicros.size <= 4096 && timingsMicros.all { it <= 1_000_000 } && timingsMicros.sumOf { it.toLong() } <= 120_000_000L) {
            "IR waveform exceeds transmitter safety limits."
        }
    }
}
