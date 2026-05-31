package com.example.cityapp.presentation.route

import com.example.cityapp.domain.model.Stop
import java.util.Locale

enum class RouteTripDirection(val labelUa: String) {
    OUTBOUND("Прямий"),
    INBOUND("Зворотний")
}

/**
 * У GeoJSON/OSM зупинки часто йдуть парами (туди й назад на тій самій вулиці).
 * Для прямого рейсу беремо першу з пари; для зворотного — другу, у зворотному порядку.
 */
fun filterStopsForDirection(
    stops: List<Stop>,
    direction: RouteTripDirection,
    intervalMinutes: Int = 4
): List<Stop> {
    if (stops.isEmpty()) return stops

    val pairedLayout = stops.size >= 4 &&
        stops.zipWithNext().count { (a, b) -> a.name == b.name } >= stops.size / 4

    val oneWay = when {
        pairedLayout -> when (direction) {
            RouteTripDirection.OUTBOUND ->
                stops.filterIndexed { index, _ -> index % 2 == 0 }
            RouteTripDirection.INBOUND ->
                stops.filterIndexed { index, _ -> index % 2 == 1 }.asReversed()
        }
        else -> {
            val mid = stops.size / 2
            when (direction) {
                RouteTripDirection.OUTBOUND -> stops.take(mid).ifEmpty { stops }
                RouteTripDirection.INBOUND ->
                    stops.drop(mid).ifEmpty { stops }.asReversed()
            }
        }
    }

    return if (oneWay.hasPlannedSchedule()) {
        oneWay.mapIndexed { index, stop -> stop.copy(stopNumber = index + 1) }
    } else {
        renumberStopsWithIntervals(oneWay, intervalMinutes)
    }
}

private fun List<Stop>.hasPlannedSchedule(): Boolean =
    size >= 2 && all { parsePlannedTimeMinutes(it.plannedTime) != null }

private fun renumberStopsWithIntervals(stops: List<Stop>, intervalMinutes: Int): List<Stop> {
    if (stops.isEmpty()) return stops
    val startMinutes = parsePlannedTimeMinutes(stops.first().plannedTime) ?: (6 * 60)
    return stops.mapIndexed { index, stop ->
        stop.copy(
            stopNumber = index + 1,
            plannedTime = formatPlannedTimeMinutes(startMinutes + index * intervalMinutes)
        )
    }
}

private fun parsePlannedTimeMinutes(hhmm: String): Int? {
    val parts = hhmm.trim().split(':')
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

private fun formatPlannedTimeMinutes(totalMinutes: Int): String {
    val wrapped = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val h = wrapped / 60
    val m = wrapped % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
}

fun parseTripDirectionFromNotes(notes: String?): RouteTripDirection {
    val text = notes.orEmpty()
    return when {
        text.contains("зворотн", ignoreCase = true) -> RouteTripDirection.INBOUND
        else -> RouteTripDirection.OUTBOUND
    }
}

fun appendDirectionToTripNotes(
    userNotes: String,
    direction: RouteTripDirection
): String {
    val tag = "Напрямок: ${direction.labelUa.lowercase()}"
    val trimmed = userNotes.trim()
    return when {
        trimmed.isEmpty() -> tag
        trimmed.contains("Напрямок:", ignoreCase = true) -> trimmed
        else -> "$trimmed\n$tag"
    }
}
