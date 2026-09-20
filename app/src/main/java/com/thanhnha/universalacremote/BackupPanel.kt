package com.thanhnha.universalacremote

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun BackupPanel(store: SavedRemotesViewModel) {
    val context = LocalContext.current
    val home by store.state.collectAsState()
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val payload = BackupCodec.encode(home.remotes)
            context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { it.write(payload) }
                ?: error("Không thể tạo file backup.")
        }.onSuccess {
            message = "Đã xuất " + home.remotes.size + " remote vào file backup."
            isError = false
        }.onFailure {
            message = "Không thể xuất backup."
            isError = true
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val payload = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Không thể đọc file backup.")
            BackupCodec.decode(payload)
        }.onSuccess { bundle ->
            store.restoreBackup(bundle)
            message = "Đã nhập " + bundle.remotes.size + " remote từ backup."
            isError = false
        }.onFailure {
            message = "File backup không hợp lệ hoặc không tương thích."
            isError = true
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PrimaryButton(
            text = "Xuất backup",
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.Share,
            enabled = !home.loading,
        ) {
            exportLauncher.launch("universal-ac-remote-backup.json")
        }
        SecondaryButton(
            text = "Khôi phục backup",
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Filled.FileDownload,
        ) {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
        if (message.isNotBlank()) {
            InfoBanner(
                message,
                if (isError) Icons.Filled.Refresh else Icons.Filled.CheckCircle,
                if (isError) AppColors.danger else AppColors.mint,
                if (isError) AppColors.paleDanger else AppColors.paleMint,
            )
        }
    }
}