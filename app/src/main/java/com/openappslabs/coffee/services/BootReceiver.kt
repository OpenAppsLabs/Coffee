package com.openappslabs.coffee.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.openappslabs.coffee.repository.CoffeeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var coffeeRepository: CoffeeRepository
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                coffeeRepository.updateStatus(false)
                coffeeRepository.requestTileUpdate()
            } catch (e: Exception) {
            } finally {
                scope.cancel()
                pendingResult.finish()
            }
        }
    }
}