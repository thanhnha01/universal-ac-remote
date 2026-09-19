package com.thanhnha.universalacremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thanhnha.universalacremote.ir.AndroidIrTransmitter

@Composable
fun ImportedRemoteControlScreen(
    remote: SavedRemote,
    store: SavedRemotesViewModel,
    onTab: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val commands = remember(remote.id, remote.importedCommandsJson) {
        SavedRemoteConverters().decodeImportedCommands(remote.importedCommandsJson)
    }
    var feedback by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AppScaffold("remote", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar(
                title = remote.displayName,
                subtitle = listOf(remote.brand, remote.remoteModel).filterNotNull().filter(String::isNotBlank).joinToString(" • "),
                onBack = onBack,
            )

            GradientHero {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Remote đã nhập",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.navy,
                    )
                    Text(
                        "${commands.size} lệnh IR sẵn sàng. Chạm vào nút là phát ngay.",
                        color = AppColors.navySoft,
                    )
                }
            }

            if (commands.isEmpty()) {
                EmptyState("Không có lệnh hợp lệ", "Remote này không còn dữ liệu IR có thể phát.", Icons.Filled.ErrorOutline)
            } else {
                SectionTitle("Điều khiển")
                commands.chunked(2).forEach { rowCommands ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowCommands.forEach { command ->
                            ImportedCommandTile(
                                name = command.name,
                                modifier = Modifier.weight(1f),
                            ) {
                                runCatching {
                                    AndroidIrTransmitter.from(context).transmit(command.transmission)
                                }.onSuccess {
                                    feedback = "Đã phát ${command.name}."
                                    isError = false
                                }.onFailure {
                                    feedback = "Không thể phát tín hiệu IR."
                                    isError = true
                                }
                            }
                        }
                        if (rowCommands.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }

            if (feedback.isNotBlank()) {
                InfoBanner(
                    feedback,
                    if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                    if (isError) AppColors.danger else AppColors.mint,
                    if (isError) AppColors.paleDanger else AppColors.paleMint,
                )
            }

            SecondaryButton("Xóa remote", Modifier.fillMaxWidth(), Icons.Filled.DeleteOutline) {
                store.delete(remote.id)
                onBack()
            }
        }
    }
}

@Composable
private fun ImportedCommandTile(name: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                importedCommandIcon(name),
                contentDescription = null,
                tint = AppColors.blue,
                modifier = Modifier.size(30.dp),
            )
            Text(
                name.ifBlank { "Lệnh IR" },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = AppColors.navy,
                maxLines = 2,
            )
        }
    }
}

private fun importedCommandIcon(name: String) = when {
    name.contains("power", true) -> Icons.Filled.PowerSettingsNew
    name.contains("temp", true) -> Icons.Filled.Thermostat
    name.contains("fan", true) -> Icons.Filled.Air
    else -> Icons.Filled.Tune
}
