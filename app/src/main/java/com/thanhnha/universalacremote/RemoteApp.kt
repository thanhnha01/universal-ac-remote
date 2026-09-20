package com.thanhnha.universalacremote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thanhnha.universalacremote.ir.IrHardwareDiagnostics
import com.thanhnha.universalacremote.ir.RemoteQuery

@Composable
fun RemoteApp(
    diagnostics: IrHardwareDiagnostics,
    store: SavedRemotesViewModel = viewModel(),
) {
    val nav = rememberNavController()
    val home by store.state.collectAsState()
    val catalog by store.catalog.collectAsState()

    fun navigateTab(route: String) {
        val resolvedRoute = when (route) {
            "remote" -> "devices"
            "scan" -> {
                store.clearScan()
                "scan"
            }
            else -> route
        }
        nav.navigate(resolvedRoute) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    UniversalAcTheme {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                ProductionHomeScreen(home, diagnostics, store, ::navigateTab) { route ->
                    if (route.startsWith("remote/")) {
                        store.markUsed(route.removePrefix("remote/"))
                    }
                    nav.navigate(route)
                }
            }

            composable("devices") {
                ProductionDevicesScreen(home, store, ::navigateTab) { route ->
                    if (route.startsWith("remote/")) {
                        store.markUsed(route.removePrefix("remote/"))
                    }
                    nav.navigate(route)
                }
            }

            composable("add") {
                ProductionAddScreen(store, catalog, ::navigateTab) { route ->
                    nav.navigate(route)
                }
            }

            composable("import") {
                IrImportScreen(
                    store = store,
                    onTab = ::navigateTab,
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack("home", false) },
                )
            }

            composable("scan") {
                ProductionScannerScreen(
                    store = store,
                    onTab = ::navigateTab,
                    onDone = { nav.popBackStack("home", false) },
                    onChangeBrand = { store.clearScan() },
                    onImport = { nav.navigate("import") },
                )
            }

            composable(
                "remote/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val remote = home.remotes.find { it.id == entry.arguments?.getString("id") }
                when {
                    remote == null -> MissingRemoteScreen {
                        nav.popBackStack("home", false)
                    }

                    remote.importedCommandsJson.isNotBlank() -> ImportedRemoteControlScreen(
                        remote = remote,
                        onTab = ::navigateTab,
                        onDetails = { nav.navigate("details/${remote.id}") },
                        onBack = { nav.popBackStack() },
                    )

                    else -> {
                        val profile = store.profileFor(remote.catalogProfileId)
                        when {
                            catalog.loading -> LoadingScreen { nav.popBackStack() }
                            profile == null -> MissingProfileScreen(remote) {
                                store.beginScan()
                                nav.navigate("scan")
                            }

                            else -> RemoteControlScreen(
                                remote = remote,
                                candidate = profile,
                                diagnostics = diagnostics,
                                store = store,
                                onTab = ::navigateTab,
                                onDetails = { nav.navigate("details/${remote.id}") },
                                onBack = { nav.popBackStack() },
                            )
                        }
                    }
                }
            }

            composable(
                "details/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                home.remotes.find { it.id == entry.arguments?.getString("id") }?.let { remote ->
                    ProductionDetailsScreen(
                        remote = remote,
                        candidate = store.profileFor(remote.catalogProfileId),
                        store = store,
                        onOpen = { nav.navigate("remote/${remote.id}") },
                        onRetest = {
                            store.beginScan(RemoteQuery(brand = remote.brand))
                            nav.navigate("scan")
                        },
                        onBack = { nav.popBackStack() },
                    )
                }
            }

            composable("settings") {
                ProductionSettingsScreen(
                    diagnostics = diagnostics,
                    catalog = catalog,
                    store = store,
                    onTab = ::navigateTab,
                    openDiagnostics = { nav.navigate("diagnostics") },
                    openImport = { nav.navigate("import") },
                )
            }

            composable("diagnostics") {
                DiagnosticScreen(
                    diagnostics = diagnostics,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun PageWithBack(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppBackground {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title, subtitle, onBack)
            PageColumn(PaddingValues(0.dp), content)
        }
    }
}

@Composable
private fun LoadingScreen(onBack: () -> Unit) {
    PageWithBack("Đang tải…", "Mở remote", onBack) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(42.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MissingRemoteScreen(onBack: () -> Unit) {
    PageWithBack("Không tìm thấy remote", null, onBack) {
        EmptyState(
            "Remote không tồn tại",
            "Quay lại danh sách để chọn một remote khác.",
        )
    }
}

@Composable
private fun MissingProfileScreen(
    remote: SavedRemote,
    onRecheck: () -> Unit,
) {
    PageWithBack(remote.displayName, "Cần kiểm tra lại remote", onRecheck) {
        EmptyState(
            "Dữ liệu điều khiển đã thay đổi",
            "Remote đã lưu không còn khớp với thư viện hiện tại. Hãy dò lại để chọn mã điều khiển phù hợp.",
        )
        PrimaryButton(
            "Dò lại remote",
            Modifier.fillMaxWidth(),
            Icons.Filled.Refresh,
            onClick = onRecheck,
        )
    }
}
