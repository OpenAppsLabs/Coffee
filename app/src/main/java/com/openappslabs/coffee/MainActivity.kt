package com.openappslabs.coffee

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.openappslabs.coffee.ui.navigation.AppNavGraph
import com.openappslabs.coffee.ui.theme.CoffeeTheme
import com.openappslabs.coffee.widgets.NothingCoffeeWidgetReceiver
import com.openappslabs.coffee.data.CoffeeDataStore
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val openWidgetSheet = intent.getBooleanExtra("open_widget_sheet", false)
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val providerInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val isNothingVariant = providerInfo?.provider?.className == NothingCoffeeWidgetReceiver::class.java.name
        
        enableEdgeToEdge()
        setContent {
            CoffeeTheme {
                val dataStore = remember { CoffeeDataStore(this@MainActivity.applicationContext) }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        dataStore = dataStore,
                        appWidgetId = appWidgetId,
                        openWidgetSheet = openWidgetSheet,
                        initialVariant = if (isNothingVariant) "Nothing" else "Normal"
                    )
                }
            }
        }
    }
}