package com.example.cityapp.data.remote.api

import com.example.cityapp.data.remote.dto.IncidentRequestDto
import com.example.cityapp.data.remote.dto.LoginRequestDto
import com.example.cityapp.data.remote.dto.LoginResponseDto
import com.example.cityapp.data.remote.dto.RouteDto
import com.example.cityapp.data.remote.dto.StartWaybillRequestDto
import com.example.cityapp.data.remote.dto.WaybillDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @GET("api/routes")
    suspend fun getRoutes(): List<RouteDto>

    @POST("api/waybills")
    suspend fun startWaybill(@Body request: StartWaybillRequestDto): WaybillDto

    @GET("api/waybills/active")
    suspend fun getActiveWaybill(): WaybillDto

    @PATCH("api/waybills/{id}/complete")
    suspend fun completeWaybill(@Path("id") id: String): WaybillDto

    @POST("api/incidents")
    suspend fun reportIncident(@Body request: IncidentRequestDto)
}
