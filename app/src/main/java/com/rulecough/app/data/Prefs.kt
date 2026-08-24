package com.rulecough.app.data

import android.content.Context

/** Tiny SharedPreferences wrapper for the backend URL. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("rulecough", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sp.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = sp.edit().putString(KEY_URL, value).apply()

    companion object {
        private const val KEY_URL = "server_url"
        // 10.0.2.2 is the host machine as seen from the Android emulator.
        const val DEFAULT_URL = "http://10.0.2.2:8000"
    }
}
