package com.openappslabs.coffee.services

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import com.openappslabs.coffee.data.CoffeeDataStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            MainScope().launch {
                val isActive = CoffeeDataStore.observeIsActive(context).first()
                if (isActive) {
                    CoffeeDataStore.setCoffeeActive(context, false)
                }
                try {
                    TileService.requestListeningState(
                        context,
                        ComponentName(context, CoffeeTileService::class.java)
                    )
                } catch (ignored: Exception) {
                }
            }
        }
    }
}