package com.aura

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aura.core.common.ApplicationScope
import com.aura.feature.home.presentation.components.mesh.MeshMapDefaults
import com.aura.feature.home.presentation.components.mesh.WorldLandmass
import com.aura.feature.network.data.background.BackgroundProbeScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AuraApp : Application(), Configuration.Provider {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var backgroundProbeScheduler: BackgroundProbeScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        backgroundProbeScheduler.schedule()

        applicationScope.launch {
            WorldLandmass.dotGrid(
                context = this@AuraApp,
                projection = MeshMapDefaults.Projection,
                columns = MeshMapDefaults.DOT_COLUMNS,
            )
        }
    }
}
