package com.recaitelab.app.ui.prism

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.DEFAULT_PRISM_SEED_ARGB
import com.recaitelab.app.domain.model.PrismColorStyle
import com.recaitelab.app.ui.theme.RecAIteLABTheme

/**
 * Prism 转场演示的「详情页」。
 *
 * 纯假数据，不接 ViewModel / Repository；由 [PrismPreviewScreen] 以覆盖层方式推入，
 * 用于观察右侧进入、前页联动与暗化。返回由调用方负责反向动画。
 */
@Composable
fun PrismDemoDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background,
        contentColor = colors.onBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .prismPressFeedback(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.prism_preview_back),
                        tint = colors.onBackground,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.prism_preview_detail_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapes.card,
                    color = colors.surface,
                    contentColor = colors.onSurface,
                    border = BorderStroke(1.dp, colors.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.prism_preview_detail_card),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.prism_preview_detail_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                DemoRow(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = stringResource(R.string.tab_chat),
                )
                DemoRow(
                    icon = Icons.Filled.History,
                    title = stringResource(R.string.tab_history),
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .prismPressFeedback(onClick = onBack),
                    shape = shapes.pill,
                    color = colors.primary,
                    contentColor = colors.onPrimary,
                ) {
                    Text(
                        text = stringResource(R.string.prism_preview_detail_back),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }

                Text(
                    text = stringResource(R.string.prism_preview_detail_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DemoRow(
    icon: ImageVector,
    title: String,
) {
    val colors = PrismTheme.colors
    val shapes = PrismTheme.shapes

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .prismPressFeedback(onClick = {}),
        shape = shapes.listRow,
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrismDemoDetailScreenPreview() {
    RecAIteLABTheme {
        PrismTheme(style = PrismColorStyle.SOFT, seedColor = androidx.compose.ui.graphics.Color(DEFAULT_PRISM_SEED_ARGB)) {
            PrismDemoDetailScreen(onBack = {})
        }
    }
}
