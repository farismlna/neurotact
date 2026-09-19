// network/ApiService.kt

package com.neurotact.coach.network

import retrofit2.http.Body
import retrofit2.http.POST

data class SpeechRequest(
    val text: String
)

data class TacticalResponse(
    val code: String,
    val morse: String,
    val label: String,
    val status: String,
    val message: String
)

interface ApiService {
    @POST("process")
    suspend fun processText(@Body request: SpeechRequest): TacticalResponse
}