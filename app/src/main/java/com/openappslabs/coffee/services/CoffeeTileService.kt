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
import kotlinx.coroutines.flow.combine
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
            combine(
                dataStore.observeIsActive(),
                dataStore.observeDuration()
            ) { isActive, duration ->
                isActive to duration
            }.collect { (isActive, duration) ->
                updateTileState(isActive, duration)
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
            val isCurrentlyActive = dataStore.observeIsActive().first()
            val duration = dataStore.observeDuration().first()
            val newState = !isCurrentlyActive

            dataStore.setCoffeeActive(newState)

            val serviceIntent = Intent(this@CoffeeTileService, CoffeeService::class.java).apply {
                if (newState) {
                    putExtra(CoffeeService.EXTRA_DURATION_MINUTES, duration)
                } else {
                    action = CoffeeService.ACTION_STOP
                }
            }

            if (newState) {
                ContextCompat.startForegroundService(this@CoffeeTileService, serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun updateTileState(isActive: Boolean, duration: Int) {
        val tile = qsTile ?: return

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = when {
            !isActive -> "Off"
            duration == 0 -> "Infinite"
            else -> "${duration}m"
        }
        tile.updateTile()
    }
}