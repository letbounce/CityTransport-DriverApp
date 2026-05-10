@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.map

import android.Manifest
import android.app.Application
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.domain.model.LiveTripMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.concurrent.atomic.AtomicBoolean

private val KyivFallbackCenter = GeoPoint(50.4501, 30.5234)

private class TripRoutePolyline : Polyline()
private class TripTelemetryMarker(mapView: MapView) : Marker(mapView)
private class TripStopMarker(mapView: MapView) : Marker(mapView)

private fun bboxFrom(points: List<GeoPoint>): BoundingBox {
    if (points.isEmpty()) {
        return BoundingBox(50.52, 30.60, 50.49, 30.495)
    }
    val latNorth = points.maxOf { it.latitude }
    val latSouth = points.minOf { it.latitude }
    val lonEast = points.maxOf { it.longitude }
    val lonWest = points.minOf { it.longitude }
    val padLat = (latNorth - latSouth).coerceAtLeast(0.006) * 0.12
    val padLon = (lonEast - lonWest).coerceAtLeast(0.006) * 0.12
    return BoundingBox(latNorth + padLat, lonEast + padLon, latSouth - padLat, lonWest - padLon)
}

private fun syncRouteOverlays(
    map: MapView,
    model: TripRouteMapModel,
    telemetryMarkers: List<LiveTripMarker>,
    showDeviceGps: Boolean,
    devLat: Double?,
    devLng: Double?
) {
    map.overlays.removeAll {
        it is TripRoutePolyline || it is TripTelemetryMarker || it is TripStopMarker
    }

    val selfRoute = telemetryMarkers.find { it.isSelf }?.routeNumber?.trim()
    val emphasizeRoute = selfRoute != null && selfRoute == model.routeNumber.trim()

    model.polylineSegments.forEach { segment ->
        val pts = segment.map { (lat, lon) -> GeoPoint(lat, lon) }.toMutableList()
        if (pts.size < 2) return@forEach
        val poly = TripRoutePolyline().apply {
            outlinePaint.color = AndroidColor.parseColor(if (emphasizeRoute) "#0D47A1" else "#1565C0")
            outlinePaint.strokeWidth = if (emphasizeRoute) 16f else 11f
            outlinePaint.isAntiAlias = true
            setPoints(pts)
            title = model.routeTitle
        }
        map.overlays.add(poly)
    }

    model.stops.forEach { s ->
        val mk = TripStopMarker(map).apply {
            position = GeoPoint(s.lat, s.lon)
            title = s.title
            snippet = s.subtitle
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(mk)
    }

    telemetryMarkers.forEach { m ->
        val mk = TripTelemetryMarker(map).apply {
            position = GeoPoint(m.lat, m.lng)
            title = if (m.isSelf) "Ви · маршрут ${m.routeNumber}" else "Маршрут ${m.routeNumber}"
            snippet = m.driverId
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(mk)
    }

    if (showDeviceGps && devLat != null && devLng != null) {
        val mk = TripTelemetryMarker(map).apply {
            position = GeoPoint(devLat, devLng)
            title = "Ваш пристрій (GPS)"
            snippet = "GPS"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(mk)
    }

    map.invalidate()
}

private fun allPointsForInitialBBox(model: TripRouteMapModel): List<GeoPoint> {
    val pts = model.polylineSegments.flatMap { seg ->
        seg.map { (lat, lon) -> GeoPoint(lat, lon) }
    }.toMutableList()
    model.stops.forEach { pts.add(GeoPoint(it.lat, it.lon)) }
    return pts
}

@Composable
fun TripMapRouteScreen(
    routeId: String,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: TripMapViewModel = viewModel(factory = rememberTripMapVmFactory())
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val catalogEntry = remember(routeId) { TripMapCatalog.findById(routeId) }

    var routeModel by remember(routeId) {
        mutableStateOf<TripRouteMapModel?>(null)
    }
    LaunchedEffect(routeId, appContext) {
        val entry = TripMapCatalog.findById(routeId)
        routeModel = if (entry != null) {
            withContext(Dispatchers.IO) { TripRouteAssetLoader.load(appContext, entry) }
        } else {
            null
        }
    }

    val mapView = remember(appContext) {
        MapView(appContext).apply {
            setDestroyMode(false)
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            controller.setZoom(11.0)
            controller.setCenter(KyivFallbackCenter)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val modelState by rememberUpdatedState(routeModel)
    val telemetryState by rememberUpdatedState(state.markers)
    val showGpsState by rememberUpdatedState(state.showDeviceGpsOnMap)
    val myLatState by rememberUpdatedState(state.myLat)
    val myLngState by rememberUpdatedState(state.myLng)

    val zoomOncePerModel = remember(routeModel) { AtomicBoolean(false) }

    var centerOnStopNonce by remember { mutableIntStateOf(0) }
    var pendingStopCenter by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    LaunchedEffect(centerOnStopNonce, pendingStopCenter) {
        val ll = pendingStopCenter ?: return@LaunchedEffect
        mapView.controller.animateTo(GeoPoint(ll.first, ll.second))
        mapView.controller.setZoom(17.0)
        pendingStopCenter = null
    }

    LaunchedEffect(state.myLat, state.myLng, state.showDeviceGpsOnMap) {
        if (!state.showDeviceGpsOnMap) return@LaunchedEffect
        val lat = state.myLat ?: return@LaunchedEffect
        val lng = state.myLng ?: return@LaunchedEffect
        mapView.post {
            mapView.controller.animateTo(GeoPoint(lat, lng))
            mapView.controller.setZoom(15.0)
        }
    }

    val locPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.requestDeviceGpsForRouteArea()
    }

    if (catalogEntry == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Маршрут") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
                )
            }
        ) { pad ->
            Text(
                "Невідомий маршрут «$routeId».",
                Modifier.padding(pad).padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxHeight(0.92f)) {
                Text(
                    "Зупинки · ${catalogEntry.tileTitle}",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                val stops = routeModel?.stops.orEmpty()
                if (stops.isEmpty()) {
                    Text(
                        "Немає точок зупинок у файлі маршруту.",
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(Modifier.padding(horizontal = 8.dp)) {
                        items(stops, key = { "${it.orderIndex}_${it.lat}_${it.lon}" }) { stop ->
                            NavigationDrawerItem(
                                label = {
                                    Column(Modifier.padding(vertical = 4.dp)) {
                                        Text(stop.title)
                                        stop.subtitle?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                },
                                selected = false,
                                onClick = {
                                    pendingStopCenter = stop.lat to stop.lon
                                    centerOnStopNonce++
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(catalogEntry.tileTitle) },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("Назад") }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.currentValue == DrawerValue.Open) drawerState.close()
                                    else drawerState.open()
                                }
                            }
                        ) {
                            Text("Зупинки")
                        }
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
                            viewModel.dismissGpsHint()
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (granted) viewModel.requestDeviceGpsForRouteArea()
                            else locPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Моє GPS на маршруті")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigateHome,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("На головне меню")
                    }
                }
            }
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                val rm = routeModel
                if (rm == null) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { mapView },
                        update = { map ->
                            syncRouteOverlays(
                                map,
                                rm,
                                telemetryState,
                                showGpsState,
                                myLatState,
                                myLngState
                            )
                            if (zoomOncePerModel.compareAndSet(false, true)) {
                                map.post {
                                    val pts = allPointsForInitialBBox(rm).toMutableList()
                                    telemetryState.forEach { pts.add(GeoPoint(it.lat, it.lng)) }
                                    val bb = bboxFrom(pts)
                                    map.zoomToBoundingBox(bb, true, 120)
                                }
                            }
                        }
                    )
                    if (state.loading) {
                        CircularProgressIndicator(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        )
                    }
                }

                routeModel?.let { m ->
                    Text(
                        if (!m.isApproximate) {
                            "OpenStreetMap · дані маршруту з OSM (Overpass, ODbL) · телеметрія активних рейсів."
                        } else {
                            "OpenStreetMap · наближена лінія маршруту · перевірте asset GeoJSON."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                state.gpsHint?.let { hint ->
                    Text(
                        hint,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 160.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
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
                if (!state.loading && routeModel != null && state.markers.isEmpty()) {
                    Text(
                        "Немає маркерів телеметрії для активних рейсів.",
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
