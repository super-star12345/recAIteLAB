package com.recaitelab.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.AppColorTheme
import com.recaitelab.app.domain.model.AppearanceMode
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import java.util.UUID

/**
 * 设置首页（一级 Tab）：只做二级菜单入口。
 *
 * - 外观 -> [AppearanceSettingsScreen]：外观模式、配色方案、动态取色；
 * - 模型选择 -> [ModelSettingsScreen]：Provider / 模型管理与添加/编辑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenAppearance: () -> Unit,
    onOpenModels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modeLabel = stringResource(state.appearanceMode.labelRes())
    val colorLabel = stringResource(state.colorTheme.labelRes())
    val dynamicLabel = stringResource(R.string.settings_dynamic_color)
    val appearanceSummary = if (state.dynamicColorEnabled && state.isDynamicColorSupported) {
        "$modeLabel · $colorLabel · $dynamicLabel"
    } else {
        "$modeLabel · $colorLabel"
    }

    val noProviderLabel = stringResource(R.string.chat_no_provider)
    val noModelLabel = stringResource(R.string.settings_no_model)
    val activeProvider = state.providers.firstOrNull { it.id == state.activeProviderId }
    val modelsSummary = when {
        activeProvider == null -> noProviderLabel
        activeProvider.selectedModel == null -> "${activeProvider.name} · $noModelLabel"
        else -> "${activeProvider.name} · ${activeProvider.selectedModel}"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.settings_title)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 8.dp),
        ) {
            SettingsMenuRow(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_appearance),
                summary = appearanceSummary,
                onClick = onOpenAppearance,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsMenuRow(
                icon = Icons.Filled.Tune,
                title = stringResource(R.string.settings_models_title),
                summary = modelsSummary,
                onClick = onOpenModels,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    val providerId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    RecAIteLABTheme {
        SettingsScreen(
            state = SettingsUiState(
                providers = listOf(
                    ProviderUiModel(
                        id = providerId,
                        name = "DeepSeek",
                        baseUrl = "https://api.deepseek.com/v1",
                        selectedModel = "deepseek-chat",
                    ),
                ),
                activeProviderId = providerId,
                appearanceMode = AppearanceMode.SYSTEM,
                colorTheme = AppColorTheme.DEFAULT,
                dynamicColorEnabled = false,
                isDynamicColorSupported = true,
            ),
            onOpenAppearance = {},
            onOpenModels = {},
        )
    }
}
