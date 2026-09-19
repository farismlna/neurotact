package com.neurotact.coach.network

import retrofit2.http.Body
import retrofit2.http.POST

data class SpeechRequest(
    val text: String
)

data class TacticalItem(
    val code      : String,
    val label     : String,
    val timestamp : Long,
    val delay_ms  : Long
)

data class ProcessResponse(
    val results : List<TacticalItem>,
    val status  : String,
    val message : String
)

interface ApiService {
    @POST("process")
    suspend fun processText(@Body request: SpeechRequest): ProcessResponse
}