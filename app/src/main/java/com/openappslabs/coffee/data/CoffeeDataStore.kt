package com.openappslabs.coffee.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "coffee_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "coffee_prefs"))
    }
)

class CoffeeDataStore(private val context: Context) {

    companion object {
        val TIME_OPTIONS = listOf(5, 15, 30, 45, 60, 0)
        private val IS_ACTIVE = booleanPreferencesKey("is_active")
        private val SELECTED_DURATION = intPreferencesKey("selected_duration")
        private val START_TIME = longPreferencesKey("start_time")
        private val END_TIME = longPreferencesKey("end_time")

        private const val DEFAULT_DURATION = 5
    }

    private val preferencesFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    fun observeIsActive(): Flow<Boolean> = preferencesFlow
        .map { it[IS_ACTIVE] ?: false }
        .distinctUntilChanged()

    fun observeDuration(): Flow<Int> = preferencesFlow
        .map { it[SELECTED_DURATION] ?: DEFAULT_DURATION }
        .distinctUntilChanged()

    fun observeEndTime(): Flow<Long> = preferencesFlow
        .map { it[END_TIME] ?: 0L }
        .distinctUntilChanged()

    suspend fun setCoffeeStatus(active: Boolean, startTime: Long = 0L, endTime: Long = 0L) {
        context.dataStore.edit { preferences ->
            preferences[IS_ACTIVE] = active
            if (active) {
                preferences[START_TIME] = startTime
                preferences[END_TIME] = endTime
            } else {
                preferences[START_TIME] = 0L
                preferences[END_TIME] = 0L
            }
        }
    }

    suspend fun setSelectedDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_DURATION] = duration
        }
    }

    suspend fun setCoffeeActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ACTIVE] = active
            if (!active) {
                preferences[START_TIME] = 0L
                preferences[END_TIME] = 0L
            }
        }
    }
}