package com.thanhnha.universalacremote

data class RoomSummary(val name: String, val count: Int)

internal fun SavedRemote.roomLabel(): String =
    roomName.trim().ifBlank { "Chưa phân phòng" }

internal fun favoriteRemotes(remotes: List<SavedRemote>): List<SavedRemote> =
    remotes.filter(SavedRemote::favorite)
        .sortedWith(compareByDescending<SavedRemote> { it.lastUsedAtEpochMs }.thenBy { it.displayName.lowercase() })

internal fun recentRemotes(remotes: List<SavedRemote>, limit: Int = 4): List<SavedRemote> =
    remotes.asSequence()
        .filter { it.lastUsedAtEpochMs > 0L }
        .sortedByDescending { it.lastUsedAtEpochMs }
        .take(limit)
        .toList()

internal fun roomSummaries(remotes: List<SavedRemote>): List<RoomSummary> =
    remotes.groupingBy(SavedRemote::roomLabel)
        .eachCount()
        .map { RoomSummary(it.key, it.value) }
        .sortedWith(compareBy<RoomSummary> { it.name == "Chưa phân phòng" }.thenBy { it.name.lowercase() })
