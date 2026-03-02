package com.openappslabs.coffee.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class PermissionHandlerState(
    val isNotificationGranted: Boolean,
    val isBatteryOptimizationIgnored: Boolean,
    val shouldShowSettingsPrompt: Boolean,
    val requestNotificationPermission: () -> Unit,
    val requestBatteryExemption: () -> Unit,
    val openSettings: () -> Unit,
    val dismissSettingsPrompt: () -> Unit
)

@Composable
fun rememberPermissionHandler(): PermissionHandlerState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity

    var isNotificationGranted by remember { mutableStateOf(context.hasNotificationPermission()) }
    var isBatteryIgnored by remember { mutableStateOf(context.isBatteryOptimizationIgnored()) }
    var hasAskedNotification by remember { mutableStateOf(false) }
    var shouldShowSettingsPrompt by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationGranted = context.hasNotificationPermission()
                isBatteryIgnored = context.isBatteryOptimizationIgnored()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted
    }

    return remember(isNotificationGranted, isBatteryIgnored, shouldShowSettingsPrompt) {
        PermissionHandlerState(
            isNotificationGranted = isNotificationGranted,
            isBatteryOptimizationIgnored = isBatteryIgnored,
            shouldShowSettingsPrompt = shouldShowSettingsPrompt,
            requestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val perm = Manifest.permission.POST_NOTIFICATIONS
                    val showRationale = activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, perm)
                    } ?: false

                    if (!showRationale && hasAskedNotification) {
                        shouldShowSettingsPrompt = true
                    } else {
                        hasAskedNotification = true
                        notificationLauncher.launch(perm)
                    }
                } else {
                    isNotificationGranted = true
                }
            },
            requestBatteryExemption = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            },
            openSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                shouldShowSettingsPrompt = false
            },
            dismissSettingsPrompt = {
                shouldShowSettingsPrompt = false
            }
        )
    }
}

fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

fun Context.isBatteryOptimizationIgnored(): Boolean {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(packageName)
}