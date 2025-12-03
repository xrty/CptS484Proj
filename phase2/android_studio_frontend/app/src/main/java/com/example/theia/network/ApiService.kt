package com.example.theia.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

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

data class Hallway(
    val id: Int,
    val name: String,
    val status: String,
    val description: String? = null
)

data class HallwayUpdateRequest(
    val status: String
)

interface ApiService {
    @POST("alerts/fall")
    fun sendFallAlert(@Body alert: FallAlert): Call<Map<String, Any>>

    @POST("manager/login")
    fun managerLogin(@Body request: LoginRequest): Call<LoginResponse>

    @GET("building-map/hallways")
    fun getHallways(): Call<List<Hallway>>

    @PUT("building-map/hallways/{hallwayId}")
    fun updateHallwayStatus(
        @Path("hallwayId") hallwayId: Int,
        @Body request: HallwayUpdateRequest
    ): Call<Hallway>
}
