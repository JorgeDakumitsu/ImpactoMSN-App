package com.impactomsn.app.calls

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

/** Per-app incoming-call ringtone. Null means use the Android system ringtone. */
internal object CallRingtonePrefs {
    private const val FILE = "call_ringtone_preferences"
    private const val KEY_URI = "incoming_call_ringtone_uri"

    fun getUri(context: Context): Uri? = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        .getString(KEY_URI, null)
        ?.takeIf(String::isNotBlank)
        ?.let(Uri::parse)

    fun setUri(context: Context, uri: Uri?) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_URI, uri?.toString())
            .apply()
    }

    fun resolvedUri(context: Context): Uri? = getUri(context)
        ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
}
