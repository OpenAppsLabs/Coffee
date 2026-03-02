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
import androidx.compose.ui.window.DialogProperties

@Composable
fun CoffeeAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButtonText: String,
    onConfirmClick: () -> Unit,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    dismissButtonText: String? = null,
    onDismissClick: () -> Unit = onDismissRequest,
) {
    val hasDismissButton = dismissButtonText != null

    val confirmShape = remember(hasDismissButton) {
        if (hasDismissButton) {
            RoundedCornerShape(
                topStart = 24.dp, topEnd = 24.dp,
                bottomStart = 4.dp, bottomEnd = 4.dp
            )
        } else {
            RoundedCornerShape(24.dp)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.1f
                )

                Spacer(modifier = Modifier.height(16.dp))

                CoffeeDialogButton(
                    text = confirmButtonText,
                    onClick = onConfirmClick,
                    shape = confirmShape,
                    isPrimary = true
                )

                if (hasDismissButton) {
                    val dismissShape = remember {
                        RoundedCornerShape(
                            topStart = 4.dp, topEnd = 4.dp,
                            bottomStart = 24.dp, bottomEnd = 24.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    CoffeeDialogButton(
                        text = dismissButtonText,
                        onClick = onDismissClick,
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
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
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
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}