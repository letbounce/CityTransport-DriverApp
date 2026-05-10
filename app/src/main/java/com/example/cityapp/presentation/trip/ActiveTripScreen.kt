package com.example.cityapp.presentation.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ActiveTripScreen(
    waybillId: String,
    onOpenIncident: (String) -> Unit,
    onTripCompleted: () -> Unit,
    viewModel: ActiveTripViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) { if (state.completed) onTripCompleted() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Рейс В ДОРОЗІ")
        Text("Waybill: $waybillId")
        Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
        Button(
            onClick = { viewModel.completeTrip(waybillId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Text("ЗАВЕРШИТИ РЕЙС")
        }
        Button(
            onClick = { onOpenIncident(waybillId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Text("ІНЦИДЕНТ")
        }
        state.error?.let { Text(it) }
    }
}
