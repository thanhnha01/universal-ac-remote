package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IrTransmissionTest {
    @Test
    fun acceptsPositiveFrequencyAndCompleteMarkSpacePairs() {
        IrTransmission(38_000, listOf(900, 450, 560, 560)).validate()
    }

    @Test
    fun rejectsNonPositiveFrequency() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            IrTransmission(0, listOf(900, 450)).validate()
        }
        assertEquals("Carrier frequency must be greater than 0 Hz.", error.message)
    }

    @Test
    fun rejectsEmptyTimingList() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            IrTransmission(38_000, emptyList()).validate()
        }
        assertEquals("IR timing sequence must not be empty.", error.message)
    }

    @Test
    fun rejectsIncompleteMarkSpacePairAsMalformed() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            IrTransmission(38_000, listOf(900, 450, 560)).validate()
        }
        assertEquals("Malformed IR signal: expected complete mark/space pairs.", error.message)
    }

    @Test
    fun rejectsNonPositiveTimingAsMalformed() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            IrTransmission(38_000, listOf(900, 0)).validate()
        }
        assertEquals(
            "Malformed IR signal: every mark and space duration must be greater than 0 µs.",
            error.message,
        )
    }

    @Test
    fun fakeTransmitterRecordsOnlyValidatedTransmission() {
        val fake = FakeIrTransmitter()
        val signal = IrTransmission(38_000, listOf(900, 450))

        fake.transmit(signal)

        assertEquals(listOf(signal), fake.transmissions)
        assertThrows(IllegalArgumentException::class.java) {
            fake.transmit(IrTransmission(38_000, listOf(-1, 450)))
        }
        assertEquals(listOf(signal), fake.transmissions)
    }
}
