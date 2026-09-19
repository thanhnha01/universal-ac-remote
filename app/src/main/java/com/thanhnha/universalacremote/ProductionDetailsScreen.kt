package com.thanhnha.universalacremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thanhnha.universalacremote.ir.RemoteCandidate
import com.thanhnha.universalacremote.ir.VerificationCheck
import com.thanhnha.universalacremote.ir.displayModelLabel

@Composable
fun ProductionDetailsScreen(
    remote: SavedRemote,
    candidate: RemoteCandidate?,
    store: SavedRemotesViewModel,
    onOpen: () -> Unit,
    onRetest: () -> Unit,
    onBack: () -> Unit,
) {
    var renameOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var newName by remember(remote.id, remote.displayName) { mutableStateOf(remote.displayName) }
    val imported = remote.importedCommandsJson.isNotBlank()
    val transmittable = imported ||
        (candidate != null && com.thanhnha.universalacremote.ir.CatalogTransmitter.supports(candidate))
    val verified = !imported && remote.verifiedCapabilities.isNotEmpty() && transmittable

    AppScaffold(selectedRoute = null, onNavigate = {}, bottomBar = false) { padding ->
        PageColumn(padding) {
            AppTopBar(
                title = "Chi tiết máy lạnh",
                subtitle = remote.displayName,
                onBack = onBack,
            )

            SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
                Row(
                    Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AcWallUnitArt(remote.brand, Modifier.weight(0.42f).height(86.dp))
                    Column(Modifier.weight(0.58f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            remote.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.navy,
                        )
                        Text(
                            listOfNotNull(remote.brand, remote.acModel, remote.remoteModel)
                                .filter(String::isNotBlank)
                                .joinToString(" • ")
                                .ifBlank { "Remote đã lưu" },
                            color = AppColors.navySoft,
                        )
                        StatusChip(
                            when {
                                imported -> "File IR"
                                verified -> "Đã xác minh"
                                else -> "Chưa xác minh"
                            },
                            if (imported || verified) Icons.Filled.CheckCircle else Icons.Filled.Tune,
                            when {
                                imported -> AppColors.blue
                                verified -> AppColors.mint
                                else -> AppColors.warning
                            },
                            when {
                                imported -> AppColors.paleBlue
                                verified -> AppColors.paleMint
                                else -> AppColors.paleWarning
                            },
                        )
                    }
                }
            }

            if (imported) {
                SectionTitle("Remote từ file .ir")
                InfoBanner(
                    "Remote này dùng đúng các lệnh IR có trong file đã nhập. App không tự thêm chức năng khác.",
                    Icons.Filled.CheckCircle,
                    AppColors.blue,
                    AppColors.paleBlue,
                )
            } else {
                SectionTitle("Chức năng đã xác minh")
                if (remote.verifiedCapabilities.isEmpty()) {
                    EmptyState(
                        "Chưa có chức năng được xác minh",
                        "Bạn có thể kiểm tra lại remote để xác nhận từng chức năng.",
                        Icons.Filled.Tune,
                    )
                } else {
                    remote.verifiedCapabilities.mapNotNull { raw ->
                        runCatching { VerificationCheck.valueOf(raw) }.getOrNull()
                    }.forEach { check ->
                        CapabilityRow(
                            title = detailCheckLabel(check),
                            detail = "Đã xác nhận trên máy lạnh",
                            icon = detailCheckIcon(check),
                            color = AppColors.mint,
                            status = "OK",
                        )
                    }
                }
            }

            SectionTitle("Thông tin")
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    SourceInfoRow("Hãng", remote.brand.ifBlank { "Không rõ" }, Icons.Filled.AcUnit)
                    SourceInfoRow(
                        "Model",
                        candidate?.displayModelLabel()?.takeIf(String::isNotBlank)
                            ?: remote.acModel
                            ?: remote.remoteModel
                            ?: "Không rõ",
                        Icons.Filled.Tune,
                    )
                    SourceInfoRow(
                        "Nguồn điều khiển",
                        if (remote.importedCommandsJson.isNotBlank()) "File .ir đã nhập"
                        else when (candidate?.source?.lowercase()) {
                            "irremoteesp8266" -> "IRremoteESP8266"
                            "smartir" -> "SmartIR"
                            "flipper-irdb" -> "Flipper IRDB"
                            "irplus" -> "irplus"
                            else -> "Thư viện ứng dụng"
                        },
                        Icons.Filled.Refresh,
                    )
                }
            }

            SectionTitle("Quản lý")
            PrimaryButton("Mở remote", Modifier.fillMaxWidth(), Icons.Filled.PlayArrow, onClick = onOpen)
            if (imported) {
                SecondaryButton("Đổi tên", Modifier.fillMaxWidth(), Icons.Filled.Edit) {
                    newName = remote.displayName
                    renameOpen = true
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Kiểm tra lại", Modifier.weight(1f), Icons.Filled.Refresh, onClick = onRetest)
                    SecondaryButton("Đổi tên", Modifier.weight(1f), Icons.Filled.Edit) {
                        newName = remote.displayName
                        renameOpen = true
                    }
                }
            }
            SecondaryButton("Xóa khỏi máy", Modifier.fillMaxWidth(), Icons.Filled.DeleteOutline) {
                deleteOpen = true
            }
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text("Đổi tên máy lạnh") },
            text = { OutlinedField("Tên hiển thị", newName) { newName = it } },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        store.rename(remote.id, newName.trim())
                        renameOpen = false
                    },
                ) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text("Hủy") }
            },
        )
    }

    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Xóa remote?") },
            text = { Text("Remote “${remote.displayName}” sẽ bị xóa khỏi ứng dụng.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.delete(remote.id)
                        deleteOpen = false
                        onBack()
                    },
                ) { Text("Xóa", color = AppColors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteOpen = false }) { Text("Hủy") }
            },
        )
    }
}

private fun detailCheckLabel(check: VerificationCheck): String = when (check) {
    VerificationCheck.POWER -> "Bật / Tắt nguồn"
    VerificationCheck.TEMPERATURE_CHANGED -> "Nhiệt độ"
    VerificationCheck.MODE -> "Chế độ"
    VerificationCheck.FAN -> "Tốc độ quạt"
    VerificationCheck.SWING_VERTICAL -> "Đảo gió dọc"
    VerificationCheck.SWING_HORIZONTAL -> "Đảo gió ngang"
}

private fun detailCheckIcon(check: VerificationCheck) = when (check) {
    VerificationCheck.POWER -> Icons.Filled.PowerSettingsNew
    VerificationCheck.TEMPERATURE_CHANGED -> Icons.Filled.Thermostat
    VerificationCheck.MODE -> Icons.Filled.AcUnit
    VerificationCheck.FAN -> Icons.Filled.Air
    VerificationCheck.SWING_VERTICAL, VerificationCheck.SWING_HORIZONTAL -> Icons.Filled.Tune
}
