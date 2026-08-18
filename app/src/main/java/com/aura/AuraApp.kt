package com.aura

import android.app.Application
import com.aura.core.common.ApplicationScope
import com.aura.feature.home.presentation.components.mesh.MeshMapDefaults
import com.aura.feature.home.presentation.components.mesh.WorldLandmass
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AuraApp : Application() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            WorldLandmass.dotGrid(
                context = this@AuraApp,
                projection = MeshMapDefaults.Projection,
                columns = MeshMapDefaults.DOT_COLUMNS,
            )
        }
    }
}
