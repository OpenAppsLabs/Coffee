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
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "coffee_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "coffee_prefs"))
    }
)

data class CoffeeState(
    val isActive: Boolean = false,
    val duration: Int = 5,
    val endTime: Long = 0L
)

class CoffeeDataStore(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private val IS_ACTIVE = booleanPreferencesKey("is_active")
        private val SELECTED_DURATION = intPreferencesKey("selected_duration")
        private val END_TIME = longPreferencesKey("end_time")
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private const val DEFAULT_DURATION = 5
    }

    private val preferencesFlow: Flow<Preferences> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    val coffeeState: Flow<CoffeeState> = preferencesFlow
        .map { prefs ->
            CoffeeState(
                isActive = prefs[IS_ACTIVE] ?: false,
                duration = prefs[SELECTED_DURATION] ?: DEFAULT_DURATION,
                endTime = prefs[END_TIME] ?: 0L
            )
        }
        .distinctUntilChanged()

    fun observeIsActive(): Flow<Boolean> = coffeeState.map { it.isActive }.distinctUntilChanged()
    fun observeDuration(): Flow<Int> = coffeeState.map { it.duration }.distinctUntilChanged()

    suspend fun setCoffeeStatus(active: Boolean, endTime: Long = 0L) {
        appContext.dataStore.edit { preferences ->
            preferences[IS_ACTIVE] = active
            preferences[END_TIME] = if (active) endTime else 0L
        }
    }

    suspend fun setSelectedDuration(duration: Int) {
        appContext.dataStore.edit { preferences ->
            preferences[SELECTED_DURATION] = duration
        }
    }

    suspend fun getOnboardingComplete(): Boolean = appContext.dataStore.data
        .map { it[ONBOARDING_COMPLETE] ?: false }
        .first()

    suspend fun setOnboardingComplete(complete: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = complete
        }
    }
}