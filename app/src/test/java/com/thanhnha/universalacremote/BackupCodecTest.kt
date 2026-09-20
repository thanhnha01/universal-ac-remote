package com.thanhnha.universalacremote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun roundTrip_preservesSavedRemoteData() {
        val remote = SavedRemote(
            id = "id-1",
            displayName = "Phòng ngủ",
            catalogProfileId = "profile-1",
            brand = "Daikin",
            acModel = "FTXM35",
            remoteModel = "ARC",
            protocolId = "daikin",
            protocolModel = "DAIKIN",
            verifiedCapabilities = listOf("POWER", "MODE"),
            roomName = "Phòng ngủ",
            favorite = true,
            lastUsedAtEpochMs = 1234L,
            lastSentStateJson = "{\"power\":true}",
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(listOf(remote)))
        assertEquals(listOf(remote), decoded.remotes)
    }

    @Test
    fun duplicateIds_areRejected() {
        val remote = SavedRemote("same", "A", "p", "LG", null, null, null, null, emptyList())
        val json = BackupCodec.encode(listOf(remote, remote))
        assertTrue(runCatching { BackupCodec.decode(json) }.isFailure)
    }
}
