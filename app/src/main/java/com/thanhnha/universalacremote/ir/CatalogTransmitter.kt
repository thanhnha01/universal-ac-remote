package com.thanhnha.universalacremote.ir

object CatalogTransmitter {
    fun protocol(candidate: RemoteCandidate): ProtocolDefinition? {
        if (!candidate.encodingType.equals("PROTOCOL", true)) return null
        val id = candidate.protocolId ?: return null
        return ProtocolRegistry.protocols[id] ?: ProtocolRegistry.protocols.values.firstOrNull {
            it.upstreamProtocol.equals(id, ignoreCase = true)
        }
    }

    fun supports(candidate: RemoteCandidate): Boolean = protocol(candidate) != null

    fun modelId(candidate: RemoteCandidate, definition: ProtocolDefinition): String? {
        val catalogModels = candidate.protocolModel.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty)
        val firstSupported = catalogModels.firstOrNull { it in definition.modelIds }
        return firstSupported ?: definition.defaultModel
    }

    fun mode(value: String): AcMode? = when (value.lowercase()) {
        "auto" -> AcMode.AUTO
        "cool" -> AcMode.COOL
        "dry" -> AcMode.DRY
        "fan" -> AcMode.FAN
        "heat" -> AcMode.HEAT
        else -> null
    }

    fun fan(value: String): AcFan? = when (value.lowercase()) {
        "auto" -> AcFan.AUTO
        "min", "low" -> AcFan.MIN
        "medium", "mid" -> AcFan.MEDIUM
        "high" -> AcFan.HIGH
        else -> null
    }

    fun encode(candidate: RemoteCandidate, state: AcState): IrTransmission {
        val definition = protocol(candidate) ?: error("This catalog profile has no compatible on-device transmitter.")
        return NativeAcEncoder.encodeAc(definition.id, modelId(candidate, definition), state)
    }
}
