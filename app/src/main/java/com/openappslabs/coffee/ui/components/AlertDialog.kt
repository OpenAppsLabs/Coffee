package com.openappslabs.coffee.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun CoffeeAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButtonText: String,
    onConfirmClick: () -> Unit,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    dismissButtonText: String? = null,
    onDismissClick: (() -> Unit)? = null,
) {
    val outerRadius = 24.dp
    val innerRadius = 4.dp

    val confirmShape = remember(dismissButtonText) {
        if (dismissButtonText != null) {
            RoundedCornerShape(
                topStart = outerRadius, topEnd = outerRadius,
                bottomStart = innerRadius, bottomEnd = innerRadius
            )
        } else {
            RoundedCornerShape(outerRadius)
        }
    }

    val dismissShape = remember {
        RoundedCornerShape(
            topStart = innerRadius, topEnd = innerRadius,
            bottomStart = outerRadius, bottomEnd = outerRadius
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = text,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                CoffeeDialogButton(
                    text = confirmButtonText,
                    onClick = onConfirmClick,
                    shape = confirmShape,
                    isPrimary = true
                )

                if (dismissButtonText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CoffeeDialogButton(
                        text = dismissButtonText,
                        onClick = { onDismissClick?.invoke() },
                        shape = dismissShape,
                        isPrimary = false
                    )
                }
            }
        }
    }
}

@Composable
private fun CoffeeDialogButton(
    text: String,
    onClick: () -> Unit,
    shape: Shape,
    isPrimary: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (isPrimary)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}