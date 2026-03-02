package com.openappslabs.coffee.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openappslabs.coffee.data.CoffeeDataStore
import kotlinx.coroutines.launch

private val ColorAnimationSpec = tween<Color>(durationMillis = 250, easing = FastOutSlowInEasing)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplitButton(
    modifier: Modifier = Modifier,
    onToggle: (Boolean, Int) -> Boolean,
    onDurationChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = remember { CoffeeDataStore(context.applicationContext) }

    val coffeeState by dataStore.coffeeState.collectAsStateWithLifecycle(
        initialValue = com.openappslabs.coffee.data.CoffeeState()
    )

    var showPopup by remember { mutableStateOf(false) }

    val animatedContainerColor by animateColorAsState(
        targetValue = if (coffeeState.isActive)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = ColorAnimationSpec,
        label = "ContainerColorAnimation"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (coffeeState.isActive)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = ColorAnimationSpec,
        label = "ContentColorAnimation"
    )

    val toggleColors = ButtonDefaults.buttonColors(
        containerColor = animatedContainerColor,
        contentColor = animatedContentColor
    )

    SplitButtonLayout(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        spacing = 4.dp,
        leadingButton = {
            Row(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)) {
                SplitButtonDefaults.LeadingButton(
                    onClick = {
                        val newState = !coffeeState.isActive
                        onToggle(newState, coffeeState.duration)
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
            Row(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()) {
                SplitButtonDefaults.TrailingButton(
                    checked = showPopup,
                    onCheckedChange = { showPopup = it },
                    modifier = Modifier.fillMaxSize(),
                    colors = toggleColors
                ) {
                    Text(
                        text = if (coffeeState.duration == 0) "∞" else "${coffeeState.duration} Minutes",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }
            }
        }
    )

    if (showPopup) {
        TimeSelectionDialog(
            currentMinutes = coffeeState.duration,
            onTimeSelected = { newTime ->
                scope.launch {
                    dataStore.setSelectedDuration(newTime)
                    onDurationChange(newTime)
                    showPopup = false
                    if (coffeeState.isActive) {
                        onToggle(true, newTime)
                    }
                }
            },
            onDismiss = { showPopup = false }
        )
    }
}