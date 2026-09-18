package com.thanhnha.universalacremote.update

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AppUpdate(val versionCode: Int, val versionName: String, val apkUrl: String, val sha256: String, val releaseNotes: String?)

object UpdateManifestParser {
    fun parse(json: String): AppUpdate {
        val obj = JSONObject(json)
        val versionCode = obj.getInt("versionCode")
        require(versionCode > 0) { "versionCode must be positive" }
        val versionName = obj.getString("versionName").also { require(it.isNotBlank()) }
        val apkUrl = obj.getString("apkUrl").also {
            val uri = URL(it)
            require(uri.protocol == "https" && (uri.host == "github.com" || uri.host.endsWith(".githubusercontent.com")))
        }
        val sha256 = obj.getString("sha256").lowercase()
        require(sha256.matches(Regex("[0-9a-f]{64}"))) { "Invalid SHA-256" }
        return AppUpdate(versionCode, versionName, apkUrl, sha256, obj.optString("releaseNotes").takeIf(String::isNotBlank))
    }
}

fun hasNewVersion(latestVersionCode: Int, installedVersionCode: Int) = latestVersionCode > installedVersionCode
fun isStableRelease(draft: Boolean, prerelease: Boolean) = !draft && !prerelease

fun sha256(file: File): String = file.inputStream().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

fun verifySha256(file: File, expected: String): Boolean = sha256(file).equals(expected, ignoreCase = true)

class GitHubReleaseClient(
    private val owner: String = "thanhnha01",
    private val repo: String = "universal-ac-remote",
) {
    fun latestStableUpdate(): AppUpdate? {
        val releases = getJson("https://api.github.com/repos/$owner/$repo/releases?per_page=100") as? org.json.JSONArray
            ?: error("Unexpected releases response")
        for (index in 0 until releases.length()) {
            val release = releases.getJSONObject(index)
            if (!isStableRelease(release.optBoolean("draft"), release.optBoolean("prerelease"))) continue
            val assets = release.optJSONArray("assets") ?: continue
            val manifest = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name") == "update.json" } ?: continue
            val parsed = UpdateManifestParser.parse(getText(manifest.getString("browser_download_url")))
            val notes = release.optString("body").takeIf(String::isNotBlank)
            return parsed.copy(releaseNotes = parsed.releaseNotes ?: notes)
        }
        return null
    }

    fun downloadApk(update: AppUpdate, destination: File) {
        destination.parentFile?.mkdirs()
        val connection = URL(update.apkUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", "application/octet-stream")
        connection.inputStream.use { input -> destination.outputStream().use(input::copyTo) }
        connection.disconnect()
        require(verifySha256(destination, update.sha256)) {
            destination.delete()
            "Downloaded APK SHA-256 does not match update.json"
        }
    }

    private fun getJson(url: String): Any? = org.json.JSONTokener(getText(url)).nextValue()

    private fun getText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Universal-AC-Remote-Android")
        return try { connection.inputStream.bufferedReader().use { it.readText() } } finally { connection.disconnect() }
    }
}
