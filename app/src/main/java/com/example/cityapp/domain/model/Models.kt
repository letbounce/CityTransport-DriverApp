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
    val completedAt: String? = null
)

data class IncidentItem(
    val id: String,
    val waybillId: String,
    val type: String,
    val description: String,
    val status: String,
    val reportedAt: String?,
    val lat: Double,
    val lng: Double
)

data class AuthSession(
    val token: String,
    val driver: Driver
)
