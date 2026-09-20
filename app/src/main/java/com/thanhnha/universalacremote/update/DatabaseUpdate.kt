package com.thanhnha.universalacremote.update

import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class DatabaseUpdateManifest(
    val databaseVersion: Int,
    val schemaVersion: Int,
    val minAppVersionCode: Int,
    val fileName: String,
    val sha256: String,
    val signatureBase64: String,
)

@OptIn(ExperimentalEncodingApi::class)
object DatabaseUpdateManifestParser {
    fun parse(json: String): DatabaseUpdateManifest {
        val obj = JSONObject(json)
        val databaseVersion = obj.getInt("databaseVersion").also { require(it > 0) }
        val schemaVersion = obj.getInt("schemaVersion").also { require(it > 0) }
        val minAppVersionCode = obj.getInt("minAppVersionCode").also { require(it > 0) }
        val fileName = obj.getString("fileName").also {
            require(it.matches(Regex("ir-database-v[0-9]+\\.json"))) { "Invalid database filename" }
        }
        val sha256 = obj.getString("sha256").lowercase().also {
            require(it.matches(Regex("[0-9a-f]{64}"))) { "Invalid database SHA-256" }
        }
        val signature = obj.getString("signature").also {
            require(runCatching { Base64.Default.decode(it) }.getOrNull()?.isNotEmpty() == true) { "Invalid signature" }
        }
        return DatabaseUpdateManifest(databaseVersion, schemaVersion, minAppVersionCode, fileName, sha256, signature)
    }
}

@OptIn(ExperimentalEncodingApi::class)
class DatabaseUpdateInstaller(
    private val verifySignature: (payload: ByteArray, signature: ByteArray) -> Boolean,
) {
    fun install(
        manifest: DatabaseUpdateManifest,
        payload: ByteArray,
        installedAppVersionCode: Int,
        target: File,
    ) {
        require(installedAppVersionCode >= manifest.minAppVersionCode) { "App version is not compatible with this database." }
        require(payload.size <= 64 * 1024 * 1024) { "Database payload is too large." }
        require(payload.sha256Hex().equals(manifest.sha256, ignoreCase = true)) { "Database SHA-256 mismatch." }
        val signature = Base64.Default.decode(manifest.signatureBase64)
        require(verifySignature(payload, signature)) { "Database signature verification failed." }

        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".tmp")
        val backup = File(target.parentFile, target.name + ".bak")
        try {
            temp.writeBytes(payload)
            if (target.exists()) {
                backup.delete()
                require(target.renameTo(backup)) { "Could not create database rollback copy." }
            }
            require(temp.renameTo(target)) { "Could not activate database update." }
            backup.delete()
        } catch (failure: Throwable) {
            temp.delete()
            if (!target.exists() && backup.exists()) backup.renameTo(target)
            throw failure
        }
    }
}

private fun ByteArray.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }