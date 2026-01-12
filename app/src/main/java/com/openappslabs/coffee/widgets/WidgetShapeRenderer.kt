package com.openappslabs.coffee.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.graphics.shapes.toPath
import com.openappslabs.coffee.ui.theme.WidgetExpressiveLibrary

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object WidgetShapeRenderer {
    private val matrix = Matrix()
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun createShapeBitmap(
        shapeName: String,
        color: Color,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val bitmap = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        paint.color = color.toArgb()

        if (shapeName == "Square") {
            val radius = minOf(w, h) * 0.2f
            canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), radius, radius, paint)
        } else {
            val polygons = WidgetExpressiveLibrary.getRawPolygons()
            val polygon = polygons[shapeName] ?: MaterialShapes.Circle
            val path = polygon.toPath()

            matrix.reset()
            matrix.postScale(w.toFloat(), h.toFloat())

            path.transform(matrix)

            canvas.drawPath(path, paint)
        }
        return bitmap
    }
}