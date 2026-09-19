package com.thanhnha.universalacremote

import com.thanhnha.universalacremote.ir.RemoteCandidate
import com.thanhnha.universalacremote.ir.VerificationCheck
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedRemoteVerificationTest {
    private fun candidate(): RemoteCandidate =
        RemoteCandidate(
            id = "smartir:fixture",
            brand = "Example",
            acModel = "AC-1",
            remoteModel = null,
            protocolId = null,
            protocolModel = null,
            encodingType = "RAW_PROFILE",
            capabilities = setOf("power", "mode:cool", "fan:low"),
            evidence = emptyList(),
            priority = 9,
            minimumTemperatureCelsius = 16,
            maximumTemperatureCelsius = 30,
            operationModes = listOf("cool"),
            fanModes = listOf("low"),
            verificationStatus = "transmittable",
            rawCommandsJson = """{"off":"JgAEAAECAwQ=","cool":{"low":{"22":"JgAEAAECAwQ="}}}""",
            sourceMetadataJson = """{"supportedController":"Broadlink","commandsEncoding":"Base64"}""",
        )

    private fun remote(verified: List<String> = emptyList(), imported: String = "") =
        SavedRemote(
            id = "saved",
            displayName = "Bedroom",
            catalogProfileId = "smartir:fixture",
            brand = "Example",
            acModel = "AC-1",
            remoteModel = null,
            protocolId = null,
            protocolModel = null,
            verifiedCapabilities = verified,
            importedCommandsJson = imported,
        )

    @Test
    fun noVerifiedChecksIsNotVerified() {
        assertEquals(SavedVerificationState.NONE, remote().verificationState(candidate()))
    }

    @Test
    fun oneOfMultipleRequiredChecksIsPartial() {
        val saved = remote(listOf(VerificationCheck.POWER.name))
        assertEquals(SavedVerificationState.PARTIAL, saved.verificationState(candidate()))
    }

    @Test
    fun allRequiredChecksIsFull() {
        val saved = remote(
            listOf(
                VerificationCheck.POWER.name,
                VerificationCheck.TEMPERATURE_CHANGED.name,
            )
        )
        assertEquals(SavedVerificationState.FULL, saved.verificationState(candidate()))
    }

    @Test
    fun importedRemoteDoesNotPretendToHaveCatalogVerification() {
        val saved = remote(
            verified = listOf(VerificationCheck.POWER.name),
            imported = """[{"name":"Power","frequency":38000,"timings":[900,450]}]""",
        )
        assertEquals(SavedVerificationState.NONE, saved.verificationState(candidate()))
    }
}
