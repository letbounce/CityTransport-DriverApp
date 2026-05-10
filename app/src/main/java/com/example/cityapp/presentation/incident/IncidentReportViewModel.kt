package com.example.cityapp.presentation.incident

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.model.NewIncidentPayload
import com.example.cityapp.domain.model.Route
import com.example.cityapp.domain.model.Stop
import com.example.cityapp.domain.usecase.GetWaybillUseCase
import com.example.cityapp.domain.usecase.ReportIncidentUseCase
import com.example.cityapp.domain.usecase.GetActiveRouteUseCase
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

data class IncidentReportUiState(
    val incidentType: IncidentApiType = IncidentApiType.BREAKDOWN,
    val description: String = "",
    val lat: Double = 50.4501,
    val lng: Double = 30.5234,
    val routeStops: List<Stop> = emptyList(),
    val selectedStop: Stop? = null,
    val stopPickerExpanded: Boolean = false,
    val reportedCalendar: Long = System.currentTimeMillis(),
    val hour: Int = Calendar.getInstance()[Calendar.HOUR_OF_DAY],
    val minute: Int = Calendar.getInstance()[Calendar.MINUTE],
    val canMoveIndependently: Boolean = false,
    val photoBase64: String? = null,
    val photoPreview: Bitmap? = null,
    val routeLoadError: String? = null,
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null
)

class IncidentReportViewModel(application: Application) : AndroidViewModel(application) {
    private val reportIncidentUseCase = ReportIncidentUseCase(ServiceLocator.incidentRepository)
    private val getWaybillUseCase = GetWaybillUseCase(ServiceLocator.waybillRepository)
    private val getRoutesUseCase = GetActiveRouteUseCase(ServiceLocator.routeRepository)

    private val _uiState = MutableStateFlow(IncidentReportUiState())
    val uiState: StateFlow<IncidentReportUiState> = _uiState.asStateFlow()

    fun loadContextForWaybill(waybillId: String) {
        viewModelScope.launch {
            val wb = getWaybillUseCase(waybillId).getOrNull()
            val routes = getRoutesUseCase().getOrDefault(emptyList())
            if (wb == null) {
                _uiState.update { it.copy(routeLoadError = "Не вдалося завантажити дорожній лист") }
                return@launch
            }
            val route: Route? = routes.find { it.id == wb.routeId }
            val stops = route?.stops.orEmpty()
            _uiState.update {
                it.copy(
                    routeStops = stops,
                    selectedStop = stops.firstOrNull(),
                    routeLoadError = if (route == null) "Маршрут для листа не знайдено" else null
                )
            }
        }
    }

    fun onIncidentTypeSelected(type: IncidentApiType) {
        _uiState.update { it.copy(incidentType = type) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun applyTemplateForCurrentType() {
        _uiState.update { it.copy(description = it.incidentType.templateUa()) }
    }

    fun applyCurrentStopFromRoute() {
        val stop = _uiState.value.selectedStop ?: _uiState.value.routeStops.firstOrNull()
        if (stop != null) {
            _uiState.update { it.copy(lat = stop.lat, lng = stop.lng, selectedStop = stop) }
        }
    }

    fun onStopSelected(stop: Stop) {
        _uiState.update { it.copy(selectedStop = stop, lat = stop.lat, lng = stop.lng) }
    }

    fun applyNowDateTime() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        _uiState.update {
            it.copy(
                reportedCalendar = now,
                hour = cal[Calendar.HOUR_OF_DAY],
                minute = cal[Calendar.MINUTE]
            )
        }
    }

    fun setStopPickerExpanded(expanded: Boolean) {
        _uiState.update { it.copy(stopPickerExpanded = expanded) }
    }

    fun onHourChange(h: Int) {
        _uiState.update { prev ->
            val cal = Calendar.getInstance().apply { timeInMillis = prev.reportedCalendar }
            cal[Calendar.HOUR_OF_DAY] = h
            prev.copy(hour = h, reportedCalendar = cal.timeInMillis)
        }
    }

    fun onMinuteChange(m: Int) {
        _uiState.update { prev ->
            val cal = Calendar.getInstance().apply { timeInMillis = prev.reportedCalendar }
            cal[Calendar.MINUTE] = m
            prev.copy(minute = m, reportedCalendar = cal.timeInMillis)
        }
    }

    fun onCalendarDatePicked(year: Int, monthZeroBased: Int, day: Int) {
        _uiState.update { prev ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = prev.reportedCalendar
                set(year, monthZeroBased, day)
            }
            prev.copy(reportedCalendar = cal.timeInMillis)
        }
    }

    fun setCanMoveIndependently(value: Boolean) {
        _uiState.update { it.copy(canMoveIndependently = value) }
    }

    fun setPhoto(bitmap: Bitmap?) {
        if (bitmap == null) {
            _uiState.update { it.copy(photoPreview = null, photoBase64 = null) }
            return
        }
        val uriData = IncidentPhotoUtils.bitmapToJpegDataUri(bitmap)
        _uiState.update { it.copy(photoPreview = bitmap, photoBase64 = uriData) }
    }

    fun clearPhoto() {
        _uiState.update { it.copy(photoPreview = null, photoBase64 = null) }
    }

    @SuppressLint("MissingPermission")
    fun refreshDeviceLocation() {
        val client = LocationServices.getFusedLocationProviderClient(getApplication())
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                _uiState.update { it.copy(lat = loc.latitude, lng = loc.longitude) }
            }
        }
    }

    fun submit(waybillId: String) {
        viewModelScope.launch {
            val s = _uiState.value
            val desc = s.description.trim()
            if (desc.isEmpty()) {
                _uiState.update { it.copy(error = "Заповніть опис інциденту — порожні записи не приймаються") }
                return@launch
            }
            val stopLabel = s.selectedStop?.let { st ->
                "${st.stopNumber}. ${st.plannedTime} ${st.name}"
            }.orEmpty()

            val cal = Calendar.getInstance().apply {
                timeInMillis = s.reportedCalendar
                set(Calendar.HOUR_OF_DAY, s.hour)
                set(Calendar.MINUTE, s.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val instant = Instant.ofEpochMilli(cal.timeInMillis)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toString()

            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val payload = NewIncidentPayload(
                waybillId = waybillId,
                type = s.incidentType.apiKey,
                description = desc,
                lat = s.lat,
                lng = s.lng,
                reportedAtIso = instant,
                stopLabel = stopLabel.ifBlank { null },
                canMoveIndependently = s.canMoveIndependently,
                photoBase64 = s.photoBase64
            )
            val result = reportIncidentUseCase(payload)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isSubmitting = false, submitted = true)
                } else {
                    it.copy(
                        isSubmitting = false,
                        error = "Не вдалося відправити звіт. Перевірте дані та зв’язок із сервером."
                    )
                }
            }
        }
    }
}
