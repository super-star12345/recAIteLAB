package com.recaitelab.app.ui.settings

import androidx.annotation.StringRes
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.AppColorTheme
import com.recaitelab.app.domain.model.AppearanceMode

/** 外观模式 → 文案资源；设置首页与外观二级页共用。 */
@StringRes
internal fun AppearanceMode.labelRes(): Int = when (this) {
    AppearanceMode.SYSTEM -> R.string.settings_appearance_system
    AppearanceMode.LIGHT -> R.string.settings_appearance_light
    AppearanceMode.DARK -> R.string.settings_appearance_dark
}

/** 配色方案 → 文案资源；设置首页与外观二级页共用。 */
@StringRes
internal fun AppColorTheme.labelRes(): Int = when (this) {
    AppColorTheme.DEFAULT -> R.string.settings_color_default
    AppColorTheme.GREEN -> R.string.settings_color_green
    AppColorTheme.BLUE -> R.string.settings_color_blue
}
