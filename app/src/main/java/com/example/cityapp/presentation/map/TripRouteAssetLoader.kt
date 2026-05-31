package com.example.cityapp.presentation.map

import android.content.Context

object TripRouteAssetLoader {

    private val fallback101Segments: List<List<Pair<Double, Double>>> = listOf(
        listOf(
            50.51965 to 30.59185,
            50.51895 to 30.58745,
            50.51805 to 30.58205,
            50.51720 to 30.57655,
            50.51635 to 30.57105,
            50.51550 to 30.56555,
            50.51465 to 30.56005,
            50.51385 to 30.55485,
            50.51265 to 30.54805,
            50.51125 to 30.54105,
            50.50985 to 30.53405,
            50.50835 to 30.52705,
            50.50685 to 30.52005,
            50.50535 to 30.51305,
            50.50365 to 30.50705,
            50.50135 to 30.50285,
            50.49885 to 30.50045,
            50.49635 to 30.49935,
            50.49385 to 30.49865,
            50.49195 to 30.49895
        )
    )

    fun load(context: Context, entry: TripMapCatalog.Entry): TripRouteMapModel {
        val parsed = BusRouteGeoJsonParser.parseRouteFromAsset(context, entry.assetPath, entry.routeRef)
        val stops = entry.stopsAssetPath?.let { path ->
            BusRouteGeoJsonParser.loadStopsFromAsset(context, path)
        }.orEmpty()

        val base = parsed ?: TripRouteMapModel(
            routeNumber = entry.routeRef,
            routeTitle = "${entry.tileTitle} · наближено",
            polylineSegments = when (entry.id) {
                "101" -> fallback101Segments
                else -> emptyList()
            },
            stops = emptyList(),
            isApproximate = true
        )

        return base.copy(stops = stops)
    }
}
