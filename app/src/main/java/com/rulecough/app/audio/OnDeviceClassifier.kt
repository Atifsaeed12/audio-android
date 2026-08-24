package com.rulecough.app.audio

import android.content.Context
import com.rulecough.app.net.AcousticFeature
import com.rulecough.app.net.ClassProb
import com.rulecough.app.net.PredictResponse
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Runs the baked `rule_cough.tflite` model fully on-device. The model takes raw
 * 16 kHz mono audio and returns class probabilities — all DSP is inside the graph.
 */
class OnDeviceClassifier private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>,
    val numSamples: Int
) {
    fun close() = interpreter.close()

    fun classify(samples: FloatArray): PredictResponse {
        val x = AudioIO.fitLength(samples, numSamples)
        val input = arrayOf(x)                       // [1, numSamples]
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(input, output)
        val probs = output[0]

        val order = probs.indices.sortedByDescending { probs[it] }
        val topIdx = order.first()
        val pred = labels[topIdx]
        val conf = probs[topIdx]
        val (risk, advisory) = riskOf(pred, conf)

        return PredictResponse(
            prediction = pred,
            confidence = conf,
            uncertainty = 0f,
            highUncertainty = false,
            riskLevel = risk,
            advisory = advisory,
            probabilities = order.map { ClassProb(labels[it], probs[it]) },
            acousticFeatures = emptyList<AcousticFeature>(),
            mcPasses = 0,                            // 0 -> "on-device" in the UI
            filename = "on-device"
        )
    }

    private fun riskOf(pred: String, conf: Float): Pair<String, String> {
        val level = when {
            pred.equals("healthy", ignoreCase = true) -> "Low"
            conf >= 0.70f -> "High"
            conf >= 0.45f -> "Moderate"
            else -> "Low"
        }
        val advisory = when (level) {
            "High" -> "High-confidence on-device screening signal. This is a screening " +
                "aid, not a diagnosis — confirm with a clinician."
            "Moderate" -> "Moderate signal — consider a repeat recording or clinical review."
            else -> "Low-risk screening result. Re-test if symptoms persist."
        }
        return level to advisory
    }

    companion object {
        /** Load the model + labels from assets. Returns null if the model asset
         *  isn't bundled yet (so the app can fall back to Server mode gracefully). */
        fun tryCreate(context: Context, modelAsset: String = "rule_cough.tflite",
                      labelAsset: String = "labels.txt"): OnDeviceClassifier? {
            return try {
                val model = loadModelFile(context, modelAsset)
                val opts = Interpreter.Options().apply { setNumThreads(2) }
                val interp = Interpreter(model, opts)
                val n = interp.getInputTensor(0).shape().last()
                val labels = readLabels(context, labelAsset, interp.getOutputTensor(0).shape().last())
                OnDeviceClassifier(interp, labels, n)
            } catch (e: Exception) {
                null
            }
        }

        private fun loadModelFile(context: Context, asset: String): MappedByteBuffer {
            val afd = context.assets.openFd(asset)
            FileInputStream(afd.fileDescriptor).use { fis ->
                val channel = fis.channel
                return channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            }
        }

        private fun readLabels(context: Context, asset: String, fallbackCount: Int): List<String> {
            return try {
                context.assets.open(asset).bufferedReader().readLines()
                    .map { it.trim() }.filter { it.isNotEmpty() }
            } catch (e: Exception) {
                (0 until fallbackCount).map { "Class $it" }
            }
        }
    }
}
