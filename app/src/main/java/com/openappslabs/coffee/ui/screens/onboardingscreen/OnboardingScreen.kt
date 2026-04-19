package com.openappslabs.coffee.ui.screens.onboardingscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openappslabs.coffee.R
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.ui.components.CoffeeAlertDialog
import com.openappslabs.coffee.utils.rememberPermissionHandler
import kotlinx.coroutines.launch

private val IconShape = RoundedCornerShape(16.dp)
private val CardShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = remember { CoffeeDataStore(context.applicationContext) }
    val permissionHandler = rememberPermissionHandler()

    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val iconBackgroundBrush = Brush.linearGradient(
        colors = listOf(primaryContainer, primary.copy(alpha = 0.1f))
    )
    val cardContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)

    if (permissionHandler.shouldShowSettingsPrompt) {
        CoffeeAlertDialog(
            onDismissRequest = { permissionHandler.dismissSettingsPrompt() },
            title = "Permission Required",
            text = "To ensure the timer works correctly, please grant the notification permission in the system settings.",
            confirmButtonText = "Open Settings",
            onConfirmClick = { permissionHandler.openSettings() },
            dismissButtonText = "Cancel",
            onDismissClick = { permissionHandler.dismissSettingsPrompt() }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Welcome",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                shape = IconShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                tonalElevation = 8.dp,
                modifier = Modifier.size(128.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(iconBackgroundBrush)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Keep Your Screen Awake",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Coffee needs a couple of permissions to keep your screen on",
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            PermissionCard(
                icon = painterResource(id = R.drawable.bell_ring),
                title = "Notifications",
                mandatory = true,
                granted = permissionHandler.isNotificationGranted,
                explanation = "Coffee needs notifications permission to show status, timer in your notification bar.",
                hint = "Required to use Coffee.",
                containerColor = cardContainerColor,
                onClick = { permissionHandler.requestNotificationPermission() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionCard(
                icon = painterResource(id = R.drawable.battery_warning),
                title = "Battery optimization",
                mandatory = false,
                granted = permissionHandler.isBatteryOptimizationIgnored,
                explanation = "Coffee requires battery optimization to be disabled to run reliably in the background. Some systems close background apps aggressively; enabling this ensures the app functions correctly on your device.",
                hint = "Optional, but recommended.",
                containerColor = cardContainerColor,
                onClick = { permissionHandler.requestBatteryExemption() }
            )

            Spacer(modifier = Modifier.weight(1f))

            val isButtonEnabled = permissionHandler.isNotificationGranted

            val animatedContainerColor by animateColorAsState(
                targetValue = if (isButtonEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(durationMillis = 300),
                label = "Button Container Color"
            )

            val animatedContentColor by animateColorAsState(
                targetValue = if (isButtonEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 300),
                label = "Button Content Color"
            )

            Button(
                onClick = {
                    if (isButtonEnabled) {
                        scope.launch {
                            dataStore.setOnboardingComplete(true)
                        }
                        onComplete()
                    } else {
                        permissionHandler.requestNotificationPermission()
                    }
                },
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = animatedContainerColor,
                    contentColor = animatedContentColor,
                    disabledContainerColor = animatedContainerColor,
                    disabledContentColor = animatedContentColor
                )
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: Painter,
    title: String,
    mandatory: Boolean,
    granted: Boolean,
    explanation: String,
    hint: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    val badgeColor by animateColorAsState(
        targetValue = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "Badge Color"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(enabled = !granted, onClick = onClick),
        color = containerColor,
        shape = CardShape,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val badgeText = when {
                        granted -> "Granted"
                        mandatory -> "Required"
                        else -> "Optional"
                    }

                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (granted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                color = badgeColor,
                                shape = CircleShape
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}