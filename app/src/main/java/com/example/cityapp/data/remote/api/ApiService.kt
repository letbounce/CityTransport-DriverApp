package com.example.cityapp.data.remote.api

import com.example.cityapp.data.remote.dto.IncidentRequestDto
import com.example.cityapp.data.remote.dto.IncidentResponseDto
import com.example.cityapp.data.remote.dto.LoginRequestDto
import com.example.cityapp.data.remote.dto.LoginResponseDto
import com.example.cityapp.data.remote.dto.RouteDto
import com.example.cityapp.data.remote.dto.VehicleDto
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

    @GET("api/vehicles")
    suspend fun getVehicles(): List<VehicleDto>

    @GET("api/waybills")
    suspend fun listWaybills(): List<WaybillDto>

    @POST("api/waybills")
    suspend fun startWaybill(@Body request: StartWaybillRequestDto): WaybillDto

    @GET("api/waybills/active")
    suspend fun getActiveWaybill(): WaybillDto

    @PATCH("api/waybills/{id}")
    suspend fun updateWaybill(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): WaybillDto

    @PATCH("api/waybills/{id}/complete")
    suspend fun completeWaybill(@Path("id") id: String): WaybillDto

    @GET("api/incidents")
    suspend fun listIncidents(): List<IncidentResponseDto>

    @POST("api/incidents")
    suspend fun reportIncident(@Body request: IncidentRequestDto): IncidentResponseDto
}
