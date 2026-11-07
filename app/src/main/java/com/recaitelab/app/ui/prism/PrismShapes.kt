package com.recaitelab.app.ui.prism

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** Prism 形状 token：连续圆角（超椭圆近似）与胶囊形。 */
@Immutable
data class PrismShapes(
    val small: Shape = PrismCornerShape(10.dp),
    val medium: Shape = PrismCornerShape(16.dp),
    val large: Shape = PrismCornerShape(24.dp),
    val card: Shape = PrismCornerShape(22.dp),
    val listRow: Shape = PrismCornerShape(18.dp),
    val pill: Shape = CircleShape,
)

/** 连续圆角近似：用超椭圆 |x|^n + |y|^n = 1（n = 4）采样四角。 */
class PrismCornerShape(
    private val radius: Dp,
    private val exponent: Float = 0.5f,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        val r = min(with(density) { radius.toPx() }, min(size.width, size.height) / 2f)
        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(size.width - r, 0f)
            addCorner(size.width - r, r, -90f, 0f, r)
            lineTo(size.width, size.height - r)
            addCorner(size.width - r, size.height - r, 0f, 90f, r)
            lineTo(r, size.height)
            addCorner(r, size.height - r, 90f, 180f, r)
            lineTo(0f, r)
            addCorner(r, r, 180f, 270f, r)
            close()
        }
        return Outline.Generic(path)
    }

    private fun Path.addCorner(
        centerX: Float,
        centerY: Float,
        startDegrees: Float,
        endDegrees: Float,
        radiusPx: Float,
    ) {
        val steps = 6
        for (index in 1..steps) {
            val degrees = startDegrees + (endDegrees - startDegrees) * index / steps
            val angle = Math.toRadians(degrees.toDouble())
            val cosine = cos(angle).toFloat()
            val sine = sin(angle).toFloat()
            val dx = sign(cosine) * abs(cosine).pow(exponent) * radiusPx
            val dy = sign(sine) * abs(sine).pow(exponent) * radiusPx
            lineTo(centerX + dx, centerY + dy)
        }
    }
}

val LocalPrismShapes = staticCompositionLocalOf { PrismShapes() }
