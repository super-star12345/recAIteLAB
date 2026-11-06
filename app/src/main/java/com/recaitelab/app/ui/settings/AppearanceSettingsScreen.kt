package com.recaitelab.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.AppColorTheme
import com.recaitelab.app.domain.model.AppearanceMode
import com.recaitelab.app.ui.theme.RecAIteLABTheme

/**
 * 外观设置二级页（无状态）：外观模式、配色方案、动态取色。
 *
 * 入口：设置 -> 外观。事件通过 [SettingsEvent] 上抛，主题变化由 MainActivity 立即应用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onEvent(SettingsEvent.ErrorShown)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_appearance)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppearanceSection(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun AppearanceSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_appearance_mode),
            style = MaterialTheme.typography.titleSmall,
        )
        AppearanceMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = state.appearanceMode == mode,
                        role = Role.RadioButton,
                        onClick = { onEvent(SettingsEvent.AppearanceModeChange(mode)) },
                    )
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = state.appearanceMode == mode,
                    onClick = null,
                )
                Text(
                    text = stringResource(mode.labelRes()),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Text(
            text = stringResource(R.string.settings_color_scheme),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppColorTheme.entries.forEach { theme ->
                FilterChip(
                    selected = state.colorTheme == theme,
                    onClick = { onEvent(SettingsEvent.ColorThemeChange(theme)) },
                    label = { Text(text = stringResource(theme.labelRes())) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_dynamic_color),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(
                        if (state.isDynamicColorSupported) {
                            R.string.settings_dynamic_color_hint
                        } else {
                            R.string.settings_dynamic_color_unsupported
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.dynamicColorEnabled && state.isDynamicColorSupported,
                onCheckedChange = { onEvent(SettingsEvent.DynamicColorChange(it)) },
                enabled = state.isDynamicColorSupported,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppearanceSettingsScreenPreview() {
    RecAIteLABTheme {
        AppearanceSettingsScreen(
            state = SettingsUiState(isDynamicColorSupported = true),
            onBack = {},
            onEvent = {},
        )
    }
}
