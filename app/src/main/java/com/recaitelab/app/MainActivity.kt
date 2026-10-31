package com.recaitelab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recaitelab.app.di.AppContainer
import com.recaitelab.app.ui.chat.ChatScreen
import com.recaitelab.app.ui.chat.ChatViewModel
import com.recaitelab.app.ui.settings.SettingsEvent
import com.recaitelab.app.ui.settings.SettingsScreen
import com.recaitelab.app.ui.settings.SettingsViewModel
import com.recaitelab.app.ui.theme.RecAIteLABTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecAIteLABTheme {
                AppRoot()
            }
        }
    }
}

/**
 * 阶段内的简单页面切换：聊天 / 设置。
 *
 * 不引入导航库；下阶段统一迁移到 Navigation Compose 时替换这里即可。
 */
@Composable
private fun AppRoot() {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // 先注册外层返回，设置页内部的表单返回会因后注册而优先处理。
    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsRoute(onBack = { showSettings = false })
    } else {
        ChatRoute(onOpenSettings = { showSettings = true })
    }
}

@Composable
private fun ChatRoute(
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = viewModel(factory = chatViewModelFactory),
) {
    val state by viewModel.uiState.collectAsState()
    ChatScreen(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::send,
        onStop = viewModel::stop,
        onOpenSettings = onOpenSettings,
        onModelSelected = viewModel::selectModel,
    )
}

@Composable
private fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory),
) {
    val state by viewModel.uiState.collectAsState()

    // 表单打开时，系统返回先取消表单，而不是直接退回聊天页。
    BackHandler(enabled = state.form != null) {
        viewModel.onEvent(SettingsEvent.CancelForm)
    }

    SettingsScreen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
    )
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
    initializer { SettingsViewModel(AppContainer.providerRepository) }
}
