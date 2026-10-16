package com.smsmurilo.telecom.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smsmurilo.telecom.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {

    private val thresholdKey = intPreferencesKey("alert_threshold")
    internal val lastAlertKey = intPreferencesKey("last_alerted_percent")

    override val alertThreshold: Flow<Int> = context.dataStore.data.map { it[thresholdKey] ?: 80 }

    override suspend fun setAlertThreshold(percent: Int) {
        context.dataStore.edit { it[thresholdKey] = percent.coerceIn(50, 95) }
    }

    val lastAlertedPercent: Flow<Int?> = context.dataStore.data.map { it[lastAlertKey] }

    suspend fun setLastAlertedPercent(percent: Int?) {
        context.dataStore.edit { prefs -> if (percent == null) prefs.remove(lastAlertKey) else prefs[lastAlertKey] = percent }
    }
}
