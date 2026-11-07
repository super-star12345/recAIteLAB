package com.recaitelab.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.ui.common.MarkdownText
import com.recaitelab.app.ui.common.MarkdownTextStyle
import com.recaitelab.app.ui.common.message
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * 单页聊天界面（无状态）：只接收 [ChatUiState] 与事件回调。
 *
 * 业务逻辑在 [ChatViewModel]；这里只做布局与事件上抛。
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
    var showModelPicker by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val retryLabel = stringResource(R.string.action_retry)
    val error = state.error
    val errorMessage = when (error) {
        null -> null
        else -> error.message()
    }

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
        modifier = modifier
            .fillMaxSize(),
        // 状态栏由 TopAppBar 处理；底部安全区由外层 NavigationBar 处理。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 消息区占用剩余高度；输入区是 Column 最后一个固定子项，始终在底栏上方。
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

    if (showModelPicker) {
        ModalBottomSheet(
            onDismissRequest = { showModelPicker = false },
            sheetState = sheetState,
        ) {
            ModelPickerSheet(
                providers = state.providers,
                activeProviderId = state.activeProviderId,
                activeModel = state.activeModel,
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // reverseLayout 下索引 0 是最新消息；key 用原始索引，追加消息时已有 key 不变。
        items(
            count = messages.size,
            key = { listIndex -> messages.lastIndex - listIndex },
        ) { listIndex ->
            MessageBubble(message = messages[messages.lastIndex - listIndex])
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
    onSelect: (UUID, String) -> Unit,
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasModels = providers.any { it.models.isNotEmpty() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.chat_model_picker_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        if (!hasModels) {
            EmptyModelPicker(onGoSettings = onGoSettings)
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
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    if (provider.models.isEmpty()) {
                        item(key = "empty-${provider.id}") {
                            Text(
                                text = stringResource(R.string.chat_model_picker_no_models),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onGoSettings) {
            Text(text = stringResource(R.string.chat_model_picker_go_settings))
        }
    }
}

@Composable
private fun ModelRow(
    model: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
