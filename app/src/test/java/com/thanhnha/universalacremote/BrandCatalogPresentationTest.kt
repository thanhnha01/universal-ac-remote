package com.thanhnha.universalacremote

import com.thanhnha.universalacremote.ir.RemoteCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class BrandCatalogPresentationTest {
    private fun profile(id: String, brand: String, model: String?) = RemoteCandidate(
        id = id,
        brand = brand,
        acModel = model,
        remoteModel = null,
        protocolId = null,
        protocolModel = null,
        encodingType = "PROTOCOL",
        capabilities = setOf("power"),
        evidence = emptyList(),
        priority = 0,
    )

    @Test
    fun brandSections_groupsAlphabetically() {
        val sections = brandSections(listOf("Samsung", "Daikin", "Gree", "Daikin"))
        assertEquals(listOf('D','G','S'), sections.map { it.letter })
        assertEquals(listOf("Daikin"), sections.first().brands)
    }

    @Test
    fun modelSeriesGroups_groupsByModelPrefix() {
        val groups = modelSeriesGroups(
            listOf(
                profile("1", "Daikin", "FTXM35"),
                profile("2", "Daikin", "FTXM25"),
                profile("3", "Daikin", "CTXM25"),
                profile("4", "Daikin", null),
            ),
        )
        assertEquals(
            listOf("Dòng CTXM", "Dòng FTXM", "Remote / profile khác"),
            groups.map { it.name },
        )
        assertEquals(2, groups.first { it.name == "Dòng FTXM" }.profiles.size)
    }
}
