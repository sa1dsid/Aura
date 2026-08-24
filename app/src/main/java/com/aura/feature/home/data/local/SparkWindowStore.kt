package com.aura.feature.home.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sparkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "spark_window",
)

@Singleton
class SparkWindowStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    suspend fun rate(): Int = context.sparkDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .first()[RATE] ?: 0

    suspend fun saveRate(rate: Int) {
        context.sparkDataStore.edit { stored -> stored[RATE] = rate }
    }

    private companion object {
        val RATE = intPreferencesKey("window_rate")
    }
}
