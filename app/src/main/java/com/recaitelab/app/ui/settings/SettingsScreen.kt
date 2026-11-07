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
import androidx.compose.foundation.layout.height
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
import com.recaitelab.app.ui.prism.PrismCard
import com.recaitelab.app.ui.prism.PrismChip
import com.recaitelab.app.ui.prism.PrismListRow
import com.recaitelab.app.ui.prism.PrismSeedPresets
import com.recaitelab.app.ui.prism.PrismTheme
import com.recaitelab.app.ui.prism.PrismTopBar
import com.recaitelab.app.ui.prism.prismPressFeedback
import com.recaitelab.app.ui.theme.LocalAppDesignSystem
import com.recaitelab.app.ui.theme.LocalPrismDockSpace
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import java.util.UUID

/**
 * 设置首页（一级 Tab）：外观、界面风格、Prism 选项与模型管理入口。
 *
 * PRISM_GLASS 模式使用 Prism 组件（PrismCard / PrismListRow / PrismChip）；
 * MATERIAL_3 模式保持原 Material 3 布局。两种模式共用同一 state 与事件回调。
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
    val isPrism = LocalAppDesignSystem.current == AppDesignSystem.PRISM_GLASS
    val dockSpace = LocalPrismDockSpace.current

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
        containerColor = if (isPrism) {
            PrismTheme.colors.background
        } else {
            MaterialTheme.colorScheme.background
        },
        topBar = {
            if (isPrism) {
                PrismTopBar(title = stringResource(R.string.settings_title))
            } else {
                TopAppBar(title = { Text(text = stringResource(R.string.settings_title)) })
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            if (isPrism) {
                PrismSettingsContent(
                    state = state,
                    appearanceSummary = appearanceSummary,
                    modelsSummary = modelsSummary,
                    onOpenAppearance = onOpenAppearance,
                    onOpenModels = onOpenModels,
                    onEvent = onEvent,
                    onOpenPrismPreview = onOpenPrismPreview,
                )
            } else {
                MaterialSettingsContent(
                    state = state,
                    appearanceSummary = appearanceSummary,
                    modelsSummary = modelsSummary,
                    onOpenAppearance = onOpenAppearance,
                    onOpenModels = onOpenModels,
                    onEvent = onEvent,
                    onOpenPrismPreview = onOpenPrismPreview,
                )
            }

            // 末尾留白让内容可以滚过悬浮 Dock 后方，同时最后一项不会被 Dock 遮挡。
            if (isPrism) {
                Spacer(modifier = Modifier.height(dockSpace + 8.dp))
            }
        }
    }
}

@Composable
private fun MaterialSettingsContent(
    state: SettingsUiState,
    appearanceSummary: String,
    modelsSummary: String,
    onOpenAppearance: () -> Unit,
    onOpenModels: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
    onOpenPrismPreview: () -> Unit,
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PreviewEntryRow(onClick = onOpenPrismPreview)
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

@Composable
private fun PrismSettingsContent(
    state: SettingsUiState,
    appearanceSummary: String,
    modelsSummary: String,
    onOpenAppearance: () -> Unit,
    onOpenModels: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
    onOpenPrismPreview: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PrismListRow(
            icon = Icons.Filled.Palette,
            title = stringResource(R.string.settings_appearance),
            summary = appearanceSummary,
            onClick = onOpenAppearance,
        )

        PrismCard {
            DesignSystemSection(state = state, onEvent = onEvent)
            if (state.designSystem == AppDesignSystem.PRISM_GLASS) {
                HorizontalDivider(
                    color = PrismTheme.colors.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                PrismOptionsSection(state = state, onEvent = onEvent)
                HorizontalDivider(
                    color = PrismTheme.colors.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                PreviewEntryRow(onClick = onOpenPrismPreview)
            }
        }

        PrismListRow(
            icon = Icons.Filled.Tune,
            title = stringResource(R.string.settings_models_title),
            summary = modelsSummary,
            onClick = onOpenModels,
        )
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
            color = if (isPrismMode()) PrismTheme.colors.onSurface else MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppDesignSystem.entries.forEach { system ->
                SettingsChoiceChip(
                    label = stringResource(system.labelRes()),
                    selected = state.designSystem == system,
                    onClick = { onEvent(SettingsEvent.DesignSystemChange(system)) },
                )
            }
        }
        if (state.designSystem == AppDesignSystem.PRISM_GLASS) {
            Text(
                text = stringResource(R.string.settings_design_hint),
                style = MaterialTheme.typography.bodySmall,
                color = if (isPrismMode()) {
                    PrismTheme.colors.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
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
    val isPrism = isPrismMode()
    val titleColor = if (isPrism) PrismTheme.colors.onSurface else MaterialTheme.colorScheme.onSurface
    val bodyColor = if (isPrism) {
        PrismTheme.colors.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_prism_style),
            style = MaterialTheme.typography.titleSmall,
            color = titleColor,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrismColorStyle.entries.forEach { style ->
                SettingsChoiceChip(
                    label = stringResource(style.labelRes()),
                    selected = state.prismColorStyle == style,
                    onClick = { onEvent(SettingsEvent.PrismStyleChange(style)) },
                )
            }
        }

        Text(
            text = stringResource(R.string.settings_prism_seed),
            style = MaterialTheme.typography.titleSmall,
            color = titleColor,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
            color = titleColor,
            modifier = Modifier.padding(top = 4.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassQuality.entries.forEach { quality ->
                SettingsChoiceChip(
                    label = stringResource(quality.labelRes()),
                    selected = state.glassQuality == quality,
                    onClick = { onEvent(SettingsEvent.GlassQualityChange(quality)) },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_prism_glass_hint),
            style = MaterialTheme.typography.bodySmall,
            color = bodyColor,
        )
    }
}

/** Prism 模式下用 PrismChip，Material 模式保留 FilterChip。 */
@Composable
private fun SettingsChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (isPrismMode()) {
        PrismChip(label = label, selected = selected, onClick = onClick)
    } else {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(text = label) },
        )
    }
}

@Composable
private fun SeedSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val isPrism = isPrismMode()
    val borderColor = when {
        selected && isPrism -> PrismTheme.colors.onSurface
        selected -> MaterialTheme.colorScheme.onSurface
        isPrism -> PrismTheme.colors.outlineVariant
        else -> MaterialTheme.colorScheme.outlineVariant
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

/** 预览入口：Prism 模式为无边框行（已在外层 PrismCard 内），Material 模式保留旧样式。 */
@Composable
private fun PreviewEntryRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isPrismMode()) {
        val colors = PrismTheme.colors
        Row(
            modifier = modifier
                .fillMaxWidth()
                .prismPressFeedback(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.BlurOn,
                contentDescription = null,
                tint = colors.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_prism_preview),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_prism_preview_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
    } else {
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
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isPrismMode()) {
        PrismListRow(
            icon = icon,
            title = title,
            summary = summary,
            onClick = onClick,
            modifier = modifier,
        )
        return
    }

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

@Composable
private fun isPrismMode(): Boolean =
    LocalAppDesignSystem.current == AppDesignSystem.PRISM_GLASS

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
            ),
            onOpenAppearance = {},
            onOpenModels = {},
            onEvent = {},
            onOpenPrismPreview = {},
        )
    }
}

@Preview(showBackground = true, name = "Prism Glass")
@Composable
private fun SettingsScreenPrismPreview() {
    val providerId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    RecAIteLABTheme(designSystem = AppDesignSystem.PRISM_GLASS) {
        SettingsScreen(
            state = SettingsUiState(
                providers = listOf(
                    ProviderUiModel(
                        id = providerId,
                        name = "DeepSeek",
                        baseUrl = "https://api.deepseek.com/v1",
                        selectedModel = "deepseek-flash",
                    ),
                ),
                activeProviderId = providerId,
                appearanceMode = AppearanceMode.SYSTEM,
                colorTheme = AppColorTheme.DEFAULT,
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
