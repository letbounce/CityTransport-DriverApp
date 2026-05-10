package com.example.cityapp.presentation.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RouteDashboardScreen(
    onStartTrip: (String) -> Unit,
    viewModel: RouteDashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadRoutes() }
    LaunchedEffect(state.activeWaybillId) { state.activeWaybillId?.let(onStartTrip) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Route Dashboard")
        val route = state.routes.firstOrNull()
        if (route != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Route #${route.routeNumber}: ${route.routeName}")
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(route.stops) { stop ->
                            Text("Stop ${stop.stopNumber} | ${stop.plannedTime} | ${stop.name}")
                        }
                    }
                    Button(
                        onClick = { viewModel.startTrip(route.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        Text("ОТРИМАТИ ДОРОЖНІЙ ЛИСТ")
                    }
                }
            }
        }
        state.error?.let { Text(it) }
    }
}
