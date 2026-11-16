package com.impactomsn.app.notifications

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.impactomsn.app.BuildConfig

object BackgroundConnectionController {
    fun startIfEnabled(context: Context) {
        if (!BackgroundDeliveryPrefs.keepsConnectionAlive(context)) return
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MessengerConnectionService::class.java),
            )
        } catch (error: RuntimeException) {
            if (BuildConfig.DEBUG) {
                Log.w("ImpactoMsnBackground", "Could not start background connection", error)
            }
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, MessengerConnectionService::class.java))
    }
}
