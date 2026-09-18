package com.thanhnha.universalacremote

import org.junit.Assert.assertEquals
import org.junit.Test
import com.thanhnha.universalacremote.ir.IrTransmission

class SavedRemoteConvertersTest {
    private val converter = SavedRemoteConverters()

    @Test fun verifiedCapabilitiesRoundTripAsJson() {
        val values = listOf("POWER", "TEMPERATURE_CHANGED", "swing:vertical")
        assertEquals(values, converter.decodeCapabilities(converter.encodeCapabilities(values)))
    }

    @Test fun malformedJsonReturnsNoVerifiedCapabilities() {
        assertEquals(emptyList<String>(), converter.decodeCapabilities("not-json"))
    }

    @Test fun importedRawCommandsRoundTripWithoutLosingUnknownNames() {
        val commands = listOf(ImportedRawCommand("Mystery button", IrTransmission(38_000, listOf(900, 450, 560, 560))))
        val decoded = converter.decodeImportedCommands(converter.encodeImportedCommands(commands))
        assertEquals("Mystery button", decoded.single().name)
        assertEquals(commands.single().transmission, decoded.single().transmission)
    }

    @Test fun malformedImportedPayloadFailsClosed() {
        assertEquals(emptyList<ImportedRawCommand>(), converter.decodeImportedCommands("not-json"))
        assertEquals(emptyList<ImportedRawCommand>(), converter.decodeImportedCommands(""))
    }
}
