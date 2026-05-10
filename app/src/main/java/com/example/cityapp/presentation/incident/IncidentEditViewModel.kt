package com.example.cityapp.presentation.incident

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityapp.di.ServiceLocator
import com.example.cityapp.domain.model.IncidentUpdatePayload
import com.example.cityapp.domain.usecase.GetIncidentUseCase
import com.example.cityapp.domain.usecase.UpdateIncidentUseCase
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Calendar

data class IncidentEditUiState(
    val loading: Boolean = true,
    val incidentType: IncidentApiType = IncidentApiType.OTHER,
    val description: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val stopLabel: String = "",
    val reportedCalendar: Long = System.currentTimeMillis(),
    val hour: Int = 0,
    val minute: Int = 0,
    val canMoveIndependently: Boolean = false,
    val photoBase64: String? = null,
    val photoPreview: Bitmap? = null,
    /** Зображення з сервера (повний URL); ховається після «Прибрати» до збереження */
    val serverPhotoUrl: String? = null,
    /** У PATCH піде clear_photo, якщо немає нового photo_base64 */
    val pendingClearRemotePhoto: Boolean = false,
    val isModifiedRemote: Boolean = false,
    val versionsCount: Int = 0,
    val error: String? = null,
    val saved: Boolean = false
)

class IncidentEditViewModel(application: Application) : AndroidViewModel(application) {
    private val getIncidentUseCase = GetIncidentUseCase(ServiceLocator.incidentRepository)
    private val updateIncidentUseCase = UpdateIncidentUseCase(ServiceLocator.incidentRepository)

    private val _uiState = MutableStateFlow(IncidentEditUiState())
    val uiState: StateFlow<IncidentEditUiState> = _uiState.asStateFlow()

    fun load(incidentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, saved = false) }
            val result = getIncidentUseCase(incidentId)
            val inc = result.getOrNull()
            if (inc == null) {
                _uiState.update { it.copy(loading = false, error = "Не вдалося завантажити інцидент") }
                return@launch
            }
            val millis = parseReportedMillis(inc.reportedAt)
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            _uiState.update {
                IncidentEditUiState(
                    loading = false,
                    incidentType = IncidentApiType.fromApi(inc.type),
                    description = inc.description,
                    lat = inc.lat,
                    lng = inc.lng,
                    stopLabel = inc.stopLabel,
                    reportedCalendar = millis,
                    hour = cal[Calendar.HOUR_OF_DAY],
                    minute = cal[Calendar.MINUTE],
                    canMoveIndependently = inc.canMoveIndependently,
                    photoBase64 = null,
                    photoPreview = null,
                    serverPhotoUrl = inc.photoUrl?.takeIf { url -> url.isNotBlank() },
                    pendingClearRemotePhoto = false,
                    isModifiedRemote = inc.isModified,
                    versionsCount = inc.versionHistoryCount,
                    error = null,
                    saved = false
                )
            }
        }
    }

    fun onTypeSelected(type: IncidentApiType) {
        _uiState.update { it.copy(incidentType = type) }
    }

    fun onDescriptionChange(v: String) {
        _uiState.update { it.copy(description = v) }
    }

    fun onStopLabelChange(v: String) {
        _uiState.update { it.copy(stopLabel = v) }
    }

    fun setLat(v: Double) {
        _uiState.update { it.copy(lat = v) }
    }

    fun setLng(v: Double) {
        _uiState.update { it.copy(lng = v) }
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

    fun setCanMoveIndependently(v: Boolean) {
        _uiState.update { it.copy(canMoveIndependently = v) }
    }

    fun setPhoto(bitmap: Bitmap?) {
        if (bitmap == null) {
            _uiState.update { it.copy(photoPreview = null, photoBase64 = null) }
            return
        }
        _uiState.update {
            it.copy(
                photoPreview = bitmap,
                photoBase64 = IncidentPhotoUtils.bitmapToJpegDataUri(bitmap),
                pendingClearRemotePhoto = false
            )
        }
    }

    fun clearPhoto() {
        _uiState.update {
            val markClear = it.pendingClearRemotePhoto || (it.serverPhotoUrl != null)
            it.copy(
                photoPreview = null,
                photoBase64 = null,
                serverPhotoUrl = null,
                pendingClearRemotePhoto = markClear
            )
        }
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

    fun save(incidentId: String) {
        viewModelScope.launch {
            val s = _uiState.value
            val desc = s.description.trim()
            if (desc.isEmpty()) {
                _uiState.update { it.copy(error = "Опис не може бути порожнім") }
                return@launch
            }
            val cal = Calendar.getInstance().apply {
                timeInMillis = s.reportedCalendar
                set(Calendar.HOUR_OF_DAY, s.hour)
                set(Calendar.MINUTE, s.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val iso = Instant.ofEpochMilli(cal.timeInMillis)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toString()
            val payload = IncidentUpdatePayload(
                type = s.incidentType.apiKey,
                description = desc,
                lat = s.lat,
                lng = s.lng,
                reportedAtIso = iso,
                stopLabel = s.stopLabel.trim(),
                canMoveIndependently = s.canMoveIndependently,
                photoBase64 = s.photoBase64,
                clearPhoto = s.pendingClearRemotePhoto && s.photoBase64 == null
            )
            val result = updateIncidentUseCase(incidentId, payload)
            _uiState.update {
                if (result.isSuccess) {
                    val inc = result.getOrNull()
                    it.copy(
                        saved = true,
                        error = null,
                        isModifiedRemote = true,
                        versionsCount = inc?.versionHistoryCount ?: (it.versionsCount + 1),
                        serverPhotoUrl = inc?.photoUrl?.takeIf { url -> url.isNotBlank() },
                        pendingClearRemotePhoto = false,
                        photoPreview = null,
                        photoBase64 = null
                    )
                } else {
                    it.copy(error = "Не вдалося зберегти зміни")
                }
            }
        }
    }

    fun consumeSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    private fun parseReportedMillis(reportedAt: String?): Long {
        if (reportedAt.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(reportedAt).toEpochMilli()
        } catch (_: DateTimeParseException) {
            System.currentTimeMillis()
        }
    }
}
