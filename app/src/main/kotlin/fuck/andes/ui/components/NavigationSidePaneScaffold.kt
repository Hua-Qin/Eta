package fuck.andes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.R as LucideR
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.BasicComponent

private object NavDrawerMetrics {
    val PaneMaxWidth = 340.dp
    val PaneWidthFraction = 0.84f
    val EdgeSwipeWidth = 36.dp
    val PaneHorizontalPadding = 16.dp
    val TopInset = 16.dp
    val AfterHeader = 20.dp
    val AfterQuickActions = 24.dp
    val SectionTopPadding = 8.dp
    val SectionBottomPadding = 10.dp
    val SectionIconSize = 14.dp
    val SectionIconGap = 8.dp
    val RowMinHeight = 48.dp
    val RowGap = 4.dp
    val RowCornerRadius = 12.dp
    val RowHorizontalPadding = 16.dp
    val RowVerticalPadding = 12.dp
    val QuickActionIconSize = 28.dp
    val QuickActionBadgeSize = 20.dp
    val DockIconSize = 22.dp
    val ListBottomPadding = 20.dp
    val BottomInset = 12.dp
    val DockTopGap = 14.dp
}

enum class NavDestination {
    CHAT,
    MEMORY,
    TOOLS,
    SKILLS,
    WORKFLOWS,
    PERMISSIONS,
    PACKAGES,
}

@Composable
fun NavigationSidePaneScaffold(
    currentDestination: NavDestination,
    visible: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    workflowCount: Int = 0,
    packageCount: Int = 0,
    permissionStatus: String = "正常",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val paneWidth = minOf(maxWidth * NavDrawerMetrics.PaneWidthFraction, NavDrawerMetrics.PaneMaxWidth)
        val edgeSwipeWidthPx = with(density) { NavDrawerMetrics.EdgeSwipeWidth.toPx() }
        val paneWidthPx = with(density) { paneWidth.toPx() }
        var dragging by remember { mutableStateOf(false) }
        var dragOffsetPx by remember { mutableFloatStateOf(0f) }
        var acceptsDrag by remember { mutableStateOf(false) }
        val animatedOffsetPx by animateFloatAsState(
            targetValue = if (visible) paneWidthPx else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "NavPaneOffset",
        )
        val offsetPx = if (dragging) dragOffsetPx else animatedOffsetPx
        val progress = if (paneWidthPx > 0f) {
            (offsetPx / paneWidthPx).coerceIn(0f, 1f)
        } else {
            0f
        }

        if (visible) {
            BackHandler(onBack = onDismiss)
        }

        NavigationPanePanel(
            currentDestination = currentDestination,
            width = paneWidth,
            onNavigate = { dest ->
                onNavigate(dest)
                onDismiss()
            },
            onOpenSettings = onOpenSettings,
            onOpenAbout = onOpenAbout,
            onOpenHelp = onOpenHelp,
            workflowCount = workflowCount,
            packageCount = packageCount,
            permissionStatus = permissionStatus,
            modifier = Modifier.zIndex(0f),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .pointerInput(visible, paneWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            acceptsDrag = if (visible) {
                                offset.x >= paneWidthPx - edgeSwipeWidthPx
                            } else {
                                offset.x <= edgeSwipeWidthPx
                            }
                            if (acceptsDrag) {
                                dragging = true
                                dragOffsetPx = animatedOffsetPx
                            }
                        },
                        onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
                            if (acceptsDrag) {
                                change.consume()
                                dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, paneWidthPx)
                            }
                        },
                        onDragEnd = {
                            if (acceptsDrag) {
                                if (dragOffsetPx >= paneWidthPx * 0.44f) {
                                    onOpen()
                                } else {
                                    onDismiss()
                                }
                            }
                            dragging = false
                            acceptsDrag = false
                        },
                        onDragCancel = {
                            dragging = false
                            acceptsDrag = false
                        },
                    )
                }
                .zIndex(1f),
        ) {
            content()
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MiuixTheme.colorScheme.windowDimming.copy(
                                alpha = MiuixTheme.colorScheme.windowDimming.alpha * progress,
                            ),
                        )
                        .clickable(onClick = onDismiss),
                )
            }
        }
    }
}

@Composable
private fun NavigationPanePanel(
    currentDestination: NavDestination,
    width: androidx.compose.ui.unit.Dp,
    onNavigate: (NavDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    workflowCount: Int,
    packageCount: Int,
    permissionStatus: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(width)
            .fillMaxHeight(),
        color = MiuixTheme.colorScheme.surface,
        contentColor = MiuixTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .safeDrawingPadding()
                .padding(horizontal = NavDrawerMetrics.PaneHorizontalPadding),
        ) {
            Spacer(modifier = Modifier.height(NavDrawerMetrics.TopInset))
            PaneHeader()
            Spacer(modifier = Modifier.height(NavDrawerMetrics.AfterHeader))
            QuickActionsRow(
                workflowCount = workflowCount,
                packageCount = packageCount,
                permissionStatus = permissionStatus,
                onWorkflowsClick = { onNavigate(NavDestination.WORKFLOWS) },
                onPackagesClick = { onNavigate(NavDestination.PACKAGES) },
                onPermissionsClick = { onNavigate(NavDestination.PERMISSIONS) },
            )
            Spacer(modifier = Modifier.height(NavDrawerMetrics.AfterQuickActions))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = NavDrawerMetrics.ListBottomPadding),
                verticalArrangement = Arrangement.spacedBy(NavDrawerMetrics.RowGap),
            ) {
                item(key = "ai-section") {
                    NavSectionHeader(title = "AI 功能")
                }
                item(key = "nav-chat") {
                    NavRow(
                        icon = LucideR.drawable.lucide_ic_message_square,
                        label = "AI 对话",
                        selected = currentDestination == NavDestination.CHAT,
                        onClick = { onNavigate(NavDestination.CHAT) },
                    )
                }
                item(key = "nav-memory") {
                    NavRow(
                        icon = LucideR.drawable.lucide_ic_database,
                        label = "记忆库",
                        selected = currentDestination == NavDestination.MEMORY,
                        onClick = { onNavigate(NavDestination.MEMORY) },
                    )
                }
                item(key = "nav-tools") {
                    NavRow(
                        icon = LucideR.drawable.lucide_ic_grid_3x3,
                        label = "工具箱",
                        selected = currentDestination == NavDestination.TOOLS,
                        onClick = { onNavigate(NavDestination.TOOLS) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(NavDrawerMetrics.DockTopGap))
            PaneDock(
                onOpenAbout = onOpenAbout,
                onOpenHelp = onOpenHelp,
                onOpenSettings = onOpenSettings,
            )
            Spacer(modifier = Modifier.height(NavDrawerMetrics.BottomInset))
        }
    }
}

@Composable
private fun PaneHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Eta",
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759)),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "移动数据",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    workflowCount: Int,
    packageCount: Int,
    permissionStatus: String,
    onWorkflowsClick: () -> Unit,
    onPackagesClick: () -> Unit,
    onPermissionsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        QuickActionItem(
            icon = LucideR.drawable.lucide_ic_package,
            label = "包管理",
            badge = packageCount.toString(),
            onClick = onPackagesClick,
        )
        QuickActionItem(
            icon = LucideR.drawable.lucide_ic_lock,
            label = "权限",
            badge = permissionStatus,
            onClick = onPermissionsClick,
        )
        QuickActionItem(
            icon = LucideR.drawable.lucide_ic_layout_template,
            label = "工作流",
            badge = workflowCount.toString(),
            onClick = onWorkflowsClick,
        )
    }
}

@Composable
private fun QuickActionItem(
    icon: Int,
    label: String,
    badge: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(NavDrawerMetrics.QuickActionIconSize),
                tint = MiuixTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .offset(x = 8.dp, y = (-4).dp)
                    .size(NavDrawerMetrics.QuickActionBadgeSize)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge,
                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                    style = MiuixTheme.textStyles.caption2,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.footnote,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun NavSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = NavDrawerMetrics.SectionTopPadding,
                bottom = NavDrawerMetrics.SectionBottomPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun NavRow(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NavDrawerMetrics.RowMinHeight)
            .clip(RoundedCornerShape(NavDrawerMetrics.RowCornerRadius))
            .background(
                if (selected) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = NavDrawerMetrics.RowHorizontalPadding,
                vertical = NavDrawerMetrics.RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = if (selected) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.onSurface
            },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (selected) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.onSurface
            },
            style = MiuixTheme.textStyles.body1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun PaneDock(
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockButton(
            icon = LucideR.drawable.lucide_ic_info,
            label = "关于",
            onClick = onOpenAbout,
        )
        DockButton(
            icon = LucideR.drawable.lucide_ic_help_circle,
            label = "使用手册",
            onClick = onOpenHelp,
        )
        DockButton(
            icon = LucideR.drawable.lucide_ic_settings,
            label = "设置",
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun DockButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(NavDrawerMetrics.DockIconSize),
            tint = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.caption2,
            fontWeight = FontWeight.Medium,
        )
    }
}
