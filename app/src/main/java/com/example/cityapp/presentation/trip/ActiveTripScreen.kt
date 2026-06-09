@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.domain.model.Stop
import com.example.cityapp.presentation.common.AppCardShape
import com.example.cityapp.presentation.incident.IncidentApiType

@Composable
fun ActiveTripScreen(
    waybillId: String,
    onOpenIncident: (String) -> Unit,
    onTripCompleted: () -> Unit,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: ActiveTripViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(waybillId) { viewModel.loadTrip(waybillId) }
    LaunchedEffect(state.completed) { if (state.completed) onTripCompleted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Активний рейс") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        },
        bottomBar = {
            OutlinedButton(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(52.dp)
            ) {
                Text("На головне меню")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    state.loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "Завантаження даних рейсу…",
                                modifier = Modifier.padding(top = 12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    state.loadError != null -> {
                        Text(
                            state.loadError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Дорожній лист: $waybillId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        val wb = state.waybill
                        TripSummaryCard(
                            waybillId = waybillId,
                            routeNumber = wb?.routeNumber,
                            routeName = state.routeName,
                            vehicleId = wb?.vehicleId,
                            status = wb?.status,
                            startedAt = wb?.startedAt,
                            notes = wb?.notes,
                            driverName = state.driverDisplayName
                        )
                        if (state.stops.isNotEmpty()) {
                            TripStopsCard(stops = state.stops)
                        }
                        TripIncidentsCard(incidents = state.openIncidents)
                    }
                }
            }

            Button(
                onClick = { viewModel.completeTrip(waybillId) },
                enabled = !state.isCompleting && !state.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Text(if (state.isCompleting) "ЗАВЕРШЕННЯ…" else "ЗАВЕРШИТИ РЕЙС")
            }
            Button(
                onClick = { onOpenIncident(waybillId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Text("ІНЦИДЕНТ")
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TripSummaryCard(
    waybillId: String,
    routeNumber: String?,
    routeName: String?,
    vehicleId: String?,
    status: String?,
    startedAt: String?,
    notes: String?,
    driverName: String?
) {
    InfoCard(title = "Рейс у дорозі") {
        driverName?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Водій", it)
        }
        routeNumber?.let {
            InfoRow("Маршрут", "№$it")
        }
        routeName?.let {
            InfoRow("Назва маршруту", it)
        }
        vehicleId?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Транспорт", it)
        }
        status?.let {
            InfoRow("Статус", waybillStatusUa(it))
        }
        formatTripTimestamp(startedAt)?.let {
            InfoRow("Початок рейсу", it)
        }
        notes?.takeIf { it.isNotBlank() }?.let {
            InfoRow("Примітки", it)
        }
        InfoRow("ID дорожнього листа", waybillId)
    }
}

@Composable
private fun TripStopsCard(stops: List<Stop>) {
    val preview = stops.take(8)
    val more = stops.size - preview.size
    InfoCard(title = "Зупинки маршруту (${stops.size})") {
        preview.forEach { stop ->
            Text(
                text = buildString {
                    append("${stop.stopNumber}. ${stop.name}")
                    if (stop.plannedTime.isNotBlank()) {
                        append(" · ")
                        append(stop.plannedTime)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        if (more > 0) {
            Text(
                "… ще $more зупинок",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TripIncidentsCard(incidents: List<IncidentItem>) {
    InfoCard(title = "Інциденти за рейс") {
        if (incidents.isEmpty()) {
            Text(
                "Зареєстрованих інцидентів немає. Кнопка «ІНЦИДЕНТ» — для нового звіту.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Відкритих записів: ${incidents.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            incidents.take(3).forEach { inc ->
                val typeLabel = IncidentApiType.fromApi(inc.type).labelUa
                val desc = inc.description.lineSequence().firstOrNull().orEmpty()
                    .take(80)
                    .let { if (it.length < inc.description.length) "$it…" else it }
                Text(
                    text = "• $typeLabel${if (desc.isNotBlank()) ": $desc" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (incidents.size > 3) {
                Text(
                    "… ще ${incidents.size - 3}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
