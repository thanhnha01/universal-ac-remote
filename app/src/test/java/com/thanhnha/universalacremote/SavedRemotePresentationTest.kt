package com.thanhnha.universalacremote

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedRemotePresentationTest {
    private fun remote(
        id: String,
        room: String = "",
        favorite: Boolean = false,
        lastUsed: Long = 0L,
    ) = SavedRemote(
        id = id,
        displayName = id,
        catalogProfileId = "profile:$id",
        brand = "Daikin",
        acModel = null,
        remoteModel = null,
        protocolId = null,
        protocolModel = null,
        verifiedCapabilities = emptyList(),
        roomName = room,
        favorite = favorite,
        lastUsedAtEpochMs = lastUsed,
    )

    @Test
    fun roomSummaries_groupBlankRoomLast() {
        val summaries = roomSummaries(
            listOf(
                remote("a", "Phòng ngủ"),
                remote("b", ""),
                remote("c", "Phòng ngủ"),
                remote("d", "Phòng khách"),
            ),
        )

        assertEquals(
            listOf(
                RoomSummary("Phòng khách", 1),
                RoomSummary("Phòng ngủ", 2),
                RoomSummary("Chưa phân phòng", 1),
            ),
            summaries,
        )
    }

    @Test
    fun favoriteAndRecent_areUsageOrdered() {
        val remotes = listOf(
            remote("old", favorite = true, lastUsed = 10),
            remote("new", favorite = true, lastUsed = 30),
            remote("other", lastUsed = 20),
        )

        assertEquals(listOf("new", "old"), favoriteRemotes(remotes).map { it.id })
        assertEquals(listOf("new", "other"), recentRemotes(remotes, 2).map { it.id })
    }
}
