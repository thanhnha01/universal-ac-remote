package com.thanhnha.universalacremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.thanhnha.universalacremote.ir.AndroidIrTransmitter
import com.thanhnha.universalacremote.ir.IrTransmission
import com.thanhnha.universalacremote.ir.readIrHardwareDiagnostics

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diagnostics = readIrHardwareDiagnostics(this)
        val transmitter = AndroidIrTransmitter.from(this)

        setContent {
            DiagnosticScreen(diagnostics) {
                DebugRawTransmitPanel { frequency, timings ->
                    runCatching {
                        transmitter.transmit(
                            IrTransmission(
                                carrierFrequencyHz = frequency,
                                timingsMicros = timings,
                            ),
                        )
                    }
                }
            }
        }
    }
}
