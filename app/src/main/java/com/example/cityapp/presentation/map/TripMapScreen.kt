@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.map

import android.Manifest
import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

private val KyivCenter = LatLng(50.4501, 30.5234)

@Composable
fun TripMapScreen(
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: TripMapViewModel = viewModel(factory = rememberTripMapVmFactory())
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(KyivCenter, 11f)
    }

    LaunchedEffect(state.markers, state.myLat, state.myLng) {
        val selfMarker = state.markers.find { it.isSelf }
        val target = when {
            selfMarker != null -> LatLng(selfMarker.lat, selfMarker.lng)
            state.myLat != null && state.myLng != null -> LatLng(state.myLat!!, state.myLng!!)
            state.markers.isNotEmpty() -> LatLng(state.markers.first().lat, state.markers.first().lng)
            else -> KyivCenter
        }
        val zoom = if (target == KyivCenter && state.markers.isEmpty() && state.myLat == null) 11f else 12f
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, zoom))
    }

    val locPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.refreshMyLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мапа рейсів") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                },
                actions = {
                    TextButton(onClick = { viewModel.refreshMarkers() }) {
                        Text("Оновити")
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.refreshMyLocation()
                        else locPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Оновити мою позицію (GPS)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateHome, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("На головне меню")
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true, compassEnabled = true)
            ) {
                for (m in state.markers) {
                    key(m.waybillId) {
                        val pos = LatLng(m.lat, m.lng)
                        Marker(
                            state = rememberMarkerState(position = pos),
                            title = if (m.isSelf) "Ви · маршрут ${m.routeNumber}" else "Маршрут ${m.routeNumber}",
                            snippet = m.driverId,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (m.isSelf) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }
                }
                val devLat = state.myLat
                val devLng = state.myLng
                if (devLat != null && devLng != null) {
                    Marker(
                        state = rememberMarkerState(position = LatLng(devLat, devLng)),
                        title = "Ваш пристрій (GPS)",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }
            }

            Text(
                "Точки з останньої телеметрії активних дорожніх листів.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            state.error?.let { err ->
                Text(
                    err,
                    Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (!state.loading && state.markers.isEmpty()) {
                Text(
                    "Немає маркерів (немає активних рейсів або ще не надходила телеметрія).",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 100.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun rememberTripMapVmFactory(): ViewModelProvider.Factory {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext as Application
                @Suppress("UNCHECKED_CAST")
                return TripMapViewModel(app) as T
            }
        }
    }
}
