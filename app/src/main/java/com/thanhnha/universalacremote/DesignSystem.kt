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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
    val navy = Color(0xFF10213E)
    val navySoft = Color(0xFF64748B)
    val blue = Color(0xFF2563EB)
    val cyan = Color(0xFF0EA5E9)
    val paleBlue = Color(0xFFEFF6FF)
    val paleBlueStrong = Color(0xFFDBEAFE)
    val page = Color(0xFFF8FAFC)
    val line = Color(0xFFE2E8F0)
    val mint = Color(0xFF16A34A)
    val paleMint = Color(0xFFF0FDF4)
    val danger = Color(0xFFDC2626)
    val paleDanger = Color(0xFFFEF2F2)
    val warning = Color(0xFFD97706)
    val paleWarning = Color(0xFFFFFBEB)
    val purple = Color(0xFF7C3AED)
}

private val AppScheme = lightColorScheme(
    primary = AppColors.blue,
    onPrimary = Color.White,
    secondary = AppColors.cyan,
    onSecondary = Color.White,
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

val PrimaryGradient = Brush.horizontalGradient(listOf(AppColors.blue, AppColors.cyan))
val SoftHeroGradient = Brush.linearGradient(listOf(Color(0xFFF0F7FF), Color(0xFFF8FCFF)))

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.page)
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
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.shadow(4.dp).navigationBarsPadding(),
    ) {
        items.forEach { (route, item) ->
            val selected = selectedRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = { Icon(item.second, contentDescription = item.first) },
                label = {
                    Text(
                        item.first,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.blue,
                    selectedTextColor = AppColors.blue,
                    unselectedIconColor = AppColors.navySoft,
                    unselectedTextColor = AppColors.navySoft,
                    indicatorColor = AppColors.paleBlue,
                ),
            )
        }
    }
}

@Composable
fun BrandHeader(actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PrimaryGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AcUnit, null, tint = Color.White, modifier = Modifier.size(29.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Universal A/C Remote",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.navy,
            )
            Text(
                "Điều khiển máy lạnh bằng IR",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.navySoft,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun HeaderIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(44.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = AppColors.navy, modifier = Modifier.size(22.dp))
        }
    }
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
                Text(title, fontWeight = FontWeight.Bold, color = AppColors.navy)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.navySoft)
                }
            }
        },
        navigationIcon = {
            onBack?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Filled.ArrowBack, "Quay lại", tint = AppColors.navy)
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
        modifier = modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
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
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, AppColors.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, AppColors.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.blue,
            disabledContainerColor = AppColors.line,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        icon?.let { Icon(it, null); Spacer(Modifier.size(8.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold)
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
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppColors.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.navy),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        icon?.let { Icon(it, null); Spacer(Modifier.size(8.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusChip(
    text: String,
    icon: ImageVector,
    color: Color = AppColors.blue,
    background: Color = AppColors.paleBlue,
) {
    Surface(shape = RoundedCornerShape(999.dp), color = background) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
            Text(text, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun IconBubble(
    icon: ImageVector,
    tint: Color = AppColors.blue,
    background: Color = AppColors.paleBlue,
    size: Int = 48,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((size * 0.48f).dp))
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = AppColors.navySoft) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = AppColors.navySoft) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
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
            IconBubble(icon)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, color = AppColors.navySoft, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun InfoBanner(
    text: String,
    icon: ImageVector = Icons.Filled.Lightbulb,
    color: Color = AppColors.warning,
    background: Color = AppColors.paleWarning,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = background,
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Text(text, color = AppColors.navySoft, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun GradientHero(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    SurfaceCard(modifier.fillMaxWidth(), SoftHeroGradient) {
        Box(Modifier.fillMaxWidth().padding(18.dp)) { content() }
    }
}

@Composable
fun PageColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun LargePowerButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(112.dp),
        shape = RoundedCornerShape(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.blue),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(Icons.Filled.PowerSettingsNew, "Bật hoặc tắt máy", modifier = Modifier.size(48.dp))
    }
}
