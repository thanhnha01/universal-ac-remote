package com.thanhnha.universalacremote

import android.content.Context
import com.thanhnha.universalacremote.ir.ScannerSnapshot
import com.thanhnha.universalacremote.ir.VerificationCheck
import com.thanhnha.universalacremote.ir.VerificationStatus
import org.json.JSONObject

class ScannerSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("scanner-session-v2", Context.MODE_PRIVATE)

    fun save(brand: String?, snapshot: ScannerSnapshot) {
        val statuses = JSONObject().apply {
            snapshot.verificationStatuses.forEach { (check, status) ->
                put(check.name, status.name)
            }
        }
        prefs.edit()
            .putString("brand", brand.orEmpty())
            .putInt("cursor", snapshot.cursor)
            .putString("selectedCandidateId", snapshot.selectedCandidateId)
            .putString("verificationStatuses", statuses.toString())
            .apply()
    }

    fun load(candidateIds: Set<String>): Pair<String?, ScannerSnapshot>? {
        if (!prefs.contains("cursor")) return null
        val selected = prefs.getString("selectedCandidateId", null)
            ?.takeIf(candidateIds::contains)
        val rawStatuses = prefs.getString("verificationStatuses", "{}").orEmpty()
        val statuses = runCatching {
            val json = JSONObject(rawStatuses)
            buildMap {
                json.keys().forEach { key ->
                    val check = runCatching { VerificationCheck.valueOf(key) }.getOrNull()
                    val status = runCatching { VerificationStatus.valueOf(json.getString(key)) }.getOrNull()
                    if (check != null && status != null) put(check, status)
                }
            }
        }.getOrDefault(emptyMap())
        return prefs.getString("brand", null)?.takeIf(String::isNotBlank) to ScannerSnapshot(
            cursor = prefs.getInt("cursor", 0),
            selectedCandidateId = selected,
            verificationStatuses = statuses,
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
