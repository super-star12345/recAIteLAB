package com.recaitelab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.recaitelab.app.di.AppContainer
import com.recaitelab.app.domain.model.AppDesignSystem
import com.recaitelab.app.domain.model.AppearanceMode
import com.recaitelab.app.domain.model.GlassQuality
import com.recaitelab.app.ui.chat.ChatScreen
import com.recaitelab.app.ui.chat.ChatViewModel
import com.recaitelab.app.ui.chat.PrismChatOverlay
import com.recaitelab.app.ui.history.HistoryScreen
import com.recaitelab.app.ui.history.HistoryViewModel
import com.recaitelab.app.ui.knowledge.KnowledgeDetailScreen
import com.recaitelab.app.ui.knowledge.KnowledgeDetailViewModel
import com.recaitelab.app.ui.knowledge.KnowledgeScreen
import com.recaitelab.app.ui.knowledge.KnowledgeViewModel
import com.recaitelab.app.ui.navigation.ChatRoute
import com.recaitelab.app.ui.navigation.HistoryRoute
import com.recaitelab.app.ui.navigation.KnowledgeDetailRoute
import com.recaitelab.app.ui.navigation.KnowledgeRoute
import com.recaitelab.app.ui.navigation.SettingsAppearanceRoute
import com.recaitelab.app.ui.navigation.SettingsModelsRoute
import com.recaitelab.app.ui.navigation.SettingsPrismRoute
import com.recaitelab.app.ui.navigation.SettingsRoute
import com.recaitelab.app.ui.prism.LocalGlassQuality
import com.recaitelab.app.ui.prism.PrismDock
import com.recaitelab.app.ui.prism.PrismDockItem
import com.recaitelab.app.ui.prism.PrismDockSpec
import com.recaitelab.app.ui.prism.PrismMotion
import com.recaitelab.app.ui.prism.PrismPreviewScreen
import com.recaitelab.app.ui.prism.PrismTheme
import com.recaitelab.app.ui.settings.AppearanceSettingsScreen
import com.recaitelab.app.ui.settings.ModelSettingsScreen
import com.recaitelab.app.ui.settings.SettingsEvent
import com.recaitelab.app.ui.settings.SettingsScreen
import com.recaitelab.app.ui.settings.SettingsViewModel
import com.recaitelab.app.ui.theme.LocalAppDesignSystem
import com.recaitelab.app.ui.theme.LocalPrismChatInputHeight
import com.recaitelab.app.ui.theme.LocalPrismDockSpace
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)
            val settingsState by settingsViewModel.uiState.collectAsState()
            RecAIteLABTheme(
                appearanceMode = settingsState.appearanceMode,
                colorTheme = settingsState.colorTheme,
                dynamicColorEnabled = settingsState.dynamicColorEnabled,
                designSystem = settingsState.designSystem,
                prismColorStyle = settingsState.prismColorStyle,
                prismSeedColor = settingsState.prismSeedColor,
                prismGlassQuality = settingsState.glassQuality,
            ) {
                AppRoot(settingsViewModel = settingsViewModel)
            }
        }
    }
}

/**
 * 单 Activity + 四 Tab 导航骨架。
 *
 * ViewModel 提升到 Activity 作用域，避免 Tab 切换（destination 被 saveState 移出组合）
 * 时丢失聊天草稿、生成任务与设置表单；NavHost 自身负责保存/恢复各 Tab 的 Compose 状态。
 *
 * PRISM_GLASS 模式使用自定义转场与玻璃底栏；MATERIAL_3 模式保持原 Scaffold + NavigationBar。
 */
@Composable
private fun AppRoot(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel(factory = chatViewModelFactory)
    val historyViewModel: HistoryViewModel = viewModel(factory = historyViewModelFactory)
    val knowledgeViewModel: KnowledgeViewModel = viewModel(factory = knowledgeViewModelFactory)
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    // 二级页（如知识详情）不属于任何一级 Tab 时隐藏底栏；键盘显隐不参与底栏布局。
    val showBottomBar = if (currentDestination == null) {
        true
    } else {
        topLevelTabs.any { tab ->
            currentDestination.hierarchy.any { destination ->
                destination.hasRoute(tab.route::class)
            }
        }
    }

    val isPrism = LocalAppDesignSystem.current == AppDesignSystem.PRISM_GLASS
    val chatState by chatViewModel.uiState.collectAsState()
    val chatInputHeight = remember { mutableStateOf(80.dp) }

    if (isPrism) {
        val glassQuality = LocalGlassQuality.current
        // ULTRA 也需要真实背景模糊；只有 OFF 关闭模糊、走不透明降级。
        val hazeState = rememberHazeState(blurEnabled = glassQuality != GlassQuality.OFF)
        val navBarBottom = WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
        // Dock 自身高度 + 与内容/系统栏的间距；页面据此预留底部空间。
        val dockSpace = if (showBottomBar) 80.dp + navBarBottom else 0.dp

        val backdrop = if (
            currentDestination != null && !currentDestination.isTopLevelDestination()
        ) {
            // 二级页推入时用黑色底承托 10% 暗化；Tab 间 fade-through 使用主题底色避免闪黑。
            Color.Black
        } else {
            PrismTheme.colors.background
        }
        val chatActive = currentDestination
            ?.hierarchy
            ?.any { destination -> destination.hasRoute(ChatRoute::class) }
            ?: true

        // 整页 NavHost 作为 Haze source；顶栏/输入区/Sheet 在 NavHost 外层绘制，
        // 因此可以采样整页内容，而不会被 source 的子树裁掉。
        CompositionLocalProvider(
            LocalPrismDockSpace provides dockSpace,
            LocalPrismChatInputHeight provides chatInputHeight.value,
        ) {
            Box(modifier = Modifier.fillMaxSize().background(backdrop)) {
                NavHost(
                    navController = navController,
                    startDestination = ChatRoute,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState),
                enterTransition = { prismEnter() },
                exitTransition = { prismExit() },
                popEnterTransition = { prismPopEnter() },
                popExitTransition = { prismPopExit() },
            ) {
                appDestinations(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    chatViewModel = chatViewModel,
                    historyViewModel = historyViewModel,
                    knowledgeViewModel = knowledgeViewModel,
                )
            }
            if (showBottomBar) {
                val selectedIndex = topLevelTabs.indexOfFirst { tab ->
                    currentDestination
                        ?.hierarchy
                        ?.any { destination -> destination.hasRoute(tab.route::class) }
                        ?: (tab.route == ChatRoute)
                }.coerceAtLeast(0)
                PrismDock(
                    items = topLevelTabs.map { tab -> PrismDockItem(tab.icon, tab.labelRes) },
                    selectedIndex = selectedIndex,
                    quality = glassQuality,
                    hazeState = hazeState,
                    onSelect = { index -> navController.navigateToTab(topLevelTabs[index].route) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = PrismDockSpec.HorizontalMargin)
                        .padding(bottom = navBarBottom + PrismDockSpec.BottomGap),
                )
            }
            if (chatActive) {
                PrismChatOverlay(
                    state = chatState,
                    quality = glassQuality,
                    hazeState = hazeState,
                    dockSpace = dockSpace,
                    onInputChange = chatViewModel::onInputChange,
                    onSend = chatViewModel::send,
                    onStop = chatViewModel::stop,
                    onModelSelected = chatViewModel::selectModel,
                    onOpenSettings = { navController.navigateToTab(SettingsRoute) },
                    onInputHeightChanged = { chatInputHeight.value = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // 顶栏/底栏的 insets 由各页面自己的 Scaffold 与 NavigationBar 处理，外层不再重复加。
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDestination
                                ?.hierarchy
                                ?.any { destination -> destination.hasRoute(tab.route::class) }
                                ?: (tab.route == ChatRoute)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigateToTab(tab.route)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(text = stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ChatRoute,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                appDestinations(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    chatViewModel = chatViewModel,
                    historyViewModel = historyViewModel,
                    knowledgeViewModel = knowledgeViewModel,
                )
            }
        }
    }
}

/** 四个一级 Tab 与二级页共用的导航图。 */
private fun NavGraphBuilder.appDestinations(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    chatViewModel: ChatViewModel,
    historyViewModel: HistoryViewModel,
    knowledgeViewModel: KnowledgeViewModel,
) {
    composable<ChatRoute> {
        ChatDestination(
            viewModel = chatViewModel,
            onOpenSettings = { navController.navigateToTab(SettingsRoute) },
        )
    }
    composable<HistoryRoute> {
        HistoryDestination(
            historyViewModel = historyViewModel,
            chatViewModel = chatViewModel,
            onOpenChat = { navController.navigateToTab(ChatRoute) },
            onOpenKnowledge = { navController.navigateToTab(KnowledgeRoute) },
        )
    }
    composable<KnowledgeRoute> {
        KnowledgeDestination(
            viewModel = knowledgeViewModel,
            onOpenEntry = { entryId ->
                navController.navigate(KnowledgeDetailRoute(entryId))
            },
        )
    }
    composable<KnowledgeDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<KnowledgeDetailRoute>()
        val detailViewModel: KnowledgeDetailViewModel = viewModel(
            factory = knowledgeDetailViewModelFactory(route.entryId),
        )
        KnowledgeDetailDestination(
            viewModel = detailViewModel,
            onBack = { navController.popBackStack() },
            onOpenSourceConversation = { conversationId ->
                chatViewModel.openConversation(conversationId)
                navController.popBackStack()
                navController.navigateToTab(ChatRoute)
            },
        )
    }
    composable<SettingsRoute> {
        SettingsDestination(
            viewModel = settingsViewModel,
            onOpenAppearance = { navController.navigate(SettingsAppearanceRoute) },
            onOpenModels = { navController.navigate(SettingsModelsRoute) },
            onOpenPrismPreview = { navController.navigate(SettingsPrismRoute) },
        )
    }
    composable<SettingsAppearanceRoute> {
        AppearanceSettingsDestination(
            viewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
        )
    }
    composable<SettingsModelsRoute> {
        ModelSettingsDestination(
            viewModel = settingsViewModel,
            onBack = { navController.popBackStack() },
        )
    }
    composable<SettingsPrismRoute> {
        val state by settingsViewModel.uiState.collectAsState()
        val isDark = when (state.appearanceMode) {
            AppearanceMode.SYSTEM -> isSystemInDarkTheme()
            AppearanceMode.LIGHT -> false
            AppearanceMode.DARK -> true
        }
        PrismPreviewScreen(
            initialStyle = state.prismColorStyle,
            seedColorArgb = state.prismSeedColor,
            initialDark = isDark,
            glassQuality = state.glassQuality,
            onGlassQualityChange = { quality ->
                settingsViewModel.onEvent(SettingsEvent.GlassQualityChange(quality))
            },
            onBack = { navController.popBackStack() },
        )
    }
}

/** Prism 一级 Tab 转场：轻量 fade-through，不做整屏横向滑动。 */
private val prismTransitionMotion = PrismMotion()

private fun prismSpringInt(): SpringSpec<IntOffset> = spring(
    dampingRatio = prismTransitionMotion.pageSpringDamping,
    stiffness = prismTransitionMotion.pageSpringStiffness,
)

private fun prismSpringFloat(): SpringSpec<Float> = spring(
    dampingRatio = prismTransitionMotion.pageSpringDamping,
    stiffness = prismTransitionMotion.pageSpringStiffness,
)

private fun NavDestination.isTopLevelDestination(): Boolean =
    topLevelTabs.any { tab -> hasRoute(tab.route::class) }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.prismEnter(): EnterTransition {
    val targetTab = targetState.destination.isTopLevelDestination()
    val initialTab = initialState.destination.isTopLevelDestination()
    return when {
        targetTab && initialTab -> fadeIn(animationSpec = tween(200))
        targetTab -> slideInHorizontally(
            animationSpec = prismSpringInt(),
            initialOffsetX = { fullWidth -> -fullWidth * 3 / 10 },
        ) + fadeIn(animationSpec = prismSpringFloat(), initialAlpha = 0.9f)

        else -> slideInHorizontally(
            animationSpec = prismSpringInt(),
            initialOffsetX = { fullWidth -> fullWidth },
        )
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.prismExit(): ExitTransition {
    val targetTab = targetState.destination.isTopLevelDestination()
    val initialTab = initialState.destination.isTopLevelDestination()
    return when {
        targetTab && initialTab -> fadeOut(animationSpec = tween(160))
        else -> slideOutHorizontally(
            animationSpec = prismSpringInt(),
            targetOffsetX = { fullWidth -> -fullWidth * 3 / 10 },
        ) + fadeOut(animationSpec = prismSpringFloat(), targetAlpha = 0.9f)
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.prismPopEnter(): EnterTransition {
    return if (targetState.destination.isTopLevelDestination()) {
        slideInHorizontally(
            animationSpec = prismSpringInt(),
            initialOffsetX = { fullWidth -> -fullWidth * 3 / 10 },
        ) + fadeIn(animationSpec = prismSpringFloat(), initialAlpha = 0.9f)
    } else {
        slideInHorizontally(
            animationSpec = prismSpringInt(),
            initialOffsetX = { fullWidth -> fullWidth },
        )
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.prismPopExit(): ExitTransition {
    return if (initialState.destination.isTopLevelDestination()) {
        fadeOut(animationSpec = tween(160))
    } else {
        slideOutHorizontally(
            animationSpec = prismSpringInt(),
            targetOffsetX = { fullWidth -> fullWidth },
        )
    }
}

@Composable
private fun ChatDestination(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    ChatScreen(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::send,
        onStop = viewModel::stop,
        onRetry = viewModel::retry,
        onErrorShown = viewModel::onErrorShown,
        onOpenSettings = onOpenSettings,
        onModelSelected = viewModel::selectModel,
    )
}

/**
 * 历史记录 Tab：
 * - 点击会话 → ChatViewModel 加载消息，然后切到 AI 对话 Tab；
 * - 新建会话 → 清空当前上下文（不删历史），切到 AI 对话 Tab；
 * - 删除当前会话 → 先通知 ChatViewModel 停流并清空聊天页，再落库删除。
 */
@Composable
private fun HistoryDestination(
    historyViewModel: HistoryViewModel,
    chatViewModel: ChatViewModel,
    onOpenChat: () -> Unit,
    onOpenKnowledge: () -> Unit,
) {
    val state by historyViewModel.uiState.collectAsState()

    HistoryScreen(
        state = state,
        onOpenConversation = { conversationId ->
            chatViewModel.openConversation(conversationId)
            onOpenChat()
        },
        onNewConversation = {
            chatViewModel.startNewConversation()
            onOpenChat()
        },
        onOrganizeConversation = historyViewModel::organize,
        onOrganizeSuccessShown = historyViewModel::onOrganizeSuccessShown,
        onOpenKnowledge = onOpenKnowledge,
        onRequestDelete = historyViewModel::requestDelete,
        onConfirmDelete = {
            state.pendingDelete?.let { target ->
                chatViewModel.onConversationDeleted(target.id)
            }
            historyViewModel.confirmDelete()
        },
        onCancelDelete = historyViewModel::cancelDelete,
        onStartRename = historyViewModel::startRename,
        onRenameInputChange = historyViewModel::onRenameInputChange,
        onConfirmRename = historyViewModel::confirmRename,
        onCancelRename = historyViewModel::cancelRename,
        onErrorShown = historyViewModel::onErrorShown,
    )
}

/**
 * 知识库 Tab。
 *
 * 点击条目 push 知识详情二级页。
 */
@Composable
private fun KnowledgeDestination(
    viewModel: KnowledgeViewModel,
    onOpenEntry: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    KnowledgeScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onOpenEntry = onOpenEntry,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onErrorShown = viewModel::onErrorShown,
    )
}

/** 知识详情二级页；底栏由 AppRoot 按当前 destination 隐藏。 */
@Composable
private fun KnowledgeDetailDestination(
    viewModel: KnowledgeDetailViewModel,
    onBack: () -> Unit,
    onOpenSourceConversation: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    KnowledgeDetailScreen(
        state = state,
        onBack = onBack,
        onOpenSourceConversation = onOpenSourceConversation,
    )
}

@Composable
private fun SettingsDestination(
    viewModel: SettingsViewModel,
    onOpenAppearance: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenPrismPreview: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    SettingsScreen(
        state = state,
        onOpenAppearance = onOpenAppearance,
        onOpenModels = onOpenModels,
        onEvent = viewModel::onEvent,
        onOpenPrismPreview = onOpenPrismPreview,
    )
}

@Composable
private fun AppearanceSettingsDestination(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    AppearanceSettingsScreen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun ModelSettingsDestination(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    // 表单打开时，系统返回先取消表单，而不是退出二级页。
    BackHandler(enabled = state.form != null) {
        viewModel.onEvent(SettingsEvent.CancelForm)
    }

    ModelSettingsScreen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
    )
}

/** 一级 Tab：路由对象 + 文案资源 + Material 图标。 */
private data class TopLevelTab(
    val route: Any,
    val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelTabs: List<TopLevelTab> = listOf(
    TopLevelTab(
        route = ChatRoute,
        labelRes = R.string.tab_chat,
        icon = Icons.AutoMirrored.Filled.Chat,
    ),
    TopLevelTab(route = HistoryRoute, labelRes = R.string.tab_history, icon = Icons.Filled.History),
    TopLevelTab(
        route = KnowledgeRoute,
        labelRes = R.string.tab_knowledge,
        icon = Icons.Filled.AutoStories,
    ),
    TopLevelTab(route = SettingsRoute, labelRes = R.string.tab_settings, icon = Icons.Filled.Settings),
)

/**
 * 切换一级 Tab：只保留起始目的地，保存被切走 Tab 的状态，并在回来时恢复。
 * `launchSingleTop` 避免重复入栈。
 */
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private val chatViewModelFactory = viewModelFactory {
    initializer {
        ChatViewModel(
            repository = AppContainer.createChatRepository(),
            providerRepository = AppContainer.providerRepository,
        )
    }
}

private val settingsViewModelFactory = viewModelFactory {
    initializer {
        SettingsViewModel(
            providerRepository = AppContainer.providerRepository,
            preferences = AppContainer.userPreferencesRepository,
        )
    }
}

private val historyViewModelFactory = viewModelFactory {
    initializer {
        HistoryViewModel(
            conversationRepository = AppContainer.conversationRepository,
            knowledgeOrganizer = AppContainer.knowledgeOrganizer,
        )
    }
}

private val knowledgeViewModelFactory = viewModelFactory {
    initializer { KnowledgeViewModel(AppContainer.knowledgeRepository) }
}

private fun knowledgeDetailViewModelFactory(entryId: Long) = viewModelFactory {
    initializer {
        KnowledgeDetailViewModel(
            repository = AppContainer.knowledgeRepository,
            entryId = entryId,
        )
    }
}
