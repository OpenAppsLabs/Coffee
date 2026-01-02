package com.openappslabs.coffee.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

object CoffeeManager {
    private const val PREFS_NAME = "coffee_prefs"
    private const val KEY_IS_ACTIVE = "is_active"
    private const val KEY_DURATION = "selected_duration"
    private const val DEFAULT_DURATION = 5

    val TIME_OPTIONS = listOf(5, 15, 30, 45, 60, 0)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isCoffeeActive(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_IS_ACTIVE, false)

    fun getSelectedDuration(context: Context): Int =
        getPrefs(context).getInt(KEY_DURATION, DEFAULT_DURATION)

    fun setCoffeeActive(context: Context, active: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_IS_ACTIVE, active) }
    }

    fun setSelectedDuration(context: Context, duration: Int) {
        getPrefs(context).edit { putInt(KEY_DURATION, duration) }
    }

    private fun <T> observeKey(
        context: Context,
        key: String,
        defaultValue: T,
        getter: (SharedPreferences, String, T) -> T
    ): Flow<T> = callbackFlow {
        val prefs = getPrefs(context)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, changedKey ->
            if (key == changedKey) {
                trySend(getter(p, key, defaultValue))
            }
        }

        trySend(getter(prefs, key, defaultValue))

        prefs.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Main)

    fun observeIsActive(context: Context): Flow<Boolean> =
        observeKey(context, KEY_IS_ACTIVE, false) { p, k, d -> p.getBoolean(k, d) }

    fun observeDuration(context: Context): Flow<Int> =
        observeKey(context, KEY_DURATION, DEFAULT_DURATION) { p, k, d -> p.getInt(k, d) }
}