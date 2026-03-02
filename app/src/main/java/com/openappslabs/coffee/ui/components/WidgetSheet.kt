package com.openappslabs.coffee.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.openappslabs.coffee.R
import com.openappslabs.coffee.ui.theme.WidgetExpressiveLibrary
import com.openappslabs.coffee.ui.theme.ndotFamily
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(24.dp)
private val ButtonLargeShape = RoundedCornerShape(28.dp)
private val ButtonSmallShape = RoundedCornerShape(16.dp)
private val ColorAnimationSpec = tween<Color>(durationMillis = 250)
private val MorphAnimationSpec = tween<Float>(durationMillis = 250)
private val NothingRed = Color(0xFFD71921)
private val BlackColor = Color(0xFF000000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSheet(
    onDismissRequest: () -> Unit,
    initialShape: String,
    initialVariant: String,
    isEditing: Boolean = false,
    onSave: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    val rawPolygons = remember { WidgetExpressiveLibrary.getRawPolygons() }

    var selectedShapeName by remember { mutableStateOf(initialShape) }
    var selectedVariant by remember { mutableStateOf(initialVariant) }
    var morph by remember {
        mutableStateOf(Morph(start = rawPolygons[initialShape]!!, end = rawPolygons[initialShape]!!))
    }
    val morphProgress = remember { Animatable(1f) }

    LaunchedEffect(initialShape, initialVariant) {
        selectedShapeName = initialShape
        selectedVariant = initialVariant
        morph = Morph(rawPolygons[initialShape]!!, rawPolygons[initialShape]!!)
        morphProgress.snapTo(1f)
    }

    val animatedColor by animateColorAsState(
        targetValue = if (selectedVariant == "Nothing") NothingRed else BlackColor,
        label = "Widget Color Animation",
        animationSpec = ColorAnimationSpec
    )

    val animatedShape = remember(morph, morphProgress.value) {
        MorphShape(morph, morphProgress.value)
    }

    val surfaceVariantDimmed = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = CardShape
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(animatedShape)
                                .background(animatedColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.app_icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = CardShape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Variant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { selectedVariant = "Normal" },
                                modifier = Modifier.weight(1f),
                                shape = ButtonSmallShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (selectedVariant == "Normal")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        surfaceVariantDimmed,
                                    contentColor = if (selectedVariant == "Normal")
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("NORMAL")
                            }

                            FilledTonalButton(
                                onClick = { selectedVariant = "Nothing" },
                                modifier = Modifier.weight(1f),
                                shape = ButtonSmallShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (selectedVariant == "Nothing")
                                        NothingRed
                                    else
                                        surfaceVariantDimmed,
                                    contentColor = if (selectedVariant == "Nothing")
                                        Color.White
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = "NOTHING",
                                    fontFamily = ndotFamily
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SelectShape(
                    selectedShapeName = selectedShapeName,
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