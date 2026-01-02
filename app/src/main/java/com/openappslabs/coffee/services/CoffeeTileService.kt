package com.openappslabs.coffee.services

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.openappslabs.coffee.data.CoffeeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CoffeeTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private var observationJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()

        observationJob?.cancel()
        observationJob = serviceScope.launch {
            combine(
                CoffeeManager.observeIsActive(this@CoffeeTileService),
                CoffeeManager.observeDuration(this@CoffeeTileService)
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

        val isCurrentlyRunning = try { CoffeeService.isRunning } catch (e: Exception) { false }
        val newState = !isCurrentlyRunning
        val duration = CoffeeManager.getSelectedDuration(this)

        CoffeeManager.setCoffeeActive(this, newState)

        val serviceIntent = Intent(this, CoffeeService::class.java).apply {
            if (newState) {
                putExtra(CoffeeService.EXTRA_DURATION_MINUTES, duration)
            } else {
                action = CoffeeService.ACTION_STOP
            }
        }

        if (newState) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
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