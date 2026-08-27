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
import com.aura.core.session.SessionCache
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

data class MeasuredMetrics(
    val pingMs: Int?,
    val jitterMs: Int?,
    val packetLossPercent: Double?,
)

@Singleton
class NetworkLocalStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SessionCache {

    private val preferences: Flow<Preferences> = context.networkDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    val history: Flow<List<PingRecord>> = preferences.map { it[RECORDS].decodeRecords() }

    val measured: Flow<MeasuredMetrics> = preferences.map { stored ->
        MeasuredMetrics(
            pingMs = stored[PING_MS],
            jitterMs = stored[JITTER_MS],
            packetLossPercent = stored[PACKET_LOSS],
        )
    }

    override suspend fun clearSession() {
        context.networkDataStore.edit { stored ->
            stored.remove(RECORDS)
            stored.remove(PING_MS)
            stored.remove(JITTER_MS)
            stored.remove(PACKET_LOSS)
        }
    }

    suspend fun append(record: PingRecord) {
        context.networkDataStore.edit { stored ->
            stored[RECORDS] = stored[RECORDS].decodeRecords().appendCapped(record).encodeRecords()
        }
    }

    suspend fun replaceAll(records: List<PingRecord>) {
        context.networkDataStore.edit { stored ->
            stored[RECORDS] = records.encodeRecords()
        }
    }

    suspend fun savePing(pingMs: Int) {
        context.networkDataStore.edit { stored -> stored[PING_MS] = pingMs }
    }

    suspend fun saveSpeedTest(pingMs: Int, jitterMs: Int, packetLossPercent: Double) {
        context.networkDataStore.edit { stored ->
            stored[PING_MS] = pingMs
            stored[JITTER_MS] = jitterMs
            stored[PACKET_LOSS] = packetLossPercent
        }
    }

    private companion object {
        val RECORDS = stringPreferencesKey("records")
        val PING_MS = intPreferencesKey("ping_ms")
        val JITTER_MS = intPreferencesKey("jitter_ms")
        val PACKET_LOSS = doublePreferencesKey("packet_loss")
    }
}
