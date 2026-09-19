package com.thanhnha.universalacremote.update

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.thanhnha.universalacremote.AppColors
import com.thanhnha.universalacremote.BuildConfig
import com.thanhnha.universalacremote.InfoBanner
import com.thanhnha.universalacremote.PrimaryButton
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
fun UpdatePanel() {
    val context = LocalContext.current
    val client = remember { GitHubReleaseClient() }
    var update by remember { mutableStateOf<AppUpdate?>(null) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val preferences = remember { context.getSharedPreferences("app_updates", 0) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdownNow() }
    }

    val checkForUpdate: () -> Unit = {
        busy = true
        executor.execute {
            val result = runCatching { client.latestStableUpdate() }
            android.os.Handler(context.mainLooper).post {
                busy = false
                result.onSuccess { found ->
                    update = found?.takeIf { hasNewVersion(it.versionCode, BuildConfig.VERSION_CODE) }
                    message = when {
                        found == null -> "Chưa có bản cập nhật mới."
                        update == null -> "Bạn đang dùng phiên bản mới nhất."
                        else -> "Có bản cập nhật mới ${found.versionName}."
                    }
                    isError = false
                }.onFailure {
                    message = "Không thể kiểm tra cập nhật. Hãy thử lại khi có mạng."
                    isError = true
                }
                preferences.edit().putLong("last_check_ms", System.currentTimeMillis()).apply()
            }
        }
    }

    LaunchedEffect(Unit) {
        val lastCheck = preferences.getLong("last_check_ms", 0L)
        if (System.currentTimeMillis() - lastCheck >= TimeUnit.HOURS.toMillis(24)) checkForUpdate()
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton(
            text = if (busy) "Đang kiểm tra…" else "Kiểm tra cập nhật",
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.Refresh,
            enabled = !busy,
            onClick = checkForUpdate,
        )

        if (message.isNotBlank()) {
            InfoBanner(
                message,
                if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                if (isError) AppColors.danger else AppColors.mint,
                if (isError) AppColors.paleDanger else AppColors.paleMint,
            )
        }

        update?.let { available ->
            PrimaryButton(
                text = if (busy) "Đang tải…" else "Cập nhật lên ${available.versionName}",
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Download,
                enabled = !busy,
            ) {
                busy = true
                executor.execute {
                    val apk = File(context.cacheDir, "updates/update-${available.versionCode}.apk")
                    val result = runCatching { client.downloadApk(available, apk) }
                    android.os.Handler(context.mainLooper).post {
                        busy = false
                        result.onSuccess {
                            runCatching {
                                if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                    message = "Cho phép cài ứng dụng từ nguồn này, sau đó bấm Cập nhật lại."
                                    isError = false
                                } else {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
                                    context.startActivity(
                                        Intent(Intent.ACTION_INSTALL_PACKAGE)
                                            .setData(uri)
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    )
                                }
                            }.onFailure {
                                message = "Không thể mở trình cài đặt Android."
                                isError = true
                            }
                        }.onFailure {
                            message = "Tải bản cập nhật thất bại."
                            isError = true
                        }
                    }
                }
            }
        }
    }
}
