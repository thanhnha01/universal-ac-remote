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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.thanhnha.universalacremote.ir.compactDisplayModelLabel
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

    val safeState = remember(candidate.id) { CatalogTransmitter.safeProbe(candidate)?.state }
    val initialMode = safeState?.mode?.let { safeMode ->
        modes.firstOrNull { CatalogTransmitter.mode(it) == safeMode }
    } ?: modes.firstOrNull()
        ?: protocol?.modes?.firstOrNull()?.name?.lowercase()
        ?: "cool"
    val initialFan = safeState?.fan?.let { safeFan ->
        fans.firstOrNull { CatalogTransmitter.fan(it) == safeFan }
    } ?: fans.firstOrNull()
        ?: protocol?.fanSpeeds?.firstOrNull()?.let(::fanKey)
        ?: "auto"
    val initialTemperature = safeState?.temperatureCelsius
        ?.takeIf { temperatureRange == null || it in temperatureRange }
        ?: temperatureRange?.let { 24.coerceIn(it.first, it.last) }
        ?: 24

    var power by remember(candidate.id) { mutableStateOf<Boolean?>(null) }
    var temperature by remember(candidate.id) { mutableIntStateOf(initialTemperature) }
    var mode by remember(candidate.id) { mutableStateOf(initialMode) }
    var fan by remember(candidate.id) { mutableStateOf(initialFan) }
    var swingVertical by remember(candidate.id) { mutableStateOf(safeState?.swingVertical ?: false) }
    var swingHorizontal by remember(candidate.id) { mutableStateOf(safeState?.swingHorizontal ?: false) }
    var feedback by remember { mutableStateOf("") }
    var feedbackError by remember { mutableStateOf(false) }
    var hasSentState by remember(candidate.id) { mutableStateOf(false) }

    val ready = hardwareReady && transmittable
    val verification = remote.verificationState(candidate)
    val target = remember(candidate.id, candidate.protocolModel, protocol?.id) {
        if (protocol != null) candidate.copy(protocolModel = CatalogTransmitter.modelId(candidate, protocol)) else candidate
    }

    fun selectedMode(value: String): AcMode? = CatalogTransmitter.mode(value)
        ?: protocol?.modes?.firstOrNull()

    fun selectedFan(value: String): AcFan? = CatalogTransmitter.fan(value)
        ?: protocol?.fanSpeeds?.firstOrNull()

    fun resolvedState(
        nextPower: Boolean = true,
        nextTemperature: Int = temperature,
        nextMode: String = mode,
        nextFan: String = fan,
        nextVertical: Boolean = swingVertical,
        nextHorizontal: Boolean = swingHorizontal,
        lockTemperature: Boolean = false,
        lockMode: Boolean = false,
        lockFan: Boolean = false,
        lockVertical: Boolean = false,
        lockHorizontal: Boolean = false,
    ): AcState? {
        val resolvedMode = selectedMode(nextMode)
            ?: if (!nextPower && candidate.encodingType.equals("RAW_PROFILE", true)) AcMode.COOL else null
            ?: return null
        val resolvedFan = selectedFan(nextFan)
            ?: if (!nextPower && candidate.encodingType.equals("RAW_PROFILE", true)) AcFan.AUTO else null
            ?: return null
        return CatalogTransmitter.resolveState(
            target,
            AcState(
                power = nextPower,
                temperatureCelsius = nextTemperature,
                mode = resolvedMode,
                fan = resolvedFan,
                swingVertical = nextVertical,
                swingHorizontal = nextHorizontal,
            ),
            lockTemperature = lockTemperature,
            lockMode = lockMode,
            lockFan = lockFan,
            lockSwingVertical = lockVertical,
            lockSwingHorizontal = lockHorizontal,
        )
    }

    val selectableModes = modes.filter { value ->
        val requested = selectedMode(value) ?: return@filter false
        resolvedState(nextMode = value, lockMode = true)?.mode == requested
    }
    val selectableFans = fans.filter { value ->
        val requested = selectedFan(value) ?: return@filter false
        resolvedState(nextFan = value, lockFan = true)?.fan == requested
    }
    val canDecreaseTemperature = temperatureRange?.let { range ->
        temperature > range.first &&
            resolvedState(nextTemperature = temperature - 1, lockTemperature = true)?.temperatureCelsius == temperature - 1
    } == true
    val canIncreaseTemperature = temperatureRange?.let { range ->
        temperature < range.last &&
            resolvedState(nextTemperature = temperature + 1, lockTemperature = true)?.temperatureCelsius == temperature + 1
    } == true
    val verticalSupported = controls.verticalSwing.type == "ON_OFF" || controls.verticalSwing.type == "AUTO_AND_POSITIONS"
    val horizontalSupported = controls.horizontalSwing.type == "ON_OFF" || controls.horizontalSwing.type == "AUTO_AND_POSITIONS"
    val verticalFixedEnabled = verticalSupported && resolvedState(nextVertical = false, lockVertical = true)?.swingVertical == false
    val verticalAutoEnabled = verticalSupported && resolvedState(nextVertical = true, lockVertical = true)?.swingVertical == true
    val horizontalFixedEnabled = horizontalSupported && resolvedState(nextHorizontal = false, lockHorizontal = true)?.swingHorizontal == false
    val horizontalAutoEnabled = horizontalSupported && resolvedState(nextHorizontal = true, lockHorizontal = true)?.swingHorizontal == true
    val showVerticalSwing = verticalFixedEnabled || verticalAutoEnabled
    val showHorizontalSwing = horizontalFixedEnabled || horizontalAutoEnabled
    val canPowerOn = CatalogTransmitter.explicitPowerOn(candidate) != null || resolvedState(nextPower = true) != null
    val canPowerOff = resolvedState(nextPower = false) != null
    val powerVisible = controls.power && (canPowerOn || (power == true && canPowerOff))
    val powerActionEnabled = ready && if (power == true) canPowerOff else canPowerOn

    fun transmit(
        nextPower: Boolean = true,
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
                        hasSentState = true
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

        val resolved = resolvedState(
            nextPower = nextPower,
            nextTemperature = nextTemperature,
            nextMode = nextMode,
            nextFan = nextFan,
            nextVertical = nextVertical,
            nextHorizontal = nextHorizontal,
            lockTemperature = nextTemperature != temperature,
            lockMode = !nextMode.equals(mode, true),
            lockFan = !nextFan.equals(fan, true),
            lockVertical = nextVertical != swingVertical,
            lockHorizontal = nextHorizontal != swingHorizontal,
        ) ?: run {
            feedback = "Tổ hợp điều khiển này không có mã IR tương ứng."
            feedbackError = true
            return false
        }

        return runCatching {
            AndroidIrTransmitter.from(context).transmit(CatalogTransmitter.encode(target, resolved))
        }.fold(
            onSuccess = {
                power = resolved.power
                temperature = resolved.temperatureCelsius
                mode = modes.firstOrNull { CatalogTransmitter.mode(it) == resolved.mode } ?: modeKey(resolved.mode)
                fan = fans.firstOrNull { CatalogTransmitter.fan(it) == resolved.fan } ?: fanKey(resolved.fan)
                swingVertical = resolved.swingVertical
                swingHorizontal = resolved.swingHorizontal
                hasSentState = true
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
        if (selectableModes.isEmpty()) return
        val current = selectableModes.indexOfFirst { it.equals(mode, true) }.coerceAtLeast(0)
        transmit(nextMode = selectableModes[(current + 1) % selectableModes.size])
    }

    fun cycleFan() {
        if (selectableFans.isEmpty()) return
        val current = selectableFans.indexOfFirst { it.equals(fan, true) }.coerceAtLeast(0)
        transmit(nextFan = selectableFans[(current + 1) % selectableFans.size])
    }

    AppScaffold("remote", onTab) { padding ->
        PageColumn(padding) {
            RemoteTopHeader(
                remote = remote,
                candidate = candidate,
                verification = verification,
                irReady = hardwareReady,
                onDetails = onDetails,
                onBack = onBack,
            )

            RemoteHeroCard(
                brand = remote.brand,
                temperature = temperature,
                power = power,
                hasSentState = hasSentState,
                temperatureVisible = temperatureRange != null,
                decreaseTemperatureEnabled = ready && canDecreaseTemperature,
                increaseTemperatureEnabled = ready && canIncreaseTemperature,
                powerVisible = powerVisible,
                powerEnabled = powerActionEnabled,
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
                modeLabel = selectableModes.firstOrNull { it.equals(mode, true) }?.let(::modeLabel) ?: "Chế độ",
                fanLabel = selectableFans.firstOrNull { it.equals(fan, true) }?.let(::fanLabel) ?: "Quạt",
                quickModeVisible = selectableModes.size > 1,
                quickModeEnabled = selectableModes.size > 1 && ready,
                quickFanVisible = selectableFans.size > 1,
                quickFanEnabled = selectableFans.size > 1 && ready,
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

            if (selectableModes.isNotEmpty()) {
                RemoteModePanel(
                    modes = selectableModes,
                    selected = mode,
                    enabled = ready,
                    onSelect = { transmit(nextMode = it) },
                )
            }

            if (selectableFans.isNotEmpty()) {
                RemoteFanPanel(
                    fans = selectableFans,
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
                    verticalFixedEnabled = ready && verticalFixedEnabled,
                    verticalAutoEnabled = ready && verticalAutoEnabled,
                    horizontalFixedEnabled = ready && horizontalFixedEnabled,
                    horizontalAutoEnabled = ready && horizontalAutoEnabled,
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
    verification: SavedVerificationState,
    irReady: Boolean,
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(remote.brand, candidate.compactDisplayModelLabel().takeIf(String::isNotBlank)).joinToString(" • "),
                    color = AppColors.navySoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            HeaderIconButton(Icons.Filled.Tune, "Chi tiết máy lạnh", onDetails)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                when (verification) {
                    SavedVerificationState.FULL -> "Đã xác minh"
                    SavedVerificationState.PARTIAL -> "Đã kiểm tra một phần"
                    SavedVerificationState.NONE -> "Chưa xác minh"
                },
                when (verification) {
                    SavedVerificationState.FULL -> Icons.Filled.CheckCircle
                    SavedVerificationState.PARTIAL -> Icons.Filled.Info
                    SavedVerificationState.NONE -> Icons.Filled.Refresh
                },
                when (verification) {
                    SavedVerificationState.FULL -> AppColors.mint
                    SavedVerificationState.PARTIAL -> AppColors.warning
                    SavedVerificationState.NONE -> AppColors.warning
                },
                when (verification) {
                    SavedVerificationState.FULL -> AppColors.paleMint
                    SavedVerificationState.PARTIAL -> AppColors.paleWarning
                    SavedVerificationState.NONE -> AppColors.paleWarning
                },
            )
            StatusChip(
                if (irReady) "IR sẵn sàng" else "IR chưa sẵn sàng",
                Icons.Filled.SignalCellularAlt,
                if (irReady) AppColors.blue else AppColors.danger,
                if (irReady) AppColors.paleBlue else AppColors.paleDanger,
            )
        }
    }
}

@Composable
private fun RemoteHeroCard(
    brand: String,
    temperature: Int,
    power: Boolean?,
    hasSentState: Boolean,
    temperatureVisible: Boolean,
    decreaseTemperatureEnabled: Boolean,
    increaseTemperatureEnabled: Boolean,
    powerVisible: Boolean,
    powerEnabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onPower: () -> Unit,
    onMode: () -> Unit,
    onFan: () -> Unit,
    modeLabel: String,
    fanLabel: String,
    quickModeVisible: Boolean,
    quickModeEnabled: Boolean,
    quickFanVisible: Boolean,
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
                    Text(
                        if (hasSentState) "Nhiệt độ đã gửi" else "Mức đặt khi gửi",
                        color = AppColors.navySoft,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        if (temperatureVisible) "${temperature}°C" else "—",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.navy,
                    )
                    Text(
                        when (power) {
                            true -> "Đang bật"
                            false -> "Đang tắt"
                            null -> "Chưa gửi trạng thái"
                        },
                        color = when (power) {
                            true -> AppColors.mint
                            false -> AppColors.navySoft
                            null -> AppColors.warning
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (powerVisible) {
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
            }

            if (temperatureVisible || quickModeVisible || quickFanVisible) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (temperatureVisible) {
                        RemoteQuickAction(
                            "−",
                            "Giảm nhiệt",
                            Icons.Filled.Thermostat,
                            Modifier.weight(1f),
                            decreaseTemperatureEnabled,
                            onMinus,
                        )
                        RemoteQuickAction(
                            "+",
                            "Tăng nhiệt",
                            Icons.Filled.Thermostat,
                            Modifier.weight(1f),
                            increaseTemperatureEnabled,
                            onPlus,
                        )
                    }
                    if (quickModeVisible) {
                        RemoteQuickAction(
                            "",
                            modeLabel,
                            Icons.Filled.Tune,
                            Modifier.weight(1f),
                            quickModeEnabled,
                            onMode,
                        )
                    }
                    if (quickFanVisible) {
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
    verticalFixedEnabled: Boolean,
    verticalAutoEnabled: Boolean,
    horizontalFixedEnabled: Boolean,
    horizontalAutoEnabled: Boolean,
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
                    fixedEnabled = verticalFixedEnabled,
                    autoEnabled = verticalAutoEnabled,
                    onChange = onVertical,
                )
            }
            if (horizontalVisible) {
                SwingSegmentRow(
                    title = "Ngang (Trái/Phải)",
                    active = horizontal,
                    fixedEnabled = horizontalFixedEnabled,
                    autoEnabled = horizontalAutoEnabled,
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
    fixedEnabled: Boolean,
    autoEnabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = AppColors.navy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SwingChoice("Cố định", !active, fixedEnabled, Modifier.weight(1f)) { onChange(false) }
            SwingChoice("Tự động", active, autoEnabled, Modifier.weight(1f)) { onChange(true) }
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

private fun modeKey(value: AcMode): String = when (value) {
    AcMode.AUTO -> "auto"
    AcMode.COOL -> "cool"
    AcMode.DRY -> "dry"
    AcMode.FAN -> "fan"
    AcMode.HEAT -> "heat"
}

private fun fanKey(value: AcFan): String = when (value) {
    AcFan.AUTO -> "auto"
    AcFan.MIN -> "low"
    AcFan.MEDIUM -> "medium"
    AcFan.HIGH -> "high"
}
