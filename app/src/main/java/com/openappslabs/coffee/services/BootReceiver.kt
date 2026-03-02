package com.openappslabs.coffee.services

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import com.openappslabs.coffee.data.CoffeeDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val dataStore = CoffeeDataStore(context.applicationContext)
                val isActive = dataStore.observeIsActive().first()

                if (isActive) {
                    dataStore.setCoffeeStatus(false)
                }

                TileService.requestListeningState(
                    context.applicationContext,
                    ComponentName(context, CoffeeTileService::class.java)
                )
            } catch (e: Exception) {
            } finally {
                scope.cancel()
                pendingResult.finish()
            }
        }
    }
}