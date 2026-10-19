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
import com.openappslabs.coffee.utils.Constants

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val openWidgetSheet = intent.getBooleanExtra(Constants.IntentExtras.EXTRA_OPEN_WIDGET_SHEET, false)
        
        enableEdgeToEdge()
        setContent {
            CoffeeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        appWidgetId = appWidgetId,
                        openWidgetSheet = openWidgetSheet
                    )
                }
            }
        }
    }
}