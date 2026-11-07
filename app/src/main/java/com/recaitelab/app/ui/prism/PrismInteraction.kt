package com.recaitelab.app.ui.prism

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Prism 统一按压反馈：按压缩小 + 轻微变淡，松开回弹，关闭系统 ripple。
 *
 * 只修改 `graphicsLayer`（绘制属性），不触发布局；颜色/时长来自 [PrismMotion]。
 * Compose 动画会跟随系统「动画时长缩放」，系统关闭动画时会直接落到终值。
 *
 * @param onLongClick 非空时使用 combinedClickable（历史列表长按删除等）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.prismPressFeedback(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = PrismTheme.motion

    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = spring(
            dampingRatio = motion.pressSpringDamping,
            stiffness = motion.pressSpringStiffness,
        ),
        label = "prismPressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) motion.pressAlpha else 1f,
        animationSpec = spring(
            dampingRatio = motion.pressSpringDamping,
            stiffness = motion.pressSpringStiffness,
        ),
        label = "prismPressAlpha",
    )

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
    }

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .then(clickModifier)
}
