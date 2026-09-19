package com.thanhnha.universalacremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.vector.ImageVector
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

@Composable
fun RemoteControlScreen(
    remote: SavedRemote,
    candidate: RemoteCandidate,
    diagnostics: IrHardwareDiagnostics,
    onTab: (String) -> Unit,
    onDetails: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val controls = remember(candidate.id) { RemoteControls.from(candidate) }
    val protocol = remember(candidate.id) { CatalogTransmitter.protocol(candidate) }
    val transmittable = remember(candidate.id, candidate.verificationStatus) { CatalogTransmitter.supports(candidate) }
    val hardwareReady = diagnostics.hasIrEmitter

    val modes = remember(candidate.id) {
        controls.modes.filter { value ->
            CatalogTransmitter.mode(value)?.let { mode -> protocol == null || mode in protocol.modes } == true
        }
    }
    val fans = remember(candidate.id) {
        controls.fanModes.filter { value ->
            CatalogTransmitter.fan(value)?.let { fan -> protocol == null || fan in protocol.fanSpeeds } == true
        }
    }
    val temperatureRange = controls.temperatureRange
        ?: protocol?.let { it.minTemperatureCelsius..it.maxTemperatureCelsius }

    val initialMode = modes.firstOrNull()
        ?: protocol?.modes?.firstOrNull()?.name?.lowercase()
        ?: "cool"
    val initialFan = fans.firstOrNull()
        ?: protocol?.fanSpeeds?.firstOrNull()?.let(::fanKey)
        ?: "auto"
    val initialTemperature = temperatureRange?.let { 24.coerceIn(it.first, it.last) } ?: 24

    var power by remember(candidate.id) { mutableStateOf<Boolean?>(null) }
    var temperature by remember(candidate.id) { mutableIntStateOf(initialTemperature) }
    var mode by remember(candidate.id) { mutableStateOf(initialMode) }
    var fan by remember(candidate.id) { mutableStateOf(initialFan) }
    var swingVertical by remember(candidate.id) { mutableStateOf(false) }
    var swingHorizontal by remember(candidate.id) { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var feedbackError by remember { mutableStateOf(false) }

    val ready = hardwareReady && transmittable
    val showVerticalSwing = controls.verticalSwing.type == "ON_OFF" || controls.verticalSwing.type == "AUTO_AND_POSITIONS"
    val showHorizontalSwing = controls.horizontalSwing.type == "ON_OFF" || controls.horizontalSwing.type == "AUTO_AND_POSITIONS"
    val verified = remote.verifiedCapabilities.isNotEmpty() && transmittable

    fun transmit(
        nextPower: Boolean = power ?: true,
        nextTemperature: Int = temperature,
        nextMode: String = mode,
        nextFan: String = fan,
        nextVertical: Boolean = swingVertical,
        nextHorizontal: Boolean = swingHorizontal,
    ): Boolean {
        if (!hardwareReady) {
            feedback = "Điện thoại chưa nhận diện được bộ phát IR."
            feedbackError = true
            return false
        }
        if (!transmittable) {
            feedback = "Remote này chưa thể phát trên thiết bị."
            feedbackError = true
            return false
        }

        if (
            candidate.encodingType.equals("RAW_PROFILE", true) &&
            nextPower && power != true &&
            (modes.isEmpty() || fans.isEmpty() || temperatureRange == null)
        ) {
            val explicitOn = CatalogTransmitter.explicitPowerOn(candidate)
            if (explicitOn != null) {
                return runCatching {
                    AndroidIrTransmitter.from(context).transmit(explicitOn)
                }.fold(
                    onSuccess = {
                        power = true
                        feedback = "Đã phát lệnh bật."
                        feedbackError = false
                        true
                    },
                    onFailure = {
                        feedback = "Không thể phát tín hiệu IR."
                        feedbackError = true
                        false
                    },
                )
            }
        }

        val selectedMode = CatalogTransmitter.mode(nextMode)
            ?: protocol?.modes?.firstOrNull()
            ?: if (!nextPower && candidate.encodingType.equals("RAW_PROFILE", true)) AcMode.COOL else null
            ?: run {
                feedback = "Chế độ này chưa được remote hỗ trợ."
                feedbackError = true
                return false
            }
        val selectedFan = CatalogTransmitter.fan(nextFan)
            ?: protocol?.fanSpeeds?.firstOrNull()
            ?: if (!nextPower && candidate.encodingType.equals("RAW_PROFILE", true)) AcFan.AUTO else null
            ?: run {
                feedback = "Tốc độ quạt này chưa được remote hỗ trợ."
                feedbackError = true
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
                        swingVertical = nextVertical,
                        swingHorizontal = nextHorizontal,
                    )
                )
            )
        }.fold(
            onSuccess = {
                power = nextPower
                temperature = nextTemperature
                mode = nextMode
                fan = nextFan
                swingVertical = nextVertical
                swingHorizontal = nextHorizontal
                feedback = "Đã phát lệnh."
                feedbackError = false
                true
            },
            onFailure = {
                feedback = "Không thể phát tín hiệu IR."
                feedbackError = true
                false
            },
        )
    }

    fun cycleMode() {
        if (modes.isEmpty()) return
        val current = modes.indexOfFirst { it.equals(mode, true) }.coerceAtLeast(0)
        transmit(nextMode = modes[(current + 1) % modes.size])
    }

    fun cycleFan() {
        if (fans.isEmpty()) return
        val current = fans.indexOfFirst { it.equals(fan, true) }.coerceAtLeast(0)
        transmit(nextFan = fans[(current + 1) % fans.size])
    }

    AppScaffold("remote", onTab) { padding ->
        PageColumn(padding) {
            RemoteTopHeader(
                remote = remote,
                candidate = candidate,
                verified = verified,
                ready = ready,
                onDetails = onDetails,
                onBack = onBack,
            )

            RemoteHeroCard(
                brand = remote.brand,
                temperature = temperature,
                power = power,
                temperatureEnabled = temperatureRange != null && ready,
                powerEnabled = controls.power && ready,
                onMinus = {
                    val range = temperatureRange ?: return@RemoteHeroCard
                    if (temperature > range.first) transmit(nextTemperature = temperature - 1)
                },
                onPlus = {
                    val range = temperatureRange ?: return@RemoteHeroCard
                    if (temperature < range.last) transmit(nextTemperature = temperature + 1)
                },
                onPower = { transmit(nextPower = power != true) },
                onMode = ::cycleMode,
                onFan = ::cycleFan,
                modeLabel = modes.firstOrNull { it.equals(mode, true) }?.let(::modeLabel) ?: "Chế độ",
                fanLabel = fans.firstOrNull { it.equals(fan, true) }?.let(::fanLabel) ?: "Quạt",
                quickModeEnabled = modes.size > 1 && ready,
                quickFanEnabled = fans.size > 1 && ready,
            )

            if (!ready) {
                InfoBanner(
                    if (!hardwareReady)
                        "Bộ phát IR chưa sẵn sàng. Kiểm tra phần cứng trong Cài đặt."
                    else
                        "Remote này chưa thể phát. Hãy kiểm tra lại hoặc dò mã khác.",
                    Icons.Filled.ErrorOutline,
                    AppColors.danger,
                    AppColors.paleDanger,
                )
            }

            if (modes.isNotEmpty()) {
                RemoteModePanel(
                    modes = modes,
                    selected = mode,
                    enabled = ready,
                    onSelect = { transmit(nextMode = it) },
                )
            }

            if (fans.isNotEmpty()) {
                RemoteFanPanel(
                    fans = fans,
                    selected = fan,
                    enabled = ready,
                    onSelect = { transmit(nextFan = it) },
                )
            }

            if (showVerticalSwing || showHorizontalSwing) {
                RemoteSwingPanel(
                    verticalVisible = showVerticalSwing,
                    horizontalVisible = showHorizontalSwing,
                    vertical = swingVertical,
                    horizontal = swingHorizontal,
                    enabled = ready,
                    onVertical = { transmit(nextVertical = it) },
                    onHorizontal = { transmit(nextHorizontal = it) },
                )
            }

            // Special features are intentionally not rendered until the encoder
            // can represent them. Showing a dead Turbo/Eco/Quiet switch would
            // make the screen look complete while producing no valid IR state.

            if (feedback.isNotBlank()) {
                InfoBanner(
                    feedback,
                    if (feedbackError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                    if (feedbackError) AppColors.danger else AppColors.mint,
                    if (feedbackError) AppColors.paleDanger else AppColors.paleMint,
                )
            }

            Text(
                "Chạm vào nút là app phát lệnh ngay tới máy lạnh.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = AppColors.navySoft,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RemoteTopHeader(
    remote: SavedRemote,
    candidate: RemoteCandidate,
    verified: Boolean,
    ready: Boolean,
    onDetails: () -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp).clickable(onClick = onBack),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, AppColors.line),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ArrowBack, "Quay lại", tint = AppColors.navy)
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    remote.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.navy,
                )
                Text(
                    listOfNotNull(remote.brand, candidate.displayModelLabel().takeIf(String::isNotBlank)).joinToString(" • "),
                    color = AppColors.navySoft,
                )
            }

            HeaderIconButton(Icons.Filled.Tune, "Chi tiết máy lạnh", onDetails)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                if (verified) "Đã xác minh" else "Chưa xác minh",
                if (verified) Icons.Filled.CheckCircle else Icons.Filled.Refresh,
                if (verified) AppColors.mint else AppColors.warning,
                if (verified) AppColors.paleMint else AppColors.paleWarning,
            )
            StatusChip(
                if (ready) "IR sẵn sàng" else "IR chưa sẵn sàng",
                Icons.Filled.SignalCellularAlt,
                if (ready) AppColors.blue else AppColors.danger,
                if (ready) AppColors.paleBlue else AppColors.paleDanger,
            )
        }
    }
}

@Composable
private fun RemoteHeroCard(
    brand: String,
    temperature: Int,
    power: Boolean?,
    temperatureEnabled: Boolean,
    powerEnabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onPower: () -> Unit,
    onMode: () -> Unit,
    onFan: () -> Unit,
    modeLabel: String,
    fanLabel: String,
    quickModeEnabled: Boolean,
    quickFanEnabled: Boolean,
) {
    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AcWallUnitArt(brand, Modifier.width(112.dp).height(78.dp))

                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Nhiệt độ đặt", color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (temperatureEnabled) "${temperature}°C" else "—",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.navy,
                    )
                    Text(
                        when (power) {
                            true -> "Đang bật"
                            false -> "Đang tắt"
                            null -> "Chưa đồng bộ"
                        },
                        color = when (power) {
                            true -> AppColors.mint
                            false -> AppColors.navySoft
                            null -> AppColors.warning
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    modifier = Modifier.size(82.dp).clickable(enabled = powerEnabled, onClick = onPower),
                    shape = RoundedCornerShape(41.dp),
                    color = if (power == true) AppColors.mint else AppColors.blue,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.PowerSettingsNew,
                            "Bật hoặc tắt",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RemoteQuickAction(
                    "−",
                    "Giảm nhiệt",
                    Icons.Filled.Thermostat,
                    Modifier.weight(1f),
                    temperatureEnabled,
                    onMinus,
                )
                RemoteQuickAction(
                    "+",
                    "Tăng nhiệt",
                    Icons.Filled.Thermostat,
                    Modifier.weight(1f),
                    temperatureEnabled,
                    onPlus,
                )
                RemoteQuickAction(
                    "",
                    modeLabel,
                    Icons.Filled.Tune,
                    Modifier.weight(1f),
                    quickModeEnabled,
                    onMode,
                )
                RemoteQuickAction(
                    "",
                    fanLabel,
                    Icons.Filled.Air,
                    Modifier.weight(1f),
                    quickFanEnabled,
                    onFan,
                )
            }
        }
    }
}

@Composable
private fun RemoteQuickAction(
    symbol: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(82.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (enabled) Color.White else AppColors.page,
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (symbol.isNotBlank()) {
                Text(symbol, style = MaterialTheme.typography.headlineSmall, color = AppColors.blue, fontWeight = FontWeight.Bold)
            } else {
                Icon(icon, null, tint = AppColors.blue, modifier = Modifier.size(25.dp))
            }
            Text(
                label,
                textAlign = TextAlign.Center,
                color = if (enabled) AppColors.navy else AppColors.navySoft,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RemoteModePanel(
    modes: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    RemoteSectionCard(
        title = "Chế độ hoạt động",
        subtitle = "Chọn chế độ phù hợp",
        icon = Icons.Filled.Tune,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.take(5).forEach { value ->
                RemoteModeTile(
                    value = value,
                    selected = value.equals(selected, true),
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(value) },
                )
            }
        }
    }
}

@Composable
private fun RemoteModeTile(
    value: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val icon = when (value.lowercase()) {
        "cool" -> Icons.Filled.AcUnit
        "heat" -> Icons.Filled.WbSunny
        "fan" -> Icons.Filled.Air
        "auto" -> Icons.Filled.Refresh
        else -> Icons.Filled.Thermostat
    }
    Surface(
        modifier = modifier.height(92.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AppColors.blue else AppColors.page,
        border = BorderStroke(1.dp, if (selected) AppColors.blue else AppColors.line),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = if (selected) Color.White else AppColors.navySoft, modifier = Modifier.size(28.dp))
            Text(
                modeLabel(value),
                color = if (selected) Color.White else AppColors.navy,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RemoteFanPanel(
    fans: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    RemoteSectionCard(
        title = "Tốc độ quạt",
        subtitle = "Điều chỉnh lượng gió",
        icon = Icons.Filled.Air,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fans.take(4).forEach { value ->
                val active = value.equals(selected, true)
                Surface(
                    modifier = Modifier.weight(1f).height(72.dp).clickable(enabled = enabled) { onSelect(value) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) AppColors.blue else AppColors.page,
                    border = BorderStroke(1.dp, if (active) AppColors.blue else AppColors.line),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.SignalCellularAlt, null, tint = if (active) Color.White else AppColors.navySoft)
                        Text(
                            fanLabel(value),
                            color = if (active) Color.White else AppColors.navy,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteSwingPanel(
    verticalVisible: Boolean,
    horizontalVisible: Boolean,
    vertical: Boolean,
    horizontal: Boolean,
    enabled: Boolean,
    onVertical: (Boolean) -> Unit,
    onHorizontal: (Boolean) -> Unit,
) {
    RemoteSectionCard(
        title = "Hướng gió",
        subtitle = "Điều chỉnh đảo gió",
        icon = Icons.Filled.Air,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (verticalVisible) {
                SwingSegmentRow(
                    title = "Dọc (Lên/Xuống)",
                    active = vertical,
                    enabled = enabled,
                    onChange = onVertical,
                )
            }
            if (horizontalVisible) {
                SwingSegmentRow(
                    title = "Ngang (Trái/Phải)",
                    active = horizontal,
                    enabled = enabled,
                    onChange = onHorizontal,
                )
            }
        }
    }
}

@Composable
private fun SwingSegmentRow(
    title: String,
    active: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = AppColors.navy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwingChoice("Cố định", !active, enabled, Modifier.weight(1f)) { onChange(false) }
            SwingChoice("Tự động", active, enabled, Modifier.weight(1f)) { onChange(true) }
        }
    }
}

@Composable
private fun SwingChoice(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(52.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) AppColors.blue else AppColors.page,
        border = BorderStroke(1.dp, if (selected) AppColors.blue else AppColors.line),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (selected) Color.White else AppColors.navy,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RemoteSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    SurfaceCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBubble(icon, size = 42)
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                    Text(subtitle, color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall)
                }
            }
            content()
        }
    }
}

private fun fanKey(value: AcFan): String = when (value) {
    AcFan.AUTO -> "auto"
    AcFan.MIN -> "low"
    AcFan.MEDIUM -> "medium"
    AcFan.HIGH -> "high"
}
