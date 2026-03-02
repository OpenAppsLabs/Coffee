package com.openappslabs.coffee.services

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.openappslabs.coffee.data.CoffeeDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CoffeeTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var observationJob: Job? = null
    private val dataStore by lazy { CoffeeDataStore(applicationContext) }

    override fun onStartListening() {
        super.onStartListening()

        observationJob?.cancel()
        observationJob = serviceScope.launch {
            dataStore.coffeeState.collect { state ->
                updateTileState(state.isActive, state.duration)
            }
        }
    }

    override fun onStopListening() {
        observationJob?.cancel()
        observationJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()

        serviceScope.launch {
            try {
                val currentState = dataStore.coffeeState.first()
                val newActive = !currentState.isActive

                val serviceIntent = Intent(this@CoffeeTileService, CoffeeService::class.java).apply {
                    if (newActive) {
                        putExtra(CoffeeService.EXTRA_DURATION_MINUTES, currentState.duration)
                    } else {
                        action = CoffeeService.ACTION_STOP
                    }
                }

                if (newActive) {
                    ContextCompat.startForegroundService(this@CoffeeTileService, serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun updateTileState(isActive: Boolean, duration: Int) {
        qsTile?.apply {
            state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = when {
                !isActive -> "Off"
                duration == 0 -> "Infinite"
                else -> "${duration}m"
            }
            updateTile()
        }
    }
}