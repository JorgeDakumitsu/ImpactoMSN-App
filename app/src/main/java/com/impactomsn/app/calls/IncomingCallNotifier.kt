package com.impactomsn.app.calls

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.impactomsn.app.R

/**
 * Shows the incoming-call UI and rings like a phone call: the device ringtone loops and the
 * phone vibrates until the call is answered, declined, missed or cancelled (max 45 s).
 * The ringer mode is respected (silent = no sound/vibration, vibrate = vibration only).
 */
object IncomingCallNotifier {
    // New channel id: sound/importance of an existing channel cannot be changed after creation.
    private const val CHANNEL = "incoming_calls_ring"
    private const val OLD_CHANNEL = "incoming_calls"
    private const val ID = 7001
    private const val RING_TIMEOUT_MS = 45_000L
    private val VIBRATION = longArrayOf(0, 1000, 1000)

    private val main = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null
    private val stopRunnable = Runnable { stopRinging() }

    fun show(context: Context, peer: String, video: Boolean) {
        val app = context.applicationContext
        main.post { startRinging(app) }
        postNotification(app, peer, video)
    }

    private fun postNotification(context: Context, peer: String, video: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel(OLD_CHANNEL)
            // Silent channel: the ringtone and vibration are driven by startRinging() so they loop
            // and follow the ringer mode instead of playing once like a normal notification.
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.call_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.call_channel_description)
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                },
            )
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(context, 1, CallActivity.incomingIntent(context, null), flags)
        val accept = PendingIntent.getActivity(context, 2, CallActivity.incomingIntent(context, CallActivity.ACTION_ACCEPT), flags)
        val decline = PendingIntent.getActivity(context, 3, CallActivity.incomingIntent(context, CallActivity.ACTION_DECLINE), flags)
        val text = context.getString(if (video) R.string.call_incoming_video else R.string.call_incoming_voice)
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_impactomsn)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(RING_TIMEOUT_MS)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Native call notification (big Answer / Decline buttons), same look as a phone call.
            val caller = Person.Builder().setName(peer).setImportant(true).build()
            builder.setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, decline, accept))
        } else {
            builder.setContentTitle(peer).setContentText(text)
                .addAction(0, context.getString(R.string.call_decline), decline)
                .addAction(0, context.getString(R.string.call_answer), accept)
        }
        try { NotificationManagerCompat.from(context).notify(ID, builder.build()) } catch (_: SecurityException) {}
    }

    private fun startRinging(context: Context) {
        stopRinging()
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mode = audio.ringerMode
        if (mode == AudioManager.RINGER_MODE_NORMAL) {
            try {
                val uri = CallRingtonePrefs.resolvedUri(context)
                ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                    play()
                }
            } catch (_: Exception) { ringtone = null }
        }
        if (mode != AudioManager.RINGER_MODE_SILENT) vibrator(context)?.let { v ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createWaveform(VIBRATION, 0))
                else @Suppress("DEPRECATION") v.vibrate(VIBRATION, 0)
            } catch (_: Exception) {}
        }
        appContext = context
        main.removeCallbacks(stopRunnable)
        main.postDelayed(stopRunnable, RING_TIMEOUT_MS)
    }

    private var appContext: Context? = null

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private fun stopRinging() {
        main.removeCallbacks(stopRunnable)
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
        appContext?.let { vibrator(it)?.cancel() }
    }

    fun cancel(context: Context) {
        main.post { stopRinging() }
        NotificationManagerCompat.from(context).cancel(ID)
    }
}
