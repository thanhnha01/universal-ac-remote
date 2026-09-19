package com.thanhnha.universalacremote.ir

import org.json.JSONObject

object CatalogTransmitter {
    fun protocol(candidate: RemoteCandidate): ProtocolDefinition? {
        if (!candidate.encodingType.equals("PROTOCOL", true)) return null
        val id = candidate.protocolId ?: return null
        return ProtocolRegistry.protocols[id] ?: ProtocolRegistry.protocols.values.firstOrNull {
            it.upstreamProtocol.equals(id, ignoreCase = true)
        }
    }

    fun supports(candidate: RemoteCandidate): Boolean = when {
        protocol(candidate) != null -> true
        candidate.encodingType.equals("RAW_PROFILE", true) -> canDecodeSmartIr(candidate) && safeProbe(candidate) != null
        candidate.encodingType.equals("IMPORTED_RAW", true) -> firstImportedRaw(candidate) != null
        else -> false
    }

    fun safeProbe(candidate: RemoteCandidate): SafeProbe? {
        explicitPowerOn(candidate)?.let { return SafeProbe(AcState(true, 24, AcMode.COOL, AcFan.AUTO), "Thử bật máy", transmission = it) }
        explicitToggle(candidate)?.let {
            return SafeProbe(
                AcState(true, 24, AcMode.COOL, AcFan.AUTO),
                "Thử tín hiệu hồ sơ",
                "Hồ sơ này chỉ có lệnh bật/tắt. Máy có thể đổi trạng thái khi thử.",
                it,
            )
        }
        val policy = SafeProbePolicy.forCandidate(candidate) ?: return null
        val controls = RemoteControls.from(candidate)
        val definition = protocol(candidate)
        val modes = (controls.modes.mapNotNull(::mode) + (definition?.modes?.toList() ?: emptyList()))
            .distinct().ifEmpty { listOf(policy.state.mode) }
        val fans = (controls.fanModes.mapNotNull(::fan) + (definition?.fanSpeeds?.toList() ?: emptyList()))
            .distinct().ifEmpty { listOf(policy.state.fan) }
        val range = controls.temperatureRange ?: definition?.let { it.minTemperatureCelsius..it.maxTemperatureCelsius }
            ?: return null
        val states = range.asSequence().flatMap { temperature ->
            modes.asSequence().flatMap { selectedMode -> fans.asSequence().map { selectedFan ->
                policy.state.copy(power = true, temperatureCelsius = temperature, mode = selectedMode, fan = selectedFan)
            } }
        }
        return states.mapNotNull { state ->
            runCatching { encode(candidate, state) }.getOrNull()?.let { SafeProbe(state, if (state.mode == AcMode.COOL) "Thử làm lạnh ${state.temperatureCelsius}°C" else policy.description, policy.warning) }
        }.firstOrNull()
    }

    fun encodeSafeProbe(candidate: RemoteCandidate): IrTransmission {
        val probe = safeProbe(candidate) ?: error("This profile has no safe probe.")
        return probe.transmission ?: encode(candidate, probe.state)
    }

    /** Returns only an explicitly named ON command; an OFF/toggle command is never selected here. */
    fun explicitPowerOn(candidate: RemoteCandidate): IrTransmission? = runCatching {
        if (!candidate.encodingType.equals("RAW_PROFILE", true)) return@runCatching null
        val commands = candidate.rawCommandsJson?.let(::JSONObject) ?: return@runCatching null
        val encoded = listOf("power_on", "powerOn", "on").asSequence()
            .mapNotNull { key -> commands.optString(key).takeIf(String::isNotBlank) }
            .firstOrNull() ?: return@runCatching null
        BroadlinkDecoder.decodeBase64(encoded)
    }.getOrNull()

    private fun explicitToggle(candidate: RemoteCandidate): IrTransmission? = runCatching {
        if (!candidate.encodingType.equals("RAW_PROFILE", true)) return@runCatching null
        val commands = candidate.rawCommandsJson?.let(::JSONObject) ?: return@runCatching null
        val encoded = commands.optString("toggle").takeIf(String::isNotBlank) ?: return@runCatching null
        BroadlinkDecoder.decodeBase64(encoded)
    }.getOrNull()

    fun modelId(candidate: RemoteCandidate, definition: ProtocolDefinition): String? {
        val catalogModels = candidate.protocolModel.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty)
        val firstSupported = catalogModels.firstOrNull { it in definition.modelIds }
        return firstSupported ?: definition.defaultModel
    }

    fun mode(value: String): AcMode? = when (value.lowercase()) {
        "auto" -> AcMode.AUTO
        "cool" -> AcMode.COOL
        "dry" -> AcMode.DRY
        "fan", "fan_only" -> AcMode.FAN
        "heat" -> AcMode.HEAT
        "heat_cool", "heatcool" -> AcMode.AUTO
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
        val definition = protocol(candidate)
        if (definition != null) return NativeAcEncoder.encodeAc(definition.id, modelId(candidate, definition), state)
        return when {
            candidate.encodingType.equals("RAW_PROFILE", true) -> encodeSmartIr(candidate, state)
            candidate.encodingType.equals("IMPORTED_RAW", true) -> firstImportedRaw(candidate)
                ?: error("This imported profile has no valid raw command.")
            else -> error("This catalog profile has no compatible on-device transmitter.")
        }
    }

    private fun canDecodeSmartIr(candidate: RemoteCandidate): Boolean = runCatching {
        val metadata = candidate.sourceMetadataJson?.let(::JSONObject)
        require(metadata?.optString("supportedController").equals("Broadlink", true))
        require(metadata?.optString("commandsEncoding").equals("Base64", true))
        val commands = candidate.rawCommandsJson?.let(::JSONObject) ?: error("Missing SmartIR commands.")
        val leaves = stringLeaves(commands)
        require(leaves.isNotEmpty())
        leaves.all { runCatching { BroadlinkDecoder.decodeBase64(it) }.isSuccess }
    }.getOrDefault(false)

    private fun encodeSmartIr(candidate: RemoteCandidate, state: AcState): IrTransmission {
        val metadata = candidate.sourceMetadataJson?.let(::JSONObject)
        require(metadata?.optString("supportedController").equals("Broadlink", true)) {
            "SmartIR profile does not declare Broadlink support."
        }
        require(metadata?.optString("commandsEncoding").equals("Base64", true)) {
            "SmartIR profile uses an unsupported command encoding."
        }
        val commands = candidate.rawCommandsJson?.let(::JSONObject) ?: error("Missing SmartIR commands.")
        val encoded = if (!state.power) {
            commands.optString("off").takeIf(String::isNotBlank)
                ?: unsupportedState("this SmartIR profile has no off command.")
        } else {
            val modeKey = smartIrModeKey(candidate, state.mode)
            val mode = commands.opt(modeKey) ?: unsupportedState("mode '$modeKey' is not present in this profile.")
            val fanKey = smartIrFanKey(candidate, state.fan)
            val fan = selectKey(mode, fanKey, "fan '$fanKey'")
            selectTemperature(fan, state.temperatureCelsius)
        }
        return BroadlinkDecoder.decodeBase64(encoded)
    }

    private fun smartIrModeKey(candidate: RemoteCandidate, mode: AcMode): String {
        val aliases = when (mode) {
            AcMode.COOL -> listOf("cool")
            AcMode.HEAT -> listOf("heat")
            AcMode.DRY -> listOf("dry")
            AcMode.FAN -> listOf("fan_only", "fan")
            AcMode.AUTO -> listOf("auto", "heat_cool", "heatcool")
        }
        return aliases.firstOrNull { alias -> candidate.operationModes.any { it.equals(alias, true) } }
            ?: unsupportedState("mode '${mode.name.lowercase()}' is not present in this profile.")
    }

    private fun smartIrFanKey(candidate: RemoteCandidate, fan: AcFan): String {
        val aliases = when (fan) {
            AcFan.AUTO -> listOf("auto")
            AcFan.MIN -> listOf("min", "low")
            AcFan.MEDIUM -> listOf("medium", "mid")
            AcFan.HIGH -> listOf("high")
        }
        return aliases.firstOrNull { alias -> candidate.fanModes.any { it.equals(alias, true) } }
            ?: unsupportedState("fan '${fan.name.lowercase()}' is not present in this profile.")
    }

    private fun selectKey(value: Any, key: String, description: String): Any {
        val objectValue = value as? JSONObject ?: unsupportedState("$description is not present in this profile.")
        return objectValue.opt(key) ?: objectValue.keys().asSequence().firstOrNull { it.equals(key, true) }
            ?.let(objectValue::opt) ?: unsupportedState("$description is not present in this profile.")
    }

    private fun selectTemperature(value: Any, temperature: Int): String {
        val objectValue = value as? JSONObject ?: unsupportedState("temperature $temperature is not present in this profile.")
        val exact = objectValue.keys().asSequence().firstOrNull { key -> key.toDoubleOrNull()?.let { it == temperature.toDouble() } == true }
        return (exact?.let(objectValue::opt) as? String)
            ?: unsupportedState("temperature $temperature is not present in this profile.")
    }

    private fun unsupportedState(detail: String): Nothing = throw IllegalArgumentException("Unsupported state: $detail")

    private fun stringLeaves(value: Any?): List<String> = when (value) {
        is String -> listOf(value)
        is JSONObject -> value.keys().asSequence().flatMap { stringLeaves(value.opt(it)).asSequence() }.toList()
        else -> emptyList()
    }

    private fun firstImportedRaw(candidate: RemoteCandidate): IrTransmission? = runCatching {
        val commands = candidate.rawCommandsJson?.let(::JSONObject) ?: return null
        val commandObjects = commands.keys().asSequence().mapNotNull { key -> commands.optJSONObject(key) }.toList()
        require(commandObjects.isNotEmpty())
        commandObjects.forEach { command ->
            val timings = command.optJSONArray("durationsMicros")?.let { array -> (0 until array.length()).map(array::getInt) }
                ?: error("Missing imported timing data.")
            IrTransmission(command.getInt("carrierFrequencyHz"), timings).validate()
        }
        val command = commandObjects.first()
        val timings = command.optJSONArray("durationsMicros")?.let { array -> (0 until array.length()).map(array::getInt) } ?: return null
        IrTransmission(command.getInt("carrierFrequencyHz"), timings).also(IrTransmission::validate)
    }.getOrNull()
}
