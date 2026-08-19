package com.openappslabs.coffee.ui.screens.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.repository.CoffeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val dataStore: CoffeeDataStore,
    private val coffeeRepository: CoffeeRepository
) : ViewModel() {

    val coffeeState = dataStore.coffeeState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.openappslabs.coffee.data.CoffeeState()
    )

    fun toggleCoffee(isActive: Boolean, duration: Int) {
        viewModelScope.launch {
            coffeeRepository.updateStatus(isActive)
            coffeeRepository.toggleCoffee(isActive, duration)
        }
    }

    fun setAlternateMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setAlternateMode(enabled)
        }
    }
    
    fun setSelectedDuration(duration: Int) {
        viewModelScope.launch {
            dataStore.setSelectedDuration(duration)
        }
    }
}
