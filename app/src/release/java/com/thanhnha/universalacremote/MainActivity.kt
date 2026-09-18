package com.thanhnha.universalacremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.thanhnha.universalacremote.ir.readIrHardwareDiagnostics

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diagnostics = readIrHardwareDiagnostics(this)
        setContent {
            DiagnosticScreen(diagnostics)
        }
    }
}
