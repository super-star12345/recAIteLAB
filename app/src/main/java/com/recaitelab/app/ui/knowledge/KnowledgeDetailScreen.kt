package com.recaitelab.app.ui.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.KnowledgeEntry
import com.recaitelab.app.ui.common.MarkdownText
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 知识详情二级页：标题、标签、时间、来源模型、Markdown 正文、来源会话入口。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgeDetailScreen(
    state: KnowledgeDetailUiState,
    onBack: () -> Unit,
    onOpenSourceConversation: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.knowledge_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.knowledge_detail_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.entry != null -> {
                KnowledgeDetailContent(
                    entry = state.entry,
                    onOpenSourceConversation = onOpenSourceConversation,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (state.isNotFound) {
                                R.string.knowledge_detail_missing
                            } else {
                                R.string.knowledge_detail_load_error
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KnowledgeDetailContent(
    entry: KnowledgeEntry,
    onOpenSourceConversation: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (entry.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                entry.tags.forEach { tag -> DetailTagChip(text = tag) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(
                    R.string.knowledge_detail_time,
                    formatDateTime(entry.updatedAt),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.knowledge_detail_model,
                    entry.model?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.knowledge_detail_model_unknown),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        MarkdownText(
            markdown = entry.content,
            modifier = Modifier.fillMaxWidth(),
        )

        entry.sourceConversationId?.let { conversationId ->
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { onOpenSourceConversation(conversationId) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.knowledge_detail_source))
            }
        }
    }
}

@Composable
private fun DetailTagChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

@Preview(showBackground = true)
@Composable
private fun KnowledgeDetailScreenPreview() {
    val now = System.currentTimeMillis()
    RecAIteLABTheme {
        KnowledgeDetailScreen(
            state = KnowledgeDetailUiState(
                isLoading = false,
                entry = KnowledgeEntry(
                    id = 1,
                    title = "SSE 流式解析",
                    summary = "逐行读取 data:，处理 [DONE] 与 finish_reason。",
                    content = """
                        ## 关键结论

                        SSE 需要逐行读取，**空行**分隔事件。

                        ```kotlin
                        val line = source.readUtf8Line()
                        ```
                    """.trimIndent(),
                    tags = listOf("Kotlin", "SSE"),
                    sourceConversationId = 2,
                    providerId = "provider",
                    model = "deepseek-chat",
                    createdAt = now - 3_600_000L,
                    updatedAt = now - 60_000L,
                ),
            ),
            onBack = {},
            onOpenSourceConversation = {},
        )
    }
}
