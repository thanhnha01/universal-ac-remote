package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcStateV2Test {
    @Test
    fun legacyState_doesNotRequireExtendedEncoder() {
        val state = AcState(true, 24, AcMode.COOL, AcFan.AUTO, swingVertical = true)
        assertFalse(state.requiresExtendedEncoder())
    }

    @Test
    fun specialFeature_requiresExtendedEncoder() {
        val state = AcState(true, 24, AcMode.COOL, AcFan.AUTO, eco = true)
        assertTrue(state.requiresExtendedEncoder())
    }

    @Test
    fun explicitSwingPosition_requiresExtendedEncoder() {
        val state = AcState(
            true,
            24,
            AcMode.COOL,
            AcFan.AUTO,
            swingVerticalPosition = AcSwingPosition.MIDDLE,
        )
        assertTrue(state.requiresExtendedEncoder())
    }
}
