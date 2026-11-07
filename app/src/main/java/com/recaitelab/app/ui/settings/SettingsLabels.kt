package com.recaitelab.app.ui.settings

import androidx.annotation.StringRes
import com.recaitelab.app.R
import com.recaitelab.app.domain.model.AppColorTheme
import com.recaitelab.app.domain.model.AppDesignSystem
import com.recaitelab.app.domain.model.AppearanceMode
import com.recaitelab.app.domain.model.GlassQuality
import com.recaitelab.app.domain.model.PrismColorStyle

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

/** 设计系统 → 文案资源。 */
@StringRes
internal fun AppDesignSystem.labelRes(): Int = when (this) {
    AppDesignSystem.MATERIAL_3 -> R.string.settings_design_material3
    AppDesignSystem.PRISM_GLASS -> R.string.settings_design_prism
}

/** Prism 配色风格 → 文案资源。 */
@StringRes
internal fun PrismColorStyle.labelRes(): Int = when (this) {
    PrismColorStyle.VIVID -> R.string.settings_prism_style_vivid
    PrismColorStyle.TINTED -> R.string.settings_prism_style_tinted
    PrismColorStyle.SOFT -> R.string.settings_prism_style_soft
    PrismColorStyle.MONOCHROME -> R.string.settings_prism_style_monochrome
}

/** 玻璃质量 → 文案资源。 */
@StringRes
internal fun GlassQuality.labelRes(): Int = when (this) {
    GlassQuality.OFF -> R.string.settings_glass_off
    GlassQuality.STANDARD -> R.string.settings_glass_standard
    GlassQuality.ULTRA -> R.string.settings_glass_ultra
}
