package com.recaitelab.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.AppColorTheme
import com.recaitelab.app.domain.model.AppDesignSystem
import com.recaitelab.app.domain.model.AppearanceMode
import com.recaitelab.app.domain.model.GlassQuality
import com.recaitelab.app.domain.model.PrismColorStyle
import com.recaitelab.app.ui.prism.PrismSeedPresets
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import java.util.UUID

/**
 * 设置首页（一级 Tab）：外观、界面风格、Prism 选项与模型管理入口。
 *
 * - 外观 -> [AppearanceSettingsScreen]：外观模式、配色方案、动态取色；
 * - 界面风格：Material 3 / Prism Glass；选 Prism 后展开配色风格、种子色、玻璃质量；
 * - Prism 主题预览 -> 静态样式预览（T1，不接业务数据）；
 * - 模型选择 -> [ModelSettingsScreen]：Provider / 模型管理与添加/编辑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenAppearance: () -> Unit,
    onOpenModels: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
    onOpenPrismPreview: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            SettingsMenuRow(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_appearance),
                summary = appearanceSummary,
                onClick = onOpenAppearance,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DesignSystemSection(state = state, onEvent = onEvent)

            if (state.designSystem == AppDesignSystem.PRISM_GLASS) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PrismOptionsSection(state = state, onEvent = onEvent)
                PrismPreviewRow(onClick = onOpenPrismPreview)
            }

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

/** 界面风格：Material 3 / Prism Glass。 */
@Composable
private fun DesignSystemSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_design_system),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppDesignSystem.entries.forEach { system ->
                FilterChip(
                    selected = state.designSystem == system,
                    onClick = { onEvent(SettingsEvent.DesignSystemChange(system)) },
                    label = { Text(text = stringResource(system.labelRes())) },
                )
            }
        }
        if (state.designSystem == AppDesignSystem.PRISM_GLASS) {
            Text(
                text = stringResource(R.string.settings_design_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 选择 Prism 后展开：配色风格、种子色、玻璃质量。 */
@Composable
private fun PrismOptionsSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_prism_style),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PrismColorStyle.entries.forEach { style ->
                FilterChip(
                    selected = state.prismColorStyle == style,
                    onClick = { onEvent(SettingsEvent.PrismStyleChange(style)) },
                    label = { Text(text = stringResource(style.labelRes())) },
                )
            }
        }

        Text(
            text = stringResource(R.string.settings_prism_seed),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrismSeedPresets.forEach { seed ->
                val argb = seed.toArgbLong()
                SeedSwatch(
                    color = seed,
                    selected = state.prismSeedColor == argb,
                    onClick = { onEvent(SettingsEvent.PrismSeedChange(argb)) },
                )
            }
        }

        Text(
            text = stringResource(R.string.settings_prism_glass_quality),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassQuality.entries.forEach { quality ->
                FilterChip(
                    selected = state.glassQuality == quality,
                    onClick = { onEvent(SettingsEvent.GlassQualityChange(quality)) },
                    label = { Text(text = stringResource(quality.labelRes())) },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_prism_glass_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SeedSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    // 外层保证 48dp 触控区域，内层是 32dp 的色块。
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(if (selected) 3.dp else 1.dp, borderColor, CircleShape),
        )
    }
}

@Composable
private fun PrismPreviewRow(
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
            imageVector = Icons.Filled.BlurOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_prism_preview),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_prism_preview_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

/** 把 Compose Color 转成 DataStore 用的无符号 ARGB Long。 */
internal fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

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
                designSystem = AppDesignSystem.PRISM_GLASS,
                prismColorStyle = PrismColorStyle.SOFT,
            ),
            onOpenAppearance = {},
            onOpenModels = {},
            onEvent = {},
            onOpenPrismPreview = {},
        )
    }
}
