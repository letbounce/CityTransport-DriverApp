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

data class ArchiveReasonDto(
    val reason_code: String,
    val reason_note: String? = null
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
    val completed_at: String? = null,
    val deleted_at: String? = null,
    val deletion_reason_code: String? = null,
    val deletion_reason_note: String? = null
)

data class IncidentRequestDto(
    val waybill_id: String,
    val type: String,
    val description: String,
    val location: IncidentLocationDto,
    val reported_at: String? = null,
    val stop_label: String? = null,
    val can_move_independently: Boolean? = null,
    val photo_base64: String? = null,
    val photo_url: String? = null
)

data class IncidentLocationDto(
    val lat: Double,
    val lng: Double
)

data class IncidentSnapshotDto(
    val type: String? = null,
    val description: String? = null,
    val reported_at: String? = null,
    val stop_label: String? = null,
    val can_move_independently: Boolean? = null,
    val photo_url: String? = null,
    val location: IncidentLocationDto? = null
)

data class IncidentVersionEntryDto(
    val saved_at: String? = null,
    val driver_id: String? = null,
    val snapshot: IncidentSnapshotDto? = null
)

data class IncidentResponseDto(
    val _id: String,
    val waybill_id: String,
    val driver_id: String? = null,
    val type: String,
    val description: String,
    val status: String,
    val reported_at: String? = null,
    val deleted_at: String? = null,
    val stop_label: String? = null,
    val can_move_independently: Boolean? = null,
    val photo_url: String? = null,
    val is_modified: Boolean? = null,
    val last_edited_at: String? = null,
    val deletion_reason_code: String? = null,
    val deletion_reason_note: String? = null,
    val version_history: List<IncidentVersionEntryDto>? = null,
    val location: IncidentLocationDto
)

data class LiveTripMarkerDto(
    val waybill_id: String,
    val driver_id: String,
    val route_number: String,
    val lat: Double,
    val lng: Double,
    val updated_at: String? = null,
    val is_self: Boolean = false
)

data class LiveTripMarkersResponseDto(
    val markers: List<LiveTripMarkerDto>? = null
)
