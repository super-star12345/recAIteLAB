package com.recaitelab.app.ui.prism

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Prism 统一按压反馈：按压缩小 + 轻微变淡，松开回弹，关闭系统 ripple。
 *
 * 使用显式 [Animatable] + 弹簧，而不是 tween/透明度替代：
 * - 松开时允许一次轻微过冲，再回到 1f；
 * - 目标变化时从当前值和当前速度继续，不排队、不抖動。
 *
 * 只修改 `graphicsLayer`（绘制属性），不触发布局；颜色/时长来自 [PrismMotion]。
 * Compose 动画会跟随系统「动画时长缩放」，系统关闭动画时会直接落到终值。
 *
 * @param emphasized Dock Tab 使用更深的缩放和更明显的回弹；其它组件保持默认。
 * @param onLongClick 非空时使用 combinedClickable（历史列表长按删除等）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.prismPressFeedback(
    enabled: Boolean = true,
    emphasized: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = PrismTheme.motion

    val targetScale = if (emphasized) motion.dockPressScale else motion.pressScale
    val damping = if (emphasized) motion.dockPressSpringDamping else motion.pressSpringDamping
    val stiffness = if (emphasized) {
        motion.dockPressSpringStiffness
    } else {
        motion.pressSpringStiffness
    }
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(pressed, emphasized) {
        val spec = spring<Float>(
            dampingRatio = damping,
            stiffness = stiffness,
        )
        launch {
            scale.animateTo(
                targetValue = if (pressed) targetScale else 1f,
                animationSpec = spec,
                initialVelocity = scale.velocity,
            )
        }
        launch {
            alpha.animateTo(
                targetValue = if (pressed) motion.pressAlpha else 1f,
                animationSpec = spec,
                initialVelocity = alpha.velocity,
            )
        }
    }

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
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        }
        .then(clickModifier)
}
