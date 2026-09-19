package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalAcScannerTest {
    private val candidate = RemoteCandidate(
        "p", "Acme", null, null, "proto", null, "PROTOCOL",
        setOf("power", "temperature", "mode:cool", "fan:auto", "swing:vertical"), emptyList(), 6,
        minimumTemperatureCelsius = 16, maximumTemperatureCelsius = 30,
        operationModes = listOf("cool"), fanModes = listOf("auto"),
        verticalSwing = SwingCapability("ON_OFF"),
    )

    @Test fun stopPreventsFurtherTransmission() {
        var sends = 0
        val scanner = UniversalAcScanner(listOf(candidate, candidate), 100) { sends++ }
        scanner.tryCurrent(0)
        scanner.stop()
        assertEquals(ScanState.STOPPED, scanner.state)
        org.junit.Assert.assertThrows(IllegalStateException::class.java) { scanner.tryCurrent(100) }
        assertEquals(1, sends)
    }

    @Test fun candidateCannotBeTransmittedUntilFeedbackOrDelay() {
        var sends = 0
        val scanner = UniversalAcScanner(listOf(candidate, candidate), 100) { sends++ }
        scanner.tryCurrent(100)
        org.junit.Assert.assertThrows(IllegalStateException::class.java) { scanner.tryCurrent(200) }
        scanner.onFeedbackTimeout()
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { scanner.tryCurrent(150) }
        scanner.tryCurrent(200)
        assertEquals(2, sends)
    }

    @Test fun incompleteVerificationIsPartialAndSavesCapabilities() {
        val scanner = UniversalAcScanner(listOf(candidate)) {}
        scanner.tryCurrent(0)
        scanner.reportReaction()
        scanner.recordVerification(VerificationCheck.POWER)
        assertEquals(ScanResult.PARTIAL_MATCH, scanner.finishVerification())
        assertEquals(setOf(VerificationCheck.POWER), scanner.verifiedCapabilities)
    }

    @Test fun allRequiredVerificationIsFullMatchButPowerAloneIsNot() {
        val scanner = UniversalAcScanner(listOf(candidate)) {}
        scanner.tryCurrent(0)
        scanner.reportReaction()
        scanner.recordVerification(VerificationCheck.POWER)
        scanner.recordVerification(VerificationCheck.TEMPERATURE_CHANGED)
        scanner.recordVerification(VerificationCheck.MODE)
        scanner.recordVerification(VerificationCheck.FAN)
        scanner.recordVerification(VerificationCheck.SWING_VERTICAL)
        assertEquals(ScanResult.FULL_MATCH, scanner.finishVerification())
        assertTrue(scanner.verifiedCapabilities.contains(VerificationCheck.SWING_VERTICAL))
    }

    @Test fun unsupportedVerificationChecksAreIgnored() {
        val powerOnly = candidate.copy(capabilities = setOf("power"), minimumTemperatureCelsius = null, maximumTemperatureCelsius = null)
        val scanner = UniversalAcScanner(listOf(powerOnly)) {}
        scanner.tryCurrent(0)
        scanner.reportReaction()
        scanner.recordVerification(VerificationCheck.POWER)
        scanner.recordVerification(VerificationCheck.TEMPERATURE_CHANGED)
        assertEquals(setOf(VerificationCheck.POWER), scanner.verifiedCapabilities)
        assertEquals(ScanResult.PARTIAL_MATCH, scanner.finishVerification())
    }

    @Test fun verificationOrderKeepsPowerLastAndReactionLocksCandidate() {
        val scanner = UniversalAcScanner(listOf(candidate, candidate)) {}
        scanner.tryCurrent(0)
        scanner.reportReaction()

        assertEquals(VerificationCheck.TEMPERATURE_CHANGED, scanner.nextVerificationCheck())
        scanner.recordVerification(VerificationCheck.TEMPERATURE_CHANGED)
        assertEquals(VerificationCheck.SWING_VERTICAL, scanner.nextVerificationCheck())
        scanner.recordVerification(VerificationCheck.SWING_VERTICAL)
        assertEquals(VerificationCheck.POWER, scanner.nextVerificationCheck())
        org.junit.Assert.assertThrows(IllegalStateException::class.java) { scanner.tryCurrent(2_000) }
    }

    @Test fun failedAndSkippedVerificationStatusesAreRetained() {
        val modeCandidate = candidate.copy(
            capabilities = candidate.capabilities + "mode:dry",
            operationModes = listOf("cool", "dry"),
        )
        val scanner = UniversalAcScanner(listOf(modeCandidate)) {}
        scanner.tryCurrent(0)
        scanner.reportReaction()
        scanner.recordVerification(VerificationCheck.TEMPERATURE_CHANGED, supported = false)
        scanner.skipVerification(VerificationCheck.MODE)

        assertEquals(VerificationStatus.FAILED, scanner.verificationStatuses[VerificationCheck.TEMPERATURE_CHANGED])
        assertEquals(VerificationStatus.SKIPPED, scanner.verificationStatuses[VerificationCheck.MODE])
    }
}
