package com.openappslabs.coffee.ui.screens.homescreen

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.openappslabs.coffee.R
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.services.CoffeeService
import com.openappslabs.coffee.services.CoffeeTileService
import com.openappslabs.coffee.ui.components.CoffeeAlertDialog
import com.openappslabs.coffee.ui.components.CoffeeCard
import com.openappslabs.coffee.ui.components.SplitButton
import com.openappslabs.coffee.ui.components.WidgetSheet
import com.openappslabs.coffee.widgets.VARIANT_KEY
import com.openappslabs.coffee.widgets.createApiPreview
import kotlinx.coroutines.launch

private val SHAPE_KEY = stringPreferencesKey("widget_shape")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAboutClick: () -> Unit = {},
    appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    openWidgetSheet: Boolean = false,
    initialVariant: String = "Normal"
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val dataStore = remember { CoffeeDataStore(context.applicationContext) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasAskedPermission by remember { mutableStateOf(false) }
    var selectedShapeName by remember { mutableStateOf("Circle") }
    var selectedVariant by remember { mutableStateOf(initialVariant) }
    var showWidgetSheet by remember { 
        mutableStateOf(openWidgetSheet || appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) 
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notifications are needed for the timer status.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(appWidgetId) {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            try {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)

                if (prefs.contains(SHAPE_KEY)) {
                    selectedShapeName = prefs[SHAPE_KEY] ?: "Circle"
                }
                if (prefs.contains(VARIANT_KEY)) {
                    selectedVariant = prefs[VARIANT_KEY] ?: "Normal"
                }
            } catch (e: Exception) {
            }
        }
    }

    if (showSettingsDialog) {
        CoffeeAlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = "Permission Required",
            text = "Notification permission is required to show the timer. Please enable it in system settings.",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CoffeeCard(
                title = "Stay Awake",
                description = "Coffee keeps your display on without changing settings. Perfect for reading or following recipes."
            )

            SplitButton(
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
                        toggleCoffeeService(context, dataStore, isActive, durationMinutes)
                    }
                    true
                }
            )

            HomeScreenActionButton(
                text = "Add Quick Settings Tile",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                        statusBarManager?.requestAddTileService(
                            ComponentName(context, CoffeeTileService::class.java),
                            "Coffee",
                            Icon.createWithResource(context, R.drawable.app_icon),
                            { it.run() },
                            {}
                        )
                    } else {
                        Toast.makeText(context, "Add tile manually via status bar", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            HomeScreenActionButton(
                text = "Add Widget to Home",
                onClick = {
                    selectedShapeName = "Circle"
                    selectedVariant = "Normal"
                    showWidgetSheet = true
                }
            )
        }
    }

    if (showWidgetSheet) {
        WidgetSheet(
            onDismissRequest = {
                showWidgetSheet = false
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) activity?.finish()
            },
            initialShape = selectedShapeName,
            initialVariant = selectedVariant,
            isEditing = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID,
            onSave = { shape, variant ->
                scope.launch {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                        updateAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                            prefs.toMutablePreferences().apply {
                                this[SHAPE_KEY] = shape
                                this[VARIANT_KEY] = variant
                            }
                        }

                        if (variant == "Nothing") {
                            com.openappslabs.coffee.widgets.NothingCoffeeWidget().update(context, glanceId)
                        } else {
                            com.openappslabs.coffee.widgets.CoffeeWidget().update(context, glanceId)
                        }

                        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        activity?.setResult(Activity.RESULT_OK, resultValue)
                        activity?.finish()
                    } else {
                        handleWidgetPinning(context, shape, variant, appWidgetManager)
                    }
                    showWidgetSheet = false
                }
            }
        )
    }
}

@Composable
private fun HomeScreenActionButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

private fun handleWidgetPinning(
    context: Context,
    shape: String,
    variant: String,
    manager: AppWidgetManager
) {
    val receiverClass = if (variant == "Nothing") {
        com.openappslabs.coffee.widgets.NothingCoffeeWidgetReceiver::class.java
    } else {
        com.openappslabs.coffee.widgets.CoffeeWidgetReceiver::class.java
    }

    if (manager.isRequestPinAppWidgetSupported) {
        val callbackIntent = Intent(context, receiverClass).apply {
            putExtra("widget_shape", shape)
            putExtra("widget_variant", variant)
        }

        val successCallback = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            callbackIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bundle = Bundle()
        val previewViews = createApiPreview(context, shape, variant)
        bundle.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)

        manager.requestPinAppWidget(
            ComponentName(context, receiverClass),
            bundle,
            successCallback
        )
    }
}

private suspend fun toggleCoffeeService(context: Context, dataStore: CoffeeDataStore, isActive: Boolean, duration: Int) {
    dataStore.setCoffeeActive(isActive)

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

    TileService.requestListeningState(context, ComponentName(context, CoffeeTileService::class.java))
}