package com.rulecough.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * Records mono 16-bit PCM at 16 kHz using AudioRecord and writes a proper WAV
 * file. WAV is chosen because the backend reads it with no ffmpeg dependency.
 */
class WavRecorder(private val sampleRate: Int = 16000) {

    private var recorder: AudioRecord? = null
    @Volatile private var recording = false
    private var worker: Thread? = null
    private val pcm = ByteArrayOutputStream()

    val isRecording: Boolean get() = recording

    @SuppressLint("MissingPermission")   // RECORD_AUDIO checked before start() is called
    fun start(onError: (String) -> Unit) {
        if (recording) return
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) {
            onError("Audio recording is not supported on this device.")
            return
        }
        val bufSize = minBuf * 2
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            onError("Could not initialise the microphone.")
            rec.release()
            return
        }
        recorder = rec
        pcm.reset()
        recording = true
        rec.startRecording()
        worker = thread(name = "wav-recorder") {
            val buffer = ByteArray(bufSize)
            while (recording) {
                val read = rec.read(buffer, 0, buffer.size)
                if (read > 0) pcm.write(buffer, 0, read)
            }
        }
    }

    /** Stop recording; return the raw 16-bit PCM captured (empty if none). */
    fun stopAndGetPcm(): ByteArray {
        if (!recording) return ByteArray(0)
        recording = false
        try {
            worker?.join(500)
        } catch (_: InterruptedException) {
        }
        recorder?.apply {
            try {
                stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        recorder = null
        return pcm.toByteArray()
    }

    /** Stop recording and write out a .wav file; returns it (or null if empty). */
    fun stopAndSave(outFile: File): File? {
        val data = stopAndGetPcm()
        if (data.isEmpty()) return null
        writeWav(outFile, data, sampleRate)
        return outFile
    }

    /** Write a WAV file from already-captured PCM (used after stopAndGetPcm). */
    fun savePcmToWav(outFile: File, pcmData: ByteArray): File {
        writeWav(outFile, pcmData, sampleRate)
        return outFile
    }

    fun cancel() {
        recording = false
        try {
            worker?.join(500)
        } catch (_: InterruptedException) {
        }
        recorder?.apply {
            try {
                stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        recorder = null
        pcm.reset()
    }

    private fun writeWav(file: File, pcmData: ByteArray, sr: Int) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sr * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataLen = pcmData.size
        val totalLen = 36 + dataLen

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalLen)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)                 // PCM sub-chunk size
            header.putShort(1)                // audio format = PCM
            header.putShort(channels.toShort())
            header.putInt(sr)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray())
            header.putInt(dataLen)
            out.write(header.array())
            out.write(pcmData)
            out.flush()
        }
    }
}
