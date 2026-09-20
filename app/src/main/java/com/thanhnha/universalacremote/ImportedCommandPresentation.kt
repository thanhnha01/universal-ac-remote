package com.thanhnha.universalacremote

import com.thanhnha.universalacremote.ir.FlipperRawCommand

enum class ImportedCommandGroup(val order: Int) {
    POWER(0),
    TEMPERATURE(1),
    MODE(2),
    FAN(3),
    SWING(4),
    TIMER(5),
    LIGHT(6),
    OTHER(7),
}

data class ImportedCommandPresentation(
    val command: FlipperRawCommand,
    val label: String,
    val group: ImportedCommandGroup,
)

fun smartImportedCommands(commands: List<FlipperRawCommand>): List<ImportedCommandPresentation> =
    commands.mapIndexed { index, command ->
        val raw = command.name.trim()
        val normalized = raw.lowercase().replace('-', '_').replace(' ', '_')
        val presentation = when {
            listOf("power_on", "on").any(normalized::contains) ->
                "Bật" to ImportedCommandGroup.POWER
            listOf("power_off", "off").any(normalized::contains) ->
                "Tắt" to ImportedCommandGroup.POWER
            "power" in normalized ->
                "Nguồn" to ImportedCommandGroup.POWER
            listOf("temp_up", "temperature_up", "temp_plus", "warmer").any(normalized::contains) ->
                "Nhiệt độ +" to ImportedCommandGroup.TEMPERATURE
            listOf("temp_down", "temperature_down", "temp_minus", "cooler").any(normalized::contains) ->
                "Nhiệt độ −" to ImportedCommandGroup.TEMPERATURE
            "temp" in normalized || "temperature" in normalized ->
                "Nhiệt độ" to ImportedCommandGroup.TEMPERATURE
            "mode" in normalized ->
                "Chế độ" to ImportedCommandGroup.MODE
            "fan" in normalized ->
                "Quạt" to ImportedCommandGroup.FAN
            "swing" in normalized ->
                if ("horizontal" in normalized || normalized.endsWith("_h")) "Swing ngang" to ImportedCommandGroup.SWING
                else if ("vertical" in normalized || normalized.endsWith("_v")) "Swing dọc" to ImportedCommandGroup.SWING
                else "Swing" to ImportedCommandGroup.SWING
            "timer" in normalized ->
                "Hẹn giờ" to ImportedCommandGroup.TIMER
            "light" in normalized || "led" in normalized ->
                "Đèn" to ImportedCommandGroup.LIGHT
            else ->
                raw.ifBlank { "Lệnh IR " + (index + 1) } to ImportedCommandGroup.OTHER
        }
        ImportedCommandPresentation(command, presentation.first, presentation.second)
    }.sortedWith(
        compareBy<ImportedCommandPresentation> { it.group.order }
            .thenBy { it.label.lowercase() }
            .thenBy { it.command.name.lowercase() },
    )
