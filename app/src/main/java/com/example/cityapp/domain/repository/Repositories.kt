package com.example.cityapp.domain.repository

import com.example.cityapp.domain.model.AuthSession
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.domain.model.Route
import com.example.cityapp.domain.model.Vehicle
import com.example.cityapp.domain.model.Waybill

interface AuthRepository {
    suspend fun login(driverId: String, password: String): Result<AuthSession>
    suspend fun logout()
    suspend fun getToken(): String?
    suspend fun getDriverDisplayName(): String?
}

interface RouteRepository {
    suspend fun getRoutes(): Result<List<Route>>
}

interface VehicleRepository {
    suspend fun listVehicles(): Result<List<Vehicle>>
}

interface WaybillRepository {
    suspend fun getActiveWaybill(): Result<Waybill?>
    suspend fun listWaybills(): Result<List<Waybill>>
    suspend fun startWaybill(routeId: String, vehicleId: String?, notes: String?): Result<Waybill>
    suspend fun updateWaybill(waybillId: String, vehicleId: String?, notes: String?): Result<Waybill>
    suspend fun completeWaybill(waybillId: String): Result<Unit>
}

interface IncidentRepository {
    suspend fun reportIncident(
        waybillId: String,
        type: String,
        description: String,
        lat: Double,
        lng: Double
    ): Result<Unit>

    suspend fun listIncidents(): Result<List<IncidentItem>>
}
