package com.recaitelab.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.AppDesignSystem
import com.recaitelab.app.domain.model.GlassQuality
import com.recaitelab.app.ui.common.MarkdownText
import com.recaitelab.app.ui.common.MarkdownTextStyle
import com.recaitelab.app.ui.common.message
import com.recaitelab.app.ui.prism.LocalGlassQuality
import com.recaitelab.app.ui.prism.PrismButton
import com.recaitelab.app.ui.prism.PrismButtonStyle
import com.recaitelab.app.ui.prism.PrismEmptyState
import com.recaitelab.app.ui.prism.PrismTheme
import com.recaitelab.app.ui.prism.prismGlassSurface
import com.recaitelab.app.ui.theme.LocalAppDesignSystem
import com.recaitelab.app.ui.theme.LocalPrismDockSpace
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * 单页聊天界面（无状态）：只接收 [ChatUiState] 与事件回调。
 *
 * PRISM_GLASS：顶栏为覆盖在消息列表上的玻璃模型选择栏，模型 Sheet 为同窗口玻璃面板；
 * MATERIAL_3：保持原 TopAppBar + ModalBottomSheet。
 * 两种模式的流式、停止、重试、模型切换与会话恢复逻辑完全共用 [ChatViewModel]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onInputChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit,
    onOpenSettings: () -> Unit,
    onModelSelected: (UUID, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val isPrism = LocalAppDesignSystem.current == AppDesignSystem.PRISM_GLASS
    val glassQuality = LocalGlassQuality.current
    var showModelPicker by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val retryLabel = stringResource(R.string.action_retry)
    val error = state.error
    val errorMessage = when (error) {
        null -> null
        else -> error.message()
    }

    val hazeEnabled = isPrism && glassQuality == GlassQuality.STANDARD
    val chatHazeState = rememberHazeState(blurEnabled = hazeEnabled)
    val topBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val dockSpace = LocalPrismDockSpace.current
    val density = LocalDensity.current
    var prismInputHeight by remember { mutableStateOf(0.dp) }

    LaunchedEffect(error, state.canRetry) {
        errorMessage ?: return@LaunchedEffect
        val canRetry = state.canRetry
        val result = snackbarHostState.showSnackbar(
            message = errorMessage,
            actionLabel = if (canRetry) retryLabel else null,
            withDismissAction = canRetry,
            duration = if (canRetry) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        // 先清掉本次错误，再发起重试，避免重试立即失败时的错误被这里误清。
        onErrorShown()
        if (result == SnackbarResult.ActionPerformed) {
            onRetry()
        }
    }

    val providerName = state.activeProviderName
    val modelName = state.activeModel
    val modelLabel = when {
        providerName == null -> stringResource(R.string.chat_no_provider)
        modelName == null -> "$providerName / ${stringResource(R.string.chat_no_model)}"
        else -> "$providerName / $modelName"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isPrism) {
            PrismTheme.colors.background
        } else {
            MaterialTheme.colorScheme.background
        },
        // 状态栏由顶栏处理；底部安全区由外层导航处理。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isPrism) {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable(
                                    onClickLabel = stringResource(R.string.chat_model_picker_title),
                                    onClick = { showModelPicker = true },
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = modelLabel,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
            // Prism 顶栏由消息区覆盖层绘制，这里不再占布局高度。
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (isPrism) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // 消息内容铺满整屏并作为 Haze 源；顶栏、输入区、Sheet 都浮在它上面。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(chatHazeState),
                ) {
                    when {
                        state.isLoadingHistory -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = PrismTheme.colors.primary,
                            )
                        }

                        state.messages.isEmpty() -> {
                            PrismEmptyState(
                                icon = Icons.Filled.AutoAwesome,
                                title = stringResource(R.string.chat_empty_message),
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }

                        else -> {
                            MessageList(
                                messages = state.messages,
                                isPrism = true,
                                topPadding = topBarTopInset + 72.dp,
                                bottomPadding = prismInputHeight + dockSpace + 16.dp,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                PrismChatTopBar(
                    modelLabel = modelLabel,
                    onOpenPicker = { showModelPicker = true },
                    quality = glassQuality,
                    hazeState = chatHazeState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp)
                        .padding(top = topBarTopInset + 8.dp),
                )

                PrismChatInputBar(
                    inputText = state.inputText,
                    isGenerating = state.isGenerating,
                    isLoadingHistory = state.isLoadingHistory,
                    onInputChange = onInputChange,
                    onSend = { onSend(state.inputText) },
                    onStop = onStop,
                    quality = glassQuality,
                    hazeState = chatHazeState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = dockSpace)
                        // IME insets 仅在此处消费一次，与 Material 分支保持同一约束。
                        .imePadding()
                        .onSizeChanged { size ->
                            prismInputHeight = with(density) { size.height.toDp() }
                        },
                )

                if (showModelPicker) {
                    PrismPickerOverlay(
                        providers = state.providers,
                        activeProviderId = state.activeProviderId,
                        activeModel = state.activeModel,
                        quality = glassQuality,
                        hazeState = chatHazeState,
                        dockSpace = dockSpace,
                        onSelect = { providerId, model ->
                            onModelSelected(providerId, model)
                            showModelPicker = false
                        },
                        onGoSettings = {
                            showModelPicker = false
                            onOpenSettings()
                        },
                        onDismiss = { showModelPicker = false },
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when {
                        state.isLoadingHistory -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        state.messages.isEmpty() -> {
                            EmptyState(modifier = Modifier.align(Alignment.Center))
                        }

                        else -> {
                            MessageList(
                                messages = state.messages,
                                isPrism = false,
                                topPadding = 12.dp,
                                bottomPadding = 12.dp,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                ChatInputBar(
                    inputText = state.inputText,
                    isGenerating = state.isGenerating,
                    isLoadingHistory = state.isLoadingHistory,
                    onInputChange = onInputChange,
                    onSend = { onSend(state.inputText) },
                    onStop = onStop,
                    modifier = Modifier.imePadding(),
                )
            }
        }
    }

    if (!isPrism && showModelPicker) {
        ModalBottomSheet(
            onDismissRequest = { showModelPicker = false },
            sheetState = sheetState,
        ) {
            ModelPickerSheet(
                providers = state.providers,
                activeProviderId = state.activeProviderId,
                activeModel = state.activeModel,
                isPrism = false,
                onSelect = { providerId, model ->
                    onModelSelected(providerId, model)
                    scope.launch {
                        sheetState.hide()
                        showModelPicker = false
                    }
                },
                onGoSettings = {
                    showModelPicker = false
                    onOpenSettings()
                },
            )
        }
    }
}

/** Prism 模型选择：同窗口玻璃面板，可采样对话页内容；点击遮罩/返回关闭。 */
@Composable
private fun PrismPickerOverlay(
    providers: List<ProviderModels>,
    activeProviderId: UUID?,
    activeModel: String?,
    quality: GlassQuality,
    hazeState: HazeState,
    dockSpace: Dp,
    onSelect: (UUID, String) -> Unit,
    onGoSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val colors = PrismTheme.colors
    val scrimInteraction = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
                .padding(bottom = dockSpace + 8.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
                .prismGlassSurface(
                    quality = quality,
                    hazeState = hazeState,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .background(colors.outlineVariant, PrismTheme.shapes.pill),
                )
                ModelPickerSheet(
                    providers = providers,
                    activeProviderId = activeProviderId,
                    activeModel = activeModel,
                    isPrism = true,
                    onSelect = onSelect,
                    onGoSettings = onGoSettings,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.chat_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MessageList(
    messages: List<UiMessage>,
    isPrism: Boolean,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        reverseLayout = true,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // reverseLayout 下索引 0 是最新消息；key 用原始索引，追加消息时已有 key 不变。
        items(
            count = messages.size,
            key = { listIndex -> messages.lastIndex - listIndex },
        ) { listIndex ->
            val message = messages[messages.lastIndex - listIndex]
            if (isPrism) {
                PrismChatBubble(message = message)
            } else {
                MessageBubble(message = message)
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: UiMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == UiMessage.ROLE_USER
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            when {
                message.content.isEmpty() && message.isStreaming -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }

                // 用户消息保持纯文本；助手流式期间也用纯文本，避免每个增量都重新解析 Markdown。
                isUser || message.isStreaming -> {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }

                // 助手回复完成后（含历史消息、停止后的半截内容）一次性切 Markdown 渲染。
                else -> {
                    MarkdownText(
                        markdown = message.content,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MarkdownTextStyle.CHAT,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isGenerating: Boolean,
    isLoadingHistory: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                enabled = !isGenerating && !isLoadingHistory,
                placeholder = { Text(text = stringResource(R.string.chat_input_hint)) },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
            )
            FilledIconButton(
                onClick = { if (isGenerating) onStop() else onSend() },
                enabled = !isLoadingHistory && (isGenerating || inputText.isNotBlank()),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (isGenerating) {
                        Icons.Filled.Stop
                    } else {
                        Icons.AutoMirrored.Filled.Send
                    },
                    contentDescription = stringResource(
                        if (isGenerating) R.string.chat_stop else R.string.chat_send,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ModelPickerSheet(
    providers: List<ProviderModels>,
    activeProviderId: UUID?,
    activeModel: String?,
    isPrism: Boolean,
    onSelect: (UUID, String) -> Unit,
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasModels = providers.any { it.models.isNotEmpty() }
    val titleColor = if (isPrism) {
        PrismTheme.colors.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val primaryColor = if (isPrism) {
        PrismTheme.colors.primary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val mutedColor = if (isPrism) {
        PrismTheme.colors.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.chat_model_picker_title),
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        if (!hasModels) {
            EmptyModelPicker(isPrism = isPrism, onGoSettings = onGoSettings)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                providers.forEach { provider ->
                    item(key = "provider-${provider.id}") {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryColor,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    if (provider.models.isEmpty()) {
                        item(key = "empty-${provider.id}") {
                            Text(
                                text = stringResource(R.string.chat_model_picker_no_models),
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        items(
                            items = provider.models,
                            key = { model -> "${provider.id}-$model" },
                        ) { model ->
                            ModelRow(
                                model = model,
                                selected = provider.id == activeProviderId && model == activeModel,
                                isPrism = isPrism,
                                onClick = { onSelect(provider.id, model) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyModelPicker(
    isPrism: Boolean,
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mutedColor = if (isPrism) {
        PrismTheme.colors.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.chat_model_picker_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = mutedColor,
            textAlign = TextAlign.Center,
        )
        if (isPrism) {
            PrismButton(
                text = stringResource(R.string.chat_model_picker_go_settings),
                onClick = onGoSettings,
                style = PrismButtonStyle.TONAL,
            )
        } else {
            TextButton(onClick = onGoSettings) {
                Text(text = stringResource(R.string.chat_model_picker_go_settings))
            }
        }
    }
}

@Composable
private fun ModelRow(
    model: String,
    selected: Boolean,
    isPrism: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isPrism) {
        PrismModelRow(
            model = model,
            selected = selected,
            onClick = onClick,
            modifier = modifier,
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = model,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.chat_model_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    val providerId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    RecAIteLABTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    UiMessage(
                        role = UiMessage.ROLE_USER,
                        content = "用一句话解释 SSE 是什么？",
                    ),
                    UiMessage(
                        role = UiMessage.ROLE_ASSISTANT,
                        content = "SSE 是服务器通过一条长连接向客户端单向推送文本事件的协议。",
                        isStreaming = true,
                    ),
                    UiMessage(
                        role = UiMessage.ROLE_ASSISTANT,
                        content = """
                            ### 示例代码

                            ```kotlin
                            fun main() = println("hello")
                            ```
                        """.trimIndent(),
                    ),
                ),
                isGenerating = true,
                providers = listOf(
                    ProviderModels(
                        id = providerId,
                        name = "DeepSeek",
                        models = listOf("deepseek-chat", "deepseek-reasoner"),
                    ),
                ),
                activeProviderId = providerId,
                activeProviderName = "DeepSeek",
                activeModel = "deepseek-chat",
            ),
            onInputChange = {},
            onSend = {},
            onStop = {},
            onRetry = {},
            onErrorShown = {},
            onOpenSettings = {},
            onModelSelected = { _, _ -> },
        )
    }
}
