package com.openappslabs.coffee.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.openappslabs.coffee.R
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.services.CoffeeService
import com.openappslabs.coffee.services.CoffeeTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val VARIANT_KEY = stringPreferencesKey("widget_variant")
private val SHAPE_KEY = stringPreferencesKey("widget_shape")

abstract class BaseCoffeeWidget(
    private val activeContent: ColorProvider,
    private val inactiveContent: ColorProvider,
    private val activeHex: Color,
    private val inactiveHex: Color
) : GlanceAppWidget() {

    private val ndotFont = FontFamily("ndot")
    override val sizeMode: SizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dataStore = CoffeeDataStore(context.applicationContext)

        provideContent {
            val isActive by dataStore.observeIsActive().collectAsState(initial = false)
            val size = LocalSize.current
            val prefs = currentState<Preferences>()
            val shapeName = prefs[SHAPE_KEY] ?: "Circle"
            val variant = prefs[VARIANT_KEY] ?: "Normal"
            val dynamicActiveHex = remember(variant) {
                if (variant == "Nothing") Color(0xFFD71921) else Color(0xFF000000)
            }

            GlanceTheme {
                CoffeeWidgetContent(
                    isActive = isActive,
                    size = size,
                    shapeName = shapeName,
                    variant = variant,
                    activeContent = activeContent,
                    inactiveContent = inactiveContent,
                    activeHex = dynamicActiveHex,
                    inactiveHex = Color(0xFF2C2C2C)
                )
            }
        }
    }

    @Composable
    private fun CoffeeWidgetContent(
        isActive: Boolean,
        size: DpSize,
        shapeName: String,
        variant: String,
        activeContent: ColorProvider,
        inactiveContent: ColorProvider,
        activeHex: Color,
        inactiveHex: Color
    ) {
        val context = LocalContext.current

        val shapeBitmap = remember(size, shapeName, isActive) {
            val density = context.resources.displayMetrics.density
            val bgColor = if (isActive) activeHex else inactiveHex

            val isSquareShape = shapeName == "Square"
            val isRectangular = kotlin.math.abs(size.width.value - size.height.value) > 25f
            val shouldForceSquare = !isSquareShape && isRectangular

            val targetWidth = if (shouldForceSquare) minOf(size.width, size.height) else size.width
            val targetHeight = if (shouldForceSquare) minOf(size.width, size.height) else size.height

            WidgetShapeRenderer.createShapeBitmap(
                shapeName = shapeName,
                color = bgColor,
                widthPx = (targetWidth.value * density).toInt(),
                heightPx = (targetHeight.value * density).toInt()
            ).apply {
                this.density = context.resources.displayMetrics.densityDpi
            }
        }

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val isRectangular = kotlin.math.abs(size.width.value - size.height.value) > 25f
            val isSquareShape = shapeName == "Square"

            Box(
                modifier = GlanceModifier
                    .then(
                        if (!isSquareShape && isRectangular) {
                            GlanceModifier.size(minOf(size.width, size.height))
                        } else {
                            GlanceModifier.fillMaxSize()
                        }
                    )
                    .background(ImageProvider(shapeBitmap))
                    .clickable(actionRunCallback<ToggleCoffeeAction>()),
                contentAlignment = Alignment.Center
            ) {
                WidgetLayoutBuilder(size, if (isActive) activeContent else inactiveContent, shapeName, variant)
            }
        }
    }

    @Composable
    private fun WidgetLayoutBuilder(
        size: DpSize,
        contentColor: ColorProvider,
        shapeName: String,
        variant: String
    ) {
        val isTall = size.height >= 100.dp
        val isWide = size.width >= 100.dp
        val showText = (isTall && isWide) || (shapeName == "Square" && (isTall || isWide))
        val useHorizontal = isWide && !isTall

        if (useHorizontal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoffeeIcon(contentColor)
                if (showText) {
                    WidgetLabel(if (variant == "Nothing") "COFFEE" else "Coffee", contentColor, variant)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CoffeeIcon(contentColor)
                if (showText) {
                    val label = if (shapeName == "Square" && isTall && !isWide) "CO\nFF\nEE" else "COFFEE"
                    WidgetLabel(if (variant == "Nothing") label.uppercase() else label, contentColor, variant)
                }
            }
        }
    }

    @Composable
    private fun WidgetLabel(text: String, color: ColorProvider, variant: String) {
        Text(
            text = text,
            style = TextStyle(
                color = color,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                fontFamily = if (variant == "Nothing") ndotFont else FontFamily.SansSerif
            ),
            modifier = GlanceModifier.padding(if (text.contains("\n")) 0.dp else 4.dp)
        )
    }

    @Composable
    private fun CoffeeIcon(tintColor: ColorProvider) {
        Image(
            provider = ImageProvider(R.drawable.app_icon),
            contentDescription = null,
            modifier = GlanceModifier.size(22.dp),
            colorFilter = ColorFilter.tint(tintColor)
        )
    }
}

private fun handleWidgetSettings(context: Context, intent: Intent, widget: GlanceAppWidget) {
    val shapeName = intent.getStringExtra("widget_shape") ?: return
    val variantName = intent.getStringExtra("widget_variant") ?: return
    val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        CoroutineScope(Dispatchers.IO).launch {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[SHAPE_KEY] = shapeName
                    this[VARIANT_KEY] = variantName
                }
            }
            widget.update(context, glanceId)
        }
    }
}

class CoffeeWidget : BaseCoffeeWidget(
    activeContent = ColorProvider(R.color.coffee_active_content),
    inactiveContent = ColorProvider(R.color.coffee_inactive_content),
    activeHex = Color(0xFF000000),
    inactiveHex = Color(0xFF2C2C2C)
)

class NothingCoffeeWidget : BaseCoffeeWidget(
    activeContent = ColorProvider(R.color.coffee_active_content),
    inactiveContent = ColorProvider(R.color.coffee_inactive_content),
    activeHex = Color(0xFFD71921),
    inactiveHex = Color(0xFF000000),
)

class CoffeeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CoffeeWidget()
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        handleWidgetSettings(context, intent, glanceAppWidget)
    }
}

class NothingCoffeeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NothingCoffeeWidget()
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        handleWidgetSettings(context, intent, glanceAppWidget)
    }
}

class ToggleCoffeeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val dataStore = CoffeeDataStore(context.applicationContext)
        val isActive = dataStore.observeIsActive().first()
        val newState = !isActive

        dataStore.setCoffeeActive(newState)

        val intent = Intent(context, CoffeeService::class.java).apply {
            if (newState) {
                val duration = dataStore.observeDuration().first()
                putExtra(CoffeeService.EXTRA_DURATION_MINUTES, duration)
            } else {
                action = CoffeeService.ACTION_STOP
            }
        }

        if (newState) ContextCompat.startForegroundService(context, intent)
        else context.startService(intent)

        android.service.quicksettings.TileService.requestListeningState(
            context, ComponentName(context, CoffeeTileService::class.java)
        )
    }
}

fun createApiPreview(context: Context, shapeName: String, variant: String): RemoteViews {
    val displayMetrics = context.resources.displayMetrics
    val density = displayMetrics.density
    val sizePx = (110 * density).toInt()

    val bgColor = if (variant == "Nothing") {
        android.graphics.Color.parseColor("#D71921")
    } else {
        android.graphics.Color.BLACK
    }

    val bitmap = WidgetShapeRenderer.createShapeBitmap(
        shapeName = shapeName,
        color = androidx.compose.ui.graphics.Color(bgColor),
        widthPx = sizePx,
        heightPx = sizePx
    ).apply {
        this.density = displayMetrics.densityDpi
    }

    return RemoteViews(context.packageName, R.layout.widget_pin_preview).apply {
        setImageViewBitmap(R.id.preview_image, bitmap)
    }
}