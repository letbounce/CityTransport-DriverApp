package com.example.cityapp.domain.usecase

import com.example.cityapp.domain.repository.AuthRepository
import com.example.cityapp.domain.repository.IncidentRepository
import com.example.cityapp.domain.repository.RouteRepository
import com.example.cityapp.domain.repository.WaybillRepository

class LoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(driverId: String, password: String) = repository.login(driverId, password)
}

class GetActiveRouteUseCase(
    private val routeRepository: RouteRepository
) {
    suspend operator fun invoke() = routeRepository.getRoutes()
}

class StartTripUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke(routeId: String) = waybillRepository.startWaybill(routeId)
}

class EndTripUseCase(
    private val waybillRepository: WaybillRepository
) {
    suspend operator fun invoke(waybillId: String) = waybillRepository.completeWaybill(waybillId)
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
