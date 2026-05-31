package com.example.cityapp.data.repository

import com.example.cityapp.data.remote.ApiStatics
import com.example.cityapp.data.remote.api.ApiService
import com.example.cityapp.data.remote.dto.ArchiveReasonDto
import com.example.cityapp.data.remote.dto.IncidentLocationDto
import com.example.cityapp.data.remote.dto.IncidentRequestDto
import com.example.cityapp.data.remote.dto.IncidentResponseDto
import com.example.cityapp.data.remote.dto.LoginRequestDto
import com.example.cityapp.data.remote.dto.StartWaybillRequestDto
import com.example.cityapp.data.remote.dto.WaybillDto
import com.example.cityapp.domain.model.AuthSession
import com.example.cityapp.domain.model.Driver
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.domain.model.IncidentUpdatePayload
import com.example.cityapp.domain.model.LiveTripMarker
import com.example.cityapp.domain.model.NewIncidentPayload
import com.example.cityapp.domain.model.Route
import com.example.cityapp.domain.model.Stop
import com.example.cityapp.domain.model.Vehicle
import com.example.cityapp.domain.model.Waybill
import com.example.cityapp.domain.repository.AuthRepository
import com.example.cityapp.domain.repository.IncidentRepository
import com.example.cityapp.domain.repository.RouteRepository
import com.example.cityapp.domain.repository.TripMapRepository
import com.example.cityapp.domain.repository.VehicleRepository
import com.example.cityapp.domain.repository.WaybillRepository
import retrofit2.HttpException

private fun IncidentResponseDto.toIncidentItem(): IncidentItem {
    val vcount = version_history?.size ?: 0
    return IncidentItem(
        id = _id,
        waybillId = waybill_id,
        type = type,
        description = description,
        status = status,
        reportedAt = reported_at,
        lat = location.lat,
        lng = location.lng,
        deletedAt = deleted_at,
        stopLabel = stop_label.orEmpty(),
        canMoveIndependently = can_move_independently == true,
        photoUrl = ApiStatics.resolveMediaUrl(photo_url),
        isModified = is_modified == true,
        lastEditedAt = last_edited_at,
        versionHistoryCount = vcount,
        deletionReasonCode = deletion_reason_code,
        deletionReasonNote = deletion_reason_note
    )
}

private fun WaybillDto.toDomain(): Waybill = Waybill(
    id = _id,
    routeId = route_id,
    routeNumber = route_number,
    status = status,
    vehicleId = vehicle_id ?: "KP-3204",
    notes = notes.orEmpty(),
    startedAt = started_at,
    completedAt = completed_at,
    deletedAt = deleted_at,
    deletionReasonCode = deletion_reason_code,
    deletionReasonNote = deletion_reason_note
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

    override suspend fun getWaybill(waybillId: String): Result<Waybill> = runCatching {
        api.getWaybill(waybillId).toDomain()
    }

    override suspend fun listWaybills(): Result<List<Waybill>> = runCatching {
        api.listWaybills().map { it.toDomain() }
    }

    override suspend fun listArchivedWaybills(): Result<List<Waybill>> = runCatching {
        api.listArchivedWaybills().map { it.toDomain() }
    }

    override suspend fun startWaybill(
        routeId: String,
        vehicleId: String?,
        notes: String?
    ): Result<Waybill> = runCatching {
        api.startWaybill(
            StartWaybillRequestDto(
                route_id = routeId,
                vehicle_id = vehicleId ?: "KP-3204",
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

    override suspend fun archiveWaybill(waybillId: String, reasonCode: String, reasonNote: String?): Result<Unit> =
        runCatching {
            api.archiveWaybill(
                waybillId,
                ArchiveReasonDto(reason_code = reasonCode, reason_note = reasonNote)
            )
            Unit
        }
}

class TripMapRepositoryImpl(
    private val api: ApiService
) : TripMapRepository {
    override suspend fun getLiveTripMarkers(): Result<List<LiveTripMarker>> = runCatching {
        api.getLiveTripMarkers().markers.orEmpty().map { dto ->
            LiveTripMarker(
                waybillId = dto.waybill_id,
                driverId = dto.driver_id,
                routeNumber = dto.route_number,
                lat = dto.lat,
                lng = dto.lng,
                updatedAtIso = dto.updated_at,
                isSelf = dto.is_self
            )
        }
    }
}

class IncidentRepositoryImpl(
    private val api: ApiService
) : IncidentRepository {
    override suspend fun reportIncident(payload: NewIncidentPayload): Result<Unit> = runCatching {
        api.reportIncident(
            IncidentRequestDto(
                waybill_id = payload.waybillId,
                type = payload.type,
                description = payload.description,
                location = IncidentLocationDto(payload.lat, payload.lng),
                reported_at = payload.reportedAtIso,
                stop_label = payload.stopLabel?.takeIf { it.isNotBlank() },
                can_move_independently = payload.canMoveIndependently,
                photo_base64 = payload.photoBase64
            )
        )
        Unit
    }

    override suspend fun getIncident(incidentId: String): Result<IncidentItem> = runCatching {
        api.getIncident(incidentId).toIncidentItem()
    }

    override suspend fun updateIncident(incidentId: String, payload: IncidentUpdatePayload): Result<IncidentItem> =
        runCatching {
            val body = buildMap<String, Any?> {
                put("type", payload.type)
                put("description", payload.description)
                put("location", mapOf("lat" to payload.lat, "lng" to payload.lng))
                put("reported_at", payload.reportedAtIso)
                put("stop_label", payload.stopLabel)
                put("can_move_independently", payload.canMoveIndependently)
                payload.photoBase64?.let { put("photo_base64", it) }
                if (payload.clearPhoto && payload.photoBase64 == null) {
                    put("clear_photo", true)
                }
            }
            api.updateIncident(incidentId, body).toIncidentItem()
        }

    override suspend fun listIncidents(): Result<List<IncidentItem>> = runCatching {
        api.listIncidents().map { it.toIncidentItem() }
    }

    override suspend fun listArchivedIncidents(): Result<List<IncidentItem>> = runCatching {
        api.listArchivedIncidents().map { it.toIncidentItem() }
    }

    override suspend fun archiveIncident(incidentId: String, reasonCode: String, reasonNote: String?): Result<Unit> =
        runCatching {
            api.archiveIncident(
                incidentId,
                ArchiveReasonDto(reason_code = reasonCode, reason_note = reasonNote)
            )
            Unit
        }
}
