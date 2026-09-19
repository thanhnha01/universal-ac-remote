package com.thanhnha.universalacremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.thanhnha.universalacremote.ir.RemoteQuery
import com.thanhnha.universalacremote.ir.ScanResult
import com.thanhnha.universalacremote.ir.ScanState
import com.thanhnha.universalacremote.ir.UniversalAcScanner
import com.thanhnha.universalacremote.ir.VerificationCheck
import com.thanhnha.universalacremote.ir.displayModelLabel
import com.thanhnha.universalacremote.update.UpdatePanel
import java.util.UUID

@Composable
fun ProductionHomeScreen(
    state: HomeUiState,
    diagnostics: IrHardwareDiagnostics,
    store: SavedRemotesViewModel,
    onTab: (String) -> Unit,
    navigate: (String) -> Unit,
) {
    AppScaffold("home", onTab) { padding ->
        PageColumn(padding) {
            BrandHeader {
                HeaderIconButton(Icons.Filled.Search, "Thêm máy lạnh") { navigate("add") }
            }

            SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconBubble(Icons.Filled.AcUnit, size = 62)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (state.remotes.isEmpty()) "Thêm máy lạnh đầu tiên" else "Điều khiển nhanh",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.navy,
                        )
                        Text(
                            if (state.remotes.isEmpty()) "Tìm model, dò 1000-in-1 hoặc nhập file .ir."
                            else "${state.remotes.size} remote đã lưu trên thiết bị.",
                            color = AppColors.navySoft,
                        )
                    }
                    HeaderIconButton(Icons.Filled.Add, "Thêm") { navigate("add") }
                }
            }

            if (!diagnostics.hasIrEmitter) {
                InfoBanner(
                    "Android chưa xác nhận được bộ phát IR. Bạn vẫn có thể quản lý hồ sơ nhưng chưa thể phát lệnh.",
                    Icons.Filled.ErrorOutline,
                    AppColors.danger,
                    AppColors.paleDanger,
                )
            }

            SectionTitle("Máy lạnh của bạn")
            when {
                state.loading -> SurfaceCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(22.dp))
                        Text("Đang tải remote đã lưu…", color = AppColors.navySoft)
                    }
                }
                state.remotes.isEmpty() -> EmptyState(
                    "Chưa có remote",
                    "Thêm máy lạnh để bắt đầu điều khiển.",
                    Icons.Outlined.AcUnit,
                )
                else -> state.remotes.forEach { remote ->
                    val profile = store.profileFor(remote.catalogProfileId)
                    val transmittable = remote.importedCommandsJson.isNotBlank() ||
                        (profile != null && CatalogTransmitter.supports(profile))
                    val verified = remote.verifiedCapabilities.isNotEmpty() && transmittable
                    SurfaceCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { navigate("remote/${remote.id}") }
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            AcWallUnitArt(remote.brand, Modifier.width(110.dp).height(72.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    remote.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.navy,
                                )
                                Text(
                                    listOfNotNull(remote.brand, remote.acModel, remote.remoteModel)
                                        .filter(String::isNotBlank)
                                        .joinToString(" • "),
                                    color = AppColors.navySoft,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                StatusChip(
                                    when {
                                        verified -> "Đã xác minh"
                                        transmittable -> "Sẵn sàng thử"
                                        else -> "Cần kiểm tra lại"
                                    },
                                    when {
                                        verified -> Icons.Filled.CheckCircle
                                        transmittable -> Icons.Filled.SignalCellularAlt
                                        else -> Icons.Filled.Info
                                    },
                                    when {
                                        verified -> AppColors.mint
                                        transmittable -> AppColors.blue
                                        else -> AppColors.warning
                                    },
                                    when {
                                        verified -> AppColors.paleMint
                                        transmittable -> AppColors.paleBlue
                                        else -> AppColors.paleWarning
                                    },
                                )
                            }
                            Icon(Icons.Filled.ArrowForward, null, tint = AppColors.navySoft)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionAddScreen(
    store: SavedRemotesViewModel,
    catalog: CatalogUiState,
    onTab: (String) -> Unit,
    navigate: (String) -> Unit,
) {
    val popular by store.popularBrands.collectAsState()
    val search by store.search.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf<String?>(null) }

    fun searchNow(value: String) {
        query = value
        selectedBrand = popular.firstOrNull { it.equals(value.trim(), true) } ?: selectedBrand
        store.updateSearchText(value)
    }

    val usableResults = search.results.filter { CatalogTransmitter.supports(it) }
    val unavailableCount = search.results.size - usableResults.size

    AppScaffold("home", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar(
                "Thêm máy lạnh",
                "Chọn cách thiết lập phù hợp",
                onBack = { onTab("home") },
            )

            SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBubble(Icons.Filled.Search, size = 52)
                        Column {
                            Text("Tôi biết model", fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                            Text("Tìm theo hãng, model máy hoặc model remote.", color = AppColors.navySoft)
                        }
                    }
                    SearchField(query, ::searchNow, "Ví dụ: Daikin, FTXM35, ARC…")
                }
            }

            if (catalog.loading) {
                InfoBanner("Đang tải thư viện điều khiển…", Icons.Filled.Refresh, AppColors.blue, AppColors.paleBlue)
            }
            catalog.error?.let {
                InfoBanner("Không thể tải thư viện điều khiển.", Icons.Filled.ErrorOutline, AppColors.danger, AppColors.paleDanger)
            }

            if (query.isBlank()) {
                SectionTitle("Hãng phổ biến")
                if (popular.isEmpty() && !catalog.loading) {
                    EmptyState("Chưa có dữ liệu hãng", "Thư viện điều khiển chưa sẵn sàng.")
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(popular.take(12)) { brand ->
                            BrandChoiceChip(brand, selectedBrand.equals(brand, true)) {
                                selectedBrand = brand
                                query = brand
                                store.updateSearchText(brand)
                            }
                        }
                    }
                }
            }

            if (search.loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }

            if (query.isNotBlank() && !search.loading) {
                SectionTitle("Hồ sơ phù hợp")
                if (usableResults.isEmpty()) {
                    EmptyState(
                        "Chưa có hồ sơ có thể phát",
                        "Bạn có thể thử dò theo hãng hoặc nhập file .ir.",
                        Icons.Filled.Search,
                    )
                } else {
                    usableResults.take(16).forEach { candidate ->
                        UserProfileCard(candidate) {
                            store.beginScan(selected = candidate)
                            navigate("scan")
                        }
                    }
                }
                if (unavailableCount > 0) {
                    Text(
                        "$unavailableCount hồ sơ khác chưa có đường phát tương thích nên được ẩn khỏi danh sách.",
                        color = AppColors.navySoft,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            SectionTitle("Tôi không biết model")
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBubble(Icons.Outlined.Radio, background = AppColors.paleMint, tint = AppColors.mint)
                        Column(Modifier.weight(1f)) {
                            Text("Dò remote 1000-in-1", fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                            Text("Chọn hãng rồi thử lần lượt các hồ sơ có thể phát.", color = AppColors.navySoft)
                        }
                    }
                    if (popular.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(popular.take(12)) { brand ->
                                BrandChoiceChip(brand, selectedBrand.equals(brand, true)) {
                                    selectedBrand = brand
                                }
                            }
                        }
                    }
                    selectedBrand?.let { brand ->
                        PrimaryButton(
                            "Bắt đầu dò $brand",
                            Modifier.fillMaxWidth(),
                            Icons.Filled.PlayArrow,
                        ) {
                            store.beginScan(RemoteQuery(brand = brand))
                            navigate("scan")
                        }
                    } ?: InfoBanner(
                        "Chọn hãng máy lạnh trước khi bắt đầu dò.",
                        Icons.Filled.Info,
                        AppColors.blue,
                        AppColors.paleBlue,
                    )
                }
            }

            SectionTitle("Dữ liệu có sẵn")
            SurfaceCard(
                Modifier
                    .fillMaxWidth()
                    .clickable { navigate("import") }
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconBubble(Icons.Filled.FileDownload, background = Color(0xFFF3F0FF), tint = AppColors.purple)
                    Column(Modifier.weight(1f)) {
                        Text("Nhập file .ir", fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                        Text("Dùng file IR đã lưu trên điện thoại.", color = AppColors.navySoft)
                    }
                    Icon(Icons.Filled.ArrowForward, null, tint = AppColors.navySoft)
                }
            }
        }
    }
}

@Composable
fun ProductionScannerScreen(
    store: SavedRemotesViewModel,
    onTab: (String) -> Unit,
    onDone: () -> Unit,
    onChangeBrand: () -> Unit,
    onImport: () -> Unit,
) {
    val context = LocalContext.current
    val candidates by store.scanCandidates.collectAsState()
    val candidateKey = candidates.joinToString("|") { it.id }
    var refresh by remember { mutableIntStateOf(0) }
    var scanStarted by remember(candidateKey) { mutableStateOf(false) }
    var pendingCheck by remember { mutableStateOf<VerificationCheck?>(null) }
    var machineName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    @Suppress("UNUSED_VARIABLE") val stateRefresh = refresh

    val scanner = remember(candidateKey) {
        UniversalAcScanner(candidates) { candidate ->
            AndroidIrTransmitter.from(context).transmit(CatalogTransmitter.encodeSafeProbe(candidate))
        }
    }

    val current = scanner.selected ?: candidates.getOrNull(scanner.cursor)
    val controls = current?.let(RemoteControls::from)
    val requirements = controls?.verificationOrder().orEmpty()
    val nextCheck = if (scanner.state == ScanState.VERIFYING) scanner.nextVerificationCheck() else null
    val safeProbe = current?.let { CatalogTransmitter.safeProbe(it) }

    AppScaffold("scan", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar(
                "Dò remote 1000-in-1",
                current?.brand ?: "Chọn hãng để bắt đầu",
                onBack = onDone,
            )

            if (candidates.isEmpty()) {
                EmptyState(
                    "Chưa có hồ sơ có thể phát",
                    "Chọn hãng khác hoặc nhập file .ir để tiếp tục.",
                    Icons.Filled.Search,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Chọn hãng", Modifier.weight(1f), Icons.Filled.Refresh, onClick = onChangeBrand)
                    SecondaryButton("Nhập .ir", Modifier.weight(1f), Icons.Filled.FileDownload, onClick = onImport)
                }
                return@PageColumn
            }

            ScannerBrandHero(
                brand = current?.brand.orEmpty(),
                model = current?.displayModelLabel().orEmpty(),
                onChangeBrand = onChangeBrand,
            )

            if (!scanStarted && scanner.state == ScanState.READY) {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Chuẩn bị trước khi dò", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        ScannerInstruction("1", "Bật máy lạnh bằng remote gốc hoặc nút trên máy.")
                        ScannerInstruction("2", "Hướng đầu phát IR của điện thoại về máy lạnh.")
                        ScannerInstruction("3", "Mỗi lần app chỉ thử một hồ sơ và chờ bạn xác nhận.")
                        InfoBanner(
                            "Lệnh thử đầu tiên ưu tiên trạng thái BẬT an toàn, không dùng OFF làm probe ban đầu.",
                            Icons.Filled.Info,
                            AppColors.blue,
                            AppColors.paleBlue,
                        )
                        PrimaryButton("Bắt đầu dò", Modifier.fillMaxWidth(), Icons.Filled.PlayArrow) {
                            scanStarted = true
                        }
                    }
                }
            }

            if (scanStarted) {
                val progress = ((scanner.cursor + 1).toFloat() / candidates.size).coerceIn(0f, 1f)
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Hồ sơ ${scanner.cursor + 1} / ${candidates.size}",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.navy,
                            )
                            Text("${(progress * 100).toInt()}%", color = AppColors.blue, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = AppColors.blue,
                            trackColor = AppColors.paleBlueStrong,
                        )
                    }
                }

                if (scanner.state == ScanState.READY && current != null) {
                    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                AcWallUnitArt(current.brand, Modifier.width(126.dp).height(78.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(current.brand, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        current.displayModelLabel().ifBlank { "Model chưa xác định" },
                                        color = AppColors.navySoft,
                                    )
                                }
                            }
                            safeProbe?.warning?.let {
                                InfoBanner(it, Icons.Filled.Info, AppColors.warning, AppColors.paleWarning)
                            }
                            PrimaryButton(
                                safeProbe?.description ?: "Phát thử",
                                Modifier.fillMaxWidth(),
                                Icons.Filled.PlayArrow,
                            ) {
                                message = runCatching {
                                    scanner.tryCurrent(System.currentTimeMillis())
                                    "Đã phát tín hiệu. Chờ 2–3 giây rồi xác nhận phản ứng của máy."
                                }.getOrElse {
                                    "Không thể phát tín hiệu này. Hãy kiểm tra phần cứng IR hoặc thử hồ sơ khác."
                                }
                                refresh++
                            }
                        }
                    }
                }

                if (scanner.state == ScanState.AWAITING_FEEDBACK) {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Máy lạnh có phản ứng không?", fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                            Text("Beep, bật máy hoặc thay đổi trạng thái đều được xem là có phản ứng.", color = AppColors.navySoft)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PrimaryButton("Có phản ứng", Modifier.weight(1f), Icons.Filled.Check) {
                                    scanner.reportReaction()
                                    pendingCheck = null
                                    message = "Đã giữ hồ sơ này. Tiếp tục xác minh từng chức năng."
                                    refresh++
                                }
                                SecondaryButton("Không", Modifier.weight(1f), Icons.Filled.Close) {
                                    scanner.reportNoReaction()
                                    message = if (scanner.state == ScanState.COMPLETE) "Đã thử hết hồ sơ." else "Chuyển sang hồ sơ tiếp theo."
                                    refresh++
                                }
                            }
                        }
                    }
                }

                if (scanner.state == ScanState.VERIFYING && current != null) {
                    if (nextCheck != null) {
                        VerificationWizardCard(
                            check = nextCheck,
                            step = requirements.indexOf(nextCheck) + 1,
                            total = requirements.size,
                            waitingForAnswer = pendingCheck == nextCheck,
                            onSend = {
                                val selected = scanner.selected ?: return@VerificationWizardCard
                                message = runCatching {
                                    AndroidIrTransmitter.from(context).transmit(
                                        CatalogTransmitter.encode(
                                            selected,
                                            scannerVerificationState(selected, nextCheck, RemoteControls.from(selected)),
                                        )
                                    )
                                    pendingCheck = nextCheck
                                    "Đã gửi lệnh kiểm tra. Hãy quan sát máy lạnh."
                                }.getOrElse {
                                    "Không thể phát lệnh kiểm tra này."
                                }
                                refresh++
                            },
                            onPass = {
                                val selected = scanner.selected
                                scanner.recordVerification(nextCheck, supported = true)
                                pendingCheck = null
                                if (nextCheck == VerificationCheck.POWER && selected != null) {
                                    runCatching {
                                        AndroidIrTransmitter.from(context).transmit(CatalogTransmitter.encodeSafeProbe(selected))
                                    }
                                }
                                refresh++
                            },
                            onFail = {
                                scanner.recordVerification(nextCheck, supported = false)
                                pendingCheck = null
                                refresh++
                            },
                            onSkip = {
                                scanner.skipVerification(nextCheck)
                                pendingCheck = null
                                refresh++
                            },
                        )
                    } else {
                        PrimaryButton("Xem kết quả", Modifier.fillMaxWidth(), Icons.Filled.CheckCircle) {
                            scanner.finishVerification()
                            refresh++
                        }
                    }
                }

                if (scanner.state == ScanState.COMPLETE) {
                    val scanResult = scanner.result
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val title = when (scanResult) {
                                ScanResult.FULL_MATCH -> "Hồ sơ phù hợp"
                                ScanResult.PARTIAL_MATCH -> "Hồ sơ hoạt động một phần"
                                else -> "Chưa tìm thấy hồ sơ phù hợp"
                            }
                            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            when (scanResult) {
                                ScanResult.FULL_MATCH, ScanResult.PARTIAL_MATCH -> {
                                    OutlinedField("Tên máy lạnh", machineName, { machineName = it })
                                    PrimaryButton(
                                        "Lưu remote",
                                        Modifier.fillMaxWidth(),
                                        Icons.Filled.CheckCircle,
                                        enabled = machineName.isNotBlank() && scanner.selected != null,
                                    ) {
                                        val candidate = scanner.selected ?: return@PrimaryButton
                                        store.save(
                                            SavedRemote(
                                                id = UUID.randomUUID().toString(),
                                                displayName = machineName.trim(),
                                                catalogProfileId = candidate.id,
                                                brand = candidate.brand,
                                                acModel = candidate.acModel,
                                                remoteModel = candidate.remoteModel,
                                                protocolId = candidate.protocolId,
                                                protocolModel = candidate.protocolModel,
                                                verifiedCapabilities = scanner.verifiedCapabilities.map { it.name },
                                            )
                                        )
                                        onDone()
                                    }
                                    if (scanner.cursor + 1 < candidates.size) {
                                        SecondaryButton("Thử hồ sơ khác", Modifier.fillMaxWidth(), Icons.Filled.Refresh) {
                                            scanner.continueAfterResult()
                                            pendingCheck = null
                                            message = ""
                                            refresh++
                                        }
                                    }
                                }
                                else -> {
                                    SecondaryButton("Chọn hãng khác", Modifier.fillMaxWidth(), Icons.Filled.Refresh, onClick = onChangeBrand)
                                }
                            }
                        }
                    }
                }

                if (message.isNotBlank()) {
                    InfoBanner(
                        message,
                        if (message.startsWith("Không")) Icons.Filled.ErrorOutline else Icons.Filled.Info,
                        if (message.startsWith("Không")) AppColors.danger else AppColors.blue,
                        if (message.startsWith("Không")) AppColors.paleDanger else AppColors.paleBlue,
                    )
                }

                if (scanner.state != ScanState.COMPLETE && scanner.state != ScanState.STOPPED) {
                    TextButton(
                        onClick = {
                            scanner.stop()
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Stop, null, tint = AppColors.danger)
                        Text("  Dừng dò", color = AppColors.danger)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionSettingsScreen(
    diagnostics: IrHardwareDiagnostics,
    catalog: CatalogUiState,
    onTab: (String) -> Unit,
    openDiagnostics: () -> Unit,
    openImport: () -> Unit,
) {
    AppScaffold("settings", onTab) { padding ->
        PageColumn(padding) {
            SettingsHero()

            SectionTitle("Ứng dụng")
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    SourceInfoRow("Phiên bản ứng dụng", BuildConfig.VERSION_NAME, Icons.Filled.Build)
                    SourceInfoRow(
                        "Thư viện điều khiển",
                        if (catalog.loading) "Đang tải…" else "${catalog.profileCount} hồ sơ",
                        Icons.Filled.FilterAlt,
                    )
                    SourceInfoRow(
                        "Bộ phát IR",
                        if (diagnostics.hasIrEmitter) "Sẵn sàng" else "Không khả dụng",
                        Icons.Filled.SignalCellularAlt,
                        if (diagnostics.hasIrEmitter) AppColors.mint else AppColors.danger,
                    )
                }
            }

            SectionTitle("Cập nhật")
            SurfaceCard(
                Modifier.fillMaxWidth(),
                Brush.linearGradient(listOf(Color(0xFFE8F5FF), Color(0xFFDDF0FF))),
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconBubble(Icons.Filled.CloudDownload, size = 66, background = Color.White, tint = AppColors.blue)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Cập nhật ứng dụng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Bản mới bao gồm cả cập nhật thư viện IR.", color = AppColors.navySoft)
                    }
                }
            }
            UpdatePanel()

            SectionTitle("Công cụ")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToolCard(
                    title = "Phần cứng IR",
                    subtitle = "Kiểm tra thiết bị",
                    icon = Icons.Filled.SignalCellularAlt,
                    tint = AppColors.mint,
                    background = AppColors.paleMint,
                    modifier = Modifier.weight(1f),
                    onClick = openDiagnostics,
                )
                SettingsToolCard(
                    title = "Nhập file .ir",
                    subtitle = "Dùng file có sẵn",
                    icon = Icons.Filled.FileDownload,
                    tint = AppColors.purple,
                    background = Color(0xFFF4F0FF),
                    modifier = Modifier.weight(1f),
                    onClick = openImport,
                )
                SettingsToolCard(
                    title = "Dò remote",
                    subtitle = "1000-in-1",
                    icon = Icons.Outlined.Radio,
                    tint = AppColors.blue,
                    background = AppColors.paleBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onTab("scan") },
                )
            }

            SectionTitle("Thông tin nguồn")
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    ProductionSourceRow("IRremoteESP8266", "Bộ mã protocol máy lạnh")
                    ProductionSourceRow("SmartIR", "Hồ sơ điều khiển cộng đồng")
                    ProductionSourceRow("Flipper IRDB", "Nguồn file IR khi hợp lệ")
                    ProductionSourceRow("irplus", "Nguồn profile bổ sung")
                }
            }
        }
    }
}

@Composable
fun AcWallUnitArt(brand: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF8FBFF),
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                brand.ifBlank { "A/C" },
                color = AppColors.blue,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(AppColors.paleBlueStrong)
            )
        }
    }
}

@Composable
private fun BrandChoiceChip(brand: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AppColors.blue else Color.White,
        border = BorderStroke(1.dp, if (selected) AppColors.blue else AppColors.line),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                Icons.Filled.AcUnit,
                null,
                tint = if (selected) Color.White else AppColors.blue,
                modifier = Modifier.size(18.dp),
            )
            Text(
                brand,
                color = if (selected) Color.White else AppColors.navy,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun UserProfileCard(candidate: RemoteCandidate, onClick: () -> Unit) {
    SurfaceCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(Icons.Filled.AcUnit)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(candidate.brand, fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                Text(
                    candidate.displayModelLabel().ifBlank { "Model chưa xác định" },
                    color = AppColors.navySoft,
                )
                candidate.remoteModel?.takeIf(String::isNotBlank)?.let {
                    Text("Remote $it", color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall)
                }
            }
            StatusChip("Sẵn sàng thử", Icons.Filled.SignalCellularAlt)
        }
    }
}

@Composable
private fun ScannerBrandHero(brand: String, model: String, onChangeBrand: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AcWallUnitArt(brand, Modifier.width(126.dp).height(78.dp))
            Column(Modifier.weight(1f)) {
                Text(brand.ifBlank { "Máy lạnh" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(model.ifBlank { "Không rõ model" }, color = AppColors.navySoft)
            }
            TextButton(onClick = onChangeBrand) { Text("Đổi hãng") }
        }
    }
}

@Composable
private fun ScannerInstruction(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(shape = RoundedCornerShape(999.dp), color = AppColors.paleBlue) {
            Text(
                number,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                color = AppColors.blue,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(text, modifier = Modifier.weight(1f), color = AppColors.navy)
    }
}

@Composable
private fun VerificationWizardCard(
    check: VerificationCheck,
    step: Int,
    total: Int,
    waitingForAnswer: Boolean,
    onSend: () -> Unit,
    onPass: () -> Unit,
    onFail: () -> Unit,
    onSkip: () -> Unit,
) {
    val (title, instruction, action) = when (check) {
        VerificationCheck.TEMPERATURE_CHANGED -> Triple("Nhiệt độ", "Quan sát máy có nhận thay đổi nhiệt độ không.", "Gửi thử nhiệt độ")
        VerificationCheck.MODE -> Triple("Chế độ", "Quan sát máy có đổi chế độ hoạt động không.", "Gửi thử chế độ")
        VerificationCheck.FAN -> Triple("Quạt", "Quan sát tốc độ quạt có thay đổi không.", "Gửi thử quạt")
        VerificationCheck.SWING_VERTICAL -> Triple("Đảo gió dọc", "Quan sát cánh gió lên/xuống.", "Gửi thử đảo gió dọc")
        VerificationCheck.SWING_HORIZONTAL -> Triple("Đảo gió ngang", "Quan sát cánh gió trái/phải.", "Gửi thử đảo gió ngang")
        VerificationCheck.POWER -> Triple("Bật/Tắt nguồn", "Nguồn được kiểm tra cuối để không làm gián đoạn các bước trước.", "Thử tắt máy")
    }

    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusChip("Bước $step / $total", Icons.Filled.CheckCircle)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(instruction, color = AppColors.navySoft)
            PrimaryButton(action, Modifier.fillMaxWidth(), Icons.Filled.Send, enabled = !waitingForAnswer, onClick = onSend)
            if (waitingForAnswer) {
                Text("Kết quả trên máy lạnh?", fontWeight = FontWeight.Bold, color = AppColors.navy)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Đúng", Modifier.weight(1f), Icons.Filled.Check, onClick = onPass)
                    SecondaryButton("Không đúng", Modifier.weight(1f), Icons.Filled.Close, onClick = onFail)
                }
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Bỏ qua bước này") }
            }
        }
    }
}

private fun scannerVerificationState(
    candidate: RemoteCandidate,
    check: VerificationCheck,
    controls: RemoteControls,
): AcState {
    val definition = CatalogTransmitter.protocol(candidate)
    val modes = controls.modes.mapNotNull(CatalogTransmitter::mode)
        .filter { definition == null || it in definition.modes }
    val fans = controls.fanModes.mapNotNull(CatalogTransmitter::fan)
        .filter { definition == null || it in definition.fanSpeeds }
    val mode = modes.firstOrNull() ?: definition?.modes?.firstOrNull() ?: AcMode.COOL
    val fan = fans.firstOrNull() ?: definition?.fanSpeeds?.firstOrNull() ?: AcFan.AUTO
    val range = controls.temperatureRange
        ?: definition?.let { it.minTemperatureCelsius..it.maxTemperatureCelsius }
        ?: 16..30
    val baseTemp = 24.coerceIn(range.first, range.last)
    val changedTemp = if (range.first < range.last) (baseTemp + 1).coerceAtMost(range.last) else baseTemp

    return when (check) {
        VerificationCheck.POWER -> AcState(false, baseTemp, mode, fan)
        VerificationCheck.TEMPERATURE_CHANGED -> AcState(true, changedTemp, mode, fan)
        VerificationCheck.MODE -> AcState(true, baseTemp, modes.getOrNull(1) ?: mode, fan)
        VerificationCheck.FAN -> AcState(true, baseTemp, mode, fans.getOrNull(1) ?: fan)
        VerificationCheck.SWING_VERTICAL -> AcState(true, baseTemp, mode, fan, swingVertical = true)
        VerificationCheck.SWING_HORIZONTAL -> AcState(true, baseTemp, mode, fan, swingHorizontal = true)
    }
}

@Composable
private fun SettingsHero() {
    SurfaceCard(Modifier.fillMaxWidth(), Brush.linearGradient(listOf(Color.White, Color(0xFFEAF6FF)))) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconBubble(Icons.Filled.Settings, size = 62, background = Color(0xFF1687EE), tint = Color.White)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cài đặt", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Tùy chỉnh ứng dụng và kiểm tra trạng thái", color = AppColors.navySoft)
            }
            AcWallUnitArt("A/C", Modifier.width(110.dp).height(66.dp))
        }
    }
}

@Composable
private fun SettingsToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(142.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = background,
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconBubble(icon, tint = tint, background = Color.White, size = 46)
            Text(
                title,
                modifier = Modifier.padding(top = 9.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.navy,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                subtitle,
                textAlign = TextAlign.Center,
                color = AppColors.navySoft,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ProductionSourceRow(name: String, detail: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBubble(Icons.Filled.FilterAlt, size = 38)
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, color = AppColors.navy)
            Text(detail, color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall)
        }
    }
}
