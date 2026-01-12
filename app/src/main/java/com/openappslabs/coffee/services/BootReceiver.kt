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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            val pendingResult = goAsync()

            scope.launch {
                try {
                    val dataStore = CoffeeDataStore(context.applicationContext)
                    val isActive = dataStore.observeIsActive().first()

                    if (isActive) {
                        dataStore.setCoffeeActive(false)
                    }

                    TileService.requestListeningState(
                        context.applicationContext,
                        ComponentName(context, CoffeeTileService::class.java)
                    )
                } catch (e: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}