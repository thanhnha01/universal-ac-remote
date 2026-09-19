package com.thanhnha.universalacremote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteResolverTest {
    private fun profile(id: String, brand: String = "Acme", ac: String? = null, remote: String? = null, protocolModel: String? = null, evidence: List<String> = emptyList()) =
        RemoteCandidate(id, brand, ac, remote, "proto", protocolModel, "PROTOCOL", setOf("power", "mode:cool", "fan:auto"), evidence, 6)

    @Test fun exactRemoteAndAcModelAreRankedAheadOfBrand() {
        val resolver = RemoteResolver(listOf(profile("brand"), profile("remote", remote = "R1"), profile("ac", ac = "A1")))
        assertEquals("remote", resolver.resolve(RemoteQuery(remoteModel = "R1")).first().id)
        assertEquals("ac", resolver.resolve(RemoteQuery(acModel = "A1")).first().id)
        assertEquals(listOf("ac", "brand", "remote"), resolver.resolve(RemoteQuery(brand = "Acme")).map { it.id })
    }

    @Test fun exactModelCanResolveWithoutBrand() {
        assertEquals("match", RemoteResolver(listOf(profile("match", remote = "R1"))).resolve(RemoteQuery(remoteModel = "r1")).first().id)
    }

    @Test fun sameBrandFallbackIsReturnedWithEvidence() {
        val result = RemoteResolver(listOf(profile("brand"))).resolve(RemoteQuery(brand = "Acme")).single()
        assertTrue(result.evidence.any { it.contains("Same brand") })
    }

    @Test fun catalogOemEvidenceGetsItsOwnPriority() {
        val result = RemoteResolver(listOf(profile("oem", evidence = listOf("oem:rebrandOf=Acme"))))
            .resolve(RemoteQuery(brand = "Acme")).single()
        assertEquals(4, result.priority)
        assertTrue(result.evidence.any { it.startsWith("oem:") })
    }

    @Test fun aliasesAndModelSearchAreCaseAndPunctuationInsensitive() {
        val candidate = profile("midea", brand = "Midea", ac = "MS-12/ABC", remote = "RG57A6")
            .copy(aliases = listOf("MHI", "Mitsubishi Heavy Industries"))
        val resolver = RemoteResolver(listOf(candidate))
        assertEquals("midea", resolver.resolve(RemoteQuery(brand = "mitsubishi heavy-industries")).single().id)
        assertEquals("midea", resolver.resolve(RemoteQuery(acModel = "ms 12 abc")).single().id)
        assertEquals("midea", resolver.resolve(RemoteQuery(remoteModel = "rg-57 a6")).single().id)
    }

    @Test fun scannerCandidatesFilterByTransmitterAndMissingIdFallsBack() {
        val resolver = RemoteResolver(listOf(profile("usable"), profile("raw").copy(encodingType = "IMPORTED_RAW")))
        assertEquals(listOf("usable"), resolver.scannerCandidates { it.encodingType == "PROTOCOL" }.map { it.id })
        assertEquals(null, resolver.findById("removed-from-catalog"))
    }

    @Test fun transmittableExactModelRanksAboveUnsupportedExactModel() {
        val usable = profile("usable", ac = "A1")
        val unsupported = profile("unsupported", ac = "A1").copy(
            encodingType = "RAW_PROFILE", verificationStatus = "unsupported",
        )
        assertEquals(listOf("usable", "unsupported"), RemoteResolver(listOf(unsupported, usable))
            .resolve(RemoteQuery(acModel = "A1")).map { it.id })
    }

    @Test fun unknownSmartIrProfilesKeepDistinctDisplayIdentity() {
        val first = profile("smartir:100", brand = "Midea", ac = "Unknown").copy(
            id = "smartir:100", encodingType = "RAW_PROFILE", source = "smartir", sourceProfileId = "100",
        )
        val second = first.copy(id = "smartir:101", sourceProfileId = "101")
        assertEquals("Model chưa xác định · SmartIR #100", first.displayModelLabel())
        assertEquals("Model chưa xác định · SmartIR #101", second.displayModelLabel())
        assertEquals(listOf("smartir:100", "smartir:101"), RemoteResolver(listOf(first, second))
            .resolve(RemoteQuery(brand = "Midea")).map { it.id })
    }
    @Test fun scannerBrandsExposeAllEligibleBrandsNotOnlyPopularOnes() {
        val resolver = RemoteResolver(
            listOf(
                profile("a1", brand = "Daikin"),
                profile("a2", brand = "daikin"),
                profile("b", brand = "LG"),
                profile("blocked", brand = "Blocked"),
            )
        )
        assertEquals(
            listOf("Daikin", "LG"),
            resolver.scannerBrands { it.id != "blocked" },
        )
    }

    @Test fun longSupportedModelListsAreCompactedForCards() {
        val candidate = profile(
            "models",
            brand = "Daikin",
            ac = "FTXS20LVMA, FTXS25LVMA, FTXS35LVMA, FTXS46LVMA",
        )
        assertEquals("FTXS20LVMA, FTXS25LVMA  +2 model", candidate.compactDisplayModelLabel())
    }

}
