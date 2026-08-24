package com.rulecough.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rulecough.app.audio.AudioIO
import com.rulecough.app.audio.OnDeviceClassifier
import com.rulecough.app.audio.WavRecorder
import com.rulecough.app.data.HistoryEntry
import com.rulecough.app.data.HistoryRepository
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
    data class Success(val result: PredictResponse, val audioPath: String?) : UiState
    data class Error(val message: String) : UiState
}

enum class ConnStatus { UNKNOWN, CHECKING, OK, NO_MODEL, FAIL }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val recorder = WavRecorder()
    private val historyRepo = HistoryRepository(app)
    private val clipsDir = File(app.filesDir, "clips").apply { mkdirs() }

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    var serverUrl by mutableStateOf(prefs.serverUrl)
        private set

    var connStatus by mutableStateOf(ConnStatus.UNKNOWN)
        private set

    var onDevice by mutableStateOf(prefs.onDevice)
        private set

    var themeMode by mutableStateOf(prefs.themeMode)
        private set

    var history by mutableStateOf(historyRepo.all())
        private set

    private var timerJob: Job? = null
    private val maxSeconds = 5
    private var currentAudioPath: String? = null

    // on-device model is loaded lazily on first use (off the main thread)
    private var classifier: OnDeviceClassifier? = null
    private var classifierTried = false
    private fun onDeviceClassifier(): OnDeviceClassifier? {
        if (!classifierTried) {
            classifier = OnDeviceClassifier.tryCreate(getApplication<Application>())
            classifierTried = true
        }
        return classifier
    }

    /** Cheap check (does not build the interpreter) for the Settings UI. */
    val onDeviceModelAvailable: Boolean
        get() = try {
            getApplication<Application>().assets.open("rule_cough.tflite").use { true }
        } catch (e: Exception) {
            false
        }

    // -------------------------------------------------------------- settings
    fun setOnDevice(value: Boolean) {
        onDevice = value
        prefs.onDevice = value
    }

    fun setThemeMode(mode: String) {
        themeMode = mode
        prefs.themeMode = mode
    }

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

    // -------------------------------------------------------------- history
    fun openHistory(entry: HistoryEntry) {
        uiState = UiState.Success(historyRepo.parse(entry), entry.audioPath)
    }

    fun clearHistory() {
        historyRepo.clear()
        history = emptyList()
    }

    private fun onResult(result: PredictResponse) {
        val path = currentAudioPath
        val now = System.currentTimeMillis()
        historyRepo.add(result, path, onDevice, now)
        history = historyRepo.all()
        uiState = UiState.Success(result, path)
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
        val pcm = recorder.stopAndGetPcm()
        if (pcm.isEmpty()) {
            uiState = UiState.Error("The recording was empty. Please try again.")
            return
        }
        val clip = File(clipsDir, "clip_${System.currentTimeMillis()}.wav")
        recorder.savePcmToWav(clip, pcm)
        currentAudioPath = clip.absolutePath
        if (onDevice) {
            classifyOnDevice(AudioIO.pcm16ToFloats(pcm))
        } else {
            upload(clip)
        }
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
                val file = withContext(Dispatchers.IO) { copyUriToClips(uri) }
                currentAudioPath = file.absolutePath
                if (onDevice) {
                    val samples = withContext(Dispatchers.IO) {
                        file.inputStream().use { AudioIO.readWav(it) }
                    }
                    if (samples == null) {
                        uiState = UiState.Error(
                            "On-device mode can read WAV files only. Pick a .wav file, " +
                                "record with the app, or switch to Server mode for other formats."
                        )
                    } else {
                        classifyOnDevice(samples)
                    }
                } else {
                    upload(file)
                }
            } catch (e: Exception) {
                uiState = UiState.Error("Could not read that file: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------- on-device
    private fun classifyOnDevice(samples: FloatArray) {
        uiState = UiState.Analyzing
        viewModelScope.launch {
            val clf = withContext(Dispatchers.IO) { onDeviceClassifier() }
            if (clf == null) {
                uiState = UiState.Error(
                    "The on-device model isn't bundled yet.\n\n" +
                        "Add rule_cough.tflite and labels.txt to the app's assets folder " +
                        "(from the notebook), or switch to Server mode in Settings."
                )
                return@launch
            }
            try {
                val res = withContext(Dispatchers.Default) { clf.classify(samples) }
                onResult(res)
            } catch (e: Exception) {
                uiState = UiState.Error("On-device inference failed: ${e.message}")
            }
        }
    }

    private fun copyUriToClips(uri: Uri): File {
        val ctx = getApplication<Application>()
        val name = queryName(uri) ?: "upload.wav"
        val ext = name.substringAfterLast('.', "wav")
        val out = File(clipsDir, "clip_${System.currentTimeMillis()}.$ext")
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
                onResult(res)
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
