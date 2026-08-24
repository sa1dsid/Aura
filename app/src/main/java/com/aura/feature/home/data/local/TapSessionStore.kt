package com.aura.feature.home.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tapSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "spark_window",
)

interface TapSessionStore {

    suspend fun rate(): Int

    suspend fun saveRate(rate: Int)

    suspend fun pendingSessionId(): String?

    suspend fun savePendingSessionId(sessionId: String)

    suspend fun clearPendingSessionId()
}

@Singleton
class DataStoreTapSessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TapSessionStore {

    override suspend fun rate(): Int = read()[RATE] ?: 0

    override suspend fun saveRate(rate: Int) {
        context.tapSessionDataStore.edit { stored -> stored[RATE] = rate }
    }

    override suspend fun pendingSessionId(): String? = read()[SESSION_ID]

    override suspend fun savePendingSessionId(sessionId: String) {
        context.tapSessionDataStore.edit { stored -> stored[SESSION_ID] = sessionId }
    }

    override suspend fun clearPendingSessionId() {
        context.tapSessionDataStore.edit { stored -> stored.remove(SESSION_ID) }
    }

    private suspend fun read(): Preferences = context.tapSessionDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .first()

    private companion object {
        val RATE = intPreferencesKey("window_rate")
        val SESSION_ID = stringPreferencesKey("pending_session_id")
    }
}
