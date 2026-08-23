package com.openappslabs.coffee.services

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.repository.CoffeeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CoffeeTileService : TileService() {

    @Inject lateinit var dataStore: CoffeeDataStore
    @Inject lateinit var coffeeRepository: CoffeeRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var observationJob: Job? = null

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
                
                coffeeRepository.updateStatus(newActive)
                coffeeRepository.toggleCoffee(newActive, currentState.duration)
            } catch (e: Exception) {
            }
        }
    }

    private fun updateTileState(isActive: Boolean, duration: Int) {
        qsTile?.apply {
            state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (isActive) "${duration}m" else "Off"
            updateTile()
        }
    }
}
