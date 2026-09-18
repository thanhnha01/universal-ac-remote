package com.thanhnha.universalacremote.ir

class FakeIrTransmitter : IrTransmitter {
    private val sent = mutableListOf<IrTransmission>()

    val transmissions: List<IrTransmission>
        get() = sent.toList()

    override fun transmit(transmission: IrTransmission) {
        transmission.validate()
        sent += transmission
    }
}
