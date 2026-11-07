package com.recaitelab.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.recaitelab.app.domain.model.AppColorTheme
import com.recaitelab.app.domain.model.AppDesignSystem
import com.recaitelab.app.domain.model.AppearanceMode
import com.recaitelab.app.domain.model.DEFAULT_PRISM_SEED_ARGB
import com.recaitelab.app.domain.model.GlassQuality
import com.recaitelab.app.domain.model.PrismColorStyle
import com.recaitelab.app.ui.prism.LocalGlassQuality
import com.recaitelab.app.ui.prism.PrismTheme

/** 当前设计系统；页面用它做视觉组件分流，不重复业务逻辑。 */
val LocalAppDesignSystem = staticCompositionLocalOf { AppDesignSystem.MATERIAL_3 }

/** Prism 悬浮 Dock 占用的底部空间（含系统导航栏与间距）；Material 模式恒为 0。 */
val LocalPrismDockSpace = staticCompositionLocalOf { 0.dp }

/**
 * 应用主题：外观模式（跟随系统 / 浅色 / 深色）+ 配色方案（默认 / 护眼绿 / 商务蓝）。
 *
 * 开启动态取色且系统为 Android 12+ 时，使用系统壁纸配色；低版本自动忽略，回退到所选配色。
 * 偏好由调用方（MainActivity）从 DataStore 收集后传入，变化时立即重组生效。
 */
@Composable
fun RecAIteLABTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    colorTheme: AppColorTheme = AppColorTheme.DEFAULT,
    dynamicColorEnabled: Boolean = false,
    designSystem: AppDesignSystem = AppDesignSystem.MATERIAL_3,
    prismColorStyle: PrismColorStyle = PrismColorStyle.SOFT,
    prismSeedColor: Long = DEFAULT_PRISM_SEED_ARGB,
    prismGlassQuality: GlassQuality = GlassQuality.STANDARD,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appearanceMode) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = if (dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        colorTheme.schemeFor(darkTheme)
    }

    // 系统栏图标明暗跟随主题，避免浅色主题下白底配白图标（状态栏几乎不可见）。
    val view = LocalView.current
    val window = if (view.isInEditMode) null else view.context.findActivity()?.window
    SideEffect {
        window?.applySystemBarIconAppearance(darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
    ) {
        CompositionLocalProvider(
            LocalAppDesignSystem provides designSystem,
            LocalGlassQuality provides prismGlassQuality,
        ) {
            if (designSystem == AppDesignSystem.PRISM_GLASS) {
                // 只注入 Prism token，不替换 MaterialTheme：正式页面按需读取 Prism 组件。
                PrismTheme(
                    style = prismColorStyle,
                    seedColor = Color(prismSeedColor.toInt()),
                    darkTheme = darkTheme,
                    content = content,
                )
            } else {
                content()
            }
        }
    }
}

/**
 * 设置系统栏图标的明暗（状态栏 + 导航栏）。
 *
 * - 浅色主题：深色图标（`APPEARANCE_LIGHT_*` / `LIGHT_*` flag）；
 * - 深色主题：浅色图标。
 *
 * 只使用平台 API，避免为这一处新增依赖。API 30+ 用 `WindowInsetsController`，
 * 低版本用已废弃但兼容的 `systemUiVisibility`，并保留其它 flag。
 */
@Suppress("DEPRECATION")
private fun Window.applySystemBarIconAppearance(darkTheme: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = insetsController ?: return
        val lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        controller.setSystemBarsAppearance(if (darkTheme) 0 else lightBars, lightBars)
    } else {
        val lightBars = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val current = decorView.systemUiVisibility
        decorView.systemUiVisibility = if (darkTheme) {
            current and lightBars.inv()
        } else {
            current or lightBars
        }
    }
}

/** ComposeView 的 context 可能被主题包装过，逐层解包到 Activity。 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun AppColorTheme.schemeFor(darkTheme: Boolean): ColorScheme = when (this) {
    AppColorTheme.DEFAULT -> if (darkTheme) DarkColorScheme else LightColorScheme
    AppColorTheme.GREEN -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
    AppColorTheme.BLUE -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
}
