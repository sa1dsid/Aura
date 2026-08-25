package com.aura.core.geo

import com.aura.core.session.SessionCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLocationSource @Inject constructor() : SessionCache {

    private val _city = MutableStateFlow<String?>(null)

    val city: StateFlow<String?> = _city.asStateFlow()

    override suspend fun clearSession() {
        _city.value = null
    }

    fun remember(city: String?) {
        val known = city?.takeIf { it.isNotBlank() } ?: return
        _city.value = known
    }
}
