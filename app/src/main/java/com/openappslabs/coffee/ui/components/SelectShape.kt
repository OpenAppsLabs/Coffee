package com.openappslabs.coffee.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openappslabs.coffee.ui.theme.WidgetExpressiveLibrary

@Composable
fun SelectShape(
    selectedShapeName: String,
    onShapeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rawShapes = WidgetExpressiveLibrary.getAllShapes()
    val shapes = remember(rawShapes) { rawShapes }
    val shapesList = remember(shapes) { shapes.toList() }
    val currentSelection by remember(selectedShapeName, shapes) {
        derivedStateOf {
            if (selectedShapeName.isEmpty() && shapes.isNotEmpty()) {
                shapes.keys.first()
            } else {
                selectedShapeName
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Widget Shapes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose a unique shape for your next coffee widget",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = shapesList,
                    key = { it.first },
                    contentType = { "shape_item" }
                ) { (name, shape) ->
                    ShapeItem(
                        name = name,
                        shape = shape,
                        isSelected = name == currentSelection,
                        onClick = { onShapeSelected(name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShapeItem(
    name: String,
    shape: Shape,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else Color.Transparent
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) { }

        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}