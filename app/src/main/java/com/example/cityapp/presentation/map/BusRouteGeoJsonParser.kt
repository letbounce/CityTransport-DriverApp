package com.example.cityapp.presentation.map

import android.content.Context
import com.example.cityapp.domain.model.Stop
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * GeoJSON з [Overpass turbo](https://overpass-turbo.eu/): полілінії `type=route` і точки зупинок з `@relations`.
 * Координати в GeoJSON: [довгота, широта].
 */
object BusRouteGeoJsonParser {

    fun parseRouteFromAsset(context: Context, assetPath: String, routeRef: String): TripRouteMapModel? {
        return try {
            val jsonText = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            parseRouteJson(jsonText, routeRef)
        } catch (_: Exception) {
            null
        }
    }

    fun parseRouteJson(jsonText: String, routeRef: String): TripRouteMapModel? {
        val root = JSONObject(jsonText)
        if (root.optString("type") != "FeatureCollection") return null
        val features = root.optJSONArray("features") ?: return null

        val segments = mutableListOf<List<Pair<Double, Double>>>()
        var routeLabelFromOsm: String? = null

        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val geom = feature.optJSONObject("geometry") ?: continue
            val props = feature.optJSONObject("properties") ?: JSONObject()

            when (geom.optString("type")) {
                "LineString" -> {
                    if (!isRouteFeature(props, routeRef)) continue
                    if (routeLabelFromOsm == null) {
                        routeLabelFromOsm = listOf(
                            props.optString("name:uk"),
                            props.optString("name")
                        ).firstOrNull { it.isNotBlank() }
                    }
                    val arr = geom.optJSONArray("coordinates") ?: continue
                    val pts = lineStringToLatLngPairs(arr)
                    if (pts.size >= 2) segments.add(dedupeConsecutivePairs(pts))
                }
                "MultiLineString" -> {
                    if (!isRouteFeature(props, routeRef)) continue
                    if (routeLabelFromOsm == null) {
                        routeLabelFromOsm = listOf(
                            props.optString("name:uk"),
                            props.optString("name")
                        ).firstOrNull { it.isNotBlank() }
                    }
                    val lines = geom.optJSONArray("coordinates") ?: continue
                    for (j in 0 until lines.length()) {
                        val segment = lines.optJSONArray(j) ?: continue
                        val pts = lineStringToLatLngPairs(segment)
                        if (pts.size >= 2) segments.add(dedupeConsecutivePairs(pts))
                    }
                }
            }
        }

        if (segments.isEmpty()) return null

        val title = routeLabelFromOsm?.takeIf { it.isNotBlank() }?.let { "$it · OSM" }
            ?: "Автобус $routeRef (OpenStreetMap, ODbL)"

        return TripRouteMapModel(
            routeNumber = routeRef,
            routeTitle = title,
            polylineSegments = segments,
            stops = emptyList(),
            isApproximate = false
        )
    }

    /**
     * Зупинки в порядку файлу з полем `planned_time` (узгоджено з Mongo seed).
     * Дублікати за координатами не зливаються — щоб графік збігався з екраном дорожнього листа.
     */
    fun loadStopsOrderedWithSchedule(context: Context, assetPath: String): List<Stop> {
        return try {
            val jsonText = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            parseStopsOrderedWithSchedule(jsonText)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseStopsOrderedWithSchedule(jsonText: String): List<Stop> {
        val root = JSONObject(jsonText)
        if (root.optString("type") != "FeatureCollection") return emptyList()
        val features = root.optJSONArray("features") ?: return emptyList()
        val out = mutableListOf<Stop>()
        var n = 1
        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val geom = feature.optJSONObject("geometry") ?: continue
            if (geom.optString("type") != "Point") continue
            val coords = geom.optJSONArray("coordinates") ?: continue
            val lon = coords.optDouble(0, Double.NaN)
            val lat = coords.optDouble(1, Double.NaN)
            if (lon.isNaN() || lat.isNaN()) continue

            val props = feature.optJSONObject("properties") ?: JSONObject()
            val tags = props.optJSONObject("tags")
            fun prop(key: String): String {
                val direct = props.optString(key).trim()
                if (direct.isNotEmpty()) return direct
                return tags?.optString(key)?.trim().orEmpty()
            }
            val name = listOf(
                prop("name:uk"),
                prop("name"),
                prop("official_name")
            ).firstOrNull { it.isNotBlank() } ?: "Зупинка $n"
            val plannedTime = normalizePlannedTimeHHmm(prop("planned_time")) ?: "--:--"
            out.add(Stop(stopNumber = n, name = name, plannedTime = plannedTime, lat = lat, lng = lon))
            n++
        }
        return out
    }

    /** Зупинки з окремого GeoJSON (тільки Point + назви в properties). */
    fun loadStopsFromAsset(context: Context, assetPath: String): List<TripRouteStop> {
        return try {
            val jsonText = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            parseStopsFeatureCollection(jsonText)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseStopsFeatureCollection(jsonText: String): List<TripRouteStop> {
        val root = JSONObject(jsonText)
        if (root.optString("type") != "FeatureCollection") return emptyList()
        val features = root.optJSONArray("features") ?: return emptyList()

        val raw = mutableListOf<Pair<Pair<Double, Double>, Pair<String, String?>>>()
        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val geom = feature.optJSONObject("geometry") ?: continue
            if (geom.optString("type") != "Point") continue
            val coords = geom.optJSONArray("coordinates") ?: continue
            val lon = coords.optDouble(0, Double.NaN)
            val lat = coords.optDouble(1, Double.NaN)
            if (lon.isNaN() || lat.isNaN()) continue

            val props = feature.optJSONObject("properties") ?: JSONObject()
            val tags = props.optJSONObject("tags")

            fun tag(key: String): String {
                val direct = props.optString(key).trim()
                if (direct.isNotEmpty()) return direct
                return tags?.optString(key)?.trim().orEmpty()
            }

            val title = listOf(
                tag("name:uk"),
                tag("name"),
                tag("official_name"),
                tag("alt_name:uk")
            ).firstOrNull { it.isNotBlank() } ?: run {
                val localRef = tag("local_ref").ifBlank { tag("ref") }
                if (localRef.isNotBlank()) "Зупинка $localRef" else ""
            }

            if (title.isBlank()) continue

            val sub = listOf(
                tag("from"),
                tag("to"),
                tag("network")
            ).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() }

            raw.add((lat to lon) to (title to sub))
        }

        val seen = LinkedHashSet<String>()
        val out = mutableListOf<TripRouteStop>()
        var idx = 0
        for ((latlng, titleSub) in raw) {
            val (lat, lon) = latlng
            val (title, sub) = titleSub
            val key = "${String.format(Locale.US, "%.5f", lat)},${String.format(Locale.US, "%.5f", lon)}"
            if (!seen.add(key)) continue
            idx++
            out.add(
                TripRouteStop(
                    orderIndex = idx,
                    title = title,
                    subtitle = sub,
                    lat = lat,
                    lon = lon
                )
            )
        }
        return out
    }

    private fun normalizePlannedTimeHHmm(raw: String): String? {
        val parts = raw.trim().split(':')
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    private fun isRouteFeature(props: JSONObject, routeRef: String): Boolean =
        props.optString("type") == "route" && props.optString("ref") == routeRef

    private fun lineStringToLatLngPairs(coords: JSONArray): List<Pair<Double, Double>> {
        val out = mutableListOf<Pair<Double, Double>>()
        for (k in 0 until coords.length()) {
            val pt = coords.optJSONArray(k) ?: continue
            val lon = pt.optDouble(0, Double.NaN)
            val lat = pt.optDouble(1, Double.NaN)
            if (!lon.isNaN() && !lat.isNaN()) out.add(lat to lon)
        }
        return out
    }

    private fun dedupeConsecutivePairs(pts: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        if (pts.size <= 1) return pts
        val res = mutableListOf(pts.first())
        for (i in 1 until pts.size) {
            if (pts[i] != pts[i - 1]) res.add(pts[i])
        }
        return res
    }
}
