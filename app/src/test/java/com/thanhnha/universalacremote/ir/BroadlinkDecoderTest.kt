package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BroadlinkDecoderTest {
    private val validBase64 = "JgAEAAECAwQ="

    @Test
    fun validBase64BroadlinkPacketBecomesTransmission() {
        val result = BroadlinkDecoder.decodeBase64(validBase64)
        assertEquals(38_000, result.carrierFrequencyHz)
        assertEquals(listOf(32, 65, 98, 131), result.timingsMicros)
        result.validate()
    }

    @Test
    fun invalidBase64FailsClosed() {
        assertThrows(IllegalArgumentException::class.java) { BroadlinkDecoder.decodeBase64("not-base64") }
    }

    @Test
    fun malformedAndUnsupportedPacketsFailClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            BroadlinkDecoder.decodePacket(byteArrayOf(0x26, 0, 4, 0, 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BroadlinkDecoder.decodePacket(byteArrayOf(0xB2.toByte(), 0, 1, 0, 1))
        }
    }

    @Test
    fun trailingMarkIsKeptAsAnOddCanonicalWaveform() {
        val packet = byteArrayOf(0x26, 0, 3, 0, 1, 2, 3)
        val result = BroadlinkDecoder.decodePacket(packet)
        assertTrue(result.timingsMicros.size % 2 == 1)
        assertEquals(listOf(32, 65, 98), result.timingsMicros)
    }
}
