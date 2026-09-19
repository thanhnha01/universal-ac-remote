package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FlipperRawImporterTest {
    private fun fixture() = javaClass.getResource("/flipper/multi_raw.ir")!!.readText()

    @Test fun importsMultipleRawCommandsDeterministically() {
        val commands = FlipperRawImporter.parse(fixture())
        assertEquals(listOf("Power", "Mode"), commands.map { it.name })
        assertEquals(38_000, commands[0].transmission.carrierFrequencyHz)
        assertEquals(listOf(9000, 4500, 560, 560, 560, 1690), commands[0].transmission.timingsMicros)
        assertEquals(36_000, commands[1].transmission.carrierFrequencyHz)
    }

    @Test fun reportsMissingFrequency() {
        assertThrows(IllegalStateException::class.java) { FlipperRawImporter.parse(raw("data: 10 20")) }
    }

    @Test fun rejectsInvalidFrequency() {
        assertThrows(IllegalArgumentException::class.java) { FlipperRawImporter.parse(raw("frequency: 0\ndata: 10 20")) }
        assertThrows(IllegalStateException::class.java) { FlipperRawImporter.parse(raw("frequency: nope\ndata: 10 20")) }
    }

    @Test fun rejectsEmptyDataAndNonPositiveTiming() {
        assertThrows(IllegalArgumentException::class.java) { FlipperRawImporter.parse(raw("frequency: 38000\ndata:")) }
        assertThrows(IllegalArgumentException::class.java) { FlipperRawImporter.parse(raw("frequency: 38000\ndata: 10 0")) }
    }

    @Test fun rejectsMalformedFormatAndAcceptsTrailingMark() {
        assertThrows(IllegalArgumentException::class.java) { FlipperRawImporter.parse("not a field") }
        assertEquals(3, FlipperRawImporter.parse(raw("frequency: 38000\ndata: 10 20 30"))[0].transmission.timingsMicros.size)
    }

    private fun raw(fields: String) = "Filetype: IR signals\nVersion: 1\n#\nname: Test\ntype: raw\n$fields\n"
}
