package com.recaitelab.app.ui.prism

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.recaitelab.app.domain.model.GlassQuality
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlinx.coroutines.launch

/** PrismSnapSheet 的三个吸附状态；实际高度由 [SnapSheetSpec] 与内容高度提示共同决定。 */
enum class SnapSheetState { CLOSED, HALF, EXPANDED }

/** 吸附、回弹与尺寸参数集中在这里，真机调参不散落到页面。 */
object SnapSheetSpec {
    /** 可用高度的 45% / 85%；内容较矮时会被 contentHeightHint 截断。 */
    const val HalfFraction = 0.45f
    const val ExpandedFraction = 0.85f

    /** 向下拖动超过该比例（相对展开高度）直接关闭。 */
    const val CloseFraction = 0.25f

    /** 释放后按速度投影再吸附；投影系数越小越“跟手”。 */
    const val VelocityProjectionSeconds = 0.12f

    /** 离手速度阈值（px/s）：超过则按方向吸附到端点。 */
    const val FlingVelocity = 1400f

    const val SpringDamping = 0.88f
    const val SpringStiffness = 520f

    val CornerRadius = 28.dp
    val HandleHeight = 28.dp
    val TitleHeight = 44.dp
    val HorizontalMargin = 20.dp
    val ShadowElevation = 12.dp
}

/**
 * 可复用的 Prism 吸附面板（CLOSED / HALF / EXPANDED）。
 *
 * - 从悬浮 Dock 上方展开：由调用方在 modifier 上预留底部 Dock 空间；
 * - 拖动把手下拉/上推，释放后按高度与速度弹簧吸附，不会停在任意半高；
 * - 列表滚到顶部后继续下拉时，未消费的滚动会转成收起手势；
 * - 系统返回键、遮罩点击都会关闭；关闭动画结束后才真正移除视觉。
 *
 * 本步使用普通 Prism 表面（surface + 描边 + 阴影），玻璃在下一步统一接入。
 */
@Composable
fun PrismSnapSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    quality: GlassQuality,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    contentHeightHint: Dp? = null,
    onStateChange: (SnapSheetState) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colors = PrismTheme.colors
    val density = LocalDensity.current
    val height = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val sheetInteraction = remember { MutableInteractionSource() }
    var state by remember { mutableStateOf(SnapSheetState.CLOSED) }

    val handleAndTitlePx = with(density) {
        (SnapSheetSpec.HandleHeight + SnapSheetSpec.TitleHeight).toPx()
    }
    val hintSheetPx = contentHeightHint?.let { with(density) { it.toPx() } + handleAndTitlePx }

    BackHandler(enabled = visible) { onDismiss() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeight = constraints.maxHeight.toFloat()
        val halfAnchor = minOf(maxHeight * SnapSheetSpec.HalfFraction, hintSheetPx ?: Float.MAX_VALUE)
        val expandedAnchor = minOf(
            maxHeight * SnapSheetSpec.ExpandedFraction,
            hintSheetPx ?: Float.MAX_VALUE,
        )
        val anchors = listOf(0f, halfAnchor, expandedAnchor).filter { it > 0f }.distinct().sorted()

        fun currentState(): SnapSheetState = when {
            height.value <= 0f -> SnapSheetState.CLOSED
            height.value >= expandedAnchor - 1f -> SnapSheetState.EXPANDED
            else -> SnapSheetState.HALF
        }

        fun settle(velocity: Float) {
            if (!visible) return
            val projected = (height.value - velocity * SnapSheetSpec.VelocityProjectionSeconds)
                .coerceIn(0f, expandedAnchor)
            val target = when {
                projected <= expandedAnchor * SnapSheetSpec.CloseFraction -> 0f
                velocity > SnapSheetSpec.FlingVelocity -> 0f
                velocity < -SnapSheetSpec.FlingVelocity -> expandedAnchor
                else -> anchors.minByOrNull { abs(it - projected) } ?: halfAnchor
            }
            if (target <= 0f) {
                onDismiss()
                return
            }
            scope.launch {
                height.animateTo(target, snapSpring())
                state = currentState()
                onStateChange(state)
            }
        }

        val dragState = rememberDraggableState { delta ->
            scope.launch {
                val next = (height.value - delta).coerceIn(0f, expandedAnchor)
                height.snapTo(next)
            }
        }

        val progress = if (expandedAnchor > 0f) {
            (height.value / expandedAnchor).coerceIn(0f, 1f)
        } else {
            0f
        }

        var overscrollActive by remember { mutableStateOf(false) }
        var overscrollVelocity by remember { mutableStateOf(0f) }

        val nestedScroll = object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // 列表到顶后继续下拉：把未消费的向下滚动转成收起手势。
                if (available.y > 0f && height.value > 0f) {
                    overscrollActive = true
                    scope.launch {
                        height.snapTo((height.value - available.y).coerceAtLeast(0f))
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // 不拦截列表 fling，只在 gesture 结束时吸附 Sheet。
                if (overscrollActive) overscrollVelocity = available.y
                return Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                if (overscrollActive) {
                    overscrollActive = false
                    settle(overscrollVelocity)
                }
                return Velocity.Zero
            }
        }

        if (visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f * progress))
                    .clickable(onClick = onDismiss),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = SnapSheetSpec.HorizontalMargin)
                .fillMaxWidth()
                .height(with(density) { height.value.toDp() })
                .prismGlassSurface(
                    quality = quality,
                    hazeState = hazeState,
                    shape = PrismCornerShape(SnapSheetSpec.CornerRadius),
                    shadowElevation = SnapSheetSpec.ShadowElevation,
                )
                // 吞掉面板空白处的点击，避免穿透到遮罩直接关闭；拖动仍由头部区域处理。
                .clickable(
                    interactionSource = sheetInteraction,
                    indication = null,
                ) {}
                .nestedScroll(nestedScroll),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = dragState,
                        onDragStopped = { velocity -> settle(velocity) },
                    )
                    .padding(horizontal = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(colors.outlineVariant, PrismTheme.shapes.pill),
                )
                Spacer(modifier = Modifier.height(SnapSheetSpec.HandleHeight - 14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.height(SnapSheetSpec.TitleHeight),
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }

        LaunchedEffect(visible) {
            if (visible) {
                height.animateTo(halfAnchor, snapSpring())
                state = currentState()
                onStateChange(state)
            } else {
                height.animateTo(0f, snapSpring())
                state = SnapSheetState.CLOSED
                onStateChange(state)
            }
        }
    }
}

private fun snapSpring() = spring<Float>(
    dampingRatio = SnapSheetSpec.SpringDamping,
    stiffness = SnapSheetSpec.SpringStiffness,
)
