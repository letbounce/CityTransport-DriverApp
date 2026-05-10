@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.domain.model.Route
import com.example.cityapp.domain.model.Vehicle
import com.example.cityapp.domain.model.Waybill
import kotlinx.coroutines.delay

private fun waybillStatusUa(status: String): String = when (status) {
    "assigned" -> "Призначено"
    "in_progress" -> "В дорозі"
    "completed" -> "Завершено"
    "cancelled" -> "Скасовано"
    else -> status
}

@Composable
fun RouteDashboardScreen(
    onStartTrip: (String) -> Unit,
    onNavigateToMenu: () -> Unit,
    viewModel: RouteDashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingWaybill by remember { mutableStateOf<Waybill?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(state.pendingNavigateWaybillId) {
        state.pendingNavigateWaybillId?.let { id ->
            onStartTrip(id)
            viewModel.consumePendingNavigation()
        }
    }

    LaunchedEffect(state.saveSuccessMessage) {
        if (state.saveSuccessMessage != null) {
            delay(2800)
            viewModel.consumeSaveMessage()
        }
    }

    editingWaybill?.let { wb ->
        WaybillEditDialog(
            waybill = wb,
            vehicles = state.vehicles,
            onDismiss = { editingWaybill = null },
            onSave = { vehicle, notes ->
                viewModel.updateWaybill(wb.id, vehicle, notes)
                editingWaybill = null
            }
        )
    }

    val selectedRoute = state.routes.find { it.id == state.selectedRouteId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Дорожні листи")
                        state.driverDisplayName?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateToMenu,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text("Меню")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            state.saveSuccessMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(msg, Modifier.padding(12.dp))
                    }
                }
            }

            state.error?.let { err ->
                item {
                    Text(err, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                state.activeWaybill?.let { active ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Активний дорожній лист", style = MaterialTheme.typography.titleMedium)
                            Text("Маршрут №${active.routeNumber}")
                            Text("Статус: ${waybillStatusUa(active.status)}")
                            Text("Транспорт: ${active.vehicleId}")
                            if (active.notes.isNotBlank()) {
                                Text("Примітки: ${active.notes}")
                            }
                            Button(
                                onClick = { viewModel.continueTrip(active.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text("Продовжити рейс")
                            }
                            TextButton(
                                onClick = { editingWaybill = active },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Редагувати дорожній лист")
                            }
                        }
                    }
                }
            }

            item {
                if (state.routes.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Новий дорожній лист", style = MaterialTheme.typography.titleMedium)

                            RouteDropdown(
                                routes = state.routes,
                                selectedRouteId = state.selectedRouteId,
                                onRouteSelected = viewModel::onRouteSelected
                            )

                            VehicleDropdown(
                                vehicles = state.vehicles,
                                selectedVehicleId = state.selectedVehicleId,
                                onVehicleSelected = viewModel::onVehicleSelected,
                                vehiclesLoaded = state.vehiclesLoaded
                            )

                            Text(
                                "Зупинки обраного маршруту:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            selectedRoute?.stops?.take(8)?.forEach { stop ->
                                Text("${stop.stopNumber}. ${stop.plannedTime} — ${stop.name}")
                            }

                            OutlinedTextField(
                                value = state.newTripNotes,
                                onValueChange = viewModel::onNewTripNotesChange,
                                label = { Text("Примітки до рейсу (необов’язково)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                minLines = 3,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )
                            Button(
                                onClick = viewModel::createWaybill,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                            ) {
                                Text("Створити дорожній лист")
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Мої дорожні листи", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Зведення з сервера (MongoDB)",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            items(state.waybills, key = { it.id }) { wb ->
                WaybillRowCard(
                    waybill = wb,
                    onEdit = { editingWaybill = wb }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun RouteDropdown(
    routes: List<Route>,
    selectedRouteId: String,
    onRouteSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = routes.find { it.id == selectedRouteId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            value = selected?.let { "№${it.routeNumber} — ${it.routeName}" } ?: "Оберіть маршрут",
            onValueChange = {},
            label = { Text("Маршрут") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            routes.forEach { route ->
                DropdownMenuItem(
                    text = { Text("№${route.routeNumber} — ${route.routeName}") },
                    onClick = {
                        onRouteSelected(route.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun VehicleDropdown(
    vehicles: List<Vehicle>,
    selectedVehicleId: String,
    onVehicleSelected: (String) -> Unit,
    vehiclesLoaded: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = vehicles.find { it.vehicleId == selectedVehicleId }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!vehiclesLoaded && vehicles.isEmpty()) {
            Text(
                "Транспорт не завантажено. Перезапустіть сервер і виконайте npm run seed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                value = selected?.let { "${it.vehicleId} — ${it.label}" }
                    ?: selectedVehicleId.ifBlank { "Оберіть транспорт" },
                onValueChange = {},
                label = { Text("Борт / транспорт") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                enabled = vehicles.isNotEmpty(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                vehicles.forEach { v ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("${v.vehicleId} — ${v.label}")
                                if (v.plateNumber.isNotBlank()) {
                                    Text(
                                        v.plateNumber,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        onClick = {
                            onVehicleSelected(v.vehicleId)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WaybillRowCard(
    waybill: Waybill,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "№${waybill.routeNumber} · ${waybillStatusUa(waybill.status)}",
                style = MaterialTheme.typography.titleSmall
            )
            Text("ID: ${waybill.id}")
            Text("Транспорт: ${waybill.vehicleId}")
            if (waybill.notes.isNotBlank()) {
                Text("Примітки: ${waybill.notes}")
            }
            waybill.startedAt?.let { Text("Початок: $it", style = MaterialTheme.typography.bodySmall) }
            waybill.completedAt?.let { Text("Завершено: $it", style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Редагувати")
            }
        }
    }
}

@Composable
private fun WaybillEditDialog(
    waybill: Waybill,
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (vehicleId: String, notes: String) -> Unit
) {
    var vehicleManual by remember(waybill.id) { mutableStateOf(waybill.vehicleId) }
    var notes by remember(waybill.id) { mutableStateOf(waybill.notes) }
    var vehicleExpanded by remember { mutableStateOf(false) }
    val selectedVehicle = vehicles.find { it.vehicleId == vehicleManual }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагування листа №${waybill.routeNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Статус: ${waybillStatusUa(waybill.status)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (vehicles.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = vehicleExpanded,
                        onExpandedChange = { vehicleExpanded = !vehicleExpanded }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            value = selectedVehicle?.let { "${it.vehicleId} — ${it.label}" }
                                ?: vehicleManual,
                            onValueChange = {},
                            label = { Text("Транспорт") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = vehicleExpanded,
                            onDismissRequest = { vehicleExpanded = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.vehicleId} — ${v.label}") },
                                    onClick = {
                                        vehicleManual = v.vehicleId
                                        vehicleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = vehicleManual,
                        onValueChange = { vehicleManual = it },
                        label = { Text("Борт № / транспорт") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Примітки") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    minLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(vehicleManual, notes) }) { Text("Зберегти") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}
