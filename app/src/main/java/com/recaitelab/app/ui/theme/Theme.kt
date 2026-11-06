package com.recaitelab.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.recaitelab.app.domain.model.AppColorTheme
import com.recaitelab.app.domain.model.AppearanceMode

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

private fun AppColorTheme.schemeFor(darkTheme: Boolean): ColorScheme = when (this) {
    AppColorTheme.DEFAULT -> if (darkTheme) DarkColorScheme else LightColorScheme
    AppColorTheme.GREEN -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
    AppColorTheme.BLUE -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
}
