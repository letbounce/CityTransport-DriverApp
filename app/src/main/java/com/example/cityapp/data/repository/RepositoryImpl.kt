package com.example.cityapp.data.repository

import com.example.cityapp.data.remote.api.ApiService
import com.example.cityapp.data.remote.dto.IncidentLocationDto
import com.example.cityapp.data.remote.dto.IncidentRequestDto
import com.example.cityapp.data.remote.dto.LoginRequestDto
import com.example.cityapp.data.remote.dto.StartWaybillRequestDto
import com.example.cityapp.data.remote.dto.WaybillDto
import com.example.cityapp.domain.model.AuthSession
import com.example.cityapp.domain.model.Driver
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.domain.model.Route
import com.example.cityapp.domain.model.Stop
import com.example.cityapp.domain.model.Vehicle
import com.example.cityapp.domain.model.Waybill
import com.example.cityapp.domain.repository.AuthRepository
import com.example.cityapp.domain.repository.IncidentRepository
import com.example.cityapp.domain.repository.RouteRepository
import com.example.cityapp.domain.repository.VehicleRepository
import com.example.cityapp.domain.repository.WaybillRepository
import retrofit2.HttpException

private fun WaybillDto.toDomain(): Waybill = Waybill(
    id = _id,
    routeId = route_id,
    routeNumber = route_number,
    status = status,
    vehicleId = vehicle_id ?: "BUS-007",
    notes = notes.orEmpty(),
    startedAt = started_at,
    completedAt = completed_at
)

class AuthRepositoryImpl(
    private val api: ApiService,
    private val sessionStore: SessionStore,
    private val tokenProvider: TokenProvider
) : AuthRepository {
    override suspend fun login(driverId: String, password: String): Result<AuthSession> = runCatching {
        val response = api.login(LoginRequestDto(driver_id = driverId, password = password))
        sessionStore.save(response.token, response.driver.full_name)
        tokenProvider.setToken(response.token)
        AuthSession(
            token = response.token,
            driver = Driver(driverId = response.driver.driver_id, fullName = response.driver.full_name)
        )
    }

    override suspend fun logout() {
        sessionStore.clear()
        tokenProvider.setToken(null)
    }

    override suspend fun getToken(): String? = sessionStore.token()
    override suspend fun getDriverDisplayName(): String? = sessionStore.driverName()
}

class RouteRepositoryImpl(
    private val api: ApiService
) : RouteRepository {
    override suspend fun getRoutes(): Result<List<Route>> = runCatching {
        api.getRoutes().map { dto ->
            Route(
                id = dto._id,
                routeNumber = dto.route_number,
                routeName = dto.route_name,
                stops = dto.stops.map {
                    Stop(it.stop_number, it.name, it.planned_time, it.lat, it.lng)
                }
            )
        }
    }
}

class VehicleRepositoryImpl(
    private val api: ApiService
) : VehicleRepository {
    override suspend fun listVehicles(): Result<List<Vehicle>> = runCatching {
        api.getVehicles().map { dto ->
            Vehicle(
                vehicleId = dto.vehicle_id,
                label = dto.label,
                plateNumber = dto.plate_number.orEmpty()
            )
        }
    }
}

class WaybillRepositoryImpl(
    private val api: ApiService
) : WaybillRepository {
    override suspend fun getActiveWaybill(): Result<Waybill?> =
        try {
            val remote = api.getActiveWaybill()
            Result.success(remote.toDomain())
        } catch (e: HttpException) {
            if (e.code() == 404) Result.success(null)
            else Result.failure(e)
        } catch (e: Throwable) {
            Result.failure(e)
        }

    override suspend fun listWaybills(): Result<List<Waybill>> = runCatching {
        api.listWaybills().map { it.toDomain() }
    }

    override suspend fun startWaybill(
        routeId: String,
        vehicleId: String?,
        notes: String?
    ): Result<Waybill> = runCatching {
        api.startWaybill(
            StartWaybillRequestDto(
                route_id = routeId,
                vehicle_id = vehicleId ?: "BUS-007",
                notes = notes?.takeIf { it.isNotBlank() }
            )
        ).toDomain()
    }

    override suspend fun updateWaybill(
        waybillId: String,
        vehicleId: String?,
        notes: String?
    ): Result<Waybill> = runCatching {
        val payload = buildMap<String, Any?> {
            vehicleId?.takeIf { it.isNotBlank() }?.let { put("vehicle_id", it) }
            notes?.let { put("notes", it) }
        }
        api.updateWaybill(waybillId, payload).toDomain()
    }

    override suspend fun completeWaybill(waybillId: String): Result<Unit> = runCatching {
        api.completeWaybill(waybillId)
    }
}

class IncidentRepositoryImpl(
    private val api: ApiService
) : IncidentRepository {
    override suspend fun reportIncident(
        waybillId: String,
        type: String,
        description: String,
        lat: Double,
        lng: Double
    ): Result<Unit> = runCatching {
        api.reportIncident(
            IncidentRequestDto(
                waybill_id = waybillId,
                type = type,
                description = description,
                location = IncidentLocationDto(lat, lng)
            )
        )
        Unit
    }

    override suspend fun listIncidents(): Result<List<IncidentItem>> = runCatching {
        api.listIncidents().map { dto ->
            IncidentItem(
                id = dto._id,
                waybillId = dto.waybill_id,
                type = dto.type,
                description = dto.description,
                status = dto.status,
                reportedAt = dto.reported_at,
                lat = dto.location.lat,
                lng = dto.location.lng
            )
        }
    }
}
