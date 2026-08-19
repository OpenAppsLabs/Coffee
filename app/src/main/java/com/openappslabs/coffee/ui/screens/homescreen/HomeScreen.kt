package com.openappslabs.coffee.ui.screens.homescreen

import android.app.Activity
import android.app.PendingIntent
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openappslabs.coffee.R
import com.openappslabs.coffee.services.CoffeeTileService
import com.openappslabs.coffee.ui.components.AlternateMode
import com.openappslabs.coffee.ui.components.Header
import com.openappslabs.coffee.ui.components.HomeActionButton
import com.openappslabs.coffee.ui.components.TimeToggleButton
import com.openappslabs.coffee.ui.components.WidgetSheet
import com.openappslabs.coffee.utils.Constants
import com.openappslabs.coffee.utils.rememberPermissionHandler
import com.openappslabs.coffee.widgets.VARIANT_KEY
import com.openappslabs.coffee.widgets.createApiPreview
import kotlinx.coroutines.launch

private val SHAPE_KEY = stringPreferencesKey(Constants.Widget.KEY_SHAPE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAboutClick: () -> Unit = {},
    appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    openWidgetSheet: Boolean = false,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val scope = rememberCoroutineScope()
    val coffeeState by viewModel.coffeeState.collectAsStateWithLifecycle()
    val permissionHandler = rememberPermissionHandler()

    var selectedShapeName by remember { mutableStateOf("Circle") }
    var selectedVariant by remember { mutableStateOf("Default") }
    var showWidgetSheet by remember {
        mutableStateOf(openWidgetSheet || appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    LaunchedEffect(permissionHandler.isWriteSettingsGranted) {
        if (!permissionHandler.isWriteSettingsGranted && coffeeState.isAlternateMode) {
            viewModel.setAlternateMode(false)
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
                    selectedVariant = prefs[VARIANT_KEY] ?: "Default"
                }
            } catch (e: Exception) {
            }
        }
    }

    val onToggle = remember(viewModel) { viewModel::toggleCoffee }
    val onDurationSelected = remember(viewModel) { viewModel::setSelectedDuration }
    val onAddTileClick = remember(context) {
        {
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
            Unit
        }
    }
    val onAddWidgetClick = remember {
        {
            selectedShapeName = "Circle"
            showWidgetSheet = true
        }
    }
    val onAlternateModeChange = remember(viewModel, permissionHandler) {
        { enabled: Boolean ->
            if (enabled) {
                if (permissionHandler.isWriteSettingsGranted) {
                    viewModel.setAlternateMode(true)
                } else {
                    permissionHandler.requestWriteSettingsPermission()
                }
            } else {
                viewModel.setAlternateMode(false)
            }
        }
    }

    Scaffold(
        topBar = {
            Header(
                title = "Coffee",
                actionIcon = painterResource(id = R.drawable.info),
                onActionClick = onAboutClick,
                actionContentDescription = "About"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TimeToggleButton(
                coffeeState = coffeeState,
                onToggle = onToggle,
                onDurationSelected = onDurationSelected
            )

            HomeActionButton(
                text = "Add Quick Settings Tile",
                onClick = onAddTileClick
            )

            HomeActionButton(
                text = "Add Widget to Home",
                onClick = onAddWidgetClick
            )

            AlternateMode(
                checked = coffeeState.isAlternateMode,
                onCheckedChange = onAlternateModeChange,
                enabled = !coffeeState.isActive
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
                        com.openappslabs.coffee.widgets.CoffeeWidget().update(context, glanceId)
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

private fun handleWidgetPinning(
    context: Context,
    shape: String,
    variant: String,
    manager: AppWidgetManager
) {
    val receiverClass = com.openappslabs.coffee.widgets.CoffeeWidgetReceiver::class.java

    if (manager.isRequestPinAppWidgetSupported) {
        val callbackIntent = Intent(context, receiverClass).apply {
            putExtra(Constants.Widget.KEY_SHAPE, shape)
            putExtra(Constants.Widget.KEY_VARIANT, variant)
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
