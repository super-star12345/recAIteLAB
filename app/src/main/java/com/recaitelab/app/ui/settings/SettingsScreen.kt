package com.recaitelab.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recaitelab.app.R
import com.recaitelab.app.ui.theme.RecAIteLABTheme
import java.util.UUID

/**
 * 设置页（无状态）：Provider 列表 + 添加/编辑表单（含模型管理）+ 删除确认。
 *
 * 事件统一通过 [SettingsEvent] 上抛；页面本身不接触 Repository，也不展示 apiKey。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val form = state.form

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onEvent(SettingsEvent.ErrorShown)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            if (form == null) {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.settings_back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEvent(SettingsEvent.AddProvider) }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.settings_add_provider),
                            )
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                if (form.isNew) R.string.settings_add_provider
                                else R.string.settings_edit_provider,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(SettingsEvent.CancelForm) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.settings_back),
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (form != null) {
            ProviderForm(
                form = form,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            ProviderList(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    state.pendingDelete?.let { provider ->
        DeleteProviderDialog(
            provider = provider,
            onConfirm = { onEvent(SettingsEvent.ConfirmDelete) },
            onDismiss = { onEvent(SettingsEvent.CancelDelete) },
        )
    }
}

@Composable
private fun ProviderList(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.providers.isEmpty()) {
        EmptyProviders(
            onAdd = { onEvent(SettingsEvent.AddProvider) },
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = state.providers, key = { it.id }) { provider ->
            ProviderCard(
                provider = provider,
                isActive = provider.id == state.activeProviderId,
                onSetActive = { onEvent(SettingsEvent.SetActive(provider)) },
                onEdit = { onEvent(SettingsEvent.EditProvider(provider)) },
                onDelete = { onEvent(SettingsEvent.DeleteProvider(provider)) },
            )
        }
    }
}

@Composable
private fun ProviderCard(
    provider: ProviderUiModel,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val model = provider.selectedModel ?: stringResource(R.string.settings_no_model)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isActive,
                onClick = onSetActive,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = provider.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.settings_current_model, model),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.settings_edit),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.settings_delete_provider),
                )
            }
        }
    }
}

@Composable
private fun EmptyProviders(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Filled.Cloud,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.settings_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAdd) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Text(
                text = stringResource(R.string.settings_add_provider),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ProviderForm(
    form: ProviderFormState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameError = form.nameError
    val baseUrlError = form.baseUrlError
    val apiKeyError = form.apiKeyError
    val modelFetchError = form.modelFetchError
    val newModelError = form.newModelError
    val inputEnabled = !form.isSaving
    val modelActionsEnabled = !form.isSaving && !form.isFetchingModels

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { onEvent(SettingsEvent.NameChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.settings_name)) },
                singleLine = true,
                enabled = inputEnabled,
                isError = nameError != null,
                supportingText = if (nameError != null) {
                    { Text(text = nameError) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = form.baseUrl,
                onValueChange = { onEvent(SettingsEvent.BaseUrlChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.settings_base_url)) },
                placeholder = { Text(text = "https://...") },
                singleLine = true,
                enabled = inputEnabled,
                isError = baseUrlError != null,
                supportingText = if (baseUrlError != null) {
                    { Text(text = baseUrlError) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = form.apiKey,
                onValueChange = { onEvent(SettingsEvent.ApiKeyChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.settings_api_key)) },
                placeholder = {
                    Text(
                        text = stringResource(
                            if (form.isNew) R.string.settings_api_key_new_hint
                            else R.string.settings_api_key_keep_hint,
                        ),
                    )
                },
                singleLine = true,
                enabled = inputEnabled,
                isError = apiKeyError != null,
                supportingText = if (apiKeyError != null) {
                    { Text(text = apiKeyError) }
                } else {
                    null
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onEvent(SettingsEvent.Save) }),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.settings_type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.settings_type_openai),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Text(
                text = stringResource(R.string.settings_models),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(
                onClick = { onEvent(SettingsEvent.FetchModels) },
                modifier = Modifier.fillMaxWidth(),
                enabled = modelActionsEnabled,
            ) {
                if (form.isFetchingModels) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.settings_fetching_models))
                } else {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.settings_fetch_models))
                }
            }
            if (modelFetchError != null) {
                Text(
                    text = modelFetchError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (form.models.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_models),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                form.models.forEach { model ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = model == form.selectedModel,
                            onClick = { onEvent(SettingsEvent.SelectModel(model)) },
                            enabled = modelActionsEnabled,
                        )
                        Text(
                            text = model,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        IconButton(
                            onClick = { onEvent(SettingsEvent.RemoveModel(model)) },
                            enabled = modelActionsEnabled,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.settings_remove_model),
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = form.newModelName,
                    onValueChange = { onEvent(SettingsEvent.NewModelNameChange(it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = stringResource(R.string.settings_model_name)) },
                    singleLine = true,
                    enabled = inputEnabled,
                    isError = newModelError != null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onEvent(SettingsEvent.AddModel) },
                    enabled = form.newModelName.isNotBlank() && modelActionsEnabled,
                ) {
                    Text(text = stringResource(R.string.settings_add_model))
                }
            }
            if (newModelError != null) {
                Text(
                    text = newModelError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Button(
            onClick = { onEvent(SettingsEvent.Save) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !form.isSaving && !form.isFetchingModels,
        ) {
            Text(text = stringResource(R.string.settings_save))
        }
    }
}

@Composable
private fun DeleteProviderDialog(
    provider: ProviderUiModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_delete_title)) },
        text = { Text(text = stringResource(R.string.settings_delete_message, provider.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.settings_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    val deepSeekId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val openAiId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    RecAIteLABTheme {
        SettingsScreen(
            state = SettingsUiState(
                providers = listOf(
                    ProviderUiModel(
                        id = deepSeekId,
                        name = "DeepSeek",
                        baseUrl = "https://api.deepseek.com/v1",
                        selectedModel = "deepseek-chat",
                    ),
                    ProviderUiModel(
                        id = openAiId,
                        name = "OpenAI",
                        baseUrl = "https://api.openai.com/v1",
                        selectedModel = null,
                    ),
                ),
                activeProviderId = deepSeekId,
            ),
            onBack = {},
            onEvent = {},
        )
    }
}
