package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogTransmitterTest {
    private fun smartProfile(status: String = "transmittable", commands: String = "{\"off\":\"JgAEAAECAwQ=\",\"cool\":{\"low\":{\"22\":\"JgAEAAECAwQ=\"}}}") =
        RemoteCandidate(
            id = "smartir:fixture", brand = "Example", acModel = "Unknown", remoteModel = null,
            protocolId = null, protocolModel = null, encodingType = "RAW_PROFILE",
            capabilities = setOf("power", "mode:cool", "fan:low"), evidence = emptyList(), priority = 9,
            minimumTemperatureCelsius = 16, maximumTemperatureCelsius = 30,
            operationModes = listOf("cool"), fanModes = listOf("low"),
            verificationStatus = status, rawCommandsJson = commands,
            sourceMetadataJson = "{\"supportedController\":\"Broadlink\",\"commandsEncoding\":\"Base64\"}",
        )

    @Test
    fun validSmartIrProfileIsTransmittable() {
        val profile = smartProfile()
        assertEquals(true, CatalogTransmitter.supports(profile))
        assertEquals(listOf(32, 65, 98, 131), CatalogTransmitter.encode(profile, AcState(true, 22, AcMode.COOL, AcFan.MIN)).timingsMicros)
    }

    @Test
    fun unsupportedSmartIrStateIsExplicit() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogTransmitter.encode(smartProfile(), AcState(true, 22, AcMode.HEAT, AcFan.MIN))
        }
    }

    @Test
    fun unsupportedSmartIrProfileIsNotAdvertisedAsTransmittable() {
        val profile = smartProfile(status = "unsupported", commands = "{\"off\":\"not-base64\"}")
        assertEquals(false, CatalogTransmitter.supports(profile))
    }
}
