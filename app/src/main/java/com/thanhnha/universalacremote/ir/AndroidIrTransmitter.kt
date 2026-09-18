package com.thanhnha.universalacremote.ir

import android.content.Context
import android.hardware.ConsumerIrManager

class AndroidIrTransmitter(
    private val manager: ConsumerIrManager?,
) : IrTransmitter {
    override fun transmit(transmission: IrTransmission) {
        transmission.validate()
        val irManager = manager ?: error("This device does not provide Consumer IR service.")
        check(irManager.hasIrEmitter()) { "This device does not report an IR emitter." }

        val supportedRanges = irManager.carrierFrequencies.orEmpty()
        check(supportedRanges.any { transmission.carrierFrequencyHz in it.minFrequency..it.maxFrequency }) {
            "Carrier frequency ${transmission.carrierFrequencyHz} Hz is outside the device's reported ranges."
        }

        irManager.transmit(transmission.carrierFrequencyHz, transmission.timingsMicros.toIntArray())
    }

    companion object {
        fun from(context: Context): AndroidIrTransmitter {
            val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
            return AndroidIrTransmitter(manager)
        }
    }
}
