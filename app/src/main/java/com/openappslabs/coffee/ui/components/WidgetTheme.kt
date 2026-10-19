package com.openappslabs.coffee.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.openappslabs.coffee.ui.theme.Dimens
import com.openappslabs.coffee.widgets.WidgetVariant

private val CardShape = RoundedCornerShape(Dimens.CornerLarge)
private val ButtonSmallShape = RoundedCornerShape(Dimens.CornerSmall)

@Composable
fun WidgetTheme(
    selectedVariant: String,
    onVariantSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceVariantDimmed = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val defaultKey = WidgetVariant.DEFAULT.key
    val materialKey = WidgetVariant.MATERIAL.key

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledTonalButton(
                onClick = { onVariantSelected(defaultKey) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeightCompact),
                shape = ButtonSmallShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (selectedVariant == defaultKey)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        surfaceVariantDimmed,
                    contentColor = if (selectedVariant == defaultKey)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("DEFAULT")
            }

            FilledTonalButton(
                onClick = { onVariantSelected(materialKey) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeightCompact),
                shape = ButtonSmallShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (selectedVariant == materialKey)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        surfaceVariantDimmed,
                    contentColor = if (selectedVariant == materialKey)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("MATERIAL")
            }
        }
    }
}