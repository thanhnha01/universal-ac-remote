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
                        DiagnosticStatusRow("Hệ thống hỗ trợ IR", diagnostics.featureDeclared)
                        DiagnosticStatusRow("Dịch vụ phát IR", diagnostics.managerAvailable)
                        DiagnosticStatusRow("Bộ phát IR", diagnostics.hasIrEmitter)
                    }
                }

                SectionTitle("Thông tin tín hiệu")
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column {
                        SourceInfoRow(
                            "Khả năng phát IR",
                            if (diagnostics.hasIrEmitter) "Đã nhận diện" else "Chưa nhận diện",
                            Icons.Filled.SignalCellularAlt,
                            if (diagnostics.hasIrEmitter) AppColors.mint else AppColors.danger,
                        )
                        SourceInfoRow(
                            "Thông tin dải phát",
                            if (diagnostics.carrierFrequencyRanges.isEmpty())
                                "Thiết bị không cung cấp chi tiết"
                            else
                                "${diagnostics.carrierFrequencyRanges.size} dải được nhận diện",
                            Icons.Filled.Tune,
                            AppColors.navySoft,
                        )
                    }
                }

                InfoBanner(
                    "Màn hình này chỉ kiểm tra phần cứng IR của điện thoại. Khả năng điều khiển từng máy lạnh còn phụ thuộc remote đã chọn.",
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
