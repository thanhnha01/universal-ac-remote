package com.thanhnha.universalacremote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thanhnha.universalacremote.ir.AcState
import com.thanhnha.universalacremote.ir.AndroidIrTransmitter
import com.thanhnha.universalacremote.ir.CatalogTransmitter
import com.thanhnha.universalacremote.ir.IrHardwareDiagnostics
import com.thanhnha.universalacremote.ir.RemoteCandidate
import com.thanhnha.universalacremote.ir.RemoteControls
import com.thanhnha.universalacremote.ir.RemoteQuery
import com.thanhnha.universalacremote.ir.ScanResult
import com.thanhnha.universalacremote.ir.ScanState
import com.thanhnha.universalacremote.ir.SwingControl
import com.thanhnha.universalacremote.ir.UniversalAcScanner
import com.thanhnha.universalacremote.ir.VerificationCheck
import com.thanhnha.universalacremote.ir.displayModelLabel
import com.thanhnha.universalacremote.ir.fanLabel
import com.thanhnha.universalacremote.ir.modeLabel
import com.thanhnha.universalacremote.ir.specialCapabilityLabel
import com.thanhnha.universalacremote.ir.swingPositionLabel
import com.thanhnha.universalacremote.update.UpdatePanel
import java.util.UUID

@Composable
fun RemoteApp(diagnostics: IrHardwareDiagnostics, store: SavedRemotesViewModel = viewModel()) {
    val nav = rememberNavController()
    val home by store.state.collectAsState()
    val catalog by store.catalog.collectAsState()

    fun navigateTab(route: String) {
        val resolvedRoute = if (route == "remote") {
            home.remotes.firstOrNull()?.let { "remote/${it.id}" } ?: "add"
        } else route
        nav.navigate(resolvedRoute) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    UniversalAcTheme {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") { HomeScreen(home, diagnostics, store, ::navigateTab) { route -> nav.navigate(route) } }
            composable("add") { AddScreen(store, catalog, ::navigateTab) { route -> nav.navigate(route) } }
            composable("import") { IrImportScreen(store, ::navigateTab, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack("home", false) }) }
            composable("scan") { ScannerScreen(store, ::navigateTab, onDone = { nav.popBackStack("home", false) }, onChangeBrand = { nav.navigate("add") }, onImport = { nav.navigate("import") }) }
            composable("remote/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                val remote = home.remotes.find { saved -> saved.id == it.arguments?.getString("id") }
                if (remote == null) MissingRemoteScreen { nav.popBackStack("home", false) }
                else if (remote.importedCommandsJson.isNotBlank()) ImportedRemoteScreen(remote, store) { nav.popBackStack() }
                else {
                    val profile = store.profileFor(remote.catalogProfileId)
                    when {
                        catalog.loading -> LoadingScreen { nav.popBackStack() }
                        profile == null -> MissingProfileScreen(remote) { store.beginScan(); nav.navigate("scan") }
                        else -> RemoteScreen(remote, profile) { nav.popBackStack() }
                    }
                }
            }
            composable("details/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                home.remotes.find { remote -> remote.id == it.arguments?.getString("id") }?.let { remote ->
                    DetailsScreen(remote, store.profileFor(remote.catalogProfileId), store, { nav.navigate("remote/${remote.id}") }, { store.beginScan(RemoteQuery(brand = remote.brand)); nav.navigate("scan") }, { nav.popBackStack() })
                }
            }
            composable("settings") { SettingsScreen(diagnostics, catalog, ::navigateTab, { nav.navigate("diagnostics") }, { nav.navigate("import") }) }
            composable("diagnostics") { DiagnosticScreen(diagnostics) }
        }
    }
}

@Composable
private fun HomeScreen(state: HomeUiState, diagnostics: IrHardwareDiagnostics, store: SavedRemotesViewModel, onTab: (String) -> Unit, navigate: (String) -> Unit) {
    var renameTarget by remember { mutableStateOf<SavedRemote?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedRemote?>(null) }
    var name by remember(renameTarget) { mutableStateOf(renameTarget?.displayName.orEmpty()) }
    AppScaffold("home", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar("Universal A/C Remote", "Điều khiển máy lạnh bằng hồng ngoại", actions = { IconButton(onClick = { onTab("settings") }) { Icon(Icons.Filled.Settings, "Cài đặt") } })
            GradientHero { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { IconBubble(Icons.Filled.AcUnit, tint = Color.White, background = AppColors.blue, size = 70); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("Remote của bạn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text(if (diagnostics.hasIrEmitter) "IR sẵn sàng để phát lệnh" else "Thiết bị chưa có bộ phát IR", color = AppColors.navySoft) } } }
            PrimaryButton("Thêm máy lạnh", Modifier.fillMaxWidth(), Icons.Filled.Add) { navigate("add") }
            if (state.loading) SurfaceCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp)); Text("Đang tải remote đã lưu…") } }
            else if (state.remotes.isEmpty()) EmptyState("Chưa có máy lạnh nào", "Thêm một máy lạnh để chọn profile, xác minh khả năng và lưu remote.", Icons.Filled.AcUnit)
            else {
                SectionTitle("Máy lạnh đã lưu")
                state.remotes.forEach { remote ->
                    val profile = store.profileFor(remote.catalogProfileId)
                    val verified = verifiedChecks(remote)
                    val compatibility = when { remote.importedCommandsJson.isNotBlank() -> "Profile đã nhập"; profile == null -> "Cần kiểm tra lại profile"; verified.isEmpty() -> "Chưa xác minh"; else -> "${verified.size} chức năng đã xác minh" }
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { IconBubble(Icons.Outlined.AcUnit); Column(Modifier.weight(1f)) { Text(remote.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text(listOfNotNull(remote.brand, remote.acModel, remote.remoteModel).joinToString(" • "), color = AppColors.navySoft) }; Icon(Icons.Filled.ChevronRight, "Mở remote", tint = AppColors.navySoft) }
                            StatusChip(compatibility, if (verified.isNotEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Info, if (verified.isNotEmpty()) AppColors.mint else AppColors.blue, if (verified.isNotEmpty()) AppColors.paleMint else AppColors.paleBlue)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PrimaryButton("Mở remote", Modifier.weight(1f), Icons.Filled.PlayArrow) { navigate("remote/${remote.id}") }; SecondaryButton("Chi tiết", Modifier.weight(1f), Icons.Filled.Info) { navigate("details/${remote.id}") } }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { renameTarget = remote }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Edit, null); Spacer(Modifier.size(6.dp)); Text("Đổi tên") }; TextButton(onClick = { deleteTarget = remote }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.DeleteOutline, null, tint = AppColors.danger); Spacer(Modifier.size(6.dp)); Text("Xóa", color = AppColors.danger) } }
                        }
                    }
                }
            }
        }
    }
    RenameDialog(renameTarget, name, { name = it }, { renameTarget?.let { store.rename(it.id, name) }; renameTarget = null }, { renameTarget = null })
    if (deleteTarget != null) AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Xóa remote?") }, text = { Text("${deleteTarget?.displayName} sẽ bị xóa khỏi thiết bị.") }, confirmButton = { TextButton(onClick = { deleteTarget?.let { store.delete(it.id) }; deleteTarget = null }) { Text("Xóa", color = AppColors.danger) } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Hủy") } })
}

@Composable
private fun AddScreen(store: SavedRemotesViewModel, catalog: CatalogUiState, onTab: (String) -> Unit, navigate: (String) -> Unit) {
    val popular by store.popularBrands.collectAsState()
    val search by store.search.collectAsState()
    var query by remember { mutableStateOf("") }
    fun searchNow(value: String) { query = value; store.updateSearch(RemoteQuery(brand = value.takeIf(String::isNotBlank))) }
    AppScaffold("home", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar("Thêm máy lạnh", "Chọn cách thiết lập phù hợp", onBack = { onTab("home") })
            SearchField(query, ::searchNow, "Tìm hãng, model máy hoặc remote")
            if (catalog.loading) InfoBanner("Đang tải Unified Catalog…", Icons.Filled.Refresh, AppColors.blue, AppColors.paleBlue)
            catalog.error?.let { InfoBanner(it, Icons.Filled.ErrorOutline, AppColors.danger, AppColors.paleDanger) }
            if (query.isBlank()) {
                SectionTitle("Hãng phổ biến")
                if (popular.isEmpty() && !catalog.loading) EmptyState("Chưa có hãng", "Catalog chưa cung cấp dữ liệu hãng.")
                else ResponsiveGrid(popular, 4) { brand -> SurfaceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 18.dp, horizontal = 8.dp).clickable { searchNow(brand) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { IconBubble(Icons.Outlined.AcUnit, background = AppColors.paleBlue); Text(brand, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, maxLines = 1) } } }
            }
            if (search.loading) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
            if (query.isNotBlank() && !search.loading && search.results.isEmpty()) EmptyState("Không tìm thấy profile", "Kiểm tra lại hãng hoặc thử dò remote 1000-in-1.", Icons.Filled.Search)
            search.results.forEach { candidate -> CatalogResultCard(candidate) { store.beginScan(selected = candidate); navigate("scan") } }
            SectionTitle("Thiết lập nhanh")
            QuickActionCard("Tôi biết model", "Chọn model máy hoặc remote", Icons.Filled.Search, AppColors.paleBlue) { if (popular.isNotEmpty()) searchNow(popular.first()) }
            QuickActionCard("Tôi không biết model", "Dò remote 1000-in-1", Icons.Outlined.Radio, AppColors.paleMint) { store.beginScan(); navigate("scan") }
            SectionTitle("Nhập dữ liệu có sẵn")
            QuickActionCard("Nhập file .ir", "Dùng hồ sơ IR từ Flipper hoặc nguồn bên ngoài", Icons.Filled.FileDownload, AppColors.paleBlue) { navigate("import") }
            InfoBanner("Nếu chưa chắc model, bắt đầu bằng dò remote 1000-in-1.")
        }
    }
}

@Composable
private fun CatalogResultCard(candidate: RemoteCandidate, onSelect: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(candidate.brand, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold); candidate.displayModelLabel().takeIf(String::isNotBlank)?.let { Text(it, color = AppColors.navySoft) }; candidate.remoteModel?.let { Text(it, color = AppColors.navySoft) } }; StatusChip(if (CatalogTransmitter.supports(candidate)) "Sẵn sàng thử" else "Chưa khả dụng", Icons.Filled.SignalCellularAlt, if (CatalogTransmitter.supports(candidate)) AppColors.blue else AppColors.danger, if (CatalogTransmitter.supports(candidate)) AppColors.paleBlue else AppColors.paleDanger) }; if (CatalogTransmitter.supports(candidate)) PrimaryButton("Thử hồ sơ này", Modifier.fillMaxWidth(), Icons.Filled.PlayArrow, onClick = onSelect) else Text("Chưa có bộ phát tương thích trên thiết bị.", color = AppColors.danger, style = MaterialTheme.typography.bodySmall) } }
}

@Composable
private fun ScannerScreen(store: SavedRemotesViewModel, onTab: (String) -> Unit, onDone: () -> Unit, onChangeBrand: () -> Unit, onImport: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val candidates by store.scanCandidates.collectAsState()
    val candidateKey = candidates.joinToString("|") { it.id }
    var refresh by remember { mutableIntStateOf(0) }
    var machineName by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var pendingCheck by remember { mutableStateOf<VerificationCheck?>(null) }
    var scanStarted by remember(candidateKey) { mutableStateOf(false) }
    var machineOff by remember(candidateKey) { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE") val stateRefresh = refresh
    val scanner = remember(candidateKey) { UniversalAcScanner(candidates) { candidate ->
        val probe = CatalogTransmitter.safeProbe(candidate) ?: error("Không có lệnh thử an toàn cho hồ sơ này.")
        AndroidIrTransmitter.from(context).transmit(CatalogTransmitter.encodeSafeProbe(candidate))
    } }
    val current = scanner.selected ?: candidates.getOrNull(scanner.cursor)
    val controls = current?.let(RemoteControls::from)
    val requirements = controls?.verificationOrder().orEmpty()
    val nextCheck = if (scanner.state == ScanState.VERIFYING) scanner.nextVerificationCheck() else null
    val safeProbe = current?.let { CatalogTransmitter.safeProbe(it) }
    AppScaffold("scan", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar("Dò remote 1000-in-1", "Tìm profile tương thích", onBack = onDone)
            SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { IconBubble(Icons.Filled.AcUnit, size = 62); Column(Modifier.weight(1f)) { Text(current?.brand ?: "Chưa chọn hãng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text(current?.displayModelLabel()?.takeIf(String::isNotBlank) ?: "Không rõ model", color = AppColors.navySoft) }; SecondaryButton("Đổi hãng", Modifier.width(112.dp), Icons.Filled.Refresh, onClick = onChangeBrand) } }
            if (candidates.isEmpty()) {
                EmptyState("Chưa có hồ sơ có thể phát", "Hãy nhập file .ir hoặc chọn một hãng khác để tiếp tục.", Icons.Filled.Search)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Nhập file .ir", Modifier.weight(1f), Icons.Filled.FileDownload, onClick = onImport)
                    SecondaryButton("Chọn hãng khác", Modifier.weight(1f), Icons.Filled.Refresh, onClick = onChangeBrand)
                }
            } else if (!scanStarted && scanner.state == ScanState.READY) {
                SurfaceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Chuẩn bị máy lạnh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("1. Bật máy bằng remote gốc hoặc nút trên máy.\n2. Hướng điện thoại về mắt nhận IR.\n3. Đứng cách máy khoảng 2–5 m và không che đầu phát.", color = AppColors.navySoft)
                    SecondaryButton(if (machineOff) "Máy đang tắt • sẽ thử bật" else "Máy đang bật", Modifier.fillMaxWidth(), Icons.Filled.PowerSettingsNew) { machineOff = !machineOff }
                    PrimaryButton("Bắt đầu dò", Modifier.fillMaxWidth(), Icons.Filled.Search) { scanStarted = true }
                } }
            }
            val progress = if (candidates.isEmpty()) 0f else ((scanner.cursor + if (scanner.state == ScanState.AWAITING_FEEDBACK || scanner.state == ScanState.VERIFYING) 0 else 1).toFloat() / candidates.size).coerceIn(0f, 1f)
            if (candidates.isNotEmpty() && scanStarted) {
                SurfaceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Đang thử hồ sơ ${scanner.cursor + 1} / ${candidates.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)); Text("${(progress * 100).toInt()}%", color = AppColors.blue, fontWeight = FontWeight.ExtraBold) }; LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = AppColors.blue, trackColor = AppColors.paleBlueStrong) } }
                if (scanner.state != ScanState.COMPLETE) current?.let { candidate -> SurfaceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(listOfNotNull(candidate.brand, candidate.displayModelLabel().takeIf(String::isNotBlank)).joinToString(" / "), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text(candidate.remoteModel ?: "Model remote chưa xác định", color = AppColors.navySoft) }; StatusChip("Sẵn sàng thử", Icons.Filled.SignalCellularAlt) }; safeProbe?.warning?.let { InfoBanner(it, Icons.Filled.Info, AppColors.warning, AppColors.paleWarning) }; PrimaryButton(safeProbe?.description ?: "Phát thử", Modifier.fillMaxWidth(), Icons.Filled.PlayArrow, enabled = scanner.state == ScanState.READY) { runCatching { scanner.tryCurrent(System.currentTimeMillis()); resultMessage = "Đã gửi: ${safeProbe?.description ?: "tín hiệu hồ sơ"}. Hãy chờ 2–3 giây." }.onFailure { resultMessage = "Không thể phát tín hiệu này. Thử hồ sơ tiếp theo hoặc kiểm tra phần cứng IR." }; refresh++ }; if (scanner.state == ScanState.AWAITING_FEEDBACK) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PrimaryButton("Có phản ứng", Modifier.weight(1f), Icons.Filled.Check) { scanner.reportReaction(); resultMessage = "Đã giữ hồ sơ này để xác minh."; refresh++ }; SecondaryButton("Không phản ứng", Modifier.weight(1f), Icons.Filled.Close) { scanner.reportNoReaction(); resultMessage = "Hồ sơ không phản hồi. Đang chờ bạn thử hồ sơ tiếp theo."; refresh++ } }; if (resultMessage.isNotBlank()) Text(resultMessage, color = AppColors.navySoft) } } }
                InfoBanner("Mỗi hồ sơ chỉ phát một lần. Hướng remote về máy lạnh và chờ 2–3 giây.")
            }
            if (scanner.state == ScanState.VERIFYING && current != null && nextCheck != null) {
                val step = requirements.indexOf(nextCheck) + 1
                SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bước $step / ${requirements.size}", color = AppColors.blue, fontWeight = FontWeight.Bold)
                    Text("Kiểm tra ${nextCheck.label().lowercase()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(if (nextCheck == VerificationCheck.POWER) "Chỉ kiểm tra nguồn ở bước cuối để không làm gián đoạn các phép thử khác." else "Hãy quan sát máy lạnh sau khi gửi lệnh.", color = AppColors.navySoft)
                    val actionLabel = when (nextCheck) { VerificationCheck.TEMPERATURE_CHANGED -> "Gửi thử 25°C"; VerificationCheck.MODE -> "Gửi thử chế độ khác"; VerificationCheck.FAN -> "Gửi thử tốc độ quạt khác"; VerificationCheck.SWING_VERTICAL -> "Gửi thử đảo gió dọc"; VerificationCheck.SWING_HORIZONTAL -> "Gửi thử đảo gió ngang"; VerificationCheck.POWER -> "Thử tắt máy" }
                    PrimaryButton(actionLabel, Modifier.fillMaxWidth(), Icons.Filled.Send, enabled = pendingCheck == null) { pendingCheck = nextCheck; resultMessage = runCatching { val selected = scanner.selected ?: error("missing"); AndroidIrTransmitter.from(context).transmit(CatalogTransmitter.encode(selected, testState(selected, nextCheck, RemoteControls.from(selected)))); "Đã gửi lệnh kiểm tra. Chọn kết quả bên dưới." }.getOrElse { "Không thể phát tín hiệu này. Thử hồ sơ tiếp theo hoặc kiểm tra phần cứng IR." }; refresh++ }
                    if (pendingCheck == nextCheck) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PrimaryButton("Có", Modifier.weight(1f), Icons.Filled.Check) { val selected = scanner.selected; if (nextCheck == VerificationCheck.POWER && selected != null) { runCatching { AndroidIrTransmitter.from(context).transmit(CatalogTransmitter.encodeSafeProbe(selected)) }; resultMessage = "Đã thử bật lại máy." }; scanner.recordVerification(nextCheck); pendingCheck = null; refresh++ }; SecondaryButton("Không", Modifier.weight(1f), Icons.Filled.Close) { scanner.recordVerification(nextCheck, supported = false); pendingCheck = null; refresh++ }; TextButton(onClick = { scanner.skipVerification(nextCheck); pendingCheck = null; refresh++ }) { Text("Bỏ qua") } }
                } }
            } else if (scanner.state == ScanState.VERIFYING) {
                PrimaryButton("Xem kết quả", Modifier.fillMaxWidth(), Icons.Filled.CheckCircle) { scanner.finishVerification(); refresh++ }
            }
            if (scanner.state == ScanState.COMPLETE) { val scanResult = scanner.result; StatusChip(when (scanResult) { ScanResult.FULL_MATCH -> "Đã xác minh"; ScanResult.PARTIAL_MATCH -> "Hồ sơ hoạt động một phần"; else -> "Không tìm thấy phản hồi" }, if (scanResult == ScanResult.NO_MATCH) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle, if (scanResult == ScanResult.NO_MATCH) AppColors.danger else AppColors.mint, if (scanResult == ScanResult.NO_MATCH) AppColors.paleDanger else AppColors.paleMint); if (scanner.cursor + 1 < candidates.size) SecondaryButton("Thử hồ sơ tiếp theo", Modifier.fillMaxWidth(), Icons.Filled.ArrowForward) { scanner.continueAfterResult(); resultMessage = "Đã chuyển sang hồ sơ tiếp theo."; refresh++ }; if (scanResult != ScanResult.NO_MATCH) { OutlinedField("Tên máy", machineName, { machineName = it }); PrimaryButton("Lưu hồ sơ này", Modifier.fillMaxWidth(), Icons.Filled.CheckCircle, enabled = machineName.isNotBlank() && scanner.verifiedCapabilities.isNotEmpty()) { val candidate = scanner.selected ?: return@PrimaryButton; store.save(SavedRemote(UUID.randomUUID().toString(), machineName.trim(), candidate.id, candidate.brand, candidate.acModel, candidate.remoteModel, candidate.protocolId, candidate.protocolModel, scanner.verifiedCapabilities.map { it.name })); onDone() } } }
            SecondaryButton("Dừng quét", Modifier.fillMaxWidth(), Icons.Filled.Stop) { scanner.stop(); onDone() }
        }
    }
}

@Composable
private fun RemoteScreen(remote: SavedRemote, candidate: RemoteCandidate, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controls = remember(candidate.id) { RemoteControls.from(candidate) }
    val protocol = remember(candidate.id) { CatalogTransmitter.protocol(candidate) }
    val usableModes = controls.modes.filter { CatalogTransmitter.mode(it)?.let { mode -> protocol?.modes?.contains(mode) } == true }
    val usableFans = controls.fanModes.filter { CatalogTransmitter.fan(it)?.let { fan -> protocol?.fanSpeeds?.contains(fan) } == true }
    val initialTemp = controls.temperatureRange?.let { (it.first + it.last) / 2 } ?: 24
    var power by remember { mutableStateOf(false) }
    var temperature by remember(candidate.id) { mutableIntStateOf(initialTemp) }
    var mode by remember(candidate.id) { mutableStateOf(usableModes.firstOrNull().orEmpty()) }
    var fan by remember(candidate.id) { mutableStateOf(usableFans.firstOrNull().orEmpty()) }
    var swingVertical by remember { mutableStateOf(false) }
    var swingHorizontal by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    fun send(): String = runCatching { require(protocol != null) { "Profile này chưa có bộ phát khả dụng." }; val selectedMode = CatalogTransmitter.mode(mode) ?: protocol.modes.first(); val selectedFan = CatalogTransmitter.fan(fan) ?: protocol.fanSpeeds.first(); val model = candidate.copy(protocolModel = CatalogTransmitter.modelId(candidate, protocol)); AndroidIrTransmitter.from(context).transmit(CatalogTransmitter.encode(model, AcState(power, temperature, selectedMode, selectedFan, swingVertical, swingHorizontal))); "Đã gửi lệnh" }.getOrElse { it.message ?: "Không thể phát" }
    PageWithBack(remote.displayName, listOfNotNull(remote.brand, candidate.displayModelLabel().takeIf(String::isNotBlank)).joinToString(" • "), onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { StatusChip("Đã xác minh", Icons.Filled.CheckCircle, AppColors.mint, AppColors.paleMint); StatusChip("IR sẵn sàng", Icons.Filled.SignalCellularAlt) }
        GradientHero { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { IconBubble(Icons.Filled.AcUnit, background = Color.White, size = 62); Text("Nhiệt độ cài đặt", color = AppColors.navySoft); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { if (controls.temperatureRange != null) { TemperatureButton("−", temperature > controls.temperatureRange.first) { temperature--; feedback = send() }; Text("$temperature°", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = AppColors.navy); TemperatureButton("+", temperature < controls.temperatureRange.last) { temperature++; feedback = send() } } else Text("—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold) } }; Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { LargePowerButton(enabled = controls.power, onClick = { power = !power; feedback = send() }); Text(if (power) "Đang bật" else "Bật / Tắt", fontWeight = FontWeight.Bold, color = AppColors.navy) } } }
        if (!controls.power && controls.temperatureRange == null && usableModes.isEmpty() && usableFans.isEmpty()) EmptyState("Profile hạn chế", "Profile này không khai báo điều khiển khả dụng cho remote.")
        if (usableModes.isNotEmpty()) { SectionTitle("Chế độ hoạt động"); OptionRow(usableModes, mode, ::modeLabel, Icons.Filled.AcUnit) { mode = it; feedback = send() } }
        if (usableFans.isNotEmpty()) { SectionTitle("Tốc độ quạt"); OptionRow(usableFans, fan, ::fanLabel, Icons.Filled.Air) { fan = it; feedback = send() } }
        SwingSection("Hướng gió dọc", controls.verticalSwing, swingVertical, { swingVertical = !swingVertical; feedback = send() }) { feedback = "Vị trí này có trong profile nhưng bộ phát hiện tại chỉ hỗ trợ bật/tắt swing." }
        SwingSection("Hướng gió ngang", controls.horizontalSwing, swingHorizontal, { swingHorizontal = !swingHorizontal; feedback = send() }) { feedback = "Vị trí này có trong profile nhưng bộ phát hiện tại chỉ hỗ trợ bật/tắt swing." }
        if (controls.specialCapabilities.isNotEmpty()) { SectionTitle("Tính năng khác"); ResponsiveFeatureGrid(controls.specialCapabilities) { special -> FeatureCard(specialCapabilityLabel(special), Icons.Filled.Tune, enabled = false) { feedback = "${specialCapabilityLabel(special)} có trong profile; engine hiện chưa encode state này." } } }
        if (feedback.isNotBlank()) InfoBanner(feedback, Icons.Filled.Info, AppColors.blue, AppColors.paleBlue)
        PrimaryButton("Gửi lệnh", Modifier.fillMaxWidth(), Icons.Filled.Send, enabled = protocol != null) { feedback = send() }
    }
}

@Composable
private fun DetailsScreen(remote: SavedRemote, candidate: RemoteCandidate?, store: SavedRemotesViewModel, openRemote: () -> Unit, recheck: () -> Unit, onBack: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var displayName by remember(remote.id) { mutableStateOf(remote.displayName) }
    val verified = verifiedChecks(remote)
    PageWithBack("Chi tiết máy lạnh", listOfNotNull(remote.brand, candidate?.displayModelLabel()).joinToString(" • "), onBack) {
        SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { IconBubble(Icons.Filled.AcUnit, size = 76); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(remote.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text(listOfNotNull(remote.brand, candidate?.acModel ?: remote.acModel).joinToString(" • "), color = AppColors.navySoft); Text(listOfNotNull(candidate?.protocolId, candidate?.remoteModel, candidate?.protocolModel).joinToString(" / ").ifBlank { "Profile ${remote.catalogProfileId}" }, color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall) }; StatusChip(if (verified.isNotEmpty()) "Khớp tốt" else "Chưa xác minh", if (verified.isNotEmpty()) Icons.Filled.SignalCellularAlt else Icons.Filled.Info) } }
        SectionTitle("Chức năng đã xác minh")
        if (verified.isEmpty()) EmptyState("Chưa có xác minh", "Hãy mở scanner để kiểm tra các capability thật của profile.", Icons.Filled.Info) else verified.forEach { check -> CapabilityRow(check.label(), "Đã xác nhận", Icons.Filled.CheckCircle, AppColors.mint, "OK") }
        SectionTitle("Thông tin hồ sơ")
        SurfaceCard(Modifier.fillMaxWidth()) { Column { SourceInfoRow("Encoding", candidate?.encodingType ?: "—", Icons.Filled.Code); SourceInfoRow("Source", candidate?.source?.let(::sourceLabel) ?: "—", Icons.Filled.Description); SourceInfoRow("Database revision", candidate?.sourceCommitSha?.shortSha() ?: "Catalog runtime", Icons.Filled.FilterAlt); SourceInfoRow("Kiểm tra gần nhất", "Chưa ghi nhận", Icons.Filled.CalendarToday) } }
        SectionTitle("Quản lý")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PrimaryButton("Mở remote", Modifier.weight(1f), Icons.Filled.PlayArrow, enabled = candidate != null) { openRemote() }; SecondaryButton("Kiểm tra lại", Modifier.weight(1f), Icons.Filled.Refresh) { recheck() } }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { SecondaryButton("Đổi tên", Modifier.weight(1f), Icons.Filled.Edit) { showRename = true }; SecondaryButton("Xóa khỏi máy", Modifier.weight(1f), Icons.Filled.DeleteOutline) { showDelete = true } }
        InfoBanner("Nếu một capability không ổn định, hãy thử profile khác cùng dòng máy.", Icons.Filled.Lightbulb, AppColors.warning, AppColors.paleWarning)
    }
    RenameDialog(if (showRename) remote else null, displayName, { displayName = it }, { store.rename(remote.id, displayName); showRename = false }, { showRename = false })
    if (showDelete) AlertDialog(onDismissRequest = { showDelete = false }, title = { Text("Xóa máy khỏi thiết bị?") }, text = { Text("${remote.displayName} sẽ bị xóa khỏi danh sách remote.") }, confirmButton = { TextButton(onClick = { store.delete(remote.id); onBack() }) { Text("Xóa", color = AppColors.danger) } }, dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Hủy") } })
}

@Composable
private fun SettingsScreen(diagnostics: IrHardwareDiagnostics, catalog: CatalogUiState, onTab: (String) -> Unit, openDiagnostics: () -> Unit, openImport: () -> Unit) {
    AppScaffold("settings", onTab) { padding ->
        PageColumn(padding) {
            AppTopBar("Universal A/C Remote", "Thiết bị và dữ liệu", onBack = { onTab("home") })
            GradientHero { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Cài đặt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold); Text("Tùy chỉnh ứng dụng và cập nhật", color = AppColors.navySoft) } }
            SectionTitle("Ứng dụng")
            SurfaceCard(Modifier.fillMaxWidth()) { Column { SourceInfoRow("Phiên bản ứng dụng", BuildConfig.VERSION_NAME, Icons.Filled.Build); SourceInfoRow("Unified Catalog", if (catalog.loading) "Đang tải…" else "${catalog.profileCount} profile", Icons.Filled.FilterAlt); SourceInfoRow("Thiết bị IR", if (diagnostics.hasIrEmitter) "Sẵn sàng" else "Không khả dụng", Icons.Filled.SignalCellularAlt, if (diagnostics.hasIrEmitter) AppColors.mint else AppColors.danger) } }
            SectionTitle("Cập nhật")
            SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Cập nhật ứng dụng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text("Database/catalog mới được phát hành cùng APK khi updater hiện tại tìm thấy release.", color = AppColors.navySoft); UpdatePanel() } }
            SectionTitle("Công cụ")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolCard("Kiểm tra phần cứng IR", Icons.Filled.SignalCellularAlt, Modifier.weight(1f)) { openDiagnostics() }
                ToolCard("Nhập file .ir", Icons.Filled.FileDownload, Modifier.weight(1f)) { openImport() }
            }
            SectionTitle("Thông tin nguồn")
            SurfaceCard(Modifier.fillMaxWidth()) { Column { SourceInfoRow("Catalog runtime", "Unified Catalog", Icons.Filled.Code); SourceInfoRow("IR hardware", "Android ConsumerIrManager", Icons.Filled.SignalCellularAlt); SourceInfoRow("Upstream provenance", "Xem upstream-lock.json", Icons.Filled.Description) } }
        }
    }
}

@Composable
private fun ImportedRemoteScreen(remote: SavedRemote, store: SavedRemotesViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val commands = remember(remote.id, remote.importedCommandsJson) { SavedRemoteConverters().decodeImportedCommands(remote.importedCommandsJson) }
    var feedback by remember { mutableStateOf("") }
    PageWithBack(remote.displayName, "Profile đã nhập • ${remote.brand}", onBack) {
        GradientHero { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { IconBubble(Icons.Filled.Description, size = 64); Column { Text("Remote đã nhập", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text("${commands.size} command hợp lệ", color = AppColors.navySoft) } } }
        if (commands.isEmpty()) EmptyState("Không có command hợp lệ", "Profile import không có dữ liệu có thể phát.", Icons.Filled.ErrorOutline)
        commands.forEach { command -> PrimaryButton(command.name, Modifier.fillMaxWidth(), Icons.Filled.Send) { feedback = runCatching { AndroidIrTransmitter.from(context).transmit(command.transmission); "Đã gửi ${command.name}" }.getOrElse { it.message ?: "Không thể phát command." } } }
        if (feedback.isNotBlank()) InfoBanner(feedback, Icons.Filled.CheckCircle, AppColors.mint, AppColors.paleMint)
        SecondaryButton("Xóa remote", Modifier.fillMaxWidth(), Icons.Filled.DeleteOutline) { store.delete(remote.id); onBack() }
    }
}

@Composable
private fun PageWithBack(title: String, subtitle: String?, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AppBackground { Column(Modifier.fillMaxSize()) { AppTopBar(title, subtitle, onBack); PageColumn(PaddingValues(0.dp), content) } }
}

@Composable private fun LoadingScreen(onBack: () -> Unit) = PageWithBack("Đang tải…", "Mở remote", onBack) { Box(Modifier.fillMaxWidth().padding(42.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
@Composable private fun MissingRemoteScreen(onBack: () -> Unit) = PageWithBack("Không tìm thấy remote", null, onBack) { EmptyState("Remote không tồn tại", "Quay lại danh sách để chọn một remote khác.") }
@Composable private fun MissingProfileScreen(remote: SavedRemote, onRecheck: () -> Unit) = PageWithBack(remote.displayName, "Cần kiểm tra lại profile", onRecheck) { EmptyState("Không tìm thấy profile", "Profile không còn trong catalog hiện tại. Hãy kiểm tra lại để chọn profile tương thích."); PrimaryButton("Kiểm tra lại", Modifier.fillMaxWidth(), Icons.Filled.Refresh, onClick = onRecheck) }

@Composable
private fun QuickActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, background: Color, onClick: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth()) { Row(Modifier.clip(RoundedCornerShape(24.dp)).background(background).padding(18.dp).fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { IconBubble(icon, background = Color.White.copy(alpha = 0.72f), size = 58); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = AppColors.navySoft) }; Icon(Icons.Filled.ChevronRight, null, tint = AppColors.navySoft) } }
}

@Composable
private fun ResponsiveGrid(items: List<String>, columns: Int, content: @Composable (String) -> Unit) { items.chunked(columns).forEach { rowItems -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { rowItems.forEach { value -> Box(Modifier.weight(1f)) { content(value) } }; repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) } } } }
@Composable private fun ResponsiveFeatureGrid(items: List<String>, content: @Composable (String) -> Unit) = ResponsiveGrid(items, 2, content)

@Composable
private fun OptionRow(values: List<String>, selected: String, label: (String) -> String, icon: androidx.compose.ui.graphics.vector.ImageVector, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 2.dp)) { items(values) { value -> val isSelected = selected == value; Surface(modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { onSelect(value) }, color = if (isSelected) AppColors.blue else Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AppColors.blue else AppColors.line)) { Column(Modifier.padding(horizontal = 18.dp, vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(icon, null, tint = if (isSelected) Color.White else AppColors.blue, modifier = Modifier.size(28.dp)); Text(label(value), color = if (isSelected) Color.White else AppColors.navy, fontWeight = FontWeight.Bold) } } } }
}

@Composable
private fun SwingSection(title: String, control: SwingControl, toggle: Boolean, onToggle: () -> Unit, onUnsupportedPosition: () -> Unit) {
    if (!control.visible) return
    SectionTitle(title)
    when { control.toggleOnly -> OptionRow(listOf("AUTO", "SWING"), if (toggle) "SWING" else "AUTO", { if (it == "SWING") "Bật swing" else "Tự động" }, Icons.Filled.MoreHoriz) { if (it == "SWING") onToggle() }; control.positions.isNotEmpty() -> OptionRow(control.positions, "", ::swingPositionLabel, Icons.Filled.Tune) { onUnsupportedPosition() } }
}

@Composable
private fun FeatureCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) { SurfaceCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(15.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { IconBubble(icon, tint = if (enabled) AppColors.blue else AppColors.navySoft, background = AppColors.paleBlue); Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = AppColors.navy); Text(if (enabled) "Bật / Tắt" else "Profile có, engine chưa hỗ trợ", style = MaterialTheme.typography.bodySmall, color = AppColors.navySoft, textAlign = TextAlign.Center) } } }

@Composable
private fun VerificationTile(check: VerificationCheck, verified: Boolean, onClick: () -> Unit) { SurfaceCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { IconBubble(check.icon(), tint = if (verified) AppColors.mint else AppColors.blue, background = if (verified) AppColors.paleMint else AppColors.paleBlue); Column(Modifier.weight(1f)) { Text(check.label(), fontWeight = FontWeight.Bold); Text(if (verified) "Đã xác minh" else "Chạm để phát lệnh kiểm tra", color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall) }; IconButton(onClick = onClick) { Icon(if (verified) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow, "Kiểm tra", tint = if (verified) AppColors.mint else AppColors.blue) } } } }

@Composable
private fun CapabilityRow(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, status: String) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { IconBubble(icon, tint = color, background = if (color == AppColors.mint) AppColors.paleMint else AppColors.paleBlue, size = 42); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(detail, color = AppColors.navySoft, style = MaterialTheme.typography.bodySmall) }; StatusChip(status, Icons.Filled.CheckCircle, color, if (color == AppColors.mint) AppColors.paleMint else AppColors.paleBlue) } }

@Composable
private fun SourceInfoRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, valueColor: Color = AppColors.navySoft) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) { Icon(icon, null, tint = AppColors.navySoft, modifier = Modifier.size(24.dp)); Text(label, Modifier.weight(1f), color = AppColors.navy); Text(value, color = valueColor, textAlign = TextAlign.End) } }
@Composable private fun ToolCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) { SurfaceCard(modifier) { Column(Modifier.fillMaxWidth().padding(14.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) { IconBubble(icon, size = 52); Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) } } }
@Composable private fun TemperatureButton(text: String, enabled: Boolean, onClick: () -> Unit) { OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(24.dp)) { Text(text, style = MaterialTheme.typography.titleLarge) } }
@Composable private fun OutlinedField(label: String, value: String, onChange: (String) -> Unit) { OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(18.dp)) }
@Composable private fun RenameDialog(target: SavedRemote?, value: String, onValueChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) { if (target != null) AlertDialog(onDismissRequest = onDismiss, title = { Text("Đổi tên máy") }, text = { OutlinedField("Tên máy", value, onValueChange) }, confirmButton = { TextButton(onClick = onConfirm, enabled = value.isNotBlank()) { Text("Lưu") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }) }

private fun verifiedChecks(remote: SavedRemote): Set<VerificationCheck> = remote.verifiedCapabilities.mapNotNull { value -> runCatching { VerificationCheck.valueOf(value) }.getOrNull() }.toSet()
private fun VerificationCheck.label() = when (this) { VerificationCheck.POWER -> "Bật / Tắt nguồn"; VerificationCheck.TEMPERATURE_CHANGED -> "Nhiệt độ"; VerificationCheck.MODE -> "Chế độ"; VerificationCheck.FAN -> "Quạt"; VerificationCheck.SWING_VERTICAL -> "Đảo gió dọc"; VerificationCheck.SWING_HORIZONTAL -> "Đảo gió ngang" }
private fun VerificationCheck.icon() = when (this) { VerificationCheck.POWER -> Icons.Filled.PowerSettingsNew; VerificationCheck.TEMPERATURE_CHANGED -> Icons.Filled.Thermostat; VerificationCheck.MODE -> Icons.Filled.AcUnit; VerificationCheck.FAN -> Icons.Filled.Air; VerificationCheck.SWING_VERTICAL -> Icons.Filled.KeyboardArrowUp; VerificationCheck.SWING_HORIZONTAL -> Icons.Filled.MoreHoriz }
private fun profileTypeLabel(value: String): String = when (value.uppercase()) { "PROTOCOL" -> "PROTOCOL"; "RAW_PROFILE" -> "RAW_PROFILE"; "IMPORTED_RAW" -> "IMPORTED_RAW"; else -> value.ifBlank { "PROFILE" } }
private fun sourceLabel(value: String): String = when (value.lowercase()) { "irremoteesp8266" -> "IRremoteESP8266"; "smartir" -> "SmartIR"; "flipper-irdb" -> "Flipper-IRDB"; else -> value }
private fun String.shortSha(): String = take(8)

private fun testState(candidate: RemoteCandidate, check: VerificationCheck, controls: RemoteControls): AcState {
    val definition = CatalogTransmitter.protocol(candidate) ?: return rawTestState(candidate, check, controls)
    val modes = controls.modes.mapNotNull(CatalogTransmitter::mode).filter { it in definition.modes }
    val fans = controls.fanModes.mapNotNull(CatalogTransmitter::fan).filter { it in definition.fanSpeeds }
    val mode = modes.firstOrNull() ?: definition.modes.first()
    val fan = fans.firstOrNull() ?: definition.fanSpeeds.first()
    val range = controls.temperatureRange ?: (definition.minTemperatureCelsius..definition.maxTemperatureCelsius)
    val temperature = if (check == VerificationCheck.TEMPERATURE_CHANGED && range.first < range.last) range.first + 1 else range.first
    return when (check) { VerificationCheck.POWER -> AcState(false, temperature, mode, fan); VerificationCheck.TEMPERATURE_CHANGED -> AcState(true, temperature, mode, fan); VerificationCheck.MODE -> AcState(true, temperature, modes.getOrNull(1) ?: mode, fan); VerificationCheck.FAN -> AcState(true, temperature, mode, fans.getOrNull(1) ?: fan); VerificationCheck.SWING_VERTICAL -> AcState(true, temperature, mode, fan, swingVertical = true); VerificationCheck.SWING_HORIZONTAL -> AcState(true, temperature, mode, fan, swingHorizontal = true) }
}

private fun rawTestState(candidate: RemoteCandidate, check: VerificationCheck, controls: RemoteControls): AcState {
    val modes = controls.modes.mapNotNull(CatalogTransmitter::mode)
    val fans = controls.fanModes.mapNotNull(CatalogTransmitter::fan)
    val mode = modes.firstOrNull() ?: error("This profile has no supported operation mode.")
    val fan = fans.firstOrNull() ?: error("This profile has no supported fan mode.")
    val range = controls.temperatureRange ?: error("This profile has no supported temperature range.")
    val temperature = if (check == VerificationCheck.TEMPERATURE_CHANGED && range.first < range.last) range.first + 1 else range.first
    return when (check) { VerificationCheck.POWER -> AcState(false, temperature, mode, fan); VerificationCheck.TEMPERATURE_CHANGED -> AcState(true, temperature, mode, fan); VerificationCheck.MODE -> AcState(true, temperature, modes.getOrNull(1) ?: mode, fan); VerificationCheck.FAN -> AcState(true, temperature, mode, fans.getOrNull(1) ?: fan); VerificationCheck.SWING_VERTICAL -> AcState(true, temperature, mode, fan, swingVertical = true); VerificationCheck.SWING_HORIZONTAL -> AcState(true, temperature, mode, fan, swingHorizontal = true) }
}

private val previewCandidate = RemoteCandidate(
    id = "preview:protocol",
    brand = "Panasonic",
    acModel = "CS-PU9",
    remoteModel = "A75Cxxxx",
    protocolId = "panasonic",
    protocolModel = "PANASONIC_AC",
    encodingType = "PROTOCOL",
    capabilities = setOf("power", "mode:cool", "mode:dry", "fan:auto", "fan:high"),
    evidence = listOf("Preview fixture only"),
    priority = 1,
    operationModes = listOf("cool", "dry"),
    fanModes = listOf("auto", "high"),
    minimumTemperatureCelsius = 16,
    maximumTemperatureCelsius = 30,
)

private val previewRemote = SavedRemote(
    id = "preview:remote",
    displayName = "Phòng ngủ",
    catalogProfileId = previewCandidate.id,
    brand = "Panasonic",
    acModel = "CS-PU9",
    remoteModel = "A75Cxxxx",
    protocolId = "panasonic",
    protocolModel = "PANASONIC_AC",
    verifiedCapabilities = listOf(VerificationCheck.POWER.name, VerificationCheck.MODE.name),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddAcScreenPreview() {
    UniversalAcTheme { AppBackground { Column(Modifier.fillMaxSize()) { AppTopBar("Thêm máy lạnh", "Chọn cách thiết lập phù hợp"); PageColumn(PaddingValues(0.dp)) { SearchField("", {}, "Tìm hãng, model máy hoặc remote"); SectionTitle("Hãng phổ biến"); ResponsiveGrid(listOf("Daikin", "Panasonic", "LG", "Samsung"), 4) { brand -> Text(brand, Modifier.padding(14.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }; SectionTitle("Thiết lập nhanh"); QuickActionCard("Tôi biết model", "Chọn model máy hoặc remote", Icons.Filled.Search, AppColors.paleBlue) {}; QuickActionCard("Tôi không biết model", "Dò remote 1000-in-1", Icons.Outlined.Radio, AppColors.paleMint) {} } } } }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScannerScreenPreview() {
    UniversalAcTheme { AppBackground { PageColumn(PaddingValues(0.dp)) { AppTopBar("Dò remote 1000-in-1", "Tìm profile tương thích"); StatusChip("Casper", Icons.Filled.AcUnit); SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Đang thử hồ sơ 12 / 48", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); LinearProgressIndicator(progress = { .25f }, Modifier.fillMaxWidth()); Text("Casper / OEM Midea", fontWeight = FontWeight.Bold) } }; PrimaryButton("Phát thử", Modifier.fillMaxWidth(), Icons.Filled.PlayArrow) {} } } }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DeviceDetailScreenPreview() {
    UniversalAcTheme { DetailsPreviewContent() }
}

@Composable
private fun DetailsPreviewContent() {
    UniversalAcTheme { AppBackground { PageColumn(PaddingValues(0.dp)) { AppTopBar("Chi tiết máy lạnh", "Panasonic • CS-PU9"); SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { IconBubble(Icons.Filled.AcUnit, size = 64); Column(Modifier.weight(1f)) { Text("Phòng ngủ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text("Panasonic • CS-PU9", color = AppColors.navySoft) }; StatusChip("Khớp tốt", Icons.Filled.SignalCellularAlt) } }; SectionTitle("Chức năng đã xác minh"); CapabilityRow("Bật / Tắt nguồn", "Đã xác nhận", Icons.Filled.CheckCircle, AppColors.mint, "OK"); CapabilityRow("Chế độ", "Đã xác nhận", Icons.Filled.CheckCircle, AppColors.mint, "OK") } } }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RemoteControlScreenPreview() {
    UniversalAcTheme { RemoteScreen(previewRemote, previewCandidate) {} }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    UniversalAcTheme { SettingsPreviewContent() }
}

@Composable
private fun SettingsPreviewContent() {
    AppBackground { PageColumn(PaddingValues(0.dp)) { AppTopBar("Universal A/C Remote", "Thiết bị và dữ liệu"); GradientHero { Column { Text("Cài đặt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold); Text("Tùy chỉnh ứng dụng và cập nhật", color = AppColors.navySoft) } }; SectionTitle("Ứng dụng"); SurfaceCard(Modifier.fillMaxWidth()) { Column { SourceInfoRow("Phiên bản ứng dụng", "preview", Icons.Filled.Build); SourceInfoRow("Unified Catalog", "Catalog runtime", Icons.Filled.FilterAlt); SourceInfoRow("Thiết bị IR", "Sẵn sàng", Icons.Filled.SignalCellularAlt, AppColors.mint) } } } }
}
