package com.aura.core.network

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aura.R

@Composable
fun NetworkType.label(): String = stringResource(
    when (this) {
        NetworkType.MOBILE_2G -> R.string.network_mobile_2g
        NetworkType.MOBILE_3G -> R.string.network_mobile_3g
        NetworkType.MOBILE_4G -> R.string.network_mobile_4g
        NetworkType.MOBILE_5G -> R.string.network_mobile_5g
        NetworkType.WIFI -> R.string.network_wifi
        NetworkType.NONE -> R.string.network_none
    }
)
