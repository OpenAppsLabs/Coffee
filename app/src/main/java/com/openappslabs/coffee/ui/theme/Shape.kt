package com.openappslabs.coffee.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
object WidgetExpressiveLibrary {

    @Composable
    fun getAllShapes(): Map<String, Shape> {
        val polygons = remember { getRawPolygons() }

        val stableMap = remember { mutableStateMapOf<String, Shape>() }
        
        polygons.forEach { (name, polygon) ->
            stableMap[name] = polygon.toShape()
        }
        
        return stableMap
    }

    fun getRawPolygons(): Map<String, RoundedPolygon> {
        return mapOf(
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
            "Fan" to MaterialShapes.Fan,
            "Arrow" to MaterialShapes.Arrow,
            "Semi Circle" to MaterialShapes.SemiCircle,
            "Oval" to MaterialShapes.Oval,
            "Pill" to MaterialShapes.Pill,
            "Triangle" to MaterialShapes.Triangle,
            "Pentagon" to MaterialShapes.Pentagon,
            "Gem" to MaterialShapes.Gem,
            "Sunny" to MaterialShapes.Sunny,
            "Very Sunny" to MaterialShapes.VerySunny,
            "Cookie 4" to MaterialShapes.Cookie4Sided,
            "Cookie 6" to MaterialShapes.Cookie6Sided,
            "Cookie 7" to MaterialShapes.Cookie7Sided,
            "Cookie 9" to MaterialShapes.Cookie9Sided,
            "Cookie 12" to MaterialShapes.Cookie12Sided,
            "Ghost" to MaterialShapes.Ghostish,
            "Clover 4" to MaterialShapes.Clover4Leaf,
            "Clover 8" to MaterialShapes.Clover8Leaf,
            "Pixel Circle" to MaterialShapes.PixelCircle,
            "Bun" to MaterialShapes.Bun
        )
    }
}