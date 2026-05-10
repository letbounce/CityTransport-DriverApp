@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.route

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.domain.model.Route
import com.example.cityapp.domain.model.Vehicle
import com.example.cityapp.domain.model.Waybill
import com.example.cityapp.presentation.common.ArchiveReasonDialog
import com.example.cityapp.presentation.common.ArchiveReasonOption
import kotlinx.coroutines.delay

private fun formatWaybillForCopy(w: Waybill): String {
    val reasonLabel = w.deletionReasonCode?.let { code ->
        ArchiveReasonOption.entries.find { it.code == code }?.labelUa ?: code
    }
    return buildString {
        appendLine("Дорожній лист №${w.routeNumber}")
        appendLine("Статус: ${waybillStatusUa(w.status)}")
        appendLine("ID: ${w.id}")
        appendLine("Транспорт: ${w.vehicleId}")
        w.startedAt?.let { appendLine("Початок: $it") }
        w.completedAt?.let { appendLine("Завершено: $it") }
        w.deletedAt?.let { appendLine("Архівовано: $it") }
        reasonLabel?.let { appendLine("Причина архіву: $it") }
        w.deletionReasonNote?.takeIf { it.isNotBlank() }?.let { appendLine("Примітка: $it") }
        if (w.notes.isNotBlank()) appendLine("Примітки: ${w.notes}")
    }.trim()
}

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
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var editingWaybill by remember { mutableStateOf<Waybill?>(null) }
    var pendingArchiveWaybillId by remember { mutableStateOf<String?>(null) }
    var copyWaybillTarget by remember { mutableStateOf<Waybill?>(null) }

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
            },
            onArchiveConfirmed = {
                pendingArchiveWaybillId = wb.id
                editingWaybill = null
            }
        )
    }

    pendingArchiveWaybillId?.let { id ->
        ArchiveReasonDialog(
            title = "Архівувати дорожній лист?",
            confirmLabel = "Архівувати",
            onDismiss = { pendingArchiveWaybillId = null },
            onConfirm = { code, note ->
                viewModel.archiveWaybill(id, code, note)
                pendingArchiveWaybillId = null
            }
        )
    }

    copyWaybillTarget?.let { wb ->
        AlertDialog(
            onDismissRequest = { copyWaybillTarget = null },
            title = { Text("Копіювати дані листа?") },
            text = { Text("Текст буде скопійовано в буфер обміну.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(formatWaybillForCopy(wb)))
                        Toast.makeText(context, "Скопійовано", Toast.LENGTH_SHORT).show()
                        copyWaybillTarget = null
                    }
                ) {
                    Text("Копіювати")
                }
            },
            dismissButton = {
                TextButton(onClick = { copyWaybillTarget = null }) {
                    Text("Скасувати")
                }
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

                            state.activeWaybill?.let {
                                Text(
                                    text =
                                        "Щоб створити новий активний дорожній лист, потрібно закрити поточний.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

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

            item {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("Неактивні (архів)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Листи, які ви видалили; лише ваші записи.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.archivedWaybills.isEmpty()) {
                item {
                    Text(
                        "Архів дорожніх листів порожній",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.archivedWaybills, key = { "arch_${it.id}" }) { wb ->
                    ArchivedWaybillRowCard(waybill = wb, onClick = { copyWaybillTarget = wb })
                }
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
    onSave: (vehicleId: String, notes: String) -> Unit,
    onArchiveConfirmed: () -> Unit
) {
    var confirmDelete by remember(waybill.id) { mutableStateOf(false) }
    var vehicleManual by remember(waybill.id) { mutableStateOf(waybill.vehicleId) }
    var notes by remember(waybill.id) { mutableStateOf(waybill.notes) }
    var vehicleExpanded by remember { mutableStateOf(false) }
    val selectedVehicle = vehicles.find { it.vehicleId == vehicleManual }

    when {
        confirmDelete -> AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Архівувати дорожній лист?") },
            text = {
                Text(
                    "Активний лист буде переведений у «Завершено», потім у архів. Далі оберіть причину."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onArchiveConfirmed()
                        onDismiss()
                    }
                ) {
                        Text("Далі", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Скасувати")
                }
            }
        )

        else -> AlertDialog(
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
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Видалити дорожній лист…", color = MaterialTheme.colorScheme.error)
                    }
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
}

@Composable
private fun ArchivedWaybillRowCard(waybill: Waybill, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "№${waybill.routeNumber} · ${waybillStatusUa(waybill.status)} · архів",
                style = MaterialTheme.typography.titleSmall
            )
            Text("ID: ${waybill.id}")
            Text("Транспорт: ${waybill.vehicleId}")
            waybill.deletedAt?.let {
                Text("Прибрано: $it", style = MaterialTheme.typography.bodySmall)
            }
            waybill.startedAt?.let { Text("Початок: $it", style = MaterialTheme.typography.bodySmall) }
            waybill.completedAt?.let { Text("Завершено: $it", style = MaterialTheme.typography.bodySmall) }
            waybill.deletionReasonCode?.let { code ->
                val label = ArchiveReasonOption.entries.find { it.code == code }?.labelUa ?: code
                Text("Причина архіву: $label", style = MaterialTheme.typography.bodySmall)
            }
            waybill.deletionReasonNote?.takeIf { it.isNotBlank() }?.let {
                Text("Примітка: $it", style = MaterialTheme.typography.bodySmall)
            }
            Text("Натисніть, щоб скопіювати дані", style = MaterialTheme.typography.labelSmall)
        }
    }
}
