package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeProbePolicyTest {
    private val protocolCandidate = RemoteCandidate(
        id = "midea:generic", brand = "Midea", acModel = null, remoteModel = null,
        protocolId = "MIDEA", protocolModel = null, encodingType = "PROTOCOL",
        capabilities = setOf("power", "mode:cool", "mode:dry", "fan:auto", "fan:high"),
        evidence = emptyList(), priority = 1,
        minimumTemperatureCelsius = 17, maximumTemperatureCelsius = 30,
        operationModes = listOf("cool", "dry"), fanModes = listOf("auto", "high"),
    )

    @Test fun initialProbeNeverStartsWithPowerOff() {
        val probe = SafeProbePolicy.forCandidate(protocolCandidate)

        assertNotNull(probe)
        assertTrue(probe!!.state.power)
        assertEquals(AcMode.COOL, probe.state.mode)
    }

    @Test fun smartIrProbeFindsARealStateWithoutUsingOffCommand() {
        val candidate = protocolCandidate.copy(
            id = "smartir:fixture", protocolId = null, encodingType = "RAW_PROFILE",
            rawCommandsJson = "{\"off\":\"JgAEAAECAwQ=\",\"cool\":{\"low\":{\"22\":\"JgAEAAECAwQ=\"}}}",
            sourceMetadataJson = "{\"supportedController\":\"Broadlink\",\"commandsEncoding\":\"Base64\"}",
            capabilities = setOf("power", "mode:cool", "fan:low"),
            operationModes = listOf("cool"), fanModes = listOf("low"),
        )

        val probe = CatalogTransmitter.safeProbe(candidate)

        assertNotNull(probe)
        assertTrue(probe!!.state.power)
        assertEquals(22, probe.state.temperatureCelsius)
        assertTrue(CatalogTransmitter.supports(candidate))
    }

    @Test fun toggleOnlyRawProfileIsExplicitlyWarned() {
        val candidate = protocolCandidate.copy(
            id = "smartir:toggle", protocolId = null, encodingType = "RAW_PROFILE",
            rawCommandsJson = "{\"toggle\":\"JgAEAAECAwQ=\"}",
            sourceMetadataJson = "{\"supportedController\":\"Broadlink\",\"commandsEncoding\":\"Base64\"}",
            capabilities = setOf("power"), operationModes = emptyList(), fanModes = emptyList(),
            minimumTemperatureCelsius = null, maximumTemperatureCelsius = null,
        )

        val probe = CatalogTransmitter.safeProbe(candidate)

        assertNotNull(probe)
        assertTrue(probe!!.warning!!.contains("bật/tắt"))
        assertTrue(CatalogTransmitter.supports(candidate))
    }
}
