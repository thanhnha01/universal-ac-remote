package com.thanhnha.universalacremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

object AppColors {
    val navy = Color(0xFF0B1C51)
    val navySoft = Color(0xFF49618E)
    val blue = Color(0xFF1479EA)
    val cyan = Color(0xFF27C7D8)
    val paleBlue = Color(0xFFEAF5FF)
    val paleBlueStrong = Color(0xFFDCEEFF)
    val page = Color(0xFFF4FAFF)
    val line = Color(0xFFD5E8FA)
    val mint = Color(0xFF0BAA70)
    val paleMint = Color(0xFFE9FBF3)
    val danger = Color(0xFFE92955)
    val paleDanger = Color(0xFFFFEEF2)
    val warning = Color(0xFFF0A516)
    val paleWarning = Color(0xFFFFF8E8)
    val purple = Color(0xFF7856D9)
}

private val AppScheme = lightColorScheme(
    primary = AppColors.blue,
    onPrimary = Color.White,
    secondary = AppColors.cyan,
    onSecondary = AppColors.navy,
    background = AppColors.page,
    surface = Color.White,
    onBackground = AppColors.navy,
    onSurface = AppColors.navy,
    onSurfaceVariant = AppColors.navySoft,
    outline = AppColors.line,
    error = AppColors.danger,
)

@Composable
fun UniversalAcTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppScheme, content = content)
}

val PrimaryGradient = Brush.horizontalGradient(listOf(AppColors.cyan, AppColors.blue))
val SoftHeroGradient = Brush.linearGradient(listOf(Color(0xFFE7F6FF), Color(0xFFD4EEFF)))

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.background(
            Brush.verticalGradient(listOf(Color.White, AppColors.page, Color(0xFFEAF6FF)))
        )
    ) { content() }
}

@Composable
fun AppScaffold(
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    bottomBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (bottomBar) AppBottomNavigation(selectedRoute, onNavigate)
            },
            content = content,
        )
    }
}

@Composable
fun AppBottomNavigation(selectedRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        "home" to ("Trang chủ" to Icons.Outlined.Home),
        "remote" to ("Remote" to Icons.Outlined.AcUnit),
        "scan" to ("Dò tìm" to Icons.Outlined.Radio),
        "settings" to ("Cài đặt" to Icons.Outlined.Settings),
    )
    NavigationBar(
        containerColor = Color.White.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        modifier = Modifier.shadow(8.dp).navigationBarsPadding(),
    ) {
        items.forEach { (route, item) ->
            val selected = selectedRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) AppColors.paleBlueStrong else Color.Transparent)
                            .padding(horizontal = 18.dp, vertical = 5.dp)
                    ) { Icon(item.second, contentDescription = item.first) }
                },
                label = { Text(item.first, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.blue,
                    selectedTextColor = AppColors.blue,
                    unselectedIconColor = AppColors.navySoft,
                    unselectedTextColor = AppColors.navySoft,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
fun BrandHeader(actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(PrimaryGradient),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.AcUnit, null, tint = Color.White, modifier = Modifier.size(38.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Universal A/C Remote", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
            Surface(shape = RoundedCornerShape(18.dp), color = AppColors.paleBlue) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Filled.SignalCellularAlt, null, tint = AppColors.blue, modifier = Modifier.size(15.dp))
                    Text("OnePlus 15 IR", color = AppColors.blue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun HeaderIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp), color = AppColors.paleBlue,
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, description, tint = AppColors.navy, modifier = Modifier.size(25.dp)) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(title, fontWeight = FontWeight.ExtraBold, color = AppColors.navy)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.navySoft)
                }
            }
        },
        navigationIcon = {
            onBack?.let {
                Surface(shape = RoundedCornerShape(24.dp), color = AppColors.paleBlue) {
                    IconButton(onClick = it) { Icon(Icons.Filled.ArrowBack, "Quay lại", tint = AppColors.navy) }
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 6.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = AppColors.navy,
    )
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(24.dp), clip = false),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, AppColors.line.copy(alpha = 0.7f)),
        content = { content() },
    )
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    background: Brush,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(24.dp), clip = false),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, AppColors.line.copy(alpha = 0.7f)),
    ) {
        Box(Modifier.fillMaxWidth().background(background)) { content() }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.blue, disabledContainerColor = AppColors.line),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        icon?.let { Icon(it, null); Spacer(Modifier.size(10.dp)) }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppColors.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.navy),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        icon?.let { Icon(it, null); Spacer(Modifier.size(10.dp)) }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusChip(
    text: String,
    icon: ImageVector,
    color: Color = AppColors.blue,
    background: Color = AppColors.paleBlue,
) {
    Surface(shape = RoundedCornerShape(22.dp), color = background) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(19.dp))
            Text(text, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IconBubble(icon: ImageVector, tint: Color = AppColors.blue, background: Color = AppColors.paleBlueStrong, size: Int = 48) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(background),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size((size * 0.52f).dp)) }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = AppColors.navySoft) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = AppColors.navy) },
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = 0.88f),
            focusedContainerColor = Color.White,
            unfocusedBorderColor = AppColors.line,
            focusedBorderColor = AppColors.blue,
        ),
    )
}

@Composable
fun EmptyState(title: String, message: String, icon: ImageVector = Icons.Filled.Info) {
    SurfaceCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconBubble(icon, tint = AppColors.blue)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, color = AppColors.navySoft, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun InfoBanner(text: String, icon: ImageVector = Icons.Filled.Lightbulb, color: Color = AppColors.warning, background: Color = AppColors.paleWarning) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = background,
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Text(text, color = AppColors.navySoft)
        }
    }
}

@Composable
fun GradientHero(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(SoftHeroGradient)
            .shadow(3.dp, RoundedCornerShape(26.dp), clip = false)
            .padding(20.dp),
    ) { content() }
}

@Composable
fun PageColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun LargePowerButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(124.dp),
        shape = RoundedCornerShape(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.blue),
        contentPadding = PaddingValues(0.dp),
    ) { Icon(Icons.Filled.PowerSettingsNew, "Bật hoặc tắt máy", modifier = Modifier.size(54.dp)) }
}
