package com.openappslabs.coffee.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "coffee_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "coffee_prefs"))
    }
)

object CoffeeDataStore {
    val TIME_OPTIONS = listOf(5, 15, 30, 45, 60, 0)
    private val IS_ACTIVE = booleanPreferencesKey("is_active")
    private val SELECTED_DURATION = intPreferencesKey("selected_duration")
    private val TILE_ADDED = booleanPreferencesKey("tile_added")

    fun observeIsActive(context: Context): Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[IS_ACTIVE] ?: false }

    fun observeDuration(context: Context): Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[SELECTED_DURATION] ?: 5 }

    suspend fun setCoffeeActive(context: Context, active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ACTIVE] = active
        }
    }

    suspend fun setSelectedDuration(context: Context, duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_DURATION] = duration
        }
    }
}