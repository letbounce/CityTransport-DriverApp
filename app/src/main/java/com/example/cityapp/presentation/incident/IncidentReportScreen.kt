package com.example.cityapp.presentation.incident

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun IncidentReportScreen(
    waybillId: String,
    onSubmitted: () -> Unit,
    viewModel: IncidentReportViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.submitted) { if (state.submitted) onSubmitted() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.type,
            onValueChange = viewModel::onTypeChange,
            label = { Text("Тип інциденту") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Опис інциденту") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
        Button(
            onClick = { viewModel.submit(waybillId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Text("ВІДПРАВИТИ ЗВІТ")
        }
        state.error?.let { Text(it) }
    }
}
