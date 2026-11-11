package com.recaitelab.app.ui.prism

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.recaitelab.app.domain.model.GlassQuality
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

/** Prism Dock 的单个一级入口。 */
data class PrismDockItem(
    val icon: ImageVector,
    @StringRes val labelRes: Int,
)

/**
 * Prism 玻璃底栏：与内容共用 [HazeState]；选中指示器只做 tint，不单独采样背景。
 *
 * 纯展示组件——不读 NavController / ViewModel，选中态与点击都由调用方决定。
 * 调用方负责外层的对齐与外边距（`align(BottomCenter)` + 左右 20dp + 底部导航栏间距），
 * 本组件只补宽度与固定高度。
 *
 * P2 起 ULTRA 走更薄的模糊（[PrismDockSpec.BlurRadius]）与更低的 tint，并额外叠加顶部高光
 * 与指示器高光边；STANDARD/OFF 的材质仍由 [prismGlassSurface] 的默认参数渲染。
 *
 * P3 起支持：整条 Dock 按压缩放（[PrismMotion.dockContainerPressScale]）、选中图标放大
 * （[PrismMotion.dockSelectedIconScale]）、横向拖动切换 Tab（跨槽触发一次触觉）。
 * 拖动只改指示器 offset 与绘制，不改变 64dp 高度 / 槽位布局。
 */
@Composable
fun PrismDock(
    items: List<PrismDockItem>,
    selectedIndex: Int,
    quality: GlassQuality,
    hazeState: HazeState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PrismTheme.colors
    val motion = PrismTheme.motion
    val density = LocalDensity.current
    val isUltra = quality == GlassQuality.ULTRA
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptic = LocalHapticFeedback.current
    var dockWidthPx by remember { mutableStateOf(0f) }
    var dockHeightPx by remember { mutableStateOf(0f) }
    var dockPressed by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    var dragMoved by remember { mutableStateOf(false) }
    var lastHoveredSlot by remember { mutableStateOf(selectedIndex) }

    // 整条 Dock 的按压缩放走通用 press 弹簧；只改绘制，不影响测量与子项命中。
    val pressSpring = spring<Float>(
        dampingRatio = motion.pressSpringDamping,
        stiffness = motion.pressSpringStiffness,
    )
    val dockScale by animateFloatAsState(
        targetValue = if (dockPressed) motion.dockContainerPressScale else 1f,
        animationSpec = pressSpring,
        label = "dockContainerScale",
    )

    // 槽位按 Dock 实际宽度等分，左右各扣除 Row 的内边距；
    // 不再依赖 Tab 内容 bounds，保证指示器始终落在槽位正中。
    val dockPaddingPx = with(density) { PrismDockSpec.InnerPadding.toPx() }
    val indicatorHeightPx = with(density) { PrismDockSpec.IndicatorHeight.toPx() }
    val slotWidthPx = slotWidth(dockWidthPx, items.size, dockPaddingPx)
    val baseLeft = indicatorLeftPx(dockWidthPx, items.size, dockPaddingPx, selectedIndex, isRtl)
    val indicatorTargetWidth = slotWidthPx
    val rawIndicatorLeft = baseLeft + dragOffset
    val indicatorTargetLeft = if (dockWidthPx > 0f && slotWidthPx > 0f) {
        rawIndicatorLeft.coerceIn(dockPaddingPx, dockWidthPx - dockPaddingPx - slotWidthPx)
    } else {
        baseLeft
    }
    val dragRange = dragOffsetRange(selectedIndex, items.size, slotWidthPx, isRtl)

    val indicatorLeft by animateFloatAsState(
        targetValue = indicatorTargetLeft,
        // 拖动中指示器直接跟手，松手恢复弹簧吸附。
        animationSpec = if (dragging) {
            snap()
        } else {
            spring(
                dampingRatio = motion.dockIndicatorDamping,
                stiffness = motion.dockIndicatorStiffness,
            )
        },
        label = "dockIndicatorLeft",
    )
    val indicatorWidth by animateFloatAsState(
        targetValue = indicatorTargetWidth,
        animationSpec = spring(
            dampingRatio = motion.dockIndicatorDamping,
            stiffness = motion.dockIndicatorStiffness,
        ),
        label = "dockIndicatorWidth",
    )
    val indicatorColor = when (quality) {
        GlassQuality.STANDARD ->
            colors.primaryContainer.copy(alpha = GlassStyle.IndicatorTintAlpha)

        GlassQuality.ULTRA ->
            colors.primaryContainer.copy(alpha = PrismDockSpec.IndicatorTintAlpha)

        GlassQuality.OFF -> colors.primaryContainer
    }
    val indicatorBorderColor = Color.White.copy(
        alpha = if (colors.isDark) 0.28f else 0.55f,
    )
    val topHighlightAlpha = if (colors.isDark) 0.12f else 0.20f
    val midHighlightAlpha = if (colors.isDark) 0.03f else 0.04f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PrismDockSpec.Height)
            .onSizeChanged {
                dockWidthPx = it.width.toFloat()
                dockHeightPx = it.height.toFloat()
            }
            // 只观察按下/抬起，不消费事件；放在 Initial pass 保证先于子项点击拿到手势。
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    dockPressed = true
                    // 新手势开始即解除上一次拖动的点击保护。
                    dragMoved = false
                    try {
                        var anyPressed = true
                        while (anyPressed) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            anyPressed = event.changes.any { it.pressed }
                        }
                    } finally {
                        dockPressed = false
                    }
                }
            }
            .pointerInput(selectedIndex, items.size, dockWidthPx, slotWidthPx, isRtl) {
                if (slotWidthPx <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging = true
                        dragOffset = 0f
                        dragMoved = false
                        lastHoveredSlot = selectedIndex
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragMoved = true
                        dragOffset = (dragOffset + dragAmount).coerceIn(dragRange)
                        val hovered = slotIndexAt(
                            xPx = baseLeft + dragOffset + slotWidthPx / 2f,
                            dockWidthPx = dockWidthPx,
                            count = items.size,
                            paddingPx = dockPaddingPx,
                            rtl = isRtl,
                        )
                        if (hovered != lastHoveredSlot) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHoveredSlot = hovered
                        }
                    },
                    onDragEnd = {
                        val target = slotIndexAt(
                            xPx = baseLeft + dragOffset + slotWidthPx / 2f,
                            dockWidthPx = dockWidthPx,
                            count = items.size,
                            paddingPx = dockPaddingPx,
                            rtl = isRtl,
                        )
                        dragging = false
                        dragOffset = 0f
                        // 只在松手时切一次 Tab；拖动过程不触发导航。
                        if (target != selectedIndex) onSelect(target)
                    },
                    onDragCancel = {
                        dragging = false
                        dragOffset = 0f
                    },
                )
            }
            // 整条 Dock 按压放大：包住玻璃层与内容，仅视觉效果。
            .graphicsLayer {
                scaleX = dockScale
                scaleY = dockScale
            }
            .prismGlassSurface(
                quality = quality,
                hazeState = hazeState,
                shape = RoundedCornerShape(percent = 50),
                // 只有 ULTRA Dock 偏离默认材质；STANDARD/OFF 保持 P1 的默认参数。
                blurRadius = if (isUltra) PrismDockSpec.BlurRadius else GlassStyle.BlurRadius,
                tintAlphaLight = if (isUltra) PrismDockSpec.LightTintAlpha else GlassStyle.LightTintAlpha,
                tintAlphaDark = if (isUltra) PrismDockSpec.DarkTintAlpha else GlassStyle.DarkTintAlpha,
                glassInUltra = isUltra,
            ),
    ) {
        // 顶部高光：仅 ULTRA；位于玻璃层之上、内容之下，只是绘制，不参与命中测试。
        if (isUltra) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.White.copy(alpha = topHighlightAlpha),
                                0.45f to Color.White.copy(alpha = midHighlightAlpha),
                                1f to Color.Transparent,
                            ),
                        )
                    },
            )
        }

        // 首帧 dockWidth/dockHeight 仍为 0，指示器不渲染；测量完成后再按槽宽落位。
        if (indicatorWidth > 0f && dockHeightPx > 0f) {
            val indicatorShape = RoundedCornerShape(percent = 50)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = indicatorLeft.roundToInt(),
                            y = ((dockHeightPx - indicatorHeightPx) / 2f).roundToInt(),
                        )
                    }
                    .width(with(density) { indicatorWidth.toDp() })
                    .height(PrismDockSpec.IndicatorHeight)
                    .clip(indicatorShape)
                    .background(indicatorColor)
                    .then(
                        if (isUltra) {
                            Modifier.border(1.dp, indicatorBorderColor, indicatorShape)
                        } else {
                            Modifier
                        },
                    ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(PrismDockSpec.InnerPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) motion.dockSelectedIconScale else 1f,
                    animationSpec = pressSpring,
                    label = "dockIconScale$index",
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .prismPressFeedback(
                            emphasized = true,
                            // 拖动中与拖动刚结束都不触发点击，避免松手后多切一次。
                            onClick = { if (!selected && !dragging && !dragMoved) onSelect(index) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(
                            modifier = Modifier.height(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(PrismDockSpec.IconSize)
                                    // 只缩放绘制，24dp 尺寸与 32dp 图标盒保持不变。
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                tint = if (selected) colors.primary else colors.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
