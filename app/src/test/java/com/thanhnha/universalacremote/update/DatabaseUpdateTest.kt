package com.thanhnha.universalacremote.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.Base64

class DatabaseUpdateTest {
    @Test
    fun manifestAndInstaller_validateBeforeAtomicInstall() {
        val payload = "{\"profiles\":[]}".toByteArray()
        val sha = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }
        val signature = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))
        val manifest = DatabaseUpdateManifestParser.parse(
            """{"databaseVersion":2,"schemaVersion":1,"minAppVersionCode":1,"fileName":"ir-database-v2.json","sha256":"$sha","signature":"$signature"}""",
        )
        val dir = createTempDir(prefix = "db-update-")
        val target = File(dir, "ir-database.json")
        target.writeText("old")

        DatabaseUpdateInstaller { _, sig -> sig.contentEquals(byteArrayOf(1, 2, 3)) }
            .install(manifest, payload, 1, target)

        assertEquals(String(payload), target.readText())
        assertTrue(!File(dir, "ir-database.json.bak").exists())
    }

    @Test
    fun badSignature_doesNotReplaceCurrentDatabase() {
        val payload = "new".toByteArray()
        val sha = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }
        val manifest = DatabaseUpdateManifest(2, 1, 1, "ir-database-v2.json", sha, Base64.getEncoder().encodeToString(byteArrayOf(9)))
        val dir = createTempDir(prefix = "db-update-")
        val target = File(dir, "ir-database.json")
        target.writeText("old")

        val result = runCatching { DatabaseUpdateInstaller { _, _ -> false }.install(manifest, payload, 1, target) }

        assertTrue(result.isFailure)
        assertEquals("old", target.readText())
    }
}