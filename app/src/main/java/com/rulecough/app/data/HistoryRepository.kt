package com.rulecough.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rulecough.app.net.PredictResponse

/** One past analysis, persisted so History survives app restarts. */
data class HistoryEntry(
    val id: Long,
    val timestamp: Long,
    val prediction: String,
    val confidence: Float,
    val riskLevel: String,
    val onDevice: Boolean,
    val audioPath: String?,
    val resultJson: String        // full PredictResponse, to re-open the result
)

/** Simple SharedPreferences + Gson store for the History tab. */
class HistoryRepository(context: Context) {
    private val sp = context.getSharedPreferences("rulecough_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun all(): List<HistoryEntry> {
        val raw = sp.getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryEntry>>() {}.type
            gson.fromJson<List<HistoryEntry>>(raw, type).sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(result: PredictResponse, audioPath: String?, onDevice: Boolean, now: Long) {
        val entry = HistoryEntry(
            id = now, timestamp = now,
            prediction = result.prediction, confidence = result.confidence,
            riskLevel = result.riskLevel, onDevice = onDevice,
            audioPath = audioPath, resultJson = gson.toJson(result)
        )
        val list = all().toMutableList()
        list.add(0, entry)
        while (list.size > MAX) list.removeAt(list.size - 1)
        sp.edit().putString(KEY, gson.toJson(list)).apply()
    }

    fun parse(entry: HistoryEntry): PredictResponse =
        gson.fromJson(entry.resultJson, PredictResponse::class.java)

    fun clear() = sp.edit().remove(KEY).apply()

    companion object {
        private const val KEY = "entries"
        private const val MAX = 100
    }
}
