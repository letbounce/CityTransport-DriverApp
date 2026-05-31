package com.example.cityapp.domain.usecase

import com.example.cityapp.domain.model.IncidentUpdatePayload
import com.example.cityapp.domain.model.NewIncidentPayload
import com.example.cityapp.domain.repository.AuthRepository
import com.example.cityapp.domain.repository.IncidentRepository
import com.example.cityapp.domain.repository.RouteRepository
import com.example.cityapp.domain.repository.VehicleRepository
import com.example.cityapp.domain.repository.WaybillRepository

class LoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(driverId: String, password: String) = repository.login(driverId, password)
}

class LogoutUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.logout()
}

class GetActiveRouteUseCase(
    private val routeRepository: RouteRepository
) {
    suspend operator fun invoke() = routeRepository.getRoutes()
}

class ListVehiclesUseCase(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke() = vehicleRepository.listVehicles()
}

class StartTripUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke(routeId: String, vehicleId: String?, notes: String?) =
        waybillRepository.startWaybill(routeId, vehicleId, notes)
}

class EndTripUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke(waybillId: String) = waybillRepository.completeWaybill(waybillId)
}

class GetWaybillUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke(waybillId: String) = waybillRepository.getWaybill(waybillId)
}

class ListWaybillsUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke() = waybillRepository.listWaybills()
}

class ListArchivedWaybillsUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke() = waybillRepository.listArchivedWaybills()
}

class ArchiveWaybillUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke(waybillId: String, reasonCode: String, reasonNote: String?) =
        waybillRepository.archiveWaybill(waybillId, reasonCode, reasonNote)
}

class UpdateWaybillUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke(waybillId: String, vehicleId: String?, notes: String?) =
        waybillRepository.updateWaybill(waybillId, vehicleId, notes)
}

class ReportIncidentUseCase(
    private val incidentRepository: IncidentRepository
) {
    suspend operator fun invoke(payload: NewIncidentPayload) = incidentRepository.reportIncident(payload)
}

class GetIncidentUseCase(
    private val incidentRepository: IncidentRepository
) {
    suspend operator fun invoke(incidentId: String) = incidentRepository.getIncident(incidentId)
}

class UpdateIncidentUseCase(
    private val incidentRepository: IncidentRepository
) {
    suspend operator fun invoke(incidentId: String, payload: IncidentUpdatePayload) =
        incidentRepository.updateIncident(incidentId, payload)
}

class ListIncidentsUseCase(
    private val incidentRepository: IncidentRepository
) {
    suspend operator fun invoke() = incidentRepository.listIncidents()
}

class ListArchivedIncidentsUseCase(
    private val incidentRepository: IncidentRepository
) {
    suspend operator fun invoke() = incidentRepository.listArchivedIncidents()
}

class ArchiveIncidentUseCase(
    private val incidentRepository: IncidentRepository
) {
    suspend operator fun invoke(incidentId: String, reasonCode: String, reasonNote: String?) =
        incidentRepository.archiveIncident(incidentId, reasonCode, reasonNote)
}
