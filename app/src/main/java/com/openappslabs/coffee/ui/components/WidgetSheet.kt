package com.openappslabs.coffee.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openappslabs.coffee.R
import com.openappslabs.coffee.ui.theme.WidgetExpressiveLibrary
import com.openappslabs.coffee.ui.theme.ndotFamily

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
    var selectedShapeName by remember { mutableStateOf(initialShape) }
    var selectedVariant by remember { mutableStateOf(initialVariant) }
    val shapes = WidgetExpressiveLibrary.getAllShapes()
    val selectedShape = shapes[selectedShapeName] ?: MaterialTheme.shapes.medium

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditing) "Edit Widget" else "Add Widget",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(24.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(selectedShape)
                            .background(if (selectedVariant == "Nothing") Color(0xFFD71921) else Color(0xFF000000)),
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
                shape = RoundedCornerShape(24.dp)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { selectedVariant = "Normal" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (selectedVariant == "Normal")
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (selectedVariant == "Nothing")
                                    Color(0xFFD71921)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                onShapeSelected = { selectedShapeName = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSave(selectedShapeName, selectedVariant) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Add to Homescreen",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}