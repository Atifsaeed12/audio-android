package com.rulecough.app.net

import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @GET("health")
    suspend fun health(): HealthResponse

    @Multipart
    @POST("predict")
    suspend fun predict(@Part file: MultipartBody.Part): PredictResponse
}
