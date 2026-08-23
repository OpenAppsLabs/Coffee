package com.openappslabs.coffee.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openappslabs.coffee.R
import com.openappslabs.coffee.data.CoffeeState

private val ColorAnimationSpec = tween<Color>(durationMillis = 300, easing = FastOutSlowInEasing)
private val StandardAnimationSpec = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)

@Composable
fun TimeToggleButton(
    coffeeState: CoffeeState,
    onToggle: (Boolean, Int) -> Unit,
    onDurationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPopup by remember { mutableStateOf(false) }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerLow
    val onActiveColor = MaterialTheme.colorScheme.onPrimary
    val onInactiveColor = MaterialTheme.colorScheme.onSurface

    val containerColor by animateColorAsState(
        targetValue = if (coffeeState.isActive) activeColor else inactiveColor,
        animationSpec = ColorAnimationSpec,
        label = "ContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (coffeeState.isActive) onActiveColor else onInactiveColor,
        animationSpec = ColorAnimationSpec,
        label = "ContentColor"
    )

    // Right button specific color (Active background when popup is open or coffee is active)
    val rightButtonBg by animateColorAsState(
        targetValue = if (coffeeState.isActive || showPopup) activeColor else inactiveColor,
        animationSpec = ColorAnimationSpec,
        label = "RightButtonBg"
    )
    
    val rightButtonContent by animateColorAsState(
        targetValue = if (coffeeState.isActive || showPopup) onActiveColor else onInactiveColor,
        animationSpec = ColorAnimationSpec,
        label = "RightButtonContent"
    )

    val rightCornerRadius by animateDpAsState(
        targetValue = if (showPopup) 28.dp else 0.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "CornerRadius"
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (showPopup) 180f else 0f,
        animationSpec = StandardAnimationSpec,
        label = "ArrowRotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Left Button: Toggle Coffee
        Button(
            onClick = { onToggle(!coffeeState.isActive, coffeeState.duration) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 0.dp, bottomEnd = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Coffee",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Right Button: Duration
        Button(
            onClick = { showPopup = true },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(
                topStart = rightCornerRadius, 
                bottomStart = rightCornerRadius, 
                topEnd = 28.dp, 
                bottomEnd = 28.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = rightButtonBg,
                contentColor = rightButtonContent
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${coffeeState.duration}m",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }
    }

    if (showPopup) {
        TimeSelectionDialog(
            currentMinutes = coffeeState.duration,
            onTimeSelected = { newTime ->
                onDurationSelected(newTime)
                showPopup = false
                if (coffeeState.isActive) {
                    onToggle(true, newTime)
                }
            },
            onDismiss = { showPopup = false }
        )
    }
}
