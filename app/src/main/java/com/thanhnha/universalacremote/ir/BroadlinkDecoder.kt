package com.thanhnha.universalacremote.ir

/**
 * Decoder for the Broadlink IR payload used by SmartIR Base64 profiles.
 *
 * The packet format is documented by the pinned implementation reference:
 * https://github.com/mjg59/python-broadlink/blob/730853e5faf2cf979596662faf9def2b1f8fee6d/protocol.md
 * and its data_to_pulses implementation in broadlink/remote.py at the same
 * pinned commit. The packet
 * carries pulse lengths, but no carrier frequency; SmartIR Broadlink input
 * is therefore normalized to the application's documented 38 kHz IR carrier.
 */
object BroadlinkDecoder {
    const val DEFAULT_CARRIER_FREQUENCY_HZ = 38_000
    private const val IR_PACKET_TYPE = 0x26
    private const val HEADER_SIZE = 4
    private const val TICK_MICROS = 32.84

    fun decodeBase64(encoded: String): IrTransmission =
        decodePacket(StrictBase64.decode(encoded))

    fun decodePacket(packet: ByteArray, carrierFrequencyHz: Int = DEFAULT_CARRIER_FREQUENCY_HZ): IrTransmission {
        require(packet.size >= HEADER_SIZE) { "Broadlink packet is truncated." }
        require((packet[0].toInt() and 0xff) == IR_PACKET_TYPE) {
            "Unsupported Broadlink packet type."
        }
        val dataLength = (packet[2].toInt() and 0xff) or ((packet[3].toInt() and 0xff) shl 8)
        require(dataLength > 0) { "Broadlink packet has an invalid data length." }
        val dataEnd = HEADER_SIZE + dataLength
        require(dataEnd <= packet.size) { "Broadlink packet is truncated." }
        require(packet.copyOfRange(dataEnd, packet.size).all { it.toInt() == 0 }) {
            "Broadlink packet has malformed trailing data."
        }

        val timings = ArrayList<Int>()
        var index = HEADER_SIZE
        while (index < dataEnd) {
            val first = packet[index++].toInt() and 0xff
            val ticks = if (first != 0) {
                first
            } else {
                require(index + 1 < dataEnd) { "Broadlink packet contains a truncated extended timing." }
                val value = ((packet[index].toInt() and 0xff) shl 8) or (packet[index + 1].toInt() and 0xff)
                index += 2
                value
            }
            require(ticks > 0) { "Broadlink packet contains a non-positive timing." }
            val micros = (ticks * TICK_MICROS).toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            require(micros > 0) { "Broadlink packet contains an invalid timing." }
            timings += micros
            require(timings.size <= 4096) { "Broadlink waveform exceeds transmitter safety limits." }
        }

        return IrTransmission(carrierFrequencyHz, timings).also(IrTransmission::validate)
    }
}

private object StrictBase64 {
    private const val INVALID = -1
    private val alphabet = IntArray(128) { INVALID }.apply {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".forEachIndexed { index, char ->
            this[char.code] = index
        }
    }

    fun decode(value: String): ByteArray {
        require(value.isNotEmpty() && value.length % 4 == 0) { "Invalid Base64 payload." }
        val padding = value.takeLastWhile { it == '=' }.length
        require(padding <= 2 && (padding == 0 || value.substring(0, value.length - padding).none { it == '=' })) {
            "Invalid Base64 payload."
        }
        val output = ByteArray(value.length / 4 * 3 - padding)
        var outputIndex = 0
        for (offset in value.indices step 4) {
            val a = value[offset].code.let { if (it < alphabet.size) alphabet[it] else INVALID }
            val b = value[offset + 1].code.let { if (it < alphabet.size) alphabet[it] else INVALID }
            val c = if (value[offset + 2] == '=') 0 else value[offset + 2].code.let { if (it < alphabet.size) alphabet[it] else INVALID }
            val d = if (value[offset + 3] == '=') 0 else value[offset + 3].code.let { if (it < alphabet.size) alphabet[it] else INVALID }
            require(a >= 0 && b >= 0 && c >= 0 && d >= 0) { "Invalid Base64 payload." }
            require(value[offset + 2] != '=' || value[offset + 3] == '=') { "Invalid Base64 payload." }
            require(offset + 4 < value.length || value[offset + 2] != '=' || (b and 0x0f) == 0) { "Invalid Base64 payload." }
            require(offset + 4 < value.length || value[offset + 3] != '=' || (c and 0x03) == 0) { "Invalid Base64 payload." }
            if (outputIndex < output.size) output[outputIndex++] = ((a shl 2) or (b shr 4)).toByte()
            if (outputIndex < output.size) output[outputIndex++] = (((b and 0x0f) shl 4) or (c shr 2)).toByte()
            if (outputIndex < output.size) output[outputIndex++] = (((c and 0x03) shl 6) or d).toByte()
        }
        return output
    }
}
