package com.rulecough.app.audio

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Helpers to turn 16-bit PCM / WAV data into the float sample array the
 *  on-device model expects (mono, [-1, 1]). */
object AudioIO {

    /** Convert little-endian 16-bit PCM bytes to float samples in [-1, 1]. */
    fun pcm16ToFloats(pcm: ByteArray): FloatArray {
        val n = pcm.size / 2
        val out = FloatArray(n)
        val bb = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until n) out[i] = bb.short.toFloat() / 32768f
        return out
    }

    /**
     * Minimal WAV reader for a picked file: returns mono float samples at the
     * file's own sample rate (the model front-end is fixed at 16 kHz, so a file
     * recorded at another rate is naively used as-is; recordings from this app
     * are already 16 kHz). Returns null if it isn't a PCM WAV.
     */
    fun readWav(input: InputStream): FloatArray? {
        val bytes = input.readBytes()
        if (bytes.size < 44) return null
        if (String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") return null
        // find 'data' chunk
        var i = 12
        var dataOffset = -1
        var dataLen = 0
        var channels = 1
        var bits = 16
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        while (i + 8 <= bytes.size) {
            val id = String(bytes, i, 4)
            val sz = bb.getInt(i + 4)
            if (id == "fmt ") {
                channels = bb.getShort(i + 10).toInt()
                bits = bb.getShort(i + 22).toInt()
            } else if (id == "data") {
                dataOffset = i + 8; dataLen = sz; break
            }
            i += 8 + sz + (sz and 1)
        }
        if (dataOffset < 0 || bits != 16) return null
        val end = minOf(dataOffset + dataLen, bytes.size)
        val pcm = bytes.copyOfRange(dataOffset, end)
        val interleaved = pcm16ToFloats(pcm)
        if (channels <= 1) return interleaved
        // down-mix to mono
        val n = interleaved.size / channels
        val mono = FloatArray(n)
        for (k in 0 until n) {
            var s = 0f
            for (c in 0 until channels) s += interleaved[k * channels + c]
            mono[k] = s / channels
        }
        return mono
    }

    /** Pad with zeros or centre-crop to exactly `target` samples. */
    fun fitLength(x: FloatArray, target: Int): FloatArray {
        if (x.size == target) return x
        if (x.size > target) {
            val start = (x.size - target) / 2
            return x.copyOfRange(start, start + target)
        }
        val out = FloatArray(target)
        System.arraycopy(x, 0, out, 0, x.size)
        return out
    }
}
