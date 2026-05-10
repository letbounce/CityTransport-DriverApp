@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.incident

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun IncidentReportScreen(
    waybillId: String,
    onSubmitted: () -> Unit,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: IncidentReportViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.submitted) { if (state.submitted) onSubmitted() }

    var typeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Звіт про інцидент") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = state.incidentType.labelUa,
                    onValueChange = {},
                    label = { Text("Тип інциденту") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    IncidentApiType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.labelUa) },
                            onClick = {
                                viewModel.onIncidentTypeSelected(type)
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            TextButton(
                onClick = viewModel::applyTemplateForCurrentType,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Вставити шаблон опису для обраного типу")
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Опис інциденту") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                minLines = 8,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
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
            Spacer(Modifier.height(24.dp))
        }
    }
}
