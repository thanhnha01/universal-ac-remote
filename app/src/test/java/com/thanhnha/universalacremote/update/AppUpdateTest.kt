package com.thanhnha.universalacremote.update

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AppUpdateTest {
    private fun manifest(version: Int = 7, hash: String = "a".repeat(64)) = """
        {"versionCode":$version,"versionName":"1.0.0","apkFileName":"UniversalAC-v1.0.0.apk",
        "apkSha256":"$hash","publishedAt":"2026-09-18T00:00:00Z","releaseNotes":"Notes",
        "irDatabaseVersion":"catalog-1","upstreamRevisions":{}}
    """.trimIndent()

    @Test fun parsesReleaseManifestAndRequiresAssetUrl() {
        val parsed = UpdateManifestParser.parse(manifest(), "https://github.com/thanhnha01/universal-ac-remote/releases/download/v1.0.0/UniversalAC-v1.0.0.apk")
        assertEquals(7, parsed.versionCode)
        assertEquals("UniversalAC-v1.0.0.apk", parsed.apkFileName)
    }

    @Test fun rejectsMalformedManifestAndUntrustedUrl() {
        assertThrows(Exception::class.java) { UpdateManifestParser.parse("{}", "https://github.com/file.apk") }
        assertThrows(IllegalArgumentException::class.java) { UpdateManifestParser.parse(manifest(), "http://evil.example/file.apk") }
        assertThrows(IllegalArgumentException::class.java) { UpdateManifestParser.parse(manifest(hash = "bad"), "https://github.com/file.apk") }
    }

    @Test fun versionComparisonAndStableReleaseFiltering() {
        assertFalse(hasNewVersion(10, 10))
        assertFalse(hasNewVersion(9, 10))
        assertTrue(hasNewVersion(11, 10))
        assertTrue(isStableRelease(false, false))
        assertFalse(isStableRelease(true, false))
        assertFalse(isStableRelease(false, true))
    }

    @Test fun shaMismatchRejectedAndMatchingHashAccepted() {
        val file = File.createTempFile("update-test", ".apk")
        try {
            file.writeText("fixture APK")
            val actual = sha256(file)
            assertTrue(verifySha256(file, actual))
            assertFalse(verifySha256(file, "0".repeat(64)))
        } finally { file.delete() }
    }
}
