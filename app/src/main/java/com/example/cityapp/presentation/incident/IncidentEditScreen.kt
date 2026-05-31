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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.presentation.common.WheelIntPickerRow
import java.util.Calendar

@Composable
fun IncidentEditScreen(
    incidentId: String,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onSaved: () -> Unit,
    viewModel: IncidentEditViewModel = viewModel(factory = rememberIncidentEditVmFactory())
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var typeExpanded by remember { mutableStateOf(false) }
    var latStr by remember(state.lat) { mutableStateOf(state.lat.toString()) }
    var lngStr by remember(state.lng) { mutableStateOf(state.lng.toString()) }

    LaunchedEffect(state.lat) { latStr = state.lat.toString() }
    LaunchedEffect(state.lng) { lngStr = state.lng.toString() }

    LaunchedEffect(incidentId) {
        viewModel.load(incidentId)
    }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
            viewModel.consumeSaved()
        }
    }

    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)?.let(viewModel::setPhoto)
        }
    }

    val takePreview = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        it?.let(viewModel::setPhoto)
    }

    val camPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) takePreview.launch(null)
    }

    val locPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { g ->
        if (g[Manifest.permission.ACCESS_FINE_LOCATION] == true) viewModel.refreshDeviceLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редагування інциденту") },
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
        if (state.loading) {
            Text("Завантаження…", Modifier.padding(innerPadding).padding(16.dp))
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (state.isModifiedRemote || state.versionsCount > 0) {
                Text(
                    "Змінений · записів у історії версій: ${state.versionsCount}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    value = state.incidentType.labelUa,
                    onValueChange = {},
                    label = { Text("Тип") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    IncidentApiType.entries.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.labelUa) },
                            onClick = {
                                viewModel.onTypeSelected(t)
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Опис *") },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                minLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            OutlinedTextField(
                value = state.stopLabel,
                onValueChange = viewModel::onStopLabelChange,
                label = { Text("Зупинка / місце (текст)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latStr,
                    onValueChange = { v ->
                        latStr = v
                        v.toDoubleOrNull()?.let(viewModel::setLat)
                    },
                    label = { Text("Широта") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = lngStr,
                    onValueChange = { v ->
                        lngStr = v
                        v.toDoubleOrNull()?.let(viewModel::setLng)
                    },
                    label = { Text("Довгота") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            OutlinedButton(
                onClick = {
                    val ok = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    if (ok == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        viewModel.refreshDeviceLocation()
                    } else {
                        locPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Підставити GPS зараз")
            }

            Text("Час події", style = MaterialTheme.typography.titleSmall)
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Обрати дату")
            }
            WheelIntPickerRow(
                hour = state.hour,
                minute = state.minute,
                onHourChange = viewModel::onHourChange,
                onMinuteChange = viewModel::onMinuteChange,
                modifier = Modifier.fillMaxWidth()
            )

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
                OutlinedButton(
                    onClick = viewModel::clearPhoto,
                    enabled = state.photoPreview != null || state.serverPhotoUrl != null
                ) {
                    Text("Прибрати")
                }
            }
            state.photoPreview?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            } ?: state.serverPhotoUrl?.let { url ->
                IncidentRemoteImage(
                    url = url,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Button(
                onClick = { viewModel.save(incidentId) },
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Text("Зберегти зміни")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun rememberIncidentEditVmFactory(): ViewModelProvider.Factory {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as Application
                @Suppress("UNCHECKED_CAST")
                return IncidentEditViewModel(app) as T
            }
        }
    }
}
