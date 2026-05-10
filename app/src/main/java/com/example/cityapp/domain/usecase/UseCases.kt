package com.example.cityapp.domain.usecase

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

class ListWaybillsUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke() = waybillRepository.listWaybills()
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
    suspend operator fun invoke(
        waybillId: String,
        type: String,
        description: String,
        lat: Double,
        lng: Double
    ) = incidentRepository.reportIncident(waybillId, type, description, lat, lng)
}

class ListIncidentsUseCase(
    private val incidentRepository: IncidentRepository
) {
    suspend operator fun invoke() = incidentRepository.listIncidents()
}
