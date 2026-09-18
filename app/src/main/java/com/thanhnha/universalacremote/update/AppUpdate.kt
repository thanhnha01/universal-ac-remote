package com.thanhnha.universalacremote.update

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkFileName: String,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String?,
)

object UpdateManifestParser {
    fun parse(json: String, apkUrl: String): AppUpdate {
        val obj = JSONObject(json)
        val versionCode = obj.getInt("versionCode")
        require(versionCode > 0) { "versionCode must be positive" }
        val versionName = obj.getString("versionName").also { require(it.isNotBlank()) }
        val fileName = obj.getString("apkFileName")
        require(fileName.matches(Regex("UniversalAC-v[0-9]+\\.[0-9]+\\.[0-9]+\\.apk"))) { "Invalid APK filename" }
        val apkSha = obj.getString("apkSha256").lowercase()
        require(apkSha.matches(Regex("[0-9a-f]{64}"))) { "Invalid APK SHA-256" }
        requireAllowedGitHubUrl(apkUrl)
        return AppUpdate(versionCode, versionName, fileName, apkUrl, apkSha,
            obj.optString("releaseNotes").takeIf(String::isNotBlank))
    }

    internal fun requireAllowedGitHubUrl(value: String) {
        val url = URL(value)
        require(url.protocol == "https" && (url.host == "github.com" || url.host.endsWith(".githubusercontent.com"))) {
            "Update URL is not an allowed GitHub HTTPS URL"
        }
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
            val byName = (0 until assets.length()).associateBy { assets.getJSONObject(it).optString("name") }
            val manifest = byName["update.json"]?.let(assets::getJSONObject) ?: continue
            val manifestUrl = manifest.getString("browser_download_url")
            UpdateManifestParser.run {
                // Validate both asset URLs before fetching release-controlled metadata.
                requireAllowedGitHubUrl(manifestUrl)
            }
            val json = getText(manifestUrl)
            val obj = JSONObject(json)
            val apkName = obj.getString("apkFileName")
            val apkAsset = byName[apkName]?.let(assets::getJSONObject) ?: continue
            val parsed = UpdateManifestParser.parse(json, apkAsset.getString("browser_download_url"))
            return parsed.copy(releaseNotes = parsed.releaseNotes ?: release.optString("body").takeIf(String::isNotBlank))
        }
        return null
    }

    fun downloadApk(update: AppUpdate, destination: File) {
        destination.parentFile?.mkdirs()
        val connection = URL(update.apkUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/octet-stream")
        try {
            connection.inputStream.use { input -> destination.outputStream().use(input::copyTo) }
            val finalUrl = connection.url
            require(finalUrl.protocol == "https" && (finalUrl.host == "github.com" || finalUrl.host.endsWith(".githubusercontent.com"))) {
                "APK download redirected outside GitHub"
            }
            require(verifySha256(destination, update.sha256)) { "Downloaded APK SHA-256 does not match update.json" }
        } catch (failure: Throwable) {
            destination.delete()
            throw failure
        } finally {
            connection.disconnect()
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
