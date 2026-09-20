package com.thanhnha.universalacremote

import com.thanhnha.universalacremote.ir.AcFan
import com.thanhnha.universalacremote.ir.AcMode
import com.thanhnha.universalacremote.ir.AcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastSentStateTest {
    @Test
    fun stateRoundTrip_keepsLegacyEncodableFields() {
        val state = AcState(
            power = true,
            temperatureCelsius = 26,
            mode = AcMode.COOL,
            fan = AcFan.MEDIUM,
            swingVertical = true,
            swingHorizontal = false,
        )

        assertEquals(state, decodeLastSentState(encodeLastSentState(state)))
    }

    @Test
    fun invalidState_isIgnored() {
        assertNull(decodeLastSentState("{bad json"))
    }
}
