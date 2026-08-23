package com.openappslabs.coffee.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class PermissionHandlerState(
    val isNotificationGranted: Boolean,
    val isBatteryOptimizationIgnored: Boolean,
    val isWriteSettingsGranted: Boolean,
    val shouldShowSettingsPrompt: Boolean,
    val requestNotificationPermission: () -> Unit,
    val requestBatteryExemption: () -> Unit,
    val requestWriteSettingsPermission: () -> Unit,
    val openSettings: () -> Unit,
    val dismissSettingsPrompt: () -> Unit
)

@Composable
fun rememberPermissionHandler(): PermissionHandlerState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }

    var isNotificationGranted by remember { mutableStateOf(context.hasNotificationPermission()) }
    var isBatteryIgnored by remember { mutableStateOf(context.isBatteryOptimizationIgnored()) }
    var isWriteSettingsGranted by remember { mutableStateOf(Settings.System.canWrite(context)) }
    var hasAskedNotification by remember { mutableStateOf(false) }
    var shouldShowSettingsPrompt by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted
        // Check for permanent denial immediately after request failure
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val rationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
            } ?: false
            if (!rationale) {
                shouldShowSettingsPrompt = true
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationGranted = context.hasNotificationPermission()
                isBatteryIgnored = context.isBatteryOptimizationIgnored()
                isWriteSettingsGranted = Settings.System.canWrite(context)
                
                // Hide settings prompt if user granted it in settings
                if (isNotificationGranted) {
                    shouldShowSettingsPrompt = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return remember(isNotificationGranted, isBatteryIgnored, isWriteSettingsGranted, shouldShowSettingsPrompt) {
        PermissionHandlerState(
            isNotificationGranted = isNotificationGranted,
            isBatteryOptimizationIgnored = isBatteryIgnored,
            isWriteSettingsGranted = isWriteSettingsGranted,
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
            requestWriteSettingsPermission = {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            openSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            dismissSettingsPrompt = {
                shouldShowSettingsPrompt = false
            }
        )
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
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
