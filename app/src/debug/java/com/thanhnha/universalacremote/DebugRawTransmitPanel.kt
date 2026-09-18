package com.thanhnha.universalacremote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DebugRawTransmitPanel(onTransmit: (Int, List<Int>) -> Result<Unit>) {
    var frequencyText by remember { mutableStateOf("") }
    var timingsText by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Debug only: transmit caller-provided raw timings", style = MaterialTheme.typography.titleMedium)
            Text("Enter a carrier frequency and comma-separated mark/space durations in microseconds.")
            OutlinedTextField(
                value = frequencyText,
                onValueChange = { frequencyText = it },
                label = { Text("Carrier frequency (Hz)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = timingsText,
                onValueChange = { timingsText = it },
                label = { Text("Timings in µs, alternating mark,space") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Button(onClick = {
                val result = runCatching {
                    val frequency = frequencyText.trim().toInt()
                    val timings = timingsText.split(',').map { it.trim().toInt() }
                    onTransmit(frequency, timings).getOrThrow()
                }
                feedback = result.fold(
                    onSuccess = { "Transmission sent." },
                    onFailure = { it.message ?: "Transmission rejected." },
                )
            }) {
                Text("Transmit raw signal")
            }
            if (feedback.isNotBlank()) Text(feedback)
        }
    }
}
