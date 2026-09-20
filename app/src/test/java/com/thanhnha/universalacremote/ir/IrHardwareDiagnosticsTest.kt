package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Test

class IrHardwareDiagnosticsTest {
    @Test
    fun readyEmitter_isReady() {
        val summary = IrHardwareDiagnostics(
            featureDeclared = true,
            managerAvailable = true,
            hasIrEmitter = true,
            carrierFrequencyRanges = listOf(CarrierFrequencyRange(36_000, 40_000)),
        ).summary()
        assertEquals(IrDiagnosticSeverity.READY, summary.severity)
    }

    @Test
    fun declaredFeatureWithoutEmitter_isLimited() {
        val summary = IrHardwareDiagnostics(true, true, false, emptyList()).summary()
        assertEquals(IrDiagnosticSeverity.LIMITED, summary.severity)
    }

    @Test
    fun noService_isUnavailable() {
        val summary = IrHardwareDiagnostics(false, false, false, emptyList()).summary()
        assertEquals(IrDiagnosticSeverity.UNAVAILABLE, summary.severity)
    }
}