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
    @Test
    fun smartIrProfileWithSwingLayerSelectsExactBranch() {
        val commandA = "JgAEAAECAwQ="
        val commandB = "JgAEAAQDAgE="
        val commands = """{"off":"$commandA","cool":{"auto":{"off":{"24":"$commandA"},"vertical":{"24":"$commandB"},"horizontal":{"24":"$commandA"},"both":{"24":"$commandB"}}}}"""
        val profile = RemoteCandidate(
            id = "smartir:3240", brand = "Casper", acModel = "SC-09FS32", remoteModel = null,
            protocolId = null, protocolModel = null, encodingType = "RAW_PROFILE",
            capabilities = setOf("power", "mode:cool", "fan:auto", "swing:vertical", "swing:horizontal"),
            evidence = emptyList(), priority = 0,
            minimumTemperatureCelsius = 16, maximumTemperatureCelsius = 32,
            operationModes = listOf("cool"), fanModes = listOf("auto"),
            verticalSwing = SwingCapability("ON_OFF"), horizontalSwing = SwingCapability("ON_OFF"),
            verificationStatus = "transmittable", rawCommandsJson = commands,
            sourceMetadataJson = """{"supportedController":"Broadlink","commandsEncoding":"Base64","swingModes":["off","vertical","horizontal","both"]}""",
        )

        val noSwing = CatalogTransmitter.encode(profile, AcState(true, 24, AcMode.COOL, AcFan.AUTO))
        val vertical = CatalogTransmitter.encode(profile, AcState(true, 24, AcMode.COOL, AcFan.AUTO, swingVertical = true))
        val both = CatalogTransmitter.encode(profile, AcState(true, 24, AcMode.COOL, AcFan.AUTO, swingVertical = true, swingHorizontal = true))

        assertEquals(listOf(32, 65, 98, 131), noSwing.timingsMicros)
        assertEquals(listOf(131, 98, 65, 32), vertical.timingsMicros)
        assertEquals(listOf(131, 98, 65, 32), both.timingsMicros)
        assertEquals(true, CatalogTransmitter.supports(profile))
    }

    @Test
    fun verificationStateFindsSparseSmartIrModeFanCombinationThroughSwingLayer() {
        val commandA = "JgAEAAECAwQ="
        val commandB = "JgAEAAQDAgE="
        val profile = RemoteCandidate(
            id = "smartir:3240-like", brand = "Casper", acModel = "SC-09FS32", remoteModel = null,
            protocolId = null, protocolModel = null, encodingType = "RAW_PROFILE",
            capabilities = setOf("power", "mode:cool", "mode:dry", "fan:auto", "fan:low"),
            evidence = emptyList(), priority = 0,
            minimumTemperatureCelsius = 16, maximumTemperatureCelsius = 30,
            operationModes = listOf("cool", "dry"), fanModes = listOf("auto", "low"),
            verificationStatus = "transmittable",
            rawCommandsJson = """{"off":"$commandA","cool":{"auto":{"off":{"24":"$commandA"}}},"dry":{"low":{"off":{"24":"$commandB"}}}}""",
            sourceMetadataJson = """{"supportedController":"Broadlink","commandsEncoding":"Base64","swingModes":["off","vertical","horizontal","both"]}""",
        )

        val state = CatalogTransmitter.verificationState(profile, VerificationCheck.MODE)!!
        assertEquals(AcMode.DRY, state.mode)
        assertEquals(AcFan.MIN, state.fan)
        assertEquals(listOf(131, 98, 65, 32), CatalogTransmitter.encode(profile, state).timingsMicros)
    }

    @Test
    fun resolveStateKeepsRequestedSparseModeAndChoosesCompatibleFan() {
        val commandA = "JgAEAAECAwQ="
        val commandB = "JgAEAAQDAgE="
        val profile = RemoteCandidate(
            id = "smartir:casper-like", brand = "Casper", acModel = "SC-09FS32", remoteModel = null,
            protocolId = null, protocolModel = null, encodingType = "RAW_PROFILE",
            capabilities = setOf("power", "mode:cool", "mode:dry", "fan:auto", "fan:low"),
            evidence = emptyList(), priority = 0,
            minimumTemperatureCelsius = 24, maximumTemperatureCelsius = 25,
            operationModes = listOf("cool", "dry"), fanModes = listOf("auto", "low"),
            verificationStatus = "transmittable",
            rawCommandsJson = """{"off":"$commandA","cool":{"auto":{"24":"$commandA","25":"$commandA"},"low":{"24":"$commandA","25":"$commandA"}},"dry":{"low":{"24":"$commandB","25":"$commandB"}}}""",
            sourceMetadataJson = """{"supportedController":"Broadlink","commandsEncoding":"Base64"}""",
        )

        val resolved = CatalogTransmitter.resolveState(
            profile,
            AcState(true, 24, AcMode.DRY, AcFan.AUTO),
            lockMode = true,
        )!!

        assertEquals(AcMode.DRY, resolved.mode)
        assertEquals(AcFan.MIN, resolved.fan)
        assertEquals(listOf(131, 98, 65, 32), CatalogTransmitter.encode(profile, resolved).timingsMicros)
    }

    @Test
    fun resolveStateRejectsLockedModeThatHasNoCommandBranch() {
        val command = "JgAEAAECAwQ="
        val profile = RemoteCandidate(
            id = "smartir:missing-dry", brand = "Example", acModel = "Sparse", remoteModel = null,
            protocolId = null, protocolModel = null, encodingType = "RAW_PROFILE",
            capabilities = setOf("power", "mode:cool", "mode:dry", "fan:auto"),
            evidence = emptyList(), priority = 0,
            minimumTemperatureCelsius = 24, maximumTemperatureCelsius = 24,
            operationModes = listOf("cool", "dry"), fanModes = listOf("auto"),
            verificationStatus = "transmittable",
            rawCommandsJson = """{"off":"$command","cool":{"auto":{"24":"$command"}}}""",
            sourceMetadataJson = """{"supportedController":"Broadlink","commandsEncoding":"Base64"}""",
        )

        assertEquals(
            null,
            CatalogTransmitter.resolveState(
                profile,
                AcState(true, 24, AcMode.DRY, AcFan.AUTO),
                lockMode = true,
            ),
        )
    }

    @Test
    fun verificationStateDoesNotExposeAdvertisedModeWithoutARealCommand() {
        val command = "JgAEAAECAwQ="
        val profile = RemoteCandidate(
            id = "smartir:missing-mode", brand = "Example", acModel = "Sparse", remoteModel = null,
            protocolId = null, protocolModel = null, encodingType = "RAW_PROFILE",
            capabilities = setOf("power", "mode:cool", "mode:dry", "fan:auto"),
            evidence = emptyList(), priority = 0,
            minimumTemperatureCelsius = 24, maximumTemperatureCelsius = 24,
            operationModes = listOf("cool", "dry"), fanModes = listOf("auto"),
            verificationStatus = "transmittable",
            rawCommandsJson = """{"off":"$command","cool":{"auto":{"24":"$command"}}}""",
            sourceMetadataJson = """{"supportedController":"Broadlink","commandsEncoding":"Base64"}""",
        )

        assertEquals(null, CatalogTransmitter.verificationState(profile, VerificationCheck.MODE))
    }

}
