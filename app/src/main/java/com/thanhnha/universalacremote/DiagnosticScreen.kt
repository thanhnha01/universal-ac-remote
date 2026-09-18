package com.thanhnha.universalacremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thanhnha.universalacremote.ir.IrHardwareDiagnostics

@Composable
fun DiagnosticScreen(
    diagnostics: IrHardwareDiagnostics,
    debugContent: (@Composable () -> Unit)? = null,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("IR hardware diagnostic", style = MaterialTheme.typography.headlineSmall)
                Text("Consumer IR is checked through Android's platform API.")
                DiagnosticRow("FEATURE_CONSUMER_IR", diagnostics.featureDeclared)
                DiagnosticRow("ConsumerIrManager available", diagnostics.managerAvailable)
                DiagnosticRow("hasIrEmitter()", diagnostics.hasIrEmitter)

                Text("Carrier frequency ranges", style = MaterialTheme.typography.titleMedium)
                if (diagnostics.carrierFrequencyRanges.isEmpty()) {
                    Text("Android reported no frequency ranges.")
                } else {
                    diagnostics.carrierFrequencyRanges.forEachIndexed { index, range ->
                        Text("${index + 1}. ${range.minFrequencyHz}–${range.maxFrequencyHz} Hz")
                    }
                }

                debugContent?.invoke()
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(if (value) "Available" else "Unavailable")
    }
}
