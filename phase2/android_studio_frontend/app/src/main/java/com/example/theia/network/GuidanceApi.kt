package com.example.theia.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GuidanceApi {

    @POST("guidance/route")
    fun getRoute(
        @Body request: GuidanceRequest
    ): Call<GuidanceResponse>
}