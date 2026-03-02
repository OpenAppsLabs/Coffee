package com.openappslabs.coffee.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
object WidgetExpressiveLibrary {

    private val rawPolygonsCache by lazy {
        mapOf(
            "Circle" to MaterialShapes.Circle,
            "Square" to RoundedPolygon.rectangle(
                width = 1f,
                height = 1f,
                centerX = 0.5f,
                centerY = 0.5f,
                rounding = CornerRounding(radius = 0.2f, smoothing = 0f)
            ),
            "Slanted" to MaterialShapes.Slanted,
            "Arch" to MaterialShapes.Arch,
            "Oval" to MaterialShapes.Oval,
            "Pill" to MaterialShapes.Pill,
            "Fan" to MaterialShapes.Fan,
            "Sunny" to MaterialShapes.Sunny,
            "Very Sunny" to MaterialShapes.VerySunny,
            "Cookie 4" to MaterialShapes.Cookie4Sided,
            "Cookie 6" to MaterialShapes.Cookie6Sided,
            "Cookie 7" to MaterialShapes.Cookie7Sided,
            "Cookie 9" to MaterialShapes.Cookie9Sided,
            "Cookie 12" to MaterialShapes.Cookie12Sided,
            "Clover 4" to MaterialShapes.Clover4Leaf,
            "Clover 8" to MaterialShapes.Clover8Leaf,
            "Soft Burst" to MaterialShapes.SoftBurst,
            "Soft Boom" to MaterialShapes.SoftBoom,
            "Flower" to MaterialShapes.Flower,
            "Puffy" to MaterialShapes.Puffy,
            "Puffy Diamond" to MaterialShapes.PuffyDiamond,
            "Ghost" to MaterialShapes.Ghostish,
            "Pixel Circle" to MaterialShapes.PixelCircle,
            "Pixel Triangle" to MaterialShapes.PixelTriangle,
            "Bun" to MaterialShapes.Bun,
        )
    }

    @Composable
    fun getAllShapes(): Map<String, Shape> {
        val polygons = remember { rawPolygonsCache }

        return buildMap {
            polygons.forEach { (name, polygon) ->
                put(name, polygon.toShape())
            }
        }
    }

    fun getRawPolygons(): Map<String, RoundedPolygon> = rawPolygonsCache
}