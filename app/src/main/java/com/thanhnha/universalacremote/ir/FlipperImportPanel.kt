package com.thanhnha.universalacremote.ir

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun FlipperImportPanel(transmitter: AndroidIrTransmitter, onSave: ((String, List<FlipperRawCommand>) -> Unit)? = null) {
    val context = LocalContext.current
    var commands by remember { mutableStateOf<List<FlipperRawCommand>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var feedback by remember { mutableStateOf("") }
    var remoteName by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = runCatching {
                val stream = context.contentResolver.openInputStream(uri) ?: error("Could not read the selected file.")
                stream.use {
                    val bytes = ByteArray(1_000_001)
                    var count = 0
                    while (count < bytes.size) {
                        val read = it.read(bytes, count, bytes.size - count)
                        if (read < 0) break
                        count += read
                    }
                    require(count <= 1_000_000) { "Flipper .ir file is too large." }
                    bytes.copyOf(count).toString(Charsets.UTF_8)
                }
            }.mapCatching(FlipperRawImporter::parse)
            result.onSuccess {
                commands = it
                selectedIndex = 0
                feedback = "Imported ${it.size} RAW command(s)."
            }.onFailure {
                commands = emptyList()
                selectedIndex = -1
                feedback = it.message ?: "Invalid Flipper .ir file."
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Flipper RAW import", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Chọn file .ir") }
            commands.forEachIndexed { index, command ->
                Button(onClick = { selectedIndex = index }, enabled = selectedIndex != index) {
                    Text("${command.name} · ${command.transmission.carrierFrequencyHz} Hz")
                }
            }
            if (selectedIndex in commands.indices) {
                Button(onClick = {
                    val result = runCatching { transmitter.transmit(commands[selectedIndex].transmission) }
                    feedback = result.fold({ "Command sent." }, { it.message ?: "Transmission rejected." })
                }) { Text("Phát command đã chọn") }
            }
            if (commands.isNotEmpty() && onSave != null) {
                OutlinedTextField(remoteName, { remoteName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tên remote") }, singleLine = true)
                Button(onClick = { onSave(remoteName.trim(), commands) }, enabled = remoteName.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Lưu remote đã nhập")
                }
            }
            if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall)
        }
    }
}
