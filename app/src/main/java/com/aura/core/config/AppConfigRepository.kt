package com.aura.core.config

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.PublicConfigDto
import com.aura.core.api.dto.SocialLinkDto
import com.aura.core.common.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigRepository @Inject constructor(
    private val api: AuraApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private val mutex = Mutex()
    private var loaded = false

    suspend fun refresh(force: Boolean = false) {
        mutex.withLock {
            if (loaded && !force) return
            val loadedConfig = withContext(ioDispatcher) {
                try {
                    api.publicConfig().toDomain()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    null
                }
            } ?: return
            _config.value = loadedConfig
            loaded = true
        }
    }
}

private fun PublicConfigDto.toDomain(): AppConfig = AppConfig(
    termsUrl = termsUrl,
    privacyUrl = privacyUrl,
    featureFlags = FeatureFlags(
        dataShare = featureFlags.dataShare,
        trafficWithdrawals = featureFlags.trafficWithdrawals,
        vpnCode = featureFlags.vpnCode,
    ),
    socialLinks = socialLinks.mapNotNull(SocialLinkDto::toDomain),
)

private fun SocialLinkDto.toDomain(): SocialLink? {
    val network = SocialNetwork.entries.firstOrNull { it.name == kind.uppercase() } ?: return null
    return SocialLink(network = network, url = url)
}
