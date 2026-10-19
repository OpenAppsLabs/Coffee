package com.openappslabs.coffee.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.openappslabs.coffee.ui.theme.Dimens
import com.openappslabs.coffee.ui.theme.WidgetShapeLibrary
import com.openappslabs.coffee.widgets.WidgetVariant
import kotlinx.coroutines.launch

private val ButtonLargeShape = RoundedCornerShape(Dimens.CornerLarge)
private val ColorAnimationSpec = tween<Color>(durationMillis = Dimens.AnimDurationStandard)
private val MorphAnimationSpec = tween<Float>(durationMillis = Dimens.AnimDurationStandard)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSheet(
    onDismissRequest: () -> Unit,
    initialShape: String,
    initialVariant: String,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onSave: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    val rawPolygons = remember { WidgetShapeLibrary.getRawPolygons() }
    val initialSafeVariant = remember(initialVariant) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || initialVariant == "Normal") WidgetVariant.DEFAULT.key else initialVariant
    }
    var selectedShapeName by remember { mutableStateOf(initialShape) }
    var selectedVariant by remember { mutableStateOf(initialSafeVariant) }
    var morph by remember {
        mutableStateOf(Morph(start = rawPolygons[initialShape]!!, end = rawPolygons[initialShape]!!))
    }
    val morphProgress = remember { Animatable(1f) }

    LaunchedEffect(initialShape, initialSafeVariant) {
        selectedShapeName = initialShape
        selectedVariant = initialSafeVariant
        morph = Morph(rawPolygons[initialShape]!!, rawPolygons[initialShape]!!)
        morphProgress.snapTo(1f)
    }

    val animatedShape = remember(morph, morphProgress.value) {
        MorphShape(morph, morphProgress.value)
    }

    val animatedColor by animateColorAsState(
        targetValue = if (selectedVariant == WidgetVariant.MATERIAL.key) MaterialTheme.colorScheme.primary else Color.Black,
        label = "ColorAnimation",
        animationSpec = ColorAnimationSpec
    )

    val animatedIconColor by animateColorAsState(
        targetValue = if (selectedVariant == WidgetVariant.MATERIAL.key) MaterialTheme.colorScheme.onPrimary else Color.White,
        label = "IconColorAnimation",
        animationSpec = ColorAnimationSpec
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEditing) "Edit Widget" else "Add Widget",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing) {
                        FilledTonalButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = ButtonLargeShape
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Button(
                        onClick = { onSave(selectedShapeName, selectedVariant) },
                        modifier = Modifier
                            .weight(if (isEditing) 1f else 2f)
                            .height(56.dp),
                        shape = ButtonLargeShape
                    ) {
                        Text(
                            text = if (isEditing) "Save Changes" else "Add to Homescreen",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WidgetPreview(
                        shape = animatedShape,
                        backgroundColor = animatedColor,
                        iconColor = animatedIconColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        WidgetTheme(
                            selectedVariant = selectedVariant,
                            onVariantSelected = { selectedVariant = it },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                WidgetShapes(
                    selectedShapeName = selectedShapeName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onShapeSelected = { newShape ->
                        if (newShape != selectedShapeName) {
                            val currentPolygon = rawPolygons[selectedShapeName]
                            val newPolygon = rawPolygons[newShape]
                            if (currentPolygon != null && newPolygon != null) {
                                morph = Morph(currentPolygon, newPolygon)
                                scope.launch {
                                    morphProgress.snapTo(0f)
                                    morphProgress.animateTo(1f, animationSpec = MorphAnimationSpec)
                                }
                            }
                            selectedShapeName = newShape
                        }
                    }
                )
            }
        }
    }
}

private class MorphShape(private val morph: Morph, private val progress: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val matrix = android.graphics.Matrix()
        matrix.setScale(size.width, size.height)
        val path = morph.toPath(progress = progress)
        path.transform(matrix)
        return androidx.compose.ui.graphics.Outline.Generic(path.asComposePath())
    }
}