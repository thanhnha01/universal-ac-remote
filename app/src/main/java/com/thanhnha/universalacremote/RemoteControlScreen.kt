package com.thanhnha.universalacremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thanhnha.universalacremote.ir.AcFan
import com.thanhnha.universalacremote.ir.AcMode
import com.thanhnha.universalacremote.ir.AcState
import com.thanhnha.universalacremote.ir.AndroidIrTransmitter
import com.thanhnha.universalacremote.ir.CatalogTransmitter
import com.thanhnha.universalacremote.ir.IrHardwareDiagnostics
import com.thanhnha.universalacremote.ir.RemoteCandidate
import com.thanhnha.universalacremote.ir.RemoteControls
import com.thanhnha.universalacremote.ir.displayModelLabel
import com.thanhnha.universalacremote.ir.fanLabel
import com.thanhnha.universalacremote.ir.modeLabel

/**
 * Production remote UI.
 *
 * Every interactive control transmits immediately. The screen never exposes
 * controls that the current encoder cannot represent, and it keeps hardware,
 * profile-transmittable and verification status separate.
 */
@Composable
fun RemoteControlScreen(
    remote: SavedRemote,
    candidate: RemoteCandidate,
    diagnostics: IrHardwareDiagnostics,
    onTab: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val controls = remember(candidate.id) { RemoteControls.from(candidate) }
    val protocol = remember(candidate.id) { CatalogTransmitter.protocol(candidate) }
    val transmittable = remember(candidate.id, candidate.verificationStatus) { CatalogTransmitter.supports(candidate) }
    val hardwareReady = diagnostics.hasIrEmitter

    val mappedModes = remember(candidate.id) {
        controls.modes.filter { value ->
            CatalogTransmitter.mode(value)?.let { mode -> protocol == null || mode in protocol.modes } == true
        }
    }
    val mappedFans = remember(candidate.id) {
        controls.fanModes.filter { value ->
            CatalogTransmitter.fan(value)?.let { fan -> protocol == null || fan in protocol.fanSpeeds } == true
        }
    }
    val temperatureRange = controls.temperatureRange
        ?: protocol?.let { it.minTemperatureCelsius..it.maxTemperatureCelsius }

    val initialMode = mappedModes.firstOrNull()
        ?: protocol?.modes?.firstOrNull()?.name?.lowercase()
        ?: "cool"
    val initialFan = mappedFans.firstOrNull()
        ?: protocol?.fanSpeeds?.firstOrNull()?.let { fanKey(it) }
        ?: "auto"
    val initialTemp = temperatureRange?.let { ((it.first + it.last) / 2).coerceIn(it) } ?: 24

    var power by remember(candidate.id) { mutableStateOf(false) }
    var temperature by remember(candidate.id) { mutableIntStateOf(initialTemp) }
    var mode by remember(candidate.id) { mutableStateOf(initialMode) }
    var fan by remember(candidate.id) { mutableStateOf(initialFan) }
    var swingVertical by remember(candidate.id) { mutableStateOf(false) }
    var swingHorizontal by remember(candidate.id) { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun transmit(
        nextPower: Boolean = power,
        nextTemperature: Int = temperature,
        nextMode: String = mode,
        nextFan: String = fan,
        nextSwingVertical: Boolean = swingVertical,
        nextSwingHorizontal: Boolean = swingHorizontal,
    ): Boolean {
        if (!hardwareReady) {
            message = "Điện thoại chưa báo có bộ phát IR."
            error = true
            return false
        }
        if (!transmittable) {
            message = "Hồ sơ này chưa thể phát trên thiết bị."
            error = true
            return false
        }

        // Some raw SmartIR profiles expose only explicit power commands.
        // Allow those profiles to power on/off without inventing mode/fan state.
        if (candidate.encodingType.equals("RAW_PROFILE", true) &&
            nextPower && !power &&
            (mappedModes.isEmpty() || mappedFans.isEmpty() || temperatureRange == null)
        ) {
            val explicitOn = CatalogTransmitter.explicitPowerOn(candidate)
            if (explicitOn != null) {
                return runCatching {
                    AndroidIrTransmitter.from(context).transmit(explicitOn)
                }.fold(
                    onSuccess = {
                        power = true
                        message = "Đã phát lệnh bật."
                        error = false
                        true
                    },
                    onFailure = {
                        message = "Không thể phát tín hiệu IR."
                        error = true
                        false
                    },
                )
            }
        }

        val selectedMode = CatalogTransmitter.mode(nextMode)
            ?: protocol?.modes?.firstOrNull()
            ?: if (!nextPower && candidate.encodingType.equals("RAW_PROFILE", true)) AcMode.COOL else null
            ?: run {
                message = "Chế độ này chưa được bộ phát hỗ trợ."
                error = true
                return false
            }
        val selectedFan = CatalogTransmitter.fan(nextFan)
            ?: protocol?.fanSpeeds?.firstOrNull()
            ?: if (!nextPower && candidate.encodingType.equals("RAW_PROFILE", true)) AcFan.AUTO else null
            ?: run {
                message = "Tốc độ quạt này chưa được bộ phát hỗ trợ."
                error = true
                return false
            }
        val target = if (protocol != null) {
            candidate.copy(protocolModel = CatalogTransmitter.modelId(candidate, protocol))
        } else candidate

        return runCatching {
            AndroidIrTransmitter.from(context).transmit(
                CatalogTransmitter.encode(
                    target,
                    AcState(
                        power = nextPower,
                        temperatureCelsius = nextTemperature,
                        mode = selectedMode,
                        fan = selectedFan,
                        swingVertical = nextSwingVertical,
                        swingHorizontal = nextSwingHorizontal,
                    )
                )
            )
        }.fold(
            onSuccess = {
                power = nextPower
                temperature = nextTemperature
                mode = nextMode
                fan = nextFan
                swingVertical = nextSwingVertical
                swingHorizontal = nextSwingHorizontal
                message = "Đã phát lệnh."
                error = false
                true
            },
            onFailure = {
                message = "Không thể phát tín hiệu IR."
                error = true
                false
            },
        )
    }

    AppScaffold("remote", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar(
                title = remote.displayName,
                subtitle = listOfNotNull(remote.brand, candidate.displayModelLabel().takeIf(String::isNotBlank)).joinToString(" • "),
                onBack = onBack,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val verified = remote.verifiedCapabilities.isNotEmpty() && transmittable
                StatusChip(
                    when {
                        verified -> "Đã xác minh"
                        remote.verifiedCapabilities.isNotEmpty() -> "Cần kiểm tra lại"
                        else -> "Chưa xác minh"
                    },
                    if (verified) Icons.Filled.CheckCircle else Icons.Filled.Info,
                    if (verified) AppColors.mint else AppColors.warning,
                    if (verified) AppColors.paleMint else AppColors.paleWarning,
                )
                StatusChip(
                    when {
                        !hardwareReady -> "Không có IR"
                        !transmittable -> "Hồ sơ chưa phát được"
                        else -> "IR sẵn sàng"
                    },
                    if (hardwareReady && transmittable) Icons.Filled.SignalCellularAlt else Icons.Filled.ErrorOutline,
                    if (hardwareReady && transmittable) AppColors.blue else AppColors.danger,
                    if (hardwareReady && transmittable) AppColors.paleBlue else AppColors.paleDanger,
                )
            }

            RemoteHero(
                temperature = temperature,
                power = power,
                canChangeTemperature = temperatureRange != null,
                canPower = controls.power && hardwareReady && transmittable,
                onMinus = {
                    val range = temperatureRange ?: return@RemoteHero
                    if (temperature > range.first) transmit(nextTemperature = temperature - 1)
                },
                onPlus = {
                    val range = temperatureRange ?: return@RemoteHero
                    if (temperature < range.last) transmit(nextTemperature = temperature + 1)
                },
                onPower = { transmit(nextPower = !power) },
            )

            if (!hardwareReady || !transmittable) {
                InfoBanner(
                    when {
                        !hardwareReady -> "Phần cứng IR chưa sẵn sàng. Mở Cài đặt → Kiểm tra phần cứng IR."
                        else -> "Hồ sơ này có trong catalog nhưng chưa có đường phát tương thích."
                    },
                    Icons.Filled.Info,
                    if (!hardwareReady) AppColors.danger else AppColors.warning,
                    if (!hardwareReady) AppColors.paleDanger else AppColors.paleWarning,
                )
            }

            if (mappedModes.isNotEmpty()) {
                RemoteSection("Chế độ", "Chọn chế độ hoạt động", Icons.Filled.AcUnit) {
                    RemoteChoiceRow(mappedModes, mode, ::modeLabel) { selected ->
                        transmit(nextMode = selected)
                    }
                }
            }

            if (mappedFans.isNotEmpty()) {
                RemoteSection("Quạt", "Tốc độ quạt gió", Icons.Filled.Air) {
                    RemoteChoiceRow(mappedFans, fan, ::fanLabel) { selected ->
                        transmit(nextFan = selected)
                    }
                }
            }

            if (controls.verticalSwing.visible || controls.horizontalSwing.visible) {
                RemoteSection("Hướng gió", "Điều chỉnh đảo gió", Icons.Filled.Tune) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (controls.verticalSwing.visible) {
                            SwingToggleRow(
                                title = "Dọc (Lên/Xuống)",
                                enabled = swingVertical,
                                onSelect = { transmit(nextSwingVertical = it) },
                            )
                        }
                        if (controls.horizontalSwing.visible) {
                            SwingToggleRow(
                                title = "Ngang (Trái/Phải)",
                                enabled = swingHorizontal,
                                onSelect = { transmit(nextSwingHorizontal = it) },
                            )
                        }
                    }
                }
            }

            // Special SmartIR/protocol flags are intentionally hidden until
            // AcState/encoder can represent them. Showing dead switches made
            // the previous UI look complete while the feature did not work.

            if (message.isNotBlank()) {
                InfoBanner(
                    message,
                    if (error) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                    if (error) AppColors.danger else AppColors.mint,
                    if (error) AppColors.paleDanger else AppColors.paleMint,
                )
            }

            Text(
                "Chạm vào điều khiển là app phát lệnh ngay tới máy lạnh.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = AppColors.navySoft,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RemoteHero(
    temperature: Int,
    power: Boolean,
    canChangeTemperature: Boolean,
    canPower: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onPower: () -> Unit,
) {
    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nhiệt độ cài đặt", color = AppColors.navySoft)
                    Text(
                        if (canChangeTemperature) "$temperature°C" else "—",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.navy,
                    )
                    Text(
                        if (power) "Lệnh gần nhất: bật" else "Lệnh gần nhất: tắt",
                        color = AppColors.navySoft,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Surface(
                    modifier = Modifier.size(112.dp).clickable(enabled = canPower, onClick = onPower),
                    shape = RoundedCornerShape(56.dp),
                    color = if (power) AppColors.mint else AppColors.blue,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.PowerSettingsNew,
                            "Bật hoặc tắt",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp),
                        )
                    }
                }
            }
            if (canChangeTemperature) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroAction("−", "Giảm nhiệt độ", Modifier.weight(1f), onMinus)
                    HeroAction("+", "Tăng nhiệt độ", Modifier.weight(1f), onPlus)
                }
            }
        }
    }
}

@Composable
private fun HeroAction(symbol: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(symbol, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppColors.blue)
            Text("  $label", fontWeight = FontWeight.Bold, color = AppColors.navy)
        }
    }
}

@Composable
private fun RemoteSection(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    SurfaceCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBubble(icon, size = 42)
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                    Text(subtitle, color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall)
                }
            }
            content()
        }
    }
}

@Composable
private fun RemoteChoiceRow(
    values: List<String>,
    selected: String,
    label: (String) -> String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(1.dp),
    ) {
        items(values) { value ->
            val active = value.equals(selected, true)
            Surface(
                modifier = Modifier.clickable { onSelect(value) },
                shape = RoundedCornerShape(18.dp),
                color = if (active) AppColors.blue else AppColors.page,
                border = BorderStroke(1.dp, if (active) AppColors.blue else AppColors.line),
            ) {
                Text(
                    label(value),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    color = if (active) Color.White else AppColors.navy,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SwingToggleRow(title: String, enabled: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = AppColors.navy)
        SwingChoice("Cố định", !enabled) { onSelect(false) }
        SwingChoice("Tự động", enabled) { onSelect(true) }
    }
}

@Composable
private fun SwingChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) AppColors.blue else AppColors.page,
        border = BorderStroke(1.dp, if (selected) AppColors.blue else AppColors.line),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            color = if (selected) Color.White else AppColors.navy,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun fanKey(value: AcFan): String = when (value) {
    AcFan.AUTO -> "auto"
    AcFan.MIN -> "low"
    AcFan.MEDIUM -> "medium"
    AcFan.HIGH -> "high"
}
