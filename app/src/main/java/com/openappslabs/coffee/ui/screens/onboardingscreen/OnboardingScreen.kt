package com.openappslabs.coffee.ui.screens.onboardingscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.openappslabs.coffee.R
import com.openappslabs.coffee.ui.components.ContinueButton
import com.openappslabs.coffee.ui.components.OnboardingHeader
import com.openappslabs.coffee.ui.components.PermissionCard
import com.openappslabs.coffee.ui.components.PermissionSettingsCard
import com.openappslabs.coffee.utils.rememberPermissionHandler

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {},
    viewModel: OnboardingScreenViewModel = hiltViewModel()
) {
    val permissionHandler = rememberPermissionHandler()
    val onCompleteClick = remember(viewModel, onComplete) {
        { viewModel.completeOnboarding(onComplete) }
    }
    val onNotificationClick = remember(permissionHandler) {
        { permissionHandler.requestNotificationPermission() }
    }
    val onBatteryClick = remember(permissionHandler) {
        { permissionHandler.requestBatteryExemption() }
    }
    val onOpenSettingsClick = remember(permissionHandler) {
        { permissionHandler.openSettings() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                OnboardingHeader()

                Spacer(modifier = Modifier.height(24.dp))

                CardSection {
                    PermissionCard(
                        icon = painterResource(id = R.drawable.bell_ring),
                        title = "Notifications",
                        granted = permissionHandler.isNotificationGranted,
                        explanation = "Show timer status in your tray.",
                        onClick = onNotificationClick
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                    PermissionCard(
                        icon = painterResource(id = R.drawable.battery_warning),
                        title = "Battery optimization",
                        granted = permissionHandler.isBatteryOptimizationIgnored,
                        explanation = "Ensure reliable background operation.",
                        onClick = onBatteryClick
                    )
                }

                if (permissionHandler.shouldShowSettingsPrompt) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PermissionSettingsCard(
                        onOpenSettings = onOpenSettingsClick
                    )
                }
                Spacer(modifier = Modifier.height(100.dp))
            }

            ContinueButton(
                onClick = onCompleteClick,
                enabled = permissionHandler.isNotificationGranted,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun CardSection(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}