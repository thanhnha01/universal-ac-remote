package com.thanhnha.universalacremote.ir

data class FlipperRawCommand(val name: String, val transmission: IrTransmission)

object FlipperRawImporter {
    private const val MAX_FILE_CHARS = 1_000_000
    private const val MAX_TIMINGS = 4096

    fun parse(text: String): List<FlipperRawCommand> {
        require(text.length <= MAX_FILE_CHARS) { "Flipper .ir file is too large." }
        val entries = mutableListOf<MutableMap<String, String>>()
        var current = linkedMapOf<String, String>()
        var sawHeader = false
        text.lineSequence().forEachIndexed { index, original ->
            val line = original.trim()
            if (line.isEmpty()) return@forEachIndexed
            if (line == "#" || line == "---") {
                if (current.isNotEmpty()) entries += current
                current = linkedMapOf()
                return@forEachIndexed
            }
            if (line.startsWith("#")) return@forEachIndexed
            val colon = line.indexOf(':')
            require(colon > 0) { "Invalid Flipper .ir format at line ${index + 1}." }
            val key = line.substring(0, colon).trim().lowercase()
            val value = line.substring(colon + 1).trim()
            if (key == "filetype") {
                require(value == "IR signals") { "Unsupported Flipper file type at line ${index + 1}." }
                sawHeader = true
            }
            require(key !in current) { "Duplicate '$key' field at line ${index + 1}." }
            current[key] = value
        }
        if (current.isNotEmpty()) entries += current
        require(sawHeader) { "Missing Flipper IR file header." }

        val commands = entries.mapNotNull { entry ->
            val type = entry["type"] ?: return@mapNotNull null // Header/document metadata, not a signal entry.
            if (type != "raw") return@mapNotNull null
            val frequencyText = entry["frequency"] ?: error("Raw entry is missing frequency.")
            val frequency = frequencyText.toIntOrNull() ?: error("Raw entry frequency is invalid.")
            require(frequency > 0) { "Raw entry frequency must be greater than 0 Hz." }
            val data = entry["data"] ?: error("Raw entry data is empty.")
            val tokens = data.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
            require(tokens.isNotEmpty()) { "Raw entry data is empty." }
            require(tokens.size <= MAX_TIMINGS) { "Raw entry has too many timing values." }
            val timings = tokens.map { token ->
                val timing = token.toIntOrNull() ?: error("Raw entry contains an invalid timing value: '$token'.")
                require(timing > 0) { "Raw entry timing values must be greater than 0 µs." }
                timing
            }
            val transmission = IrTransmission(frequency, timings)
            transmission.validate()
            FlipperRawCommand(entry["name"]?.takeIf(String::isNotBlank) ?: "", transmission)
        }
        require(commands.isNotEmpty()) { "No Flipper RAW entries found in the file." }
        return commands.mapIndexed { index, command ->
            if (command.name.isBlank()) command.copy(name = "Raw command ${index + 1}") else command
        }
    }
}
