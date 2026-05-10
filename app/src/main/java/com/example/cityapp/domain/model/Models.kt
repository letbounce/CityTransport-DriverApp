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

data class Waybill(
    val id: String,
    val routeId: String,
    val routeNumber: String,
    val status: String
)

data class AuthSession(
    val token: String,
    val driver: Driver
)
