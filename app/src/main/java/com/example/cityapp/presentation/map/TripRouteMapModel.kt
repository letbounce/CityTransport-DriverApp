package com.example.cityapp.presentation.map

/** Одна зупинка / платформа на маршруті (з GeoJSON Overpass). */
data class TripRouteStop(
    val orderIndex: Int,
    val title: String,
    val subtitle: String?,
    val lat: Double,
    val lon: Double
)

/**
 * Геометрія для відображення одного номера маршруту: кілька поліліній (наприклад, напрямки)
 * та зупинки окремими точками.
 */
data class TripRouteMapModel(
    val routeNumber: String,
    val routeTitle: String,
    val polylineSegments: List<List<Pair<Double, Double>>>,
    val stops: List<TripRouteStop>,
    val isApproximate: Boolean
)
