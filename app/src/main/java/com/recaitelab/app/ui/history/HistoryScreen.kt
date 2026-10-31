package com.recaitelab.app.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.recaitelab.app.R
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史记录 Tab（无状态）：
 * 会话列表、新建、点击打开、长按删除、溢出菜单重命名。
 *
 * 所有数据与操作通过 [HistoryUiState] 与回调上抛，不直接访问 Repository。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onOpenConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onOrganizeConversation: (HistoryConversationUiModel) -> Unit,
    onOrganizeSuccessShown: () -> Unit,
    onOpenKnowledge: () -> Unit,
    onRequestDelete: (HistoryConversationUiModel) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onStartRename: (HistoryConversationUiModel) -> Unit,
    onRenameInputChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
    onCancelRename: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val organizeSuccessMessage = stringResource(R.string.knowledge_organize_success)
    val organizeViewAction = stringResource(R.string.knowledge_organize_view)

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onErrorShown()
    }

    LaunchedEffect(state.organizeSuccessEntryId) {
        if (state.organizeSuccessEntryId == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = organizeSuccessMessage,
            actionLabel = organizeViewAction,
            withDismissAction = false,
            duration = SnackbarDuration.Short,
        )
        onOrganizeSuccessShown()
        if (result == SnackbarResult.ActionPerformed) {
            onOpenKnowledge()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.tab_history)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewConversation) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.history_new_conversation),
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.conversations.isEmpty() -> {
                    EmptyHistoryState(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    HistoryList(
                        conversations = state.conversations,
                        onOpenConversation = onOpenConversation,
                        onOrganizeConversation = onOrganizeConversation,
                        onRequestDelete = onRequestDelete,
                        onStartRename = onStartRename,
                    )
                }
            }
        }
    }

    state.pendingDelete?.let { target ->
        DeleteConversationDialog(
            title = target.title,
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete,
        )
    }

    if (state.renameTarget != null) {
        RenameConversationDialog(
            value = state.renameInput,
            error = state.renameError,
            onValueChange = onRenameInputChange,
            onConfirm = onConfirmRename,
            onDismiss = onCancelRename,
        )
    }

    if (state.isOrganizing) {
        OrganizingDialog()
    }
}

@Composable
private fun HistoryList(
    conversations: List<HistoryConversationUiModel>,
    onOpenConversation: (Long) -> Unit,
    onOrganizeConversation: (HistoryConversationUiModel) -> Unit,
    onRequestDelete: (HistoryConversationUiModel) -> Unit,
    onStartRename: (HistoryConversationUiModel) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // 给 FAB 留出滚动空间，避免最后一条被遮挡。
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        items(items = conversations, key = { it.id }) { conversation ->
            HistoryRow(
                conversation = conversation,
                onOpen = { onOpenConversation(conversation.id) },
                onOrganize = { onOrganizeConversation(conversation) },
                onDelete = { onRequestDelete(conversation) },
                onRename = { onStartRename(conversation) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    conversation: HistoryConversationUiModel,
    onOpen: () -> Unit,
    onOrganize: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onDelete,
            )
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatUpdatedAt(conversation.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.preview.ifBlank {
                    stringResource(R.string.history_no_message)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.history_more),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.knowledge_organize)) },
                    onClick = {
                        menuExpanded = false
                        onOrganize()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.history_rename)) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.history_delete)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun OrganizingDialog() {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = { Text(text = stringResource(R.string.knowledge_organizing_title)) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
                Text(text = stringResource(R.string.knowledge_organizing_message))
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.history_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeleteConversationDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.history_delete_title)) },
        text = { Text(text = stringResource(R.string.history_delete_message, title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.settings_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_cancel))
            }
        },
    )
}

@Composable
private fun RenameConversationDialog(
    value: String,
    error: HistoryRenameError?,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val errorText = when (error) {
        HistoryRenameError.EMPTY -> stringResource(R.string.history_rename_error_empty)
        HistoryRenameError.TOO_LONG -> stringResource(R.string.history_rename_error_too_long)
        null -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.history_rename_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.history_rename_hint)) },
                singleLine = true,
                isError = errorText != null,
                supportingText = if (errorText != null) {
                    { Text(text = errorText) }
                } else {
                    null
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_cancel))
            }
        },
    )
}

/** 相对时间：1 分钟内“刚刚”，之后分钟/小时/昨天，超过两天显示日期。 */
@Composable
private fun formatUpdatedAt(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> stringResource(R.string.history_just_now)
        diff < 3_600_000L -> stringResource(
            R.string.history_minutes_ago,
            (diff / 60_000L).toInt(),
        )

        diff < 86_400_000L -> stringResource(
            R.string.history_hours_ago,
            (diff / 3_600_000L).toInt(),
        )

        diff < 172_800_000L -> stringResource(R.string.history_yesterday)
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    val now = System.currentTimeMillis()
    RecAIteLABTheme {
        HistoryScreen(
            state = HistoryUiState(
                isLoading = false,
                conversations = listOf(
                    HistoryConversationUiModel(
                        id = 1,
                        title = "SSE 流式解析",
                        preview = "逐行读取 data:，处理 [DONE] 与 finish_reason。",
                        updatedAt = now - 5 * 60_000L,
                    ),
                    HistoryConversationUiModel(
                        id = 2,
                        title = "Room 迁移",
                        preview = "Provider 配置迁入 Room，并迁移加密 Key。",
                        updatedAt = now - 3 * 3_600_000L,
                    ),
                ),
            ),
            onOpenConversation = {},
            onNewConversation = {},
            onOrganizeConversation = {},
            onOrganizeSuccessShown = {},
            onOpenKnowledge = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            onStartRename = {},
            onRenameInputChange = {},
            onConfirmRename = {},
            onCancelRename = {},
            onErrorShown = {},
        )
    }
}
