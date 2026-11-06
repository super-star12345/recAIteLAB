package com.recaitelab.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.UserError

/** [UserError] 对应的用户可见文案；所有错误提示统一从这里取。 */
@StringRes
fun UserError.messageRes(): Int = when (this) {
    UserError.INVALID_API_KEY -> R.string.error_invalid_api_key
    UserError.RATE_LIMITED -> R.string.error_rate_limited
    UserError.NETWORK -> R.string.error_network
    UserError.CONFIGURATION -> R.string.error_configuration
    UserError.NOT_FOUND -> R.string.error_conversation_not_found
    UserError.UNKNOWN -> R.string.error_unknown
}

/** Compose 中直接取错误文案。 */
@Composable
fun UserError.message(): String = stringResource(messageRes())
