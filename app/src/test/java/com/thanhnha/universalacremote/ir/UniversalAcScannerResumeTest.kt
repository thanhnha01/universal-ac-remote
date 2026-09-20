package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalAcScannerResumeTest {
    private fun candidate(id: String) = RemoteCandidate(
        id = id,
        brand = "Daikin",
        acModel = "FTXM35",
        remoteModel = null,
        protocolId = "daikin",
        protocolModel = "DAIKIN",
        encodingType = "PROTOCOL",
        capabilities = setOf("power", "temp"),
        evidence = emptyList(),
        priority = 0,
        operationModes = listOf("cool"),
        fanModes = listOf("auto"),
        minimumTemperatureCelsius = 16,
        maximumTemperatureCelsius = 30,
    )

    @Test
    fun restoreAwaitingProgress_resumesReadyWithoutTransmission() {
        var transmissions = 0
        val scanner = UniversalAcScanner(listOf(candidate("a"), candidate("b"))) { transmissions++ }

        scanner.restore(ScannerSnapshot(cursor = 1))

        assertEquals(ScanState.READY, scanner.state)
        assertEquals(1, scanner.cursor)
        assertEquals(0, transmissions)
    }

    @Test
    fun restoreVerification_restoresVerifiedCapabilities() {
        val scanner = UniversalAcScanner(listOf(candidate("a"))) {}
        scanner.restore(
            ScannerSnapshot(
                cursor = 0,
                selectedCandidateId = "a",
                verificationStatuses = mapOf(VerificationCheck.TEMPERATURE_CHANGED to VerificationStatus.VERIFIED),
            ),
        )

        assertEquals(ScanState.VERIFYING, scanner.state)
        assertTrue(VerificationCheck.TEMPERATURE_CHANGED in scanner.verifiedCapabilities)
    }
}
