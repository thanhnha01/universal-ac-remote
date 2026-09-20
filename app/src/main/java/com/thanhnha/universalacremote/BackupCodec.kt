package com.thanhnha.universalacremote

import org.json.JSONArray
import org.json.JSONObject

private const val BACKUP_SCHEMA_VERSION = 1
private const val MAX_BACKUP_REMOTES = 500

data class BackupBundle(val remotes: List<SavedRemote>)

object BackupCodec {
    fun encode(remotes: List<SavedRemote>): String {
        require(remotes.size <= MAX_BACKUP_REMOTES) { "Too many saved remotes." }
        return JSONObject()
            .put("schemaVersion", BACKUP_SCHEMA_VERSION)
            .put("app", "Universal A/C Remote")
            .put("remotes", JSONArray().apply {
                remotes.forEach { remote ->
                    put(
                        JSONObject()
                            .put("id", remote.id)
                            .put("displayName", remote.displayName)
                            .put("catalogProfileId", remote.catalogProfileId)
                            .put("brand", remote.brand)
                            .put("acModel", remote.acModel)
                            .put("remoteModel", remote.remoteModel)
                            .put("protocolId", remote.protocolId)
                            .put("protocolModel", remote.protocolModel)
                            .put("verifiedCapabilities", JSONArray(remote.verifiedCapabilities))
                            .put("importedCommandsJson", remote.importedCommandsJson)
                            .put("roomName", remote.roomName)
                            .put("favorite", remote.favorite)
                            .put("lastUsedAtEpochMs", remote.lastUsedAtEpochMs)
                            .put("lastSentStateJson", remote.lastSentStateJson)
                    )
                }
            })
            .toString()
    }

    fun decode(json: String): BackupBundle {
        require(json.length <= 8_000_000) { "Backup file is too large." }
        val root = JSONObject(json)
        require(root.getInt("schemaVersion") == BACKUP_SCHEMA_VERSION) { "Unsupported backup schema." }
        val array = root.getJSONArray("remotes")
        require(array.length() <= MAX_BACKUP_REMOTES) { "Too many saved remotes." }
        val remotes = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val verified = item.optJSONArray("verifiedCapabilities") ?: JSONArray()
            SavedRemote(
                id = item.getString("id").also { require(it.isNotBlank()) },
                displayName = item.getString("displayName").also { require(it.isNotBlank()) },
                catalogProfileId = item.getString("catalogProfileId"),
                brand = item.getString("brand"),
                acModel = item.optNullableString("acModel"),
                remoteModel = item.optNullableString("remoteModel"),
                protocolId = item.optNullableString("protocolId"),
                protocolModel = item.optNullableString("protocolModel"),
                verifiedCapabilities = (0 until verified.length()).map(verified::getString),
                importedCommandsJson = item.optString("importedCommandsJson"),
                roomName = item.optString("roomName"),
                favorite = item.optBoolean("favorite", false),
                lastUsedAtEpochMs = item.optLong("lastUsedAtEpochMs", 0L).coerceAtLeast(0L),
                lastSentStateJson = item.optString("lastSentStateJson"),
            )
        }
        require(remotes.map(SavedRemote::id).distinct().size == remotes.size) { "Duplicate remote IDs in backup." }
        return BackupBundle(remotes)
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)
}
