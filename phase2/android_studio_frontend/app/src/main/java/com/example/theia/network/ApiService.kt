package com.example.theia.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class FallAlert(
    val user_id: Int,
    val latitude: Double,
    val longitude: Double,
    val source: String = "android_app"
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean
)

interface ApiService {
    @POST("alerts/fall")
    fun sendFallAlert(@Body alert: FallAlert): Call<Map<String, Any>>

    @POST("manager/login")
    fun managerLogin(@Body request: LoginRequest): Call<LoginResponse>
}
