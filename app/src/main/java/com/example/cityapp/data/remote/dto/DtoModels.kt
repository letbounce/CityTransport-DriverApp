package com.example.cityapp.data.remote.dto

data class LoginRequestDto(
    val driver_id: String,
    val password: String
)

data class DriverDto(
    val driver_id: String,
    val full_name: String
)

data class LoginResponseDto(
    val token: String,
    val driver: DriverDto
)

data class StopDto(
    val stop_number: Int,
    val name: String,
    val planned_time: String,
    val lat: Double,
    val lng: Double
)

data class RouteDto(
    val _id: String,
    val route_number: String,
    val route_name: String,
    val stops: List<StopDto>
)

data class VehicleDto(
    val _id: String,
    val vehicle_id: String,
    val label: String,
    val plate_number: String? = null,
    val is_active: Boolean = true
)

data class StartWaybillRequestDto(
    val route_id: String,
    val vehicle_id: String = "BUS-007",
    val notes: String? = null
)

data class WaybillDto(
    val _id: String,
    val route_id: String,
    val route_number: String,
    val status: String,
    val vehicle_id: String? = null,
    val notes: String? = null,
    val started_at: String? = null,
    val completed_at: String? = null
)

data class IncidentRequestDto(
    val waybill_id: String,
    val type: String,
    val description: String,
    val location: IncidentLocationDto
)

data class IncidentLocationDto(
    val lat: Double,
    val lng: Double
)

data class IncidentResponseDto(
    val _id: String,
    val waybill_id: String,
    val driver_id: String? = null,
    val type: String,
    val description: String,
    val status: String,
    val reported_at: String? = null,
    val location: IncidentLocationDto
)
