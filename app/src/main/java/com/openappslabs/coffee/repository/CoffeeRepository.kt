package com.openappslabs.coffee.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.services.CoffeeService
import com.openappslabs.coffee.services.CoffeeTileService
import com.openappslabs.coffee.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoffeeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: CoffeeDataStore
) {
    fun toggleCoffee(isActive: Boolean, duration: Int) {
        val intent = Intent(context, CoffeeService::class.java).apply {
            if (isActive) {
                putExtra(Constants.Service.EXTRA_DURATION_MINUTES, duration)
            } else {
                action = Constants.Service.ACTION_STOP
            }
        }

        if (isActive) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }

        requestTileUpdate()
    }

    fun requestTileUpdate() {
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, CoffeeTileService::class.java)
            )
        } catch (_: Exception) {}
    }
    
    suspend fun updateStatus(isActive: Boolean, endTime: Long = 0L) {
        dataStore.setCoffeeStatus(isActive, endTime)
    }
}
