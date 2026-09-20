package com.thanhnha.universalacremote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thanhnha.universalacremote.ir.AndroidIrTransmitter
import com.thanhnha.universalacremote.ir.FlipperRawCommand
import com.thanhnha.universalacremote.ir.FlipperRawImporter
import java.util.UUID

@Composable
fun IrImportScreen(
    store: SavedRemotesViewModel,
    onTab: (String) -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    var fileName by remember { mutableStateOf<String?>(null) }
    var commands by remember { mutableStateOf<List<FlipperRawCommand>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var feedback by remember { mutableStateOf("") }
    var remoteName by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var remoteModel by remember { mutableStateOf("") }

    fun loadUri(uri: Uri) {
        val result = runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(1_000_001)
                var count = 0
                while (count < buffer.size) {
                    val read = input.read(buffer, count, buffer.size - count)
                    if (read < 0) break
                    count += read
                }
                require(count <= 1_000_000) { "File .ir vượt quá giới hạn 1 MB." }
                buffer.copyOf(count).toString(Charsets.UTF_8)
            } ?: error("Không thể đọc file đã chọn.")
            FlipperRawImporter.parse(bytes)
        }
        result.onSuccess { parsed ->
            fileName = displayName(context, uri)
            commands = smartImportedCommands(parsed).map { presentation ->
                presentation.command.copy(name = presentation.label)
            }
            selectedIndex = commands.indices.firstOrNull() ?: -1
            feedback = "Đã đọc và sắp xếp ${parsed.size} lệnh IR hợp lệ."
        }.onFailure {
            fileName = null
            commands = emptyList()
            selectedIndex = -1
            feedback = "Không thể đọc file .ir này. Hãy chọn một file IR hợp lệ."
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::loadUri) }

    AppScaffold("home", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar("Nhập file .ir", "Dùng file IR có sẵn trên điện thoại", onBack = onBack)
            SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconBubble(Icons.Filled.Folder, size = 66)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Chọn file .ir", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Dùng file IR đã lưu trên điện thoại.", color = AppColors.navySoft)
                    }
                    SecondaryButton("Chọn file", Modifier, Icons.Filled.Folder) {
                        picker.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
                    }
                }
            }
            fileName?.let { name ->
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                            IconBubble(Icons.Filled.Code, size = 62)
                            Column(Modifier.weight(1f)) {
                                Text(name, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusChip("File IR", Icons.Filled.Code)
                                    commands.firstOrNull()?.let { StatusChip("${it.transmission.carrierFrequencyHz / 1000} kHz", Icons.Filled.SignalCellularAlt, AppColors.mint, AppColors.paleMint) }
                                }
                            }
                        }
                        StatusChip("${commands.size} lệnh hợp lệ", Icons.Filled.CheckCircle, AppColors.mint, AppColors.paleMint)
                        InfoBanner("App chỉ dùng các lệnh đọc được từ file; không tự thêm chức năng chưa có.", Icons.Filled.WarningAmber, AppColors.warning, AppColors.paleWarning)
                    }
                }
            }
            if (commands.isNotEmpty()) {
                SectionTitle("Lệnh đã đọc")
                ImportCommandGrid(commands, selectedIndex) { selectedIndex = it }
            }
            SectionTitle("Áp dụng cho thiết bị")
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBubble(Icons.Filled.AcUnit, size = 62)
                        Column(Modifier.weight(1f)) {
                            Text("Chưa gán thiết bị", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text("Bạn có thể nhập thông tin trước khi lưu.", color = AppColors.navySoft)
                        }
                        StatusChip("Chưa xác định", Icons.Filled.Info, AppColors.warning, AppColors.paleWarning)
                    }
                    ImportField("Tên remote", remoteName, { remoteName = it }, "Ví dụ: Remote phòng khách")
                    ImportField("Hãng (tuỳ chọn)", brand, { brand = it }, "Để trống nếu không biết")
                    ImportField("Model remote (tuỳ chọn)", remoteModel, { remoteModel = it }, "Để trống nếu không biết")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Phát thử", Modifier.weight(1f), Icons.Filled.PlayArrow, enabled = selectedIndex in commands.indices) {
                    feedback = runCatching {
                        AndroidIrTransmitter.from(context).transmit(commands[selectedIndex].transmission)
                        "Đã phát lệnh kiểm tra."
                    }.getOrElse { "Không thể phát lệnh IR. Hãy kiểm tra bộ phát IR và thử lại." }
                }
                SecondaryButton("Lưu remote", Modifier.weight(1f), Icons.Filled.CheckCircle, enabled = commands.isNotEmpty() && remoteName.isNotBlank()) {
                    val payload = SavedRemoteConverters().encodeImportedCommands(commands.map { ImportedRawCommand(it.name, it.transmission) })
                    store.save(SavedRemote(UUID.randomUUID().toString(), remoteName.trim(), "imported:${UUID.randomUUID()}", brand.trim().ifBlank { "Không rõ hãng" }, null, remoteModel.trim().takeIf(String::isNotBlank), null, null, emptyList(), payload))
                    onSaved()
                }
            }
            if (feedback.isNotBlank()) InfoBanner(feedback, Icons.Filled.Info, AppColors.blue, AppColors.paleBlue)
        }
    }
}

@Composable
private fun ImportCommandGrid(commands: List<FlipperRawCommand>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 380.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            commands.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { columnIndex, command ->
                        val index = rowIndex * columns + columnIndex
                        ImportCommandTile(command, selectedIndex == index, Modifier.weight(1f)) { onSelect(index) }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ImportCommandTile(command: FlipperRawCommand, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    SurfaceCard(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (selected) AppColors.paleBlueStrong else Color.White)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(commandIcon(command.name), null, tint = AppColors.blue, modifier = Modifier.size(29.dp))
            Text(command.name.ifBlank { "Lệnh IR" }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun ImportField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text(placeholder) }, singleLine = true, shape = RoundedCornerShape(17.dp))
}

private fun commandIcon(name: String): ImageVector = when {
    name.contains("power", true) -> Icons.Filled.PowerSettingsNew
    name.contains("temp", true) || name.contains("heat", true) -> Icons.Filled.Thermostat
    name.contains("fan", true) -> Icons.Filled.Air
    name.contains("timer", true) -> Icons.Filled.Timer
    name.contains("light", true) -> Icons.Filled.Lightbulb
    name.contains("mode", true) -> Icons.Filled.AcUnit
    name.contains("swing", true) -> Icons.Filled.MoreHoriz
    else -> Icons.Filled.Tune
}

private fun displayName(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()?.takeIf(String::isNotBlank) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "remote.ir"
