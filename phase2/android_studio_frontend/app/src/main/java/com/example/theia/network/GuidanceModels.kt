package com.example.theia.network

data class GuidanceRequest(
    val current_location: String,
    val destination: String
)

data class GuidanceStep(
    val order: Int,
    val instruction: String
)

data class GuidanceResponse(
    val summary: String,
    val steps: List<GuidanceStep>
)