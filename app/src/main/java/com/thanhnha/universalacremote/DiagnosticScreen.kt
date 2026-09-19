package com.thanhnha.universalacremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thanhnha.universalacremote.ir.IrHardwareDiagnostics

@Composable
fun DiagnosticScreen(
    diagnostics: IrHardwareDiagnostics,
    onBack: () -> Unit,
    debugContent: (@Composable () -> Unit)? = null,
) {
    AppBackground {
        Column(Modifier.fillMaxSize()) {
            AppTopBar("Kiểm tra phần cứng IR", "Trạng thái bộ phát hồng ngoại", onBack)
            PageColumn(PaddingValues(0.dp)) {
                GradientHero {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (diagnostics.hasIrEmitter) "IR sẵn sàng" else "IR chưa sẵn sàng",
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (diagnostics.hasIrEmitter) AppColors.mint else AppColors.danger,
                        )
                        Text(
                            if (diagnostics.hasIrEmitter)
                                "Android nhận diện được bộ phát IR và có thể gửi tín hiệu."
                            else
                                "Android chưa xác nhận được bộ phát IR trên thiết bị này.",
                            color = AppColors.navySoft,
                        )
                    }
                }

                SectionTitle("Trạng thái")
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column {
                        DiagnosticStatusRow("Tính năng Consumer IR", diagnostics.featureDeclared)
                        DiagnosticStatusRow("Dịch vụ ConsumerIrManager", diagnostics.managerAvailable)
                        DiagnosticStatusRow("Bộ phát IR", diagnostics.hasIrEmitter)
                    }
                }

                SectionTitle("Dải tần hỗ trợ")
                if (diagnostics.carrierFrequencyRanges.isEmpty()) {
                    EmptyState("Chưa có dữ liệu dải tần", "Android không trả về danh sách carrier frequency.", Icons.Filled.Tune)
                } else {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            diagnostics.carrierFrequencyRanges.forEachIndexed { index, range ->
                                SourceInfoRow(
                                    "Dải ${index + 1}",
                                    "${range.minFrequencyHz / 1000}–${range.maxFrequencyHz / 1000} kHz",
                                    Icons.Filled.SignalCellularAlt,
                                    AppColors.blue,
                                )
                            }
                        }
                    }
                }

                InfoBanner(
                    "Màn hình này chỉ kiểm tra khả năng phần cứng Android. Việc một profile máy lạnh có phát được hay không còn phụ thuộc encoder/profile tương ứng.",
                    Icons.Filled.SignalCellularAlt,
                    AppColors.blue,
                    AppColors.paleBlue,
                )

                debugContent?.invoke()
            }
        }
    }
}

@Composable
private fun DiagnosticStatusRow(label: String, value: Boolean) {
    CapabilityRow(
        title = label,
        detail = if (value) "Khả dụng" else "Không khả dụng",
        icon = if (value) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
        color = if (value) AppColors.mint else AppColors.danger,
        status = if (value) "OK" else "Lỗi",
    )
}
