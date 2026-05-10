package com.example.cityapp.domain.model

data class Driver(
    val driverId: String,
    val fullName: String
)

data class Stop(
    val stopNumber: Int,
    val name: String,
    val plannedTime: String,
    val lat: Double,
    val lng: Double
)

data class Route(
    val id: String,
    val routeNumber: String,
    val routeName: String,
    val stops: List<Stop>
)

data class Vehicle(
    val vehicleId: String,
    val label: String,
    val plateNumber: String = ""
)

data class Waybill(
    val id: String,
    val routeId: String,
    val routeNumber: String,
    val status: String,
    val vehicleId: String = "BUS-007",
    val notes: String = "",
    val startedAt: String? = null,
    val completedAt: String? = null,
    val deletedAt: String? = null,
    val deletionReasonCode: String? = null,
    val deletionReasonNote: String? = null
) {
    val isInactive: Boolean get() = deletedAt != null
}

data class IncidentItem(
    val id: String,
    val waybillId: String,
    val type: String,
    val description: String,
    val status: String,
    val reportedAt: String?,
    val lat: Double,
    val lng: Double,
    val deletedAt: String? = null,
    val stopLabel: String = "",
    val canMoveIndependently: Boolean = false,
    val photoUrl: String? = null,
    val isModified: Boolean = false,
    val lastEditedAt: String? = null,
    val versionHistoryCount: Int = 0,
    val deletionReasonCode: String? = null,
    val deletionReasonNote: String? = null
) {
    val isInactive: Boolean get() = deletedAt != null
}

data class AuthSession(
    val token: String,
    val driver: Driver
)

data class NewIncidentPayload(
    val waybillId: String,
    val type: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val reportedAtIso: String? = null,
    val stopLabel: String? = null,
    val canMoveIndependently: Boolean? = null,
    val photoBase64: String? = null
)

data class IncidentUpdatePayload(
    val type: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val reportedAtIso: String,
    val stopLabel: String,
    val canMoveIndependently: Boolean,
    val photoBase64: String? = null,
    val clearPhoto: Boolean = false
)

data class LiveTripMarker(
    val waybillId: String,
    val driverId: String,
    val routeNumber: String,
    val lat: Double,
    val lng: Double,
    val updatedAtIso: String?,
    val isSelf: Boolean
)
