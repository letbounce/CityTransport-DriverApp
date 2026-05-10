package com.example.cityapp.presentation.map

/** Доступні маршрути на екрані-плитках перед відкриттям мапи. */
object TripMapCatalog {

    data class Entry(
        val id: String,
        /** Великий рядок на плитці (наприклад «Автобус 101»). */
        val tileTitle: String,
        /** Підзаголовок — напрямок для водія. */
        val tileSubtitle: String,
        /** Полілінії маршруту (relation). */
        val assetPath: String,
        val routeRef: String,
        /** Окремий файл точок зупинок із назвами (FeatureCollection Point). */
        val stopsAssetPath: String? = null
    )

    val routes: List<Entry> = listOf(
        Entry(
            id = "101",
            tileTitle = "Автобус 101",
            tileSubtitle = "вул. Милославська — ст. м. Почайна",
            assetPath = BusRouteGeoJsonParser.ASSET_BUS_101,
            routeRef = "101",
            stopsAssetPath = BusRouteGeoJsonParser.ASSET_STOPS_BUS_101
        )
    )

    fun findById(routeId: String): Entry? =
        routes.find { it.id.equals(routeId.trim(), ignoreCase = true) }
}
