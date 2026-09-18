package com.thanhnha.universalacremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.thanhnha.universalacremote.ir.AcMode
import com.thanhnha.universalacremote.ir.AcState
import com.thanhnha.universalacremote.ir.AndroidIrTransmitter
import com.thanhnha.universalacremote.ir.IrHardwareDiagnostics
import com.thanhnha.universalacremote.ir.RemoteCandidate
import com.thanhnha.universalacremote.ir.ScanState
import com.thanhnha.universalacremote.ir.UniversalAcScanner
import com.thanhnha.universalacremote.ir.VerificationCheck
import java.util.UUID

@Composable
fun RemoteApp(diagnostics: IrHardwareDiagnostics, store: SavedRemotesViewModel = viewModel()) {
    val nav = rememberNavController()
    val home by store.state.collectAsState()
    val catalog by store.catalog.collectAsState()
    MaterialTheme {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") { HomeScreen(home, diagnostics, { route -> nav.navigate(route) }, store) }
            composable("add") { AddScreen(store) { route -> nav.navigate(route) } }
            composable("scan") { ScannerScreen(store) { nav.popBackStack("home", false) } }
            composable("remote/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                home.remotes.find { remote -> remote.id == it.arguments?.getString("id") }?.let { remote ->
                    if (remote.importedCommandsJson.isNotBlank()) {
                        ImportedRemoteScreen(remote, store) { nav.popBackStack() }
                        return@composable
                    }
                    val profile = store.profileFor(remote.catalogProfileId)
                    if (catalog.loading) Page("Đang tải…", { nav.popBackStack() }) { Text("Đang mở remote…") }
                    else if (profile == null) MissingProfileScreen(remote) { store.beginScan(); nav.navigate("scan") }
                    else RemoteScreen(remote, profile) { nav.popBackStack() }
                }
            }
            composable("details/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                home.remotes.find { remote -> remote.id == it.arguments?.getString("id") }?.let { remote -> DetailsScreen(remote, store.profileFor(remote.catalogProfileId), { route -> nav.navigate(route) }, store) }
            }
            composable("settings") { SettingsScreen(diagnostics) { nav.popBackStack() } }
        }
    }
}

@Composable
private fun HomeScreen(state: HomeUiState, diagnostics: IrHardwareDiagnostics, navigate: (String) -> Unit, store: SavedRemotesViewModel) {
    var renameTarget by remember { mutableStateOf<SavedRemote?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedRemote?>(null) }
    var name by remember(renameTarget) { mutableStateOf(renameTarget?.displayName.orEmpty()) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { navigate("add") }) { Text("+") } }) { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Universal A/C Remote", style = MaterialTheme.typography.headlineMedium)
            Text(if (diagnostics.hasIrEmitter) "Hồng ngoại sẵn sàng" else "Thiết bị chưa hỗ trợ hồng ngoại", color = if (diagnostics.hasIrEmitter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Button(onClick = { navigate("add") }, modifier = Modifier.fillMaxWidth()) { Text("Thêm máy lạnh") }
            if (state.remotes.isEmpty() && !state.loading) {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chưa có máy lạnh nào", style = MaterialTheme.typography.titleLarge)
                    Text("Thêm máy lạnh để tìm profile, kiểm tra khả năng tương thích và lưu remote của bạn.")
                } }
            }
            state.remotes.forEach { remote ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(remote.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(listOfNotNull(remote.brand, remote.acModel, remote.remoteModel).joinToString(" · "))
                    val profile = store.profileFor(remote.catalogProfileId)
                    val requirements = profile?.let { com.thanhnha.universalacremote.ir.RemoteControls.from(it).verificationRequirements() }.orEmpty()
                    val verified = remote.verifiedCapabilities.mapNotNull { value -> runCatching { VerificationCheck.valueOf(value) }.getOrNull() }.toSet()
                    val compatibility = when {
                        remote.importedCommandsJson.isNotBlank() -> "Đã nhập · ${com.thanhnha.universalacremote.SavedRemoteConverters().decodeImportedCommands(remote.importedCommandsJson).size} command"
                        profile == null -> "Cần kiểm tra lại profile"
                        verified.isNotEmpty() && requirements.size > 1 && verified.containsAll(requirements) -> "Tương thích · đã xác minh đầy đủ"
                        verified.isNotEmpty() -> "Tương thích một phần · ${verified.size} chức năng đã xác minh"
                        else -> "Chưa xác minh"
                    }
                    Text(compatibility, color = if (verified.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { navigate("remote/${remote.id}") }) { Text("Mở remote") }
                        OutlinedButton(onClick = { renameTarget = remote }) { Text("Đổi tên") }
                        TextButton(onClick = { deleteTarget = remote }) { Text("Xóa") }
                    }
                    TextButton(onClick = { navigate("details/${remote.id}") }) { Text("Chi tiết máy") }
                } }
            }
            TextButton(onClick = { navigate("settings") }) { Text("Cài đặt") }
        }
    }
    if (renameTarget != null) AlertDialog(onDismissRequest = { renameTarget = null }, title = { Text("Đổi tên máy") }, text = {
        OutlinedTextField(name, { name = it }, label = { Text("Tên máy") }, singleLine = true)
    }, confirmButton = { TextButton(onClick = { renameTarget?.let { store.rename(it.id, name) }; renameTarget = null }, enabled = name.isNotBlank()) { Text("Lưu") } }, dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Hủy") } })
    if (deleteTarget != null) AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Xóa remote?") }, text = { Text("${deleteTarget?.displayName} sẽ bị xóa khỏi thiết bị.") }, confirmButton = { TextButton(onClick = { deleteTarget?.let { store.delete(it.id) }; deleteTarget = null }) { Text("Xóa") } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Hủy") } })
}

@Composable
private fun AddScreen(store: SavedRemotesViewModel, navigate: (String) -> Unit) {
    val catalog by store.catalog.collectAsState()
    val popular by store.popularBrands.collectAsState()
    val search by store.search.collectAsState()
    var brand by remember { mutableStateOf("") }
    var acModel by remember { mutableStateOf("") }
    var remoteModel by remember { mutableStateOf("") }
    fun searchNow() = store.updateSearch(com.thanhnha.universalacremote.ir.RemoteQuery(brand, acModel, remoteModel))
    Page("Thêm máy lạnh", onBack = { navigate("home") }) {
        Text("Tìm theo hãng, model máy lạnh hoặc model remote trong ${catalog.profileCount} profile.")
        if (catalog.loading) Text("Đang tải danh mục…")
        catalog.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(brand, { brand = it; searchNow() }, Modifier.fillMaxWidth(), label = { Text("Hãng hoặc alias") }, singleLine = true)
        if (brand.isBlank() && acModel.isBlank() && remoteModel.isBlank()) {
            Text("Hãng phổ biến", style = MaterialTheme.typography.titleMedium)
            popular.forEach { option ->
                OutlinedButton(onClick = { brand = option; searchNow() }, modifier = Modifier.fillMaxWidth()) { Text(option) }
            }
        }
        OutlinedTextField(acModel, { acModel = it; searchNow() }, Modifier.fillMaxWidth(), label = { Text("Model máy lạnh") }, singleLine = true)
        OutlinedTextField(remoteModel, { remoteModel = it; searchNow() }, Modifier.fillMaxWidth(), label = { Text("Model remote") }, singleLine = true)
        if (search.loading) Text("Đang tìm…")
        if (!catalog.loading && (brand.isNotBlank() || acModel.isNotBlank() || remoteModel.isNotBlank()) && search.results.isEmpty() && !search.loading) {
            Text("Không tìm thấy profile khớp. Kiểm tra lại model hoặc dùng scanner.")
        }
        search.results.forEach { candidate ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(candidate.brand, style = MaterialTheme.typography.titleMedium)
                listOfNotNull(candidate.acModel, candidate.remoteModel).takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · ")) }
                Text(profileTypeLabel(candidate.encodingType) + candidate.source?.let { " · ${sourceLabel(it)}" }.orEmpty())
                if (com.thanhnha.universalacremote.ir.CatalogTransmitter.supports(candidate)) {
                    Button(onClick = { store.beginScan(selected = candidate); navigate("scan") }, modifier = Modifier.fillMaxWidth()) { Text("Kiểm tra profile") }
                } else {
                    Text("Chưa có bộ phát tương thích trên thiết bị cho profile này.", style = MaterialTheme.typography.bodySmall)
                }
            } }
        }
        Text("Hoặc nhập profile IR", style = MaterialTheme.typography.titleMedium)
        com.thanhnha.universalacremote.ir.FlipperImportPanel(AndroidIrTransmitter.from(LocalContext.current)) { name, imported ->
            val payload = SavedRemoteConverters().encodeImportedCommands(imported.map { ImportedRawCommand(it.name, it.transmission) })
            store.save(SavedRemote(UUID.randomUUID().toString(), name, "imported:${UUID.randomUUID()}", "Imported", null, null, null, null, emptyList(), payload))
            navigate("home")
        }
        Button(onClick = { store.beginScan(); navigate("scan") }, modifier = Modifier.fillMaxWidth()) { Text("Tôi không biết model · Universal Scanner") }
    }
}

@Composable
private fun ScannerScreen(store: SavedRemotesViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val candidates by store.scanCandidates.collectAsState()
    val candidateKey = candidates.joinToString("|") { it.id }
    var refresh by remember { mutableIntStateOf(0) }
    var machineName by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    val scanner = remember(candidateKey) { UniversalAcScanner(candidates) { candidate ->
        val controls = com.thanhnha.universalacremote.ir.RemoteControls.from(candidate)
        val test = testState(candidate, VerificationCheck.POWER, controls)
        AndroidIrTransmitter.from(context).transmit(com.thanhnha.universalacremote.ir.CatalogTransmitter.encode(candidate, test))
    } }
    @Suppress("UNUSED_VARIABLE") val stateRefresh = refresh
    val current = scanner.selected ?: candidates.getOrNull(scanner.cursor)
    val requirements = current?.let { com.thanhnha.universalacremote.ir.RemoteControls.from(it).verificationRequirements() }.orEmpty()
    Page("Universal Scanner", onBack = onDone) {
        if (scanner.state == ScanState.VERIFYING) {
            Text("Profile phản hồi. Kiểm tra các chức năng profile này có hỗ trợ.")
            var pendingCheck by remember { mutableStateOf<VerificationCheck?>(null) }
            requirements.forEach { check ->
                OutlinedButton(onClick = {
                    runCatching {
                        val c = scanner.selected!!
                        val controls = com.thanhnha.universalacremote.ir.RemoteControls.from(c)
                        AndroidIrTransmitter.from(context).transmit(com.thanhnha.universalacremote.ir.CatalogTransmitter.encode(c, testState(c, check, controls)))
                    }.onSuccess { pendingCheck = check; resultMessage = "Đã gửi lệnh thử. Xác nhận phản ứng thực tế." }
                        .onFailure { resultMessage = it.message ?: "Không thể phát lệnh kiểm tra." }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("${if (check in scanner.verifiedCapabilities) "✓ " else ""}Kiểm tra ${check.label()}")
                }
            }
            pendingCheck?.let { check ->
                Button(onClick = { scanner.recordVerification(check); pendingCheck = null; refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("${check.label()} hoạt động đúng") }
                TextButton(onClick = { pendingCheck = null }) { Text("Không phản hồi") }
            }
            if (resultMessage.isNotBlank()) Text(resultMessage)
            Button(onClick = { scanner.finishVerification(); refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Hoàn tất xác minh") }
        } else if (scanner.state == ScanState.COMPLETE && scanner.result != null) {
            when (scanner.result) {
                com.thanhnha.universalacremote.ir.ScanResult.FULL_MATCH -> Text("Đã xác minh đầy đủ các chức năng được kiểm tra.")
                com.thanhnha.universalacremote.ir.ScanResult.PARTIAL_MATCH -> Text("Profile tương thích một phần. Bạn có thể lưu và kiểm tra lại sau.")
                else -> Text("Không tìm thấy profile phản hồi phù hợp.")
            }
            if (scanner.result == com.thanhnha.universalacremote.ir.ScanResult.NO_MATCH && scanner.cursor + 1 < candidates.size) {
                Button(onClick = { scanner.continueAfterNoMatch(); resultMessage = ""; refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Thử ứng viên tiếp theo") }
            }
            if (scanner.result != com.thanhnha.universalacremote.ir.ScanResult.NO_MATCH) {
                OutlinedTextField(machineName, { machineName = it }, Modifier.fillMaxWidth(), label = { Text("Tên máy") }, singleLine = true)
                Button(onClick = {
                    val c = scanner.selected!!
                    store.save(SavedRemote(UUID.randomUUID().toString(), machineName.trim(), c.id, c.brand, c.acModel, c.remoteModel, c.protocolId, c.protocolModel, scanner.verifiedCapabilities.map { it.name }))
                    onDone()
                }, enabled = machineName.isNotBlank() && scanner.verifiedCapabilities.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Lưu remote") }
            }
        } else {
            if (candidates.isEmpty()) Text("Danh mục đang tải hoặc không có profile phát được cho tiêu chí này.")
            else Text("Ứng viên ${scanner.cursor + 1} / ${candidates.size}")
            current?.let { candidate ->
                Text(candidate.brand, style = MaterialTheme.typography.titleLarge)
                listOfNotNull(candidate.acModel, candidate.remoteModel).takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · ")) }
            }
            Button(enabled = scanner.state == ScanState.READY && current != null, onClick = {
                runCatching { scanner.tryCurrent(System.currentTimeMillis()); resultMessage = "" }
                    .onFailure { resultMessage = it.message ?: "Không thể phát thử profile này." }
                refresh++
            }, modifier = Modifier.fillMaxWidth()) { Text("Phát thử") }
            if (scanner.state == ScanState.AWAITING_FEEDBACK) {
                Button(onClick = { scanner.reportReaction(); refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Có phản ứng") }
                OutlinedButton(onClick = { scanner.reportNoReaction(); refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Không phản ứng") }
            }
            if (resultMessage.isNotBlank()) Text(resultMessage)
        }
        OutlinedButton(onClick = { scanner.stop(); onDone() }, modifier = Modifier.fillMaxWidth()) { Text("Dừng") }
    }
}

@Composable
private fun RemoteScreen(remote: SavedRemote, candidate: RemoteCandidate, onBack: () -> Unit) {
    val context = LocalContext.current
    val controls = remember(candidate.id) { com.thanhnha.universalacremote.ir.RemoteControls.from(candidate) }
    val protocol = remember(candidate.id) { com.thanhnha.universalacremote.ir.CatalogTransmitter.protocol(candidate) }
    val usableModes = controls.modes.filter { com.thanhnha.universalacremote.ir.CatalogTransmitter.mode(it)?.let { mode -> protocol?.modes?.contains(mode) } == true }
    val usableFans = controls.fanModes.filter { com.thanhnha.universalacremote.ir.CatalogTransmitter.fan(it)?.let { fan -> protocol?.fanSpeeds?.contains(fan) } == true }
    val initialTemp = controls.temperatureRange?.let { (it.first + it.last) / 2 } ?: 24
    var power by remember { mutableStateOf(false) }
    var temperature by remember(candidate.id) { mutableIntStateOf(initialTemp) }
    var mode by remember(candidate.id) { mutableStateOf(usableModes.firstOrNull().orEmpty()) }
    var fan by remember(candidate.id) { mutableStateOf(usableFans.firstOrNull().orEmpty()) }
    var swingVertical by remember { mutableStateOf(false) }
    var swingHorizontal by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    fun send() { feedback = runCatching {
        require(protocol != null) { "Profile này chưa có bộ phát khả dụng." }
        val selectedMode = com.thanhnha.universalacremote.ir.CatalogTransmitter.mode(mode) ?: protocol.modes.first()
        val selectedFan = com.thanhnha.universalacremote.ir.CatalogTransmitter.fan(fan) ?: protocol.fanSpeeds.first()
        val model = candidate.copy(protocolModel = com.thanhnha.universalacremote.ir.CatalogTransmitter.modelId(candidate, protocol))
        AndroidIrTransmitter.from(context).transmit(com.thanhnha.universalacremote.ir.CatalogTransmitter.encode(model, AcState(power, temperature, selectedMode, selectedFan, swingVertical, swingHorizontal)))
        "Đã gửi"
    }.fold({ it }, { it.message ?: "Không thể phát" }) }
    Page(remote.displayName, onBack) {
        if (controls.power) Button(onClick = { power = !power; send() }, modifier = Modifier.fillMaxWidth()) { Text(if (power) "⏻ Đang bật" else "⏻ Bật máy") }
        controls.temperatureRange?.let { range ->
            Text("Nhiệt độ · ${temperature}°", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (temperature > range.first) OutlinedButton(onClick = { temperature--; send() }) { Text("−") }
                if (temperature < range.last) OutlinedButton(onClick = { temperature++; send() }) { Text("+") }
            }
        }
        if (usableModes.isNotEmpty()) {
            Text("Chế độ", style = MaterialTheme.typography.titleMedium)
            usableModes.forEach { value -> OutlinedButton(onClick = { mode = value; send() }, modifier = Modifier.fillMaxWidth()) { Text("${if (mode == value) "✓ " else ""}${com.thanhnha.universalacremote.ir.modeLabel(value)}") } }
        }
        if (usableFans.isNotEmpty()) {
            Text("Tốc độ quạt", style = MaterialTheme.typography.titleMedium)
            usableFans.forEach { value -> OutlinedButton(onClick = { fan = value; send() }, modifier = Modifier.fillMaxWidth()) { Text("${if (fan == value) "✓ " else ""}${com.thanhnha.universalacremote.ir.fanLabel(value)}") } }
        }
        if (controls.verticalSwing.type == "ON_OFF") Button(onClick = { swingVertical = !swingVertical; send() }, modifier = Modifier.fillMaxWidth()) { Text("Đảo gió dọc ${if (swingVertical) "Bật" else "Tắt"}") }
        if (controls.verticalSwing.positions.isNotEmpty()) {
            Text("Hướng gió dọc", style = MaterialTheme.typography.titleMedium)
            controls.verticalSwing.positions.forEach { position -> OutlinedButton(onClick = { feedback = "Vị trí ${com.thanhnha.universalacremote.ir.swingPositionLabel(position)} chưa được engine profile này hỗ trợ." }, modifier = Modifier.fillMaxWidth()) { Text(com.thanhnha.universalacremote.ir.swingPositionLabel(position)) } }
        }
        if (controls.horizontalSwing.type == "ON_OFF") Button(onClick = { swingHorizontal = !swingHorizontal; send() }, modifier = Modifier.fillMaxWidth()) { Text("Đảo gió ngang ${if (swingHorizontal) "Bật" else "Tắt"}") }
        if (controls.horizontalSwing.positions.isNotEmpty()) {
            Text("Hướng gió ngang", style = MaterialTheme.typography.titleMedium)
            controls.horizontalSwing.positions.forEach { position -> OutlinedButton(onClick = { feedback = "Vị trí ${com.thanhnha.universalacremote.ir.swingPositionLabel(position)} chưa được engine profile này hỗ trợ." }, modifier = Modifier.fillMaxWidth()) { Text(com.thanhnha.universalacremote.ir.swingPositionLabel(position)) } }
        }
        controls.specialCapabilities.forEach { special ->
            TextButton(onClick = { feedback = "${com.thanhnha.universalacremote.ir.specialCapabilityLabel(special)} chưa được engine profile này hỗ trợ." }) { Text(com.thanhnha.universalacremote.ir.specialCapabilityLabel(special)) }
        }
        if (feedback.isNotBlank()) Text(feedback)
    }
}

@Composable
private fun ImportedRemoteScreen(remote: SavedRemote, store: SavedRemotesViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val commands = remember(remote.id, remote.importedCommandsJson) {
        SavedRemoteConverters().decodeImportedCommands(remote.importedCommandsJson)
    }
    var feedback by remember { mutableStateOf("") }
    Page(remote.displayName, onBack) {
        if (commands.isEmpty()) Text("Remote import này không có command hợp lệ.")
        commands.forEach { command ->
            Button(onClick = {
                feedback = runCatching { AndroidIrTransmitter.from(context).transmit(command.transmission); "Đã gửi ${command.name}" }
                    .getOrElse { it.message ?: "Không thể phát command." }
            }, modifier = Modifier.fillMaxWidth()) { Text(command.name) }
        }
        if (feedback.isNotBlank()) Text(feedback)
        OutlinedButton(onClick = { store.delete(remote.id); onBack() }, modifier = Modifier.fillMaxWidth()) { Text("Xóa remote") }
    }
}

@Composable
private fun MissingProfileScreen(remote: SavedRemote, onRecheck: () -> Unit) {
    Page(remote.displayName, onBack = onRecheck) {
        Text("Không tìm thấy profile máy này trong danh mục hiện tại. Hãy kiểm tra lại để chọn profile tương thích.")
        Button(onClick = onRecheck, modifier = Modifier.fillMaxWidth()) { Text("Kiểm tra lại") }
    }
}

@Composable
private fun DetailsScreen(remote: SavedRemote, candidate: RemoteCandidate?, navigate: (String) -> Unit, store: SavedRemotesViewModel) {
    var showDelete by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var displayName by remember(remote.id) { mutableStateOf(remote.displayName) }
    Page("Chi tiết máy", { navigate("home") }) {
        listOf("Tên" to remote.displayName, "Hãng" to remote.brand, "Model máy lạnh" to (candidate?.acModel ?: remote.acModel ?: "—"), "Model remote" to (candidate?.remoteModel ?: remote.remoteModel ?: "—"), "Protocol/profile" to (candidate?.protocolId ?: remote.protocolId ?: remote.catalogProfileId), "Nguồn dữ liệu" to (candidate?.source?.let(::sourceLabel) ?: "—"), "Đã xác minh" to remote.verifiedCapabilities.joinToString().ifBlank { "—" }).forEach { (key, value) -> Text(key, style = MaterialTheme.typography.labelLarge); Text(value) }
        Button(onClick = { navigate("remote/${remote.id}") }, modifier = Modifier.fillMaxWidth()) { Text("Mở remote") }
        OutlinedButton(onClick = { store.beginScan(com.thanhnha.universalacremote.ir.RemoteQuery(brand = remote.brand)); navigate("scan") }, modifier = Modifier.fillMaxWidth()) { Text("Kiểm tra lại") }
        OutlinedButton(onClick = { showRename = true }, modifier = Modifier.fillMaxWidth()) { Text("Đổi tên") }
        TextButton(onClick = { showDelete = true }) { Text("Xóa máy") }
    }
    if (showRename) AlertDialog(onDismissRequest = { showRename = false }, title = { Text("Đổi tên máy") }, text = { OutlinedTextField(displayName, { displayName = it }, label = { Text("Tên máy") }, singleLine = true) }, confirmButton = { TextButton(onClick = { store.rename(remote.id, displayName); showRename = false }, enabled = displayName.isNotBlank()) { Text("Lưu") } }, dismissButton = { TextButton(onClick = { showRename = false }) { Text("Hủy") } })
    if (showDelete) AlertDialog(onDismissRequest = { showDelete = false }, title = { Text("Xóa máy?") }, confirmButton = { TextButton(onClick = { store.delete(remote.id); navigate("home") }) { Text("Xóa") } }, dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Hủy") } })
}

@Composable
private fun SettingsScreen(diagnostics: IrHardwareDiagnostics, back: () -> Unit) {
    val context = LocalContext.current
    Page("Cài đặt", back) {
        Text("Phiên bản ứng dụng · 1.0")
        Text("Cơ sở dữ liệu IR · Unified IR Catalog")
        Text("Hồng ngoại · ${if (diagnostics.hasIrEmitter) "Sẵn sàng" else "Không khả dụng"}")
        com.thanhnha.universalacremote.ir.FlipperImportPanel(AndroidIrTransmitter.from(context))
        com.thanhnha.universalacremote.update.UpdatePanel()
        Text("Nguồn dữ liệu: IRremoteESP8266 và Flipper-IRDB theo upstream lock của ứng dụng.")
    }
}

@Composable
private fun Page(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("‹ Quay lại") }
        Text(title, style = MaterialTheme.typography.headlineSmall)
        content()
    }
}

private fun VerificationCheck.label() = when (this) {
    VerificationCheck.POWER -> "Power"
    VerificationCheck.TEMPERATURE_CHANGED -> "Nhiệt độ"
    VerificationCheck.MODE -> "Chế độ"
    VerificationCheck.FAN -> "Quạt"
    VerificationCheck.SWING_VERTICAL -> "Hướng gió dọc"
    VerificationCheck.SWING_HORIZONTAL -> "Hướng gió ngang"
}

private fun profileTypeLabel(value: String): String = when (value.uppercase()) {
    "PROTOCOL" -> "Điều khiển tiêu chuẩn"
    "RAW_PROFILE" -> "Profile tín hiệu"
    "IMPORTED_RAW" -> "Tín hiệu thô"
    else -> "Profile"
}

private fun sourceLabel(value: String): String = when (value.lowercase()) {
    "irremoteesp8266" -> "IRremoteESP8266"
    "smartir" -> "SmartIR"
    "flipper-irdb" -> "Flipper-IRDB"
    else -> value
}

private fun testState(
    candidate: RemoteCandidate,
    check: VerificationCheck,
    controls: com.thanhnha.universalacremote.ir.RemoteControls,
): AcState {
    val definition = com.thanhnha.universalacremote.ir.CatalogTransmitter.protocol(candidate)
        ?: error("This profile cannot be verified with the available transmitter.")
    val modes = controls.modes.mapNotNull(com.thanhnha.universalacremote.ir.CatalogTransmitter::mode).filter { it in definition.modes }
    val fans = controls.fanModes.mapNotNull(com.thanhnha.universalacremote.ir.CatalogTransmitter::fan).filter { it in definition.fanSpeeds }
    val mode = modes.firstOrNull() ?: definition.modes.first()
    val fan = fans.firstOrNull() ?: definition.fanSpeeds.first()
    val range = controls.temperatureRange ?: (definition.minTemperatureCelsius..definition.maxTemperatureCelsius)
    val temperature = when (check) {
        VerificationCheck.TEMPERATURE_CHANGED -> if (range.first < range.last) range.first + 1 else range.first
        else -> range.first
    }
    return when (check) {
        VerificationCheck.POWER -> AcState(false, temperature, mode, fan)
        VerificationCheck.TEMPERATURE_CHANGED -> AcState(true, temperature, mode, fan)
        VerificationCheck.MODE -> AcState(true, temperature, modes.getOrNull(1) ?: mode, fan)
        VerificationCheck.FAN -> AcState(true, temperature, mode, fans.getOrNull(1) ?: fan)
        VerificationCheck.SWING_VERTICAL -> AcState(true, temperature, mode, fan, swingVertical = true)
        VerificationCheck.SWING_HORIZONTAL -> AcState(true, temperature, mode, fan, swingHorizontal = true)
    }
}
