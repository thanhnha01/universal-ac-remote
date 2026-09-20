package com.thanhnha.universalacremote

import com.thanhnha.universalacremote.ir.RemoteCandidate
import com.thanhnha.universalacremote.ir.compactDisplayModelLabel

data class BrandSection(val letter: Char, val brands: List<String>)
data class ModelSeriesGroup(val name: String, val profiles: List<RemoteCandidate>)

internal fun brandSections(brands: List<String>): List<BrandSection> =
    brands.asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { it.lowercase() }
        .groupBy { it.first().uppercaseChar().takeIf(Char::isLetter) ?: '#' }
        .toSortedMap(compareBy<Char> { it == '#' }.thenBy { it })
        .map { (letter, values) ->
            BrandSection(letter, values.sortedWith(String.CASE_INSENSITIVE_ORDER))
        }

internal fun modelSeriesGroups(profiles: List<RemoteCandidate>): List<ModelSeriesGroup> =
    profiles.groupBy(::seriesName)
        .map { (name, values) ->
            ModelSeriesGroup(
                name = name,
                profiles = values.sortedWith(
                    compareBy<RemoteCandidate> { it.compactDisplayModelLabel().lowercase() }
                        .thenBy { it.remoteModel.orEmpty().lowercase() }
                        .thenBy { it.id },
                ),
            )
        }
        .sortedBy { it.name.lowercase() }

private fun seriesName(candidate: RemoteCandidate): String {
    val source = candidate.acModel
        ?.split(',', '/', ';')
        ?.firstOrNull()
        ?.trim()
        .orEmpty()
    if (source.isBlank() || source.equals("unknown", true)) return "Remote / profile khác"

    val normalized = source.uppercase()
    val prefix = normalized.takeWhile { it.isLetter() || it == '-' }
        .trim('-')
        .takeIf(String::isNotBlank)
    return prefix?.let { "Dòng $it" } ?: "Các model khác"
}
