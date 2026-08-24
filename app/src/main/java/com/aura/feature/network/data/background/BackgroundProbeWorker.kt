package com.aura.feature.network.data.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aura.core.auth.TokenStore
import com.aura.core.network.NetworkMonitor
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.repository.PingHistoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

private const val WORK_NAME = "aura-background-probe"

private val PROBE_INTERVAL = 12.hours

@HiltWorker
class BackgroundProbeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val pingHistory: PingHistoryRepository,
    private val networkMonitor: NetworkMonitor,
    private val tokenStore: TokenStore,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val status = networkMonitor.current()
        if (!status.isOnline || status.isVpnActive) return Result.success()
        if (tokenStore.token() == null) return Result.success()

        return try {
            pingHistory.recordProbe(PingSource.BACKGROUND)
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.retry()
        }
    }
}

@Singleton
class BackgroundProbeScheduler @Inject constructor(
    private val workManager: WorkManager,
) {

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<BackgroundProbeWorker>(
            PROBE_INTERVAL.toJavaDuration()
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
