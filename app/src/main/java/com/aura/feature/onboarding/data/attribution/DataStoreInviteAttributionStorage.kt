package com.aura.feature.onboarding.data.attribution

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aura.core.common.IoDispatcher
import com.aura.core.common.runCatchingCancellable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.inviteAttributionDataStore by preferencesDataStore(name = "aura_invite")

private val DEEP_LINK_CODE = stringPreferencesKey("deep_link_code")

private val REFERRER_CODE = stringPreferencesKey("referrer_code")

private val REFERRER_READ = booleanPreferencesKey("referrer_read")

private val CONSUMED = booleanPreferencesKey("consumed")

@Singleton
class DataStoreInviteAttributionStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InviteAttributionStorage {

    override suspend fun read(): InviteAttributionState = withContext(ioDispatcher) {
        runCatchingCancellable {
            val preferences = context.inviteAttributionDataStore.data.first()
            InviteAttributionState(
                deepLinkCode = preferences[DEEP_LINK_CODE],
                referrerCode = preferences[REFERRER_CODE],
                referrerRead = preferences[REFERRER_READ] == true,
                consumed = preferences[CONSUMED] == true,
            )
        }.getOrDefault(InviteAttributionState())
    }

    override suspend fun write(state: InviteAttributionState) {
        withContext(ioDispatcher) {
            runCatchingCancellable {
                context.inviteAttributionDataStore.edit { preferences ->
                    preferences.put(DEEP_LINK_CODE, state.deepLinkCode)
                    preferences.put(REFERRER_CODE, state.referrerCode)
                    preferences[REFERRER_READ] = state.referrerRead
                    preferences[CONSUMED] = state.consumed
                }
            }
        }
    }
}

private fun MutablePreferences.put(key: Preferences.Key<String>, value: String?) {
    if (value == null) remove(key) else set(key, value)
}
