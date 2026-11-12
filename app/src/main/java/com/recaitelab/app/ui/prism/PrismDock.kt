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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.shadow.InnerShadow
import com.recaitelab.app.domain.model.GlassQuality
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

/** Dock 单个 Tab 的图标与文案，由调用方从路由表映射而来。 */
data class PrismDockItem(
    val icon: ImageVector,
    @StringRes val labelRes: Int,
)

/**
 * Prism 玻璃底栏：与内容共用 [HazeState]；选中指示器默认只做 tint。
 *
 * 纯展示组件——不读 NavController / ViewModel，选中态与点击都由调用方决定。
 * 调用方负责外层的对齐与外边距（`align(BottomCenter)` + 左右 20dp + 底部导航栏间距），
 * 本组件只补宽度与固定高度。
 *
 * 层次：64dp 玻璃胶囊（底部对齐）→ 隐藏的 Tab 采样层 → 内容层 → 指示器层（zIndex 2）。
 * 指示器的布局始终是 56dp，P4 起 morph 只通过 graphicsLayer 的 scaleX/scaleY 完成，
 * 因此不会再向上凸出 16dp，而是以 64dp 玻璃中心为轴上下对称放大。
 *
 * P4 的 morph 跟 [dockPressed] 走：按下即可见，不需要拖动；STANDARD/OFF 永远不 morph。
 * 指示器采样三段合成的背景：页面（NavHost）、玻璃胶囊、隐藏的 Tab 行，
 * 因此透明后仍能看到 Tab 图标/文字，并叠加 API 33+ 的 lens 折射。
 */
@Composable
fun PrismDock(
    items: List<PrismDockItem>,
    selectedIndex: Int,
    quality: GlassQuality,
    hazeState: HazeState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pageBackdrop: LayerBackdrop? = null,
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

    // P4：morph 跟按下走，按下即变透明；拖动只负责位移与点击保护。
    val morphProgress by animateFloatAsState(
        targetValue = if (isUltra && dockPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = motion.dockMorphDamping,
            stiffness = motion.dockMorphStiffness,
        ),
        label = "dockMorphProgress",
    )

    // 槽位按 Dock 实际宽度等分，左右各扣除 Row 的内边距；
    // 不再依赖 Tab 内容 bounds，保证指示器始终落在槽位正中。
    val dockPaddingPx = with(density) { PrismDockSpec.InnerPadding.toPx() }
    // 指示器布局高度固定 56dp，顶边 = (64 - 56) / 2 = 4dp。
    val indicatorTopPx = with(density) { PrismDockSpec.IndicatorInset.toPx() }
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

    // 水平位置始终是 baseLeft + dragOffset；morph 的横向放大由 graphicsLayer 以中心为轴完成。
    val indicatorLeft by animateFloatAsState(
        targetValue = baseLeft + dragOffset,
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

    // 56dp 布局 → 76dp 视觉高度：scaleY 只放大绘制，不改变测量。
    val morphScaleX = 1f + (PrismDockSpec.MorphWidthScale - 1f) * morphProgress
    val morphScaleY = 1f +
        ((PrismDockSpec.MorphHeight / PrismDockSpec.IndicatorHeight) - 1f) * morphProgress

    val indicatorShape = RoundedCornerShape(percent = 50)
    val indicatorWidthDp = with(density) { animatedSlotWidth.toDp() }

    // 三段背景：页面 + 玻璃胶囊 + 隐藏的 Tab 行；指示器透明后能透出 Tab 内容。
    val glassBackdrop = rememberLayerBackdrop()
    val tabsBackdrop = rememberLayerBackdrop()
    val pageBackdropOrEmpty = remember(pageBackdrop) {
        pageBackdrop ?: emptyBackdrop()
    }
    val combinedBackdrop = rememberCombinedBackdrop(
        pageBackdropOrEmpty,
        glassBackdrop,
        tabsBackdrop,
    )

    val showLens = isUltra &&
        pageBackdrop != null &&
        isRuntimeShaderSupported() &&
        morphProgress > 0.01f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PrismDockSpec.Height)
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
        // 1) 玻璃胶囊：64dp；layerBackdrop 先记录玻璃绘制，再交给指示器合成。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PrismDockSpec.Height)
                .align(Alignment.BottomCenter)
                .layerBackdrop(glassBackdrop)
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

        // 2) 隐藏的 Tab 采样层：与真实内容同布局，alpha=0 不显示、无交互，
        //    只把图标/文字录进 tabsBackdrop，供指示器 lens 采样。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PrismDockSpec.Height)
                .align(Alignment.BottomCenter)
                .padding(PrismDockSpec.InnerPadding)
                .clearAndSetSemantics { }
                // 诊断用 0.01f：确认隐藏行到底有没有被 lens 采样；验证后应恢复 0f。
                .alpha(0.01f)
                .layerBackdrop(tabsBackdrop),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
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
                                    .graphicsLayer {
                                        val scale = if (selected) {
                                            motion.dockSelectedIconScale
                                        } else {
                                            1f
                                        }
                                        scaleX = scale
                                        scaleY = scale
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

        // 3) 指示器层：布局始终 56dp，morph 只走 graphicsLayer 的中心缩放。
        if (animatedSlotWidth > 0f && dockWidthPx > 0f) {
            if (showLens) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = indicatorLeft.roundToInt(),
                                y = indicatorTopPx.roundToInt(),
                            )
                        }
                        .width(indicatorWidthDp)
                        .height(PrismDockSpec.IndicatorHeight)
                        // 指示器画在真实内容之上，避免 lens 采样与真实 Tab 叠成双重影像。
                        .zIndex(2f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin.Center
                            // 按压时整体上抬 12dp，morph 回落后归位。
                            translationY = -12.dp.toPx() * morphProgress
                            scaleX = morphScaleX
                            scaleY = morphScaleY
                        }
                        .drawBackdrop(
                            backdrop = combinedBackdrop,
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
                            innerShadow = {
                                InnerShadow(
                                    radius = 8.dp * morphProgress,
                                    color = Color.Black,
                                    alpha = 0.25f * morphProgress,
                                )
                            },
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
                                y = indicatorTopPx.roundToInt(),
                            )
                        }
                        .width(indicatorWidthDp)
                        .height(PrismDockSpec.IndicatorHeight)
                        // 指示器画在真实内容之上，避免 lens 采样与真实 Tab 叠成双重影像。
                        .zIndex(2f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin.Center
                            // 按压时整体上抬 12dp，morph 回落后归位。
                            translationY = -12.dp.toPx() * morphProgress
                            scaleX = morphScaleX
                            scaleY = morphScaleY
                        }
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
                        )
                        // API < 33：没有 lens，但保留几何 morph、底色淡出与近似的内阴影。
                        .drawWithContent {
                            drawContent()
                            if (isUltra && morphProgress > 0.01f) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.10f * morphProgress),
                                        0.5f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.06f * morphProgress),
                                    ),
                                    style = Stroke(width = 8.dp.toPx() * morphProgress),
                                    cornerRadius = CornerRadius(size.height / 2f),
                                )
                            }
                        },
                )
            }
        }

        // 4) 内容层：声明在最后，但指示器有 zIndex(2f)，按压时会盖在内容之上。
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

/** API 33 起才有可稳定使用的 RuntimeShader；低版本只走几何/淡出降级。 */
private fun isRuntimeShaderSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
