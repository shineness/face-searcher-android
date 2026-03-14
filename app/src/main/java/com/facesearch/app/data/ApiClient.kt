package com.facesearch.app.data

import com.facesearch.app.util.Logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API client singleton
 * Configure base URL in build config or use local hashing
 */
object ApiClient {
    private const val TAG = "ApiClient"
    
    // Change this to your server URL, or leave empty to use local hashing only
    private const val BASE_URL = "" 
    
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Logger.logInfo("API: $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val service: FaceApiService by lazy {
        if (BASE_URL.isEmpty()) {
            Logger.logInfo("No API configured, using local hashing only")
            // Return a mock service when no API is configured
            createMockService()
        } else {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FaceApiService::class.java)
        }
    }
    
    /**
     * Check if API is configured
     */
    fun isApiConfigured(): Boolean = BASE_URL.isNotEmpty()
    
    private fun createMockService(): FaceApiService {
        return object : FaceApiService {
            override suspend fun analyzeFace(request: AnalyzeFaceRequest) = AnalyzeFaceResponse(
                success = false,
                message = "No API configured - using local image hashing"
            )
            
            override suspend fun searchFaces(request: SearchFaceRequest) = SearchFaceResponse(
                success = false,
                results = emptyList(),
                message = "No API configured"
            )
            
            override suspend fun healthCheck() = HealthResponse(
                status = "offline",
                version = "1.0.0"
            )
        }
    }
}
