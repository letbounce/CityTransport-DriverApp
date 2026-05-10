@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.incidents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.presentation.incident.IncidentApiType

private fun incidentTypeUa(api: String) = IncidentApiType.fromApi(api).labelUa

private fun incidentStatusUa(status: String): String = when (status) {
    "open" -> "Відкрито"
    "resolved" -> "Закрито"
    else -> status
}

@Composable
fun IncidentsListScreen(
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: IncidentsListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Журнал інцидентів") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Дані з MongoDB для поточного водія",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            if (state.error != null) {
                item { Text(state.error!!) }
            }
            items(state.incidents, key = { it.id }) { incident ->
                IncidentCard(incident)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun IncidentCard(incident: IncidentItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${incidentTypeUa(incident.type)} · ${incidentStatusUa(incident.status)}",
                style = MaterialTheme.typography.titleMedium
            )
            incident.reportedAt?.let {
                Text("Час: $it", style = MaterialTheme.typography.bodySmall)
            }
            Text("Дорожній лист: ${incident.waybillId}", style = MaterialTheme.typography.bodySmall)
            Text(incident.description.ifBlank { "— немає опису —" })
            Text(
                "Координати: ${"%.5f".format(incident.lat)}, ${"%.5f".format(incident.lng)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
