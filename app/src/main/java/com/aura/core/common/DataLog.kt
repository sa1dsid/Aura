package com.aura.core.common

import android.util.Log
import com.aura.BuildConfig

private const val TAG = "AuraData"

fun <T> Result<T>.logFailure(source: String): Result<T> = onFailure { error ->
    if (BuildConfig.DEBUG) Log.w(TAG, "$source failed", error)
}
