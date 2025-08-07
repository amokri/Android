package com.ahm.mytime

import androidx.work.Constraints
import androidx.work.NetworkType

internal fun createNetworkConstraints(): Constraints {
    return Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}