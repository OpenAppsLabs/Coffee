package com.openappslabs.coffee

import android.app.Application
import com.openappslabs.coffee.data.CoffeeManager

class CoffeeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoffeeManager.setCoffeeActive(this, false)
    }
}
