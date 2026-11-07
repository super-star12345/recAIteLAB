package com.recaitelab.app.ui.prism

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.recaitelab.app.domain.model.DEFAULT_PRISM_SEED_ARGB
import com.recaitelab.app.domain.model.PrismColorStyle

/**
 * Prism Glass 设计系统入口。
 *
 * 只负责把颜色 / 形状 / 动效 token 放进 CompositionLocal，不替换 `MaterialTheme`：
 * 正式页面继续用 Material 3，Prism 组件通过 [PrismTheme.colors] 等读取。
 * 这样 T1 的静态预览不会影响聊天、历史、知识库页面。
 */
object PrismTheme {

    val colors: PrismColors
        @Composable @ReadOnlyComposable get() = LocalPrismColors.current

    val shapes: PrismShapes
        @Composable @ReadOnlyComposable get() = LocalPrismShapes.current

    val motion: PrismMotion
        @Composable @ReadOnlyComposable get() = LocalPrismMotion.current

    @Composable
    operator fun invoke(
        style: PrismColorStyle = PrismColorStyle.SOFT,
        seedColor: Color = Color(DEFAULT_PRISM_SEED_ARGB),
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
    ) {
        val colors = remember(style, seedColor, darkTheme) {
            PrismColors.from(seedColor, style, darkTheme)
        }
        val shapes = remember { PrismShapes() }
        val motion = remember { PrismMotion() }

        CompositionLocalProvider(
            LocalPrismColors provides colors,
            LocalPrismShapes provides shapes,
            LocalPrismMotion provides motion,
            content = content,
        )
    }
}

val LocalPrismColors = staticCompositionLocalOf {
    PrismColors.from(Color(DEFAULT_PRISM_SEED_ARGB), PrismColorStyle.SOFT, dark = false)
}
