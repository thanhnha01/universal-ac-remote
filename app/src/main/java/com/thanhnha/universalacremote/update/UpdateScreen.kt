package com.thanhnha.universalacremote.update

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.thanhnha.universalacremote.BuildConfig
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
fun UpdatePanel() {
    val context = LocalContext.current
    val client = remember { GitHubReleaseClient() }
    var update by remember { mutableStateOf<AppUpdate?>(null) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val preferences = remember { context.getSharedPreferences("app_updates", 0) }
    val checkForUpdate: () -> Unit = {
        busy = true
        executor.execute {
            val result = runCatching { client.latestStableUpdate() }
            android.os.Handler(context.mainLooper).post {
                busy = false
                result.onSuccess { found ->
                    update = found?.takeIf { hasNewVersion(it.versionCode, BuildConfig.VERSION_CODE) }
                    message = when {
                        found == null -> "Không tìm thấy release stable có update.json."
                        update == null -> "Ứng dụng đang ở phiên bản mới nhất."
                        else -> "Có bản cập nhật mới."
                    }
                }.onFailure { message = "Không thể kiểm tra cập nhật. Hãy thử lại khi có mạng." }
                preferences.edit().putLong("last_check_ms", System.currentTimeMillis()).apply()
            }
        }
    }
    LaunchedEffect(Unit) {
        val lastCheck = preferences.getLong("last_check_ms", 0L)
        if (System.currentTimeMillis() - lastCheck >= TimeUnit.HOURS.toMillis(24)) checkForUpdate()
    }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = checkForUpdate, enabled = !busy) { Text(if (busy) "Đang kiểm tra…" else "Kiểm tra cập nhật") }
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
        update?.let { available ->
            Text("Phiên bản ${available.versionName} đã sẵn sàng.", style = MaterialTheme.typography.titleSmall)
            available.releaseNotes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = {
                busy = true
                executor.execute {
                    val apk = File(context.cacheDir, "updates/update-${available.versionCode}.apk")
                    val result = runCatching { client.downloadApk(available, apk) }
                    android.os.Handler(context.mainLooper).post {
                        busy = false
                        result.onSuccess {
                            if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                                message = "Cho phép cài ứng dụng từ nguồn này, sau đó chọn Cập nhật lần nữa."
                            } else {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
                                context.startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            }
                        }.onFailure { message = "Tải/cài đặt thất bại: ${it.message ?: "lỗi không xác định"}" }
                    }
                }
            }, enabled = !busy) { Text("Cập nhật") }
        }
    }
}
