package com.openappslabs.coffee.ui.screens.homescreen

import android.Manifest
import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.openappslabs.coffee.R
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.services.CoffeeService
import com.openappslabs.coffee.services.CoffeeTileService
import com.openappslabs.coffee.ui.components.CoffeeAlertDialog
import com.openappslabs.coffee.ui.components.CoffeeCard
import com.openappslabs.coffee.ui.components.SplitButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAboutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasAskedPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is required to show status.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showSettingsDialog) {
        CoffeeAlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = "Permission Required",
            text = "Notification permission is required to show the active status. Please enable it in settings.",
            confirmButtonText = "Open Settings",
            onConfirmClick = {
                showSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            dismissButtonText = "Cancel",
            onDismissClick = { showSettingsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Coffee",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light
                    )
                },
                actions = {
                    FilledTonalIconButton(onClick = onAboutClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.info),
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CoffeeCard(
                title = "Stay Awake",
                description = "Coffee keeps your display on without changing settings. Perfect for reading or following recipes."
            )

            Spacer(modifier = Modifier.height(16.dp))

            SplitButton(
                modifier = Modifier.fillMaxWidth(),
                onToggle = { isActive, durationMinutes ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permission = Manifest.permission.POST_NOTIFICATIONS
                        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                            
                            val shouldShowRationale = activity?.let {
                                ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                            } ?: false

                            if (!shouldShowRationale && hasAskedPermission) {
                                showSettingsDialog = true
                            } else {
                                hasAskedPermission = true
                                permissionLauncher.launch(permission)
                            }
                            return@SplitButton false
                        }
                    }

                    scope.launch {
                        toggleCoffeeService(context, isActive, durationMinutes)
                    }
                    true
                },
                onDurationChange = { updateTileState(context) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                        statusBarManager?.requestAddTileService(
                            ComponentName(context, CoffeeTileService::class.java),
                            "Coffee",
                            Icon.createWithResource(context, R.drawable.app_icon),
                            { it.run() },
                            { }
                        )
                    } else {
                        Toast.makeText(context, "Add tile manually in settings", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(text = "Add tile in quick settings", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private suspend fun toggleCoffeeService(context: Context, isActive: Boolean, duration: Int) {
    CoffeeDataStore.setCoffeeActive(context, isActive)

    val intent = Intent(context, CoffeeService::class.java).apply {
        if (isActive) {
            putExtra(CoffeeService.EXTRA_DURATION_MINUTES, duration)
        } else {
            action = CoffeeService.ACTION_STOP
        }
    }

    if (isActive) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.startService(intent)
    }

    updateTileState(context)
}

private fun updateTileState(context: Context) {
    android.service.quicksettings.TileService.requestListeningState(
        context,
        ComponentName(context, CoffeeTileService::class.java)
    )
}
