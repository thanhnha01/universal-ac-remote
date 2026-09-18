package com.thanhnha.universalacremote.ir

/** Generic JNI adapter for the pinned IRremoteESP8266 IRac interface. */
object NativeAcEncoder {
    init { System.loadLibrary("irremote_ac_protocols") }

    fun encodeAc(protocolId: String, modelId: String? = null, acState: AcState): IrTransmission {
        val definition = ProtocolRegistry.requireSupported(protocolId, acState)
        val selectedModelId = ProtocolRegistry.modelId(definition, modelId)
        val native = nativeEncodeAc(definition.upstreamProtocol, selectedModelId,
            acState.power, acState.temperatureCelsius, acState.mode.nativeValue,
            acState.fan.nativeValue, acState.swingVertical, acState.swingHorizontal)
        require(native.size > 2) { "Native waveform generation returned no data." }
        return IrTransmission(
            native[0], native.drop(1), protocolId = protocolId, modelId = selectedModelId,
            supportedCapabilities = definition.capabilities,
        ).also(IrTransmission::validate)
    }

    private external fun nativeEncodeAc(
        protocol: String, model: String?, power: Boolean, temperatureCelsius: Int,
        mode: Int, fan: Int, swingVertical: Boolean, swingHorizontal: Boolean,
    ): IntArray
}
