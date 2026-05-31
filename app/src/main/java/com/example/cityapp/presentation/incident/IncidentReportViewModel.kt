package com.example.cityapp.presentation.incident

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.model.NewIncidentPayload
import com.example.cityapp.domain.model.Stop
import com.example.cityapp.domain.usecase.GetWaybillUseCase
import com.example.cityapp.domain.usecase.ReportIncidentUseCase
import com.example.cityapp.domain.usecase.GetActiveRouteUseCase
import com.example.cityapp.presentation.map.BusRouteGeoJsonParser
import com.example.cityapp.presentation.map.TripMapCatalog
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
            if (wb == null) {
                _uiState.update { it.copy(routeLoadError = "Не вдалося завантажити дорожній лист") }
                return@launch
            }
            val routes = getRoutesUseCase().getOrDefault(emptyList())
            val route = routes.find { it.id == wb.routeId }
            var stops = route?.stops.orEmpty().sortedBy { it.stopNumber }

            if (stops.isEmpty()) {
                val entry = TripMapCatalog.findById(wb.routeNumber.trim())
                val path = entry?.stopsAssetPath
                if (!path.isNullOrBlank()) {
                    stops = BusRouteGeoJsonParser.loadStopsOrderedWithSchedule(getApplication(), path)
                }
            }

            val loadErr = when {
                stops.isEmpty() && route == null ->
                    "Не знайдено маршрут №${wb.routeNumber} у базі й немає офлайн-зупинок для цього номера."
                stops.isEmpty() ->
                    "У маршруту №${wb.routeNumber} немає зупинок (перевірте seed сервера або файли в застосунку)."
                else -> null
            }

            val first = stops.firstOrNull()
            _uiState.update {
                it.copy(
                    routeStops = stops,
                    selectedStop = null,
                    routeLoadError = loadErr,
                    lat = first?.lat ?: it.lat,
                    lng = first?.lng ?: it.lng
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
            if (s.routeStops.isNotEmpty() && s.selectedStop == null) {
                _uiState.update {
                    it.copy(error = "Оберіть зупинку маршруту, біля якої стався інцидент")
                }
                return@launch
            }
            val stopLabel = s.selectedStop?.let { formatStopForIncidentPayload(it) }.orEmpty()

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

private fun formatStopForIncidentPayload(st: Stop): String {
    val name = st.name.trim()
    val pt = st.plannedTime.trim()
    return buildString {
        append(st.stopNumber).append(". ").append(name)
        if (pt.isNotEmpty() && pt != "--:--") {
            append(" · ").append(pt)
        }
    }
}
