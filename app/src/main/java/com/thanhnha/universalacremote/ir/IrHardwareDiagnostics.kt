package com.thanhnha.universalacremote.ir

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager

data class CarrierFrequencyRange(
    val minFrequencyHz: Int,
    val maxFrequencyHz: Int,
)

data class IrHardwareDiagnostics(
    val featureDeclared: Boolean,
    val managerAvailable: Boolean,
    val hasIrEmitter: Boolean,
    val carrierFrequencyRanges: List<CarrierFrequencyRange>,
)

fun readIrHardwareDiagnostics(context: Context): IrHardwareDiagnostics {
    val featureDeclared = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
    val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    val hasEmitter = manager?.hasIrEmitter() == true
    val ranges = manager?.carrierFrequencies.orEmpty().map {
        CarrierFrequencyRange(it.minFrequency, it.maxFrequency)
    }

    return IrHardwareDiagnostics(
        featureDeclared = featureDeclared,
        managerAvailable = manager != null,
        hasIrEmitter = hasEmitter,
        carrierFrequencyRanges = ranges,
    )
}
