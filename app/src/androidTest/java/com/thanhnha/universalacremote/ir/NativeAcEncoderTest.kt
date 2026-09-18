package com.thanhnha.universalacremote.ir

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeAcEncoderTest {
    @Test fun lgPocWaveformStillMatchesUpstreamSendFixture() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val fixture = context.assets.open("lg_8808721.output.txt").bufferedReader().use { it.readText().trim() }
        val expectedFrequency = Regex("^f(\\d+)").find(fixture)!!.groupValues[1].toInt()
        val expectedTimings = Regex("[ms](\\d+)").findAll(fixture).map { it.groupValues[1].toInt() }.toList()
        val actual = NativeAcEncoder.encodeAc("lg", acState = AcState(true, 22, AcMode.COOL, AcFan.MEDIUM))
        assertEquals(expectedFrequency, actual.carrierFrequencyHz)
        assertEquals(expectedTimings, actual.timingsMicros)
    }

    @Test fun greeUsesGenericRegistryAndIracPipeline() {
        val result = NativeAcEncoder.encodeAc("gree", acState = AcState(true, 22, AcMode.COOL, AcFan.MEDIUM))
        assertEquals(38_000, result.carrierFrequencyHz)
        assertTrue(result.timingsMicros.isNotEmpty())
    }

    @Test fun panasonicUsesGenericRegistryAndIracPipeline() {
        val result = NativeAcEncoder.encodeAc("panasonic_ac", acState = AcState(true, 22, AcMode.COOL, AcFan.MEDIUM))
        assertEquals(36_700, result.carrierFrequencyHz)
        assertTrue(result.timingsMicros.isNotEmpty())
    }

    @Test fun additionalProtocolFamiliesUseSameGenericPipeline() {
        listOf("daikin", "fujitsu_ac", "midea", "mitsubishi_ac", "samsung_ac").forEach { id ->
            val result = NativeAcEncoder.encodeAc(id, acState = AcState(true, 22, AcMode.COOL, AcFan.MEDIUM))
            assertTrue("$id carrier missing", result.carrierFrequencyHz > 0)
            assertTrue("$id waveform missing", result.timingsMicros.isNotEmpty())
            assertEquals(id, result.protocolId)
            assertTrue("$id capability metadata missing", "power" in result.supportedCapabilities)
            result.validate()
        }
    }

    @Test fun rejectsUnknownProtocolAndUnsupportedTemperature() {
        assertThrows(IllegalArgumentException::class.java) {
            NativeAcEncoder.encodeAc("not-a-protocol", acState = AcState(true, 22, AcMode.COOL, AcFan.AUTO))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NativeAcEncoder.encodeAc("lg", acState = AcState(true, 15, AcMode.COOL, AcFan.AUTO))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NativeAcEncoder.encodeAc("daikin", modelId = "unknown-model", acState = AcState(true, 22, AcMode.COOL, AcFan.AUTO))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NativeAcEncoder.encodeAc("midea", acState = AcState(true, 22, AcMode.COOL, AcFan.AUTO, swingHorizontal = true))
        }
    }
}
