package com.recaitelab.app.ui.prism

import android.os.Build
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.recaitelab.app.domain.model.GlassQuality
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

/** Dock 单个 Tab 的图标与文案，由调用方从路由表映射而来。 */
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
 * 层次自 P3.5 起拆成三层：64dp 玻璃胶囊（底部对齐）、独立指示器层（在玻璃 clip 外）、
 * 64dp 内容层。根容器 80dp 只为拖动 morph 的 76dp 指示器留 headroom，玻璃视觉位置不变。
 *
 * P2 起 ULTRA 走更薄的模糊与更低的 tint，并额外叠加顶部高光与指示器高光边；
 * STANDARD/OFF 的材质仍由 [prismGlassSurface] 的默认参数渲染。
 *
 * P3 起支持：整条 Dock 按压缩放、选中图标放大、横向拖动切换 Tab（跨槽触发一次触觉）。
 *
 * P3.5 起仅 ULTRA 在拖动时把指示器从 56dp morph 到 76dp、宽度最大 1.04×、底色渐隐，
 * 并在 API 33+ 且提供 [backdrop] 时用 Backdrop 的 RuntimeShader lens 做边缘折射/色散；
 * 松手后弹回原位，普通点击仍只有 P3 的按压与图标缩放。
 */
@Composable
fun PrismDock(
    items: List<PrismDockItem>,
    selectedIndex: Int,
    quality: GlassQuality,
    hazeState: HazeState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
) {
    val colors = PrismTheme.colors
    val motion = PrismTheme.motion
    val density = LocalDensity.current
    val isUltra = quality == GlassQuality.ULTRA
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptic = LocalHapticFeedback.current
    var dockWidthPx by remember { mutableStateOf(0f) }
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

    // 拖动 morph 只跟 dragging 走；dockPressed 的 1.04 缩放仍由上面的弹簧独立负责。
    val morphProgress by animateFloatAsState(
        targetValue = if (isUltra && dragging) 1f else 0f,
        animationSpec = spring(
            dampingRatio = motion.dockMorphDamping,
            stiffness = motion.dockMorphStiffness,
        ),
        label = "dockMorphProgress",
    )

    // 槽位按 Dock 实际宽度等分，左右各扣除 Row 的内边距；
    // 不再依赖 Tab 内容 bounds，保证指示器始终落在槽位正中。
    val dockPaddingPx = with(density) { PrismDockSpec.InnerPadding.toPx() }
    val layoutHeightPx = with(density) { PrismDockSpec.LayoutHeight.toPx() }
    val indicatorInsetPx = with(density) { PrismDockSpec.IndicatorInset.toPx() }
    val baseIndicatorHeightPx = with(density) { PrismDockSpec.IndicatorHeight.toPx() }
    val morphHeightPx = with(density) { PrismDockSpec.MorphHeight.toPx() }
    // lens 的折射参数先在这里转成 px，避免在 BackdropEffectScope 里依赖密度。
    val refractionHeightPx = with(density) { 10.dp.toPx() }
    val refractionAmountPx = with(density) { 14.dp.toPx() }

    val slotWidthPx = slotWidth(dockWidthPx, items.size, dockPaddingPx)
    val baseLeft = indicatorLeftPx(dockWidthPx, items.size, dockPaddingPx, selectedIndex, isRtl)
    val dragRange = dragOffsetRange(selectedIndex, items.size, slotWidthPx, isRtl)

    val animatedSlotWidth by animateFloatAsState(
        targetValue = slotWidthPx,
        animationSpec = spring(
            dampingRatio = motion.dockIndicatorDamping,
            stiffness = motion.dockIndicatorStiffness,
        ),
        label = "dockIndicatorWidth",
    )

    val currentIndicatorHeight = morphIndicatorHeight(
        baseHeightPx = baseIndicatorHeightPx,
        morphHeightPx = morphHeightPx,
        progress = morphProgress,
    )
    val currentIndicatorWidth = morphIndicatorWidth(
        slotWidthPx = animatedSlotWidth,
        scale = PrismDockSpec.MorphWidthScale,
        progress = morphProgress,
    )
    val indicatorTop = morphIndicatorTop(
        layoutHeightPx = layoutHeightPx,
        insetPx = indicatorInsetPx,
        indicatorHeightPx = currentIndicatorHeight,
    )
    // 只有 ULTRA 拖动 morph 时才以中心为轴放大并做边缘 clamp；
    // 静止/切槽时保持 P3 的 baseLeft + dragOffset，STANDARD/OFF 的指示器运动完全不变。
    val indicatorTargetLeft = if (isUltra && morphProgress > 0.01f) {
        morphIndicatorLeft(
            baseLeftPx = baseLeft,
            dragOffsetPx = dragOffset,
            slotWidthPx = slotWidthPx,
            indicatorWidthPx = currentIndicatorWidth,
            dockWidthPx = dockWidthPx,
            paddingPx = dockPaddingPx,
        )
    } else {
        baseLeft + dragOffset
    }

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

    val indicatorColor = when (quality) {
        GlassQuality.STANDARD ->
            colors.primaryContainer.copy(alpha = GlassStyle.IndicatorTintAlpha)

        GlassQuality.ULTRA ->
            colors.primaryContainer.copy(alpha = PrismDockSpec.IndicatorTintAlpha)

        GlassQuality.OFF -> colors.primaryContainer
    }
    // morph 时只淡出底色与边框，不整体降 alpha，免得折射边缘一起消失。
    val morphTint = indicatorColor.copy(alpha = indicatorColor.alpha * (1f - morphProgress))
    val borderAlpha = (if (colors.isDark) 0.28f else 0.55f) *
        (0.4f + 0.6f * morphProgress)
    val topHighlightAlpha = if (colors.isDark) 0.12f else 0.20f
    val midHighlightAlpha = if (colors.isDark) 0.03f else 0.04f

    val indicatorShape = RoundedCornerShape(percent = 50)
    val indicatorWidthDp = with(density) { currentIndicatorWidth.toDp() }
    val indicatorHeightDp = with(density) { currentIndicatorHeight.toDp() }

    // Backdrop 只在 ULTRA 拖动中、拿到图层背景且 API 33+ 时启用；
    // 其余情况（含静止态）完全走原来的 Box，保证 STANDARD/OFF 与 P3 一致。
    val activeBackdrop = backdrop
    val showLens = isUltra &&
        activeBackdrop != null &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        morphProgress > 0.01f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PrismDockSpec.LayoutHeight)
            .onSizeChanged { dockWidthPx = it.width.toFloat() }
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
            },
    ) {
        // 1) 玻璃胶囊：64dp，底部对齐，指示器不在其 clip 内。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PrismDockSpec.Height)
                .align(Alignment.BottomCenter)
                .prismGlassSurface(
                    quality = quality,
                    hazeState = hazeState,
                    shape = RoundedCornerShape(percent = 50),
                    // 只有 ULTRA Dock 偏离默认材质；STANDARD/OFF 保持 P1 的默认参数。
                    blurRadius = if (isUltra) PrismDockSpec.BlurRadius else GlassStyle.BlurRadius,
                    tintAlphaLight = if (isUltra) {
                        PrismDockSpec.LightTintAlpha
                    } else {
                        GlassStyle.LightTintAlpha
                    },
                    tintAlphaDark = if (isUltra) {
                        PrismDockSpec.DarkTintAlpha
                    } else {
                        GlassStyle.DarkTintAlpha
                    },
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
        }

        // 2) 指示器层：与玻璃同级、在 clip 外；底边固定，morph 时只向上生长。
        //    首帧宽度为 0 时不渲染，测量完成后再按槽宽落位。
        if (currentIndicatorWidth > 0f && dockWidthPx > 0f) {
            if (showLens && activeBackdrop != null) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = indicatorLeft.roundToInt(),
                                y = indicatorTop.roundToInt(),
                            )
                        }
                        .width(indicatorWidthDp)
                        .height(indicatorHeightDp)
                        .drawBackdrop(
                            backdrop = activeBackdrop,
                            shape = { indicatorShape },
                            effects = {
                                lens(
                                    refractionHeight = refractionHeightPx * morphProgress,
                                    refractionAmount = refractionAmountPx * morphProgress,
                                    depthEffect = true,
                                    chromaticAberration = true,
                                )
                            },
                            highlight = null,
                            shadow = null,
                            innerShadow = null,
                            onDrawSurface = {
                                drawRect(morphTint)
                                drawRect(Color.Black.copy(alpha = 0.03f * morphProgress))
                            },
                            onDrawFront = {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = borderAlpha),
                                    style = Stroke(width = 1.dp.toPx()),
                                    cornerRadius = CornerRadius(size.height / 2f),
                                )
                            },
                        ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = indicatorLeft.roundToInt(),
                                y = indicatorTop.roundToInt(),
                            )
                        }
                        .width(indicatorWidthDp)
                        .height(indicatorHeightDp)
                        .clip(indicatorShape)
                        .background(morphTint)
                        .then(
                            if (isUltra) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = borderAlpha),
                                    shape = indicatorShape,
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }

        // 3) 内容层：最后绘制，保证指示器不会盖住图标与文字。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PrismDockSpec.Height)
                .align(Alignment.BottomCenter)
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
