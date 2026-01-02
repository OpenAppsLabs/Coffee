package com.openappslabs.coffee.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openappslabs.coffee.data.CoffeeManager

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplitButton(
    modifier: Modifier = Modifier,
    onToggle: (Boolean, Int) -> Boolean,
    onDurationChange: (Int) -> Unit = {}
) {

    val context = LocalContext.current

    val isActive by CoffeeManager.observeIsActive(context).collectAsStateWithLifecycle(
        initialValue = CoffeeManager.isCoffeeActive(context)
    )
    val selectedTime by CoffeeManager.observeDuration(context).collectAsStateWithLifecycle(
        initialValue = CoffeeManager.getSelectedDuration(context)
    )

    var showPopup by remember { mutableStateOf(false) }

    val toggleColors = ButtonDefaults.buttonColors(
        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    )

    SplitButtonLayout(
        modifier = modifier.fillMaxWidth().height(48.dp),
        spacing = 4.dp,
        leadingButton = {
            Row(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f)) {
                SplitButtonDefaults.LeadingButton(
                    onClick = {
                        val newState = !isActive
                        onToggle(newState, selectedTime)
                    },
                    modifier = Modifier.fillMaxSize(),
                    colors = toggleColors
                ) {
                    Text(
                        text = "Toggle Coffee",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }
            }
        },
        trailingButton = {
            Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                SplitButtonDefaults.TrailingButton(
                    checked = showPopup,
                    onCheckedChange = { showPopup = it },
                    modifier = Modifier.fillMaxSize(),
                    colors = toggleColors
                ) {
                    Text(
                        text = if (selectedTime == 0) "∞" else "$selectedTime Minutes",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }
            }
        }
    )

    if (showPopup) {
        TimeSelectionDialog(
            currentMinutes = selectedTime,
            onTimeSelected = { newTime ->
                CoffeeManager.setSelectedDuration(context, newTime)
                onDurationChange(newTime)
                showPopup = false
                if (isActive) {
                    onToggle(true, newTime)
                }
            },
            onDismiss = { showPopup = false }
        )
    }
}