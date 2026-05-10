@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.presentation.common.AppButtonShape
import com.example.cityapp.presentation.common.AppCardShape
import com.example.cityapp.presentation.common.primaryHorizontalGradient

@Composable
fun HomeMenuScreen(
    onOpenWaybills: () -> Unit,
    onOpenIncidents: () -> Unit,
    onOpenTripMap: () -> Unit,
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.driverDisplayName ?: "Водій",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Головне меню",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Оберіть розділ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            MenuTile(
                title = "Дорожні листи",
                subtitle = "Створення, список і активний рейс",
                leadingIcon = Icons.Outlined.FolderOpen,
                trailingIcon = Icons.Outlined.Map,
                onClick = onOpenWaybills
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            )
            MenuTile(
                title = "Інциденти",
                subtitle = "Журнал зареєстрованих подій",
                leadingIcon = Icons.Outlined.WarningAmber,
                trailingIcon = null,
                onClick = onOpenIncidents
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            )
            MenuTile(
                title = "Мапа рейсів",
                subtitle = "Активні рейси та позиції з телеметрії",
                leadingIcon = Icons.Outlined.Public,
                trailingIcon = Icons.Outlined.Map,
                onClick = onOpenTripMap
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { confirmLogoutOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = AppButtonShape,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Text("Вийти", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuTile(
    title: String,
    subtitle: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(brush = primaryHorizontalGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
