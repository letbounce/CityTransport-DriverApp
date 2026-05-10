package com.example.cityapp.domain.repository

import com.example.cityapp.domain.model.AuthSession
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.domain.model.IncidentUpdatePayload
import com.example.cityapp.domain.model.LiveTripMarker
import com.example.cityapp.domain.model.NewIncidentPayload
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
    suspend fun getWaybill(waybillId: String): Result<Waybill>
    suspend fun listWaybills(): Result<List<Waybill>>
    suspend fun listArchivedWaybills(): Result<List<Waybill>>
    suspend fun startWaybill(routeId: String, vehicleId: String?, notes: String?): Result<Waybill>
    suspend fun updateWaybill(waybillId: String, vehicleId: String?, notes: String?): Result<Waybill>
    suspend fun completeWaybill(waybillId: String): Result<Unit>
    suspend fun archiveWaybill(waybillId: String, reasonCode: String, reasonNote: String?): Result<Unit>
}

interface TripMapRepository {
    suspend fun getLiveTripMarkers(): Result<List<LiveTripMarker>>
}

interface IncidentRepository {
    suspend fun reportIncident(payload: NewIncidentPayload): Result<Unit>

    suspend fun getIncident(incidentId: String): Result<IncidentItem>

    suspend fun updateIncident(incidentId: String, payload: IncidentUpdatePayload): Result<IncidentItem>

    suspend fun listIncidents(): Result<List<IncidentItem>>

    suspend fun listArchivedIncidents(): Result<List<IncidentItem>>

    suspend fun archiveIncident(incidentId: String, reasonCode: String, reasonNote: String?): Result<Unit>
}
