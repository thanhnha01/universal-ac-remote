package com.thanhnha.universalacremote

import com.thanhnha.universalacremote.ir.FlipperRawCommand
import com.thanhnha.universalacremote.ir.IrTransmission
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportedCommandPresentationTest {
    private val tx = IrTransmission(38_000, listOf(9000, 4500, 560, 560))

    @Test
    fun commands_areClassifiedAndOrdered() {
        val result = smartImportedCommands(
            listOf(
                FlipperRawCommand("fan_high", tx),
                FlipperRawCommand("power", tx),
                FlipperRawCommand("temp_down", tx),
                FlipperRawCommand("swing_vertical", tx),
            ),
        )

        assertEquals(
            listOf("Nguồn", "Nhiệt độ −", "Quạt", "Swing dọc"),
            result.map { it.label },
        )
    }
}
