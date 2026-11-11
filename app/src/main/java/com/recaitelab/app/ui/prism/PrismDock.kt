package com.recaitelab.app.ui.prism

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
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
 * 本组件只补 `fillMaxWidth()`。
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
    var dockWidthPx by remember { mutableStateOf(0f) }
    var dockHeightPx by remember { mutableStateOf(0f) }

    // 槽位按 Dock 实际宽度等分，左右各扣除 Row 的内边距；
    // 不再依赖 Tab 内容 bounds，保证指示器始终落在槽位正中。
    val dockPaddingPx = with(density) { PrismDockSpec.InnerPadding.toPx() }
    val slotWidthPx = if (dockWidthPx > 0f && items.isNotEmpty()) {
        (dockWidthPx - dockPaddingPx * 2f) / items.size
    } else {
        0f
    }
    val indicatorTargetWidth = slotWidthPx
    val indicatorTargetLeft = dockPaddingPx + selectedIndex * slotWidthPx

    val indicatorLeft by animateFloatAsState(
        targetValue = indicatorTargetLeft,
        animationSpec = spring(
            dampingRatio = motion.dockIndicatorDamping,
            stiffness = motion.dockIndicatorStiffness,
        ),
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
    val indicatorColor = if (quality == GlassQuality.STANDARD) {
        colors.primaryContainer.copy(alpha = GlassStyle.IndicatorTintAlpha)
    } else {
        colors.primaryContainer
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged {
                dockWidthPx = it.width.toFloat()
                dockHeightPx = it.height.toFloat()
            }
            .prismGlassSurface(
                quality = quality,
                hazeState = hazeState,
                shape = RoundedCornerShape(percent = 50),
            ),
    ) {
        // 首帧 dockWidth/dockHeight 仍为 0，指示器不渲染；测量完成后从宽度 0 弹入。
        if (indicatorWidth > 0f && dockHeightPx > 0f) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = indicatorLeft.roundToInt(),
                            y = 0,
                        )
                    }
                    .width(with(density) { indicatorWidth.toDp() })
                    // 全高但仍使用底栏同语言胶囊圆角，避免整块方框。
                    .height(with(density) { dockHeightPx.toDp() })
                    .clip(RoundedCornerShape(percent = 50))
                    .background(indicatorColor),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(
                    horizontal = PrismDockSpec.InnerPadding,
                    vertical = PrismDockSpec.InnerPadding,
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .prismPressFeedback(
                            emphasized = true,
                            onClick = { if (!selected) onSelect(index) },
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
                                modifier = Modifier.size(24.dp),
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
