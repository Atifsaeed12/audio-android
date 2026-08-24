package com.rulecough.app.net

import com.google.gson.annotations.SerializedName

data class ClassProb(
    @SerializedName("label") val label: String,
    @SerializedName("prob") val prob: Float
)

data class AcousticFeature(
    @SerializedName("name") val name: String,
    @SerializedName("value") val value: Float
)

data class PredictResponse(
    @SerializedName("prediction") val prediction: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("uncertainty") val uncertainty: Float,
    @SerializedName("high_uncertainty") val highUncertainty: Boolean,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("advisory") val advisory: String,
    @SerializedName("probabilities") val probabilities: List<ClassProb>,
    @SerializedName("acoustic_features") val acousticFeatures: List<AcousticFeature>,
    @SerializedName("mc_passes") val mcPasses: Int = 0,
    @SerializedName("filename") val filename: String? = null
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("model_ready") val modelReady: Boolean
)
