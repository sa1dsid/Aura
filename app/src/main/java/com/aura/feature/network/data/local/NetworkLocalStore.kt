package com.aura.feature.network.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aura.feature.network.domain.model.PingRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.networkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "network_log",
)

data class MeasuredQuality(
    val jitterMs: Int,
    val packetLossPercent: Double,
)

@Singleton
class NetworkLocalStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val preferences: Flow<Preferences> = context.networkDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    val history: Flow<List<PingRecord>> = preferences.map { it[RECORDS].decodeRecords() }

    val quality: Flow<MeasuredQuality?> = preferences.map { stored ->
        val jitter = stored[JITTER_MS]
        val loss = stored[PACKET_LOSS]
        if (jitter == null || loss == null) null else MeasuredQuality(jitter, loss)
    }

    suspend fun append(record: PingRecord) {
        context.networkDataStore.edit { stored ->
            stored[RECORDS] = stored[RECORDS].decodeRecords().appendCapped(record).encodeRecords()
        }
    }

    suspend fun saveQuality(quality: MeasuredQuality) {
        context.networkDataStore.edit { stored ->
            stored[JITTER_MS] = quality.jitterMs
            stored[PACKET_LOSS] = quality.packetLossPercent
        }
    }

    private companion object {
        val RECORDS = stringPreferencesKey("records")
        val JITTER_MS = intPreferencesKey("jitter_ms")
        val PACKET_LOSS = doublePreferencesKey("packet_loss")
    }
}
