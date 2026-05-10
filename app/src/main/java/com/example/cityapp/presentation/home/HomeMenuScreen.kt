@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeMenuScreen(
    onOpenWaybills: () -> Unit,
    onOpenIncidents: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeMenuViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmLogoutOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (confirmLogoutOpen) {
        AlertDialog(
            onDismissRequest = { confirmLogoutOpen = false },
            title = { Text("Вийти?") },
            text = { Text("Ви дійсно хочете вийти з облікового запису?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogoutOpen = false
                        viewModel.logout(onLoggedOut)
                    }
                ) {
                    Text("Вийти")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogoutOpen = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Головне меню")
                        state.driverDisplayName?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Оберіть розділ",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            MenuTile(
                title = "Дорожні листи",
                subtitle = "Створення, список і активний рейс",
                onClick = onOpenWaybills
            )
            MenuTile(
                title = "Інциденти",
                subtitle = "Журнал зареєстрованих подій",
                onClick = onOpenIncidents
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { confirmLogoutOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Вийти з облікового запису")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
