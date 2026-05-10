@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.incident

import android.Manifest
import android.app.Application
import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.domain.model.Stop
import com.example.cityapp.presentation.common.WheelIntPickerRow
import java.util.Calendar

@Composable
fun IncidentReportScreen(
    waybillId: String,
    onSubmitted: () -> Unit,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: IncidentReportViewModel = viewModel(
        factory = rememberIncidentReportViewModelFactory()
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scroll = rememberScrollState()
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var stopMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(waybillId) {
        viewModel.loadContextForWaybill(waybillId)
    }
    LaunchedEffect(state.submitted) {
        if (state.submitted) onSubmitted()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.refreshDeviceLocation()
        }
    }

    val pickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.let { viewModel.setPhoto(it) }
        }
    }

    val takePreview = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        bmp?.let { viewModel.setPhoto(it) }
    }

    val camPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        if (ok) takePreview.launch(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Звіт про інцидент") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            state.routeLoadError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = state.incidentType.labelUa,
                    onValueChange = {},
                    label = { Text("Тип інциденту") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    IncidentApiType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.labelUa) },
                            onClick = {
                                viewModel.onIncidentTypeSelected(type)
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            TextButton(onClick = viewModel::applyTemplateForCurrentType, modifier = Modifier.fillMaxWidth()) {
                Text("Вставити шаблон опису для обраного типу")
            }

            if (state.routeStops.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = stopMenuExpanded,
                    onExpandedChange = { stopMenuExpanded = !stopMenuExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        value = state.selectedStop?.let { stopLabel(it) } ?: "Оберіть зупинку маршруту",
                        onValueChange = {},
                        label = { Text("Зупинка маршруту") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = stopMenuExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = stopMenuExpanded,
                        onDismissRequest = { stopMenuExpanded = false }
                    ) {
                        state.routeStops.forEach { stop ->
                            DropdownMenuItem(
                                text = { Text(stopLabel(stop)) },
                                onClick = {
                                    viewModel.onStopSelected(stop)
                                    stopMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedButton(onClick = viewModel::applyCurrentStopFromRoute, modifier = Modifier.fillMaxWidth()) {
                    Text("Координати поточної обраної зупинки")
                }
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Опис інциденту *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                minLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Text("Час події", style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = viewModel::applyNowDateTime) {
                    Text("Зараз")
                }
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = state.reportedCalendar }
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> viewModel.onCalendarDatePicked(y, m, d) },
                            cal[Calendar.YEAR],
                            cal[Calendar.MONTH],
                            cal[Calendar.DAY_OF_MONTH]
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Обрати дату")
                }
            }
            WheelIntPickerRow(
                hour = state.hour,
                minute = state.minute,
                onHourChange = viewModel::onHourChange,
                onMinuteChange = viewModel::onMinuteChange,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Координати / місце", style = MaterialTheme.typography.titleSmall)
            Text("%.5f, %.5f".format(state.lat, state.lng))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val needFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        if (needFine == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            viewModel.refreshDeviceLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("GPS зараз")
                }
            }

            Text(
                "Чи можна рухатися самостійно?",
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.setCanMoveIndependently(true) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.canMoveIndependently
                ) {
                    Text("Так")
                }
                Button(
                    onClick = { viewModel.setCanMoveIndependently(false) },
                    modifier = Modifier.weight(1f),
                    enabled = state.canMoveIndependently
                ) {
                    Text("Ні")
                }
            }

            Text("Фото (необов’язково)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    pickGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("Галерея")
                }
                OutlinedButton(onClick = {
                    val camOk = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (camOk == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        takePreview.launch(null)
                    } else {
                        camPermission.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Камера")
                }
                OutlinedButton(onClick = viewModel::clearPhoto, enabled = state.photoPreview != null) {
                    Text("Прибрати")
                }
            }
            state.photoPreview?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

            Button(
                onClick = { viewModel.submit(waybillId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                enabled = !state.isSubmitting
            ) {
                Text(if (state.isSubmitting) "Відправка…" else "ВІДПРАВИТИ ЗВІТ")
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun stopLabel(stop: Stop): String =
    "${stop.stopNumber}. ${stop.plannedTime} — ${stop.name}"

@Composable
private fun rememberIncidentReportViewModelFactory(): ViewModelProvider.Factory {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as Application
                @Suppress("UNCHECKED_CAST")
                return IncidentReportViewModel(app) as T
            }
        }
    }
}
