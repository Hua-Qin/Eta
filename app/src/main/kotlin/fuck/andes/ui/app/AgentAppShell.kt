package fuck.andes.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import fuck.andes.ui.components.HistorySidePaneScaffold
import fuck.andes.ui.components.NavDestination
import fuck.andes.ui.components.NavigationSidePaneScaffold
import fuck.andes.ui.navigation.AppRoute
import fuck.andes.ui.model.ConversationPaneUiState
import fuck.andes.ui.model.ConversationSummaryUi
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AgentAppShell(
    currentRoute: AppRoute?,
    conversationPaneState: ConversationPaneUiState?,
    isNavPaneOpen: Boolean,
    isHistoryPaneOpen: Boolean,
    onBack: () -> Unit,
    onOpenNavPane: () -> Unit,
    onDismissNavPane: () -> Unit,
    onOpenHistoryPane: () -> Unit,
    onDismissHistoryPane: () -> Unit,
    onSearchConversations: (String) -> Unit,
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onConversationRename: (ConversationSummaryUi) -> Unit,
    onConversationDelete: (ConversationSummaryUi) -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    workflowCount: Int = 0,
    packageCount: Int = 0,
    permissionStatus: String = "正常",
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val pageContent: @Composable () -> Unit = {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
            topBar = {
                if (currentRoute !is AppRoute.Settings) {
                    AgentTopBar(
                        route = currentRoute,
                        onBack = onBack,
                        onOpenNavPane = onOpenNavPane,
                        onOpenHistoryPane = onOpenHistoryPane,
                        onNewConversation = onNewConversation,
                    )
                }
            },
        ) { padding ->
            content(padding)
        }
    }

    NavigationSidePaneScaffold(
        currentDestination = navDestinationForRoute(currentRoute),
        visible = isNavPaneOpen,
        onOpen = onOpenNavPane,
        onDismiss = onDismissNavPane,
        onNavigate = onNavigate,
        onOpenSettings = onOpenSettings,
        onOpenAbout = onOpenAbout,
        onOpenHelp = onOpenHelp,
        workflowCount = workflowCount,
        packageCount = packageCount,
        permissionStatus = permissionStatus,
        modifier = modifier,
    ) {
        if (conversationPaneState != null && currentRoute is AppRoute.Home) {
            HistorySidePaneScaffold(
                state = conversationPaneState,
                visible = isHistoryPaneOpen,
                onOpen = onOpenHistoryPane,
                onDismiss = onDismissHistoryPane,
                onSearchChange = onSearchConversations,
                onConversationSelected = onSelectConversation,
                onConversationRename = onConversationRename,
                onConversationDelete = onConversationDelete,
                onNewConversation = onNewConversation,
            ) {
                pageContent()
            }
        } else {
            pageContent()
        }
    }
}

private fun navDestinationForRoute(route: AppRoute?): NavDestination = when (route) {
    is AppRoute.Home, is AppRoute.Chat -> NavDestination.CHAT
    is AppRoute.Tools -> NavDestination.TOOLS
    is AppRoute.Skills -> NavDestination.SKILLS
    is AppRoute.Permissions -> NavDestination.PERMISSIONS
    is AppRoute.Workflows -> NavDestination.WORKFLOWS
    else -> NavDestination.CHAT
}

@Composable
private fun AgentTopBar(
    route: AppRoute?,
    onBack: () -> Unit,
    onOpenNavPane: () -> Unit,
    onOpenHistoryPane: () -> Unit,
    onNewConversation: () -> Unit,
) {
    val isHome = route is AppRoute.Home
    SmallTopAppBar(
        title = titleForRoute(route),
        color = if (route is AppRoute.Tools) Color.Transparent else MiuixTheme.colorScheme.surface,
        navigationIcon = {
            if (isHome) {
                IconButton(onClick = onOpenNavPane) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_menu),
                        contentDescription = "菜单",
                    )
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_chevron_left),
                        contentDescription = "返回",
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenHistoryPane) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_history),
                    contentDescription = "历史对话",
                )
            }
        },
    )
}

@Composable
private fun titleForRoute(route: AppRoute?): String = when (route) {
    is AppRoute.Home -> ""
    is AppRoute.Chat -> "对话"
    is AppRoute.Browser -> "Agent 浏览器"
    is AppRoute.Tools -> "工具能力"
    is AppRoute.Skills -> "技能"
    is AppRoute.Permissions -> "权限健康"
    is AppRoute.SystemEnhance -> "系统增强"
    is AppRoute.Settings -> "设置"
    is AppRoute.LinuxEnvironment -> "Linux 工具环境"
    is AppRoute.ModelProviders -> "模型提供商"
    is AppRoute.ModelProviderDetail -> route.providerId.let { "Provider 详情" }
    is AppRoute.ModelProviderNew -> "新建提供商"
    is AppRoute.Workflows -> "工作流管理"
    is AppRoute.WorkflowEditor -> if (route.workflowId != null) "编辑工作流" else "新建工作流"
    is AppRoute.WorkflowRun -> "工作流运行"
    null -> "Eta"
}
