package com.thanhnha.universalacremote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * V3 visual tokens.
 *
 * Keep the legacy token names used by production screens while moving the app
 * to a calmer, brighter hierarchy inspired by the approved V3 mockups.
 */
object AppColors {
    val navy = Color(0xFF0B1F44)
    val navySoft = Color(0xFF64748B)
    val inkMuted = Color(0xFF8A9AAF)

    val blue = Color(0xFF087CF0)
    val blueDeep = Color(0xFF075ED1)
    val cyan = Color(0xFF19B9F5)
    val aqua = Color(0xFF58D8F7)

    val paleBlue = Color(0xFFEDF7FF)
    val paleBlueStrong = Color(0xFFD9EFFF)
    val page = Color(0xFFF4F9FE)
    val surface = Color(0xFFFFFFFF)
    val surfaceSoft = Color(0xFFF8FBFE)
    val line = Color(0xFFDCE8F3)
    val lineStrong = Color(0xFFC9DCEA)

    val mint = Color(0xFF12A86B)
    val paleMint = Color(0xFFEAFBF4)
    val danger = Color(0xFFE5484D)
    val paleDanger = Color(0xFFFFF0F1)
    val warning = Color(0xFFE88A16)
    val paleWarning = Color(0xFFFFF7E9)
    val purple = Color(0xFF7257E8)
    val palePurple = Color(0xFFF3F0FF)
}

object AppSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object AppRadius {
    val control = 16.dp
    val card = 24.dp
    val hero = 30.dp
    val dock = 30.dp
}

private val AppScheme = lightColorScheme(
    primary = AppColors.blue,
    onPrimary = Color.White,
    secondary = AppColors.cyan,
    onSecondary = Color.White,
    background = AppColors.page,
    surface = AppColors.surface,
    onBackground = AppColors.navy,
    onSurface = AppColors.navy,
    onSurfaceVariant = AppColors.navySoft,
    outline = AppColors.line,
    error = AppColors.danger,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 48.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1.1).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 40.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Bold,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(AppRadius.card),
    extraLarge = RoundedCornerShape(AppRadius.hero),
)

@Composable
fun UniversalAcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

val PrimaryGradient = Brush.horizontalGradient(
    listOf(AppColors.blueDeep, AppColors.blue, AppColors.cyan),
)

val SoftHeroGradient = Brush.linearGradient(
    listOf(
        Color(0xFFE8F6FF),
        Color(0xFFF5FBFF),
        Color.White,
    ),
)

val CoolSurfaceGradient = Brush.linearGradient(
    listOf(
        Color(0xFFDFF3FF),
        Color(0xFFF4FAFF),
    ),
)

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEFF8FF),
                        AppColors.page,
                        Color(0xFFF9FCFF),
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = AppColors.cyan.copy(alpha = 0.07f),
                radius = size.minDimension * 0.58f,
                center = Offset(size.width * 1.06f, size.height * 0.02f),
            )
            drawCircle(
                color = AppColors.blue.copy(alpha = 0.045f),
                radius = size.minDimension * 0.48f,
                center = Offset(-size.width * 0.05f, size.height * 0.72f),
            )
        }
        content()
    }
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
                if (bottomBar) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        AppBottomNavigation(selectedRoute, onNavigate)
                    }
                }
            },
            content = content,
        )
    }
}

private data class AppNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val appNavDestinations = listOf(
    AppNavDestination("home", "Trang chủ", Icons.Outlined.Home),
    AppNavDestination("remote", "Remote", Icons.Outlined.AcUnit),
    AppNavDestination("scan", "Dò mã", Icons.Outlined.Radio),
    AppNavDestination("settings", "Cài đặt", Icons.Outlined.Settings),
)

@Composable
fun AppBottomNavigation(selectedRoute: String?, onNavigate: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(AppRadius.dock),
                clip = false,
            ),
        shape = RoundedCornerShape(AppRadius.dock),
        color = Color.White.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            appNavDestinations.forEach { destination ->
                val selected = selectedRoute == destination.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onNavigate(destination.route) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (selected) Modifier.background(PrimaryGradient)
                                else Modifier.background(Color.Transparent),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            tint = if (selected) Color.White else AppColors.navySoft,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Text(
                        destination.label,
                        color = if (selected) AppColors.blueDeep else AppColors.navySoft,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun BrandHeader(actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .size(52.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier.background(PrimaryGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.AcUnit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                "Universal A/C Remote",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.navy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Mát hơn. Đơn giản hơn.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.navySoft,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun HeaderIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(46.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AppColors.line),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = AppColors.navy,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        onBack?.let { back ->
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clickable(onClick = back),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, AppColors.line),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = AppColors.navy,
                    )
                }
            }
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.navy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.navySoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleMedium,
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
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, AppColors.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
        shape = RoundedCornerShape(AppRadius.hero),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, AppColors.line),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(background),
        ) {
            content()
        }
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
        modifier = modifier
            .height(54.dp)
            .then(
                if (enabled) Modifier.shadow(5.dp, RoundedCornerShape(18.dp), clip = false)
                else Modifier,
            ),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.blue,
            contentColor = Color.White,
            disabledContainerColor = AppColors.line,
            disabledContentColor = AppColors.inkMuted,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        icon?.let {
            Icon(it, contentDescription = null)
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppColors.lineStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppColors.navy,
            disabledContentColor = AppColors.inkMuted,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        icon?.let {
            Icon(it, contentDescription = null)
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StatusChip(
    text: String,
    icon: ImageVector,
    color: Color = AppColors.blue,
    background: Color = AppColors.paleBlue,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background,
        border = BorderStroke(1.dp, color.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text,
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            .clip(RoundedCornerShape((size * 0.34f).dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.48f).dp),
        )
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                placeholder,
                color = AppColors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = AppColors.navySoft,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            disabledContainerColor = AppColors.surfaceSoft,
            unfocusedBorderColor = AppColors.line,
            focusedBorderColor = AppColors.blue,
            cursorColor = AppColors.blue,
        ),
    )
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Filled.Info,
) {
    SurfaceCard(Modifier.fillMaxWidth(), SoftHeroGradient) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            IconBubble(icon, size = 56)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.navy,
                textAlign = TextAlign.Center,
            )
            Text(
                message,
                color = AppColors.navySoft,
                textAlign = TextAlign.Center,
            )
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
        shape = RoundedCornerShape(18.dp),
        color = background,
        border = BorderStroke(1.dp, color.copy(alpha = 0.14f)),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(23.dp),
            )
            Text(
                text,
                modifier = Modifier.weight(1f),
                color = AppColors.navySoft,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun GradientHero(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        background = SoftHeroGradient,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            content()
        }
    }
}

@Composable
fun PageColumn(
    padding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
        modifier = Modifier
            .size(112.dp)
            .shadow(10.dp, RoundedCornerShape(56.dp), clip = false),
        shape = RoundedCornerShape(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.blue,
            disabledContainerColor = AppColors.line,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            Icons.Filled.PowerSettingsNew,
            contentDescription = "Bật hoặc tắt máy",
            modifier = Modifier.size(48.dp),
        )
    }
}
