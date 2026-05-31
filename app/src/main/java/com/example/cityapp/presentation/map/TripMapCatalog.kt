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
            id = "7",
            tileTitle = "Автобус 7",
            tileSubtitle = "Львівська площа — Залізничний вокзал «Центральний»",
            assetPath = "map/7_route.geojson",
            routeRef = "7",
            stopsAssetPath = "map/stops_7_route.geojson"
        ),
        Entry(
            id = "11",
            tileTitle = "Автобус 11",
            tileSubtitle = "Станція метро «Лісова» — Радіоцентр",
            assetPath = "map/11_route.geojson",
            routeRef = "11",
            stopsAssetPath = "map/stops_11_route.geojson"
        ),
        Entry(
            id = "18",
            tileTitle = "Автобус 18",
            tileSubtitle = "Станція метро «Харківська» — Харківське шосе",
            assetPath = "map/18_route.geojson",
            routeRef = "18",
            stopsAssetPath = "map/stops_18_route.geojson"
        ),
        Entry(
            id = "24",
            tileTitle = "Автобус 24",
            tileSubtitle = "Музей історії України у Другій світовій війні — Залізничний вокзал",
            assetPath = "map/24_route.geojson",
            routeRef = "24",
            stopsAssetPath = "map/stops_24_route.geojson"
        ),
        Entry(
            id = "50",
            tileTitle = "Автобус 50",
            tileSubtitle = "Залізничний вокзал «Центральний» — Вулиця Північна",
            assetPath = "map/50_route.geojson",
            routeRef = "50",
            stopsAssetPath = "map/stops_50_route.geojson"
        ),
        Entry(
            id = "55",
            tileTitle = "Автобус 55",
            tileSubtitle = "Дарницька площа — Станція метро «Палац спорту»",
            assetPath = "map/55_route.geojson",
            routeRef = "55",
            stopsAssetPath = "map/stops_55_route.geojson"
        ),
        Entry(
            id = "62",
            tileTitle = "Автобус 62",
            tileSubtitle = "Контрактова площа — Ботанічний сад",
            assetPath = "map/62_route.geojson",
            routeRef = "62",
            stopsAssetPath = "map/stops_62_route.geojson"
        ),
        Entry(
            id = "101",
            tileTitle = "Автобус 101",
            tileSubtitle = "вул. Милославська — ст. м. Почайна",
            assetPath = "map/101_route.geojson",
            routeRef = "101",
            stopsAssetPath = "map/stops_101_route.geojson"
        ),
        Entry(
            id = "114",
            tileTitle = "Автобус 114",
            tileSubtitle = "вул. Радунська — Залізничний вокзал «Центральний»",
            assetPath = "map/114_route.geojson",
            routeRef = "114",
            stopsAssetPath = "map/stops_114_route.geojson"
        ),
        Entry(
            id = "115",
            tileTitle = "Автобус 115",
            tileSubtitle = "Контрактова площа — Будинок культури",
            assetPath = "map/115_route.geojson",
            routeRef = "115",
            stopsAssetPath = "map/stops_115_route.geojson"
        )
    )

    fun findById(routeId: String): Entry? =
        routes.find { it.id.equals(routeId.trim(), ignoreCase = true) }
}
