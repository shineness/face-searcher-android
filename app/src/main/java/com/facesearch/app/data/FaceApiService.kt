package com.facesearch.app.data

import retrofit2.http.*

/**
 * Face search API service
 * Can be configured to use a backend server for more accurate face recognition
 */
interface FaceApiService {
    
    /**
     * Upload an image and get face embedding/feature vector
     */
    @POST("api/face/analyze")
    suspend fun analyzeFace(
        @Body request: AnalyzeFaceRequest
    ): AnalyzeFaceResponse
    
    /**
     * Search for similar faces in the database
     */
    @POST("api/face/search")
    suspend fun searchFaces(
        @Body request: SearchFaceRequest
    ): SearchFaceResponse
    
    /**
     * Check server health
     */
    @GET("api/health")
    suspend fun healthCheck(): HealthResponse
}

data class AnalyzeFaceRequest(
    val imageUrl: String,
    val userId: String? = null
)

data class AnalyzeFaceResponse(
    val success: Boolean,
    val faceId: String? = null,
    val embedding: List<Float>? = null,
    val message: String? = null
)

data class SearchFaceRequest(
    val embedding: List<Float>,
    val threshold: Float = 0.7f,
    val limit: Int = 10
)

data class SearchFaceResponse(
    val success: Boolean,
    val results: List<FaceSearchResult>,
    val message: String? = null
)

data class FaceSearchResult(
    val id: String,
    val name: String,
    val imageUrl: String,
    val similarity: Float
)

data class HealthResponse(
    val status: String,
    val version: String
)
