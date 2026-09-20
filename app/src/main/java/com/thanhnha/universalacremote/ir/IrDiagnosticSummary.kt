package com.thanhnha.universalacremote.ir

enum class IrDiagnosticSeverity { READY, LIMITED, UNAVAILABLE }

data class IrDiagnosticSummary(
    val severity: IrDiagnosticSeverity,
    val title: String,
    val detail: String,
)

fun IrHardwareDiagnostics.summary(): IrDiagnosticSummary = when {
    hasIrEmitter && managerAvailable -> IrDiagnosticSummary(
        IrDiagnosticSeverity.READY,
        "IR sẵn sàng",
        if (carrierFrequencyRanges.isEmpty())
            "Android nhận diện bộ phát IR, nhưng thiết bị không công bố dải tần chi tiết."
        else
            "Android nhận diện bộ phát IR và " + carrierFrequencyRanges.size + " dải tần có thể dùng.",
    )
    managerAvailable || featureDeclared -> IrDiagnosticSummary(
        IrDiagnosticSeverity.LIMITED,
        "IR chưa sẵn sàng",
        "Hệ thống có dấu hiệu hỗ trợ IR nhưng Android chưa xác nhận bộ phát khả dụng.",
    )
    else -> IrDiagnosticSummary(
        IrDiagnosticSeverity.UNAVAILABLE,
        "Không tìm thấy phần cứng IR",
        "Android không cung cấp Consumer IR service trên thiết bị này.",
    )
}