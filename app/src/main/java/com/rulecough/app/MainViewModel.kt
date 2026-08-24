package com.rulecough.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rulecough.app.audio.WavRecorder
import com.rulecough.app.data.Prefs
import com.rulecough.app.net.ApiClient
import com.rulecough.app.net.PredictResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

sealed interface UiState {
    data object Idle : UiState
    data class Recording(val seconds: Int) : UiState
    data object Analyzing : UiState
    data class Success(val result: PredictResponse) : UiState
    data class Error(val message: String) : UiState
}

enum class ConnStatus { UNKNOWN, CHECKING, OK, NO_MODEL, FAIL }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val recorder = WavRecorder()

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    var serverUrl by mutableStateOf(prefs.serverUrl)
        private set

    var connStatus by mutableStateOf(ConnStatus.UNKNOWN)
        private set

    private var timerJob: Job? = null
    private val maxSeconds = 5

    // -------------------------------------------------------------- settings
    fun updateServerUrl(url: String) {
        serverUrl = url
        prefs.serverUrl = url
        connStatus = ConnStatus.UNKNOWN
    }

    fun testConnection() {
        connStatus = ConnStatus.CHECKING
        viewModelScope.launch {
            try {
                val api = ApiClient.create(serverUrl)
                val h = withContext(Dispatchers.IO) { api.health() }
                connStatus = if (h.modelReady) ConnStatus.OK else ConnStatus.NO_MODEL
            } catch (e: Exception) {
                connStatus = ConnStatus.FAIL
            }
        }
    }

    // -------------------------------------------------------------- recording
    fun startRecording() {
        if (uiState is UiState.Recording) return
        recorder.start { msg -> uiState = UiState.Error(msg) }
        if (uiState is UiState.Error) return
        uiState = UiState.Recording(0)
        timerJob = viewModelScope.launch {
            var s = 0
            while (s < maxSeconds && uiState is UiState.Recording) {
                delay(1000)
                s++
                if (uiState is UiState.Recording) uiState = UiState.Recording(s)
            }
            if (uiState is UiState.Recording) stopRecording()
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        val out = File(getApplication<Application>().cacheDir, "recording.wav")
        val saved = recorder.stopAndSave(out)
        if (saved == null) {
            uiState = UiState.Error("The recording was empty. Please try again.")
            return
        }
        upload(saved)
    }

    fun cancelRecording() {
        timerJob?.cancel()
        recorder.cancel()
        uiState = UiState.Idle
    }

    // -------------------------------------------------------------- file pick
    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { copyUriToCache(uri) }
                upload(file)
            } catch (e: Exception) {
                uiState = UiState.Error("Could not read that file: ${e.message}")
            }
        }
    }

    private fun copyUriToCache(uri: Uri): File {
        val ctx = getApplication<Application>()
        val name = queryName(uri) ?: "upload.wav"
        val ext = name.substringAfterLast('.', "wav")
        val out = File(ctx.cacheDir, "upload.$ext")
        ctx.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "empty stream" }
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun queryName(uri: Uri): String? {
        val ctx = getApplication<Application>()
        return try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // -------------------------------------------------------------- inference
    private fun upload(file: File) {
        uiState = UiState.Analyzing
        viewModelScope.launch {
            try {
                val api = ApiClient.create(serverUrl)
                val mime = if (file.extension.lowercase() == "wav") "audio/wav" else "audio/*"
                val body = file.asRequestBody(mime.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, body)
                val res = withContext(Dispatchers.IO) { api.predict(part) }
                uiState = UiState.Success(res)
            } catch (e: Exception) {
                uiState = UiState.Error(
                    "Couldn't reach the model server.\n\n" +
                        "Check that the backend is running and the server URL in " +
                        "Settings is correct.\n\n(${e.message})"
                )
            }
        }
    }

    fun reset() {
        uiState = UiState.Idle
    }
}
