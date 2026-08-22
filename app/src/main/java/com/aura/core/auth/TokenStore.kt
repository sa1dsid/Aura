package com.aura.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore by preferencesDataStore(name = "aura_session")

private val ACCESS_TOKEN = stringPreferencesKey("access_token")

private val EXPIRES_AT = longPreferencesKey("expires_at")

@Singleton
class TokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    @Volatile
    private var cached: String? = null

    @Volatile
    private var loaded = false

    suspend fun token(): String? {
        if (!loaded) {
            val preferences = context.tokenDataStore.data.first()
            val expiresAt = preferences[EXPIRES_AT] ?: 0L
            cached = preferences[ACCESS_TOKEN]?.takeIf { expiresAt > System.currentTimeMillis() }
            loaded = true
        }
        return cached
    }

    fun blockingToken(): String? = cached ?: runBlocking { token() }

    suspend fun save(token: String, expiresInSeconds: Int) {
        cached = token
        loaded = true
        context.tokenDataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = token
            preferences[EXPIRES_AT] =
                System.currentTimeMillis() + expiresInSeconds.toLong() * MILLIS_IN_SECOND
        }
    }

    suspend fun clear() {
        cached = null
        loaded = true
        context.tokenDataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(EXPIRES_AT)
        }
    }

    private companion object {
        const val MILLIS_IN_SECOND = 1_000L
    }
}
