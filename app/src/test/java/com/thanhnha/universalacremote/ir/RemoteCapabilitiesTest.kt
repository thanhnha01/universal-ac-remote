package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCapabilitiesTest {
    private fun profile(capabilities: Set<String>, vertical: SwingCapability = SwingCapability(), horizontal: SwingCapability = SwingCapability()) =
        RemoteCandidate("p", "A", null, null, "proto", null, "PROTOCOL", capabilities, emptyList(), 0,
            minimumTemperatureCelsius = 18, maximumTemperatureCelsius = 30,
            operationModes = listOf("cool", "heat", "dry"), fanModes = listOf("auto", "low", "high"),
            verticalSwing = vertical, horizontalSwing = horizontal,
            specialCapabilities = listOf("turbo", "sleep"))

    @Test fun controlsOnlyExposeAdvertisedAndMappedCapabilities() {
        val controls = RemoteControls.from(profile(setOf("power", "mode:cool", "fan:high", "special:turbo")))
        assertTrue(controls.power)
        assertEquals(listOf("cool"), controls.modes)
        assertEquals(listOf("high"), controls.fanModes)
        assertEquals(listOf("turbo"), controls.specialCapabilities)
        assertEquals(18..30, controls.temperatureRange)
    }

    @Test fun swingNoneAndOnOffAreNotConfusedWithPositions() {
        assertFalse(RemoteControls.from(profile(emptySet())).verticalSwing.visible)
        val controls = RemoteControls.from(profile(setOf("swing:vertical"), SwingCapability("ON_OFF")))
        assertTrue(controls.verticalSwing.toggleOnly)
        assertTrue(controls.verticalSwing.positions.isEmpty())
    }

    @Test fun swingPositionsUseOnlyUpstreamListAndAutoIsAddedOnlyForAutoAndPositions() {
        val positions = listOf("HIGHEST", "MIDDLE", "LOWEST")
        val fixed = RemoteControls.from(profile(setOf("swing:vertical"), SwingCapability("POSITIONS", positions)))
        assertEquals(positions, fixed.verticalSwing.positions)
        val withAuto = RemoteControls.from(profile(setOf("swing:horizontal"), horizontal = SwingCapability("AUTO_AND_POSITIONS", listOf("LEFT", "RIGHT"))))
        assertEquals(listOf("AUTO", "LEFT", "RIGHT"), withAuto.horizontalSwing.positions)
    }

    @Test fun swingAxesAndExtraCapabilitiesRemainIndependent() {
        val controls = RemoteControls.from(profile(setOf("swing:horizontal", "special:sleep"),
            vertical = SwingCapability("ON_OFF"), horizontal = SwingCapability("POSITIONS", listOf("LEFT", "RIGHT"))))
        assertTrue(controls.verticalSwing.toggleOnly)
        assertEquals(listOf("LEFT", "RIGHT"), controls.horizontalSwing.positions)
        assertEquals(listOf("sleep"), controls.specialCapabilities)
    }
    @Test fun singleRequiredCapabilityCanBeFullMatch() {
        val controls = RemoteControls.from(
            profile(
                capabilities = setOf("power"),
                vertical = SwingCapability(),
                horizontal = SwingCapability(),
            ).copy(
                operationModes = emptyList(),
                fanModes = emptyList(),
                minimumTemperatureCelsius = null,
                maximumTemperatureCelsius = null,
            )
        )
        assertEquals(setOf(VerificationCheck.POWER), controls.verificationRequirements())
        assertEquals(ScanResult.FULL_MATCH, controls.verificationResult(setOf(VerificationCheck.POWER)))
    }

}
