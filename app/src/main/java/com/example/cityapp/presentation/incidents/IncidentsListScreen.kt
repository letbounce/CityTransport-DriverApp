@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cityapp.presentation.incidents

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.presentation.common.ArchiveReasonDialog
import com.example.cityapp.presentation.common.ArchiveReasonOption
import com.example.cityapp.presentation.incident.IncidentApiType
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private fun incidentTypeUa(api: String) = IncidentApiType.fromApi(api).labelUa

private fun incidentStatusUa(status: String): String = when (status) {
    "open" -> "Відкрито"
    "resolved" -> "Закрито"
    "completed" -> "Завершено"
    else -> status
}

private fun formatIncidentForCopy(i: IncidentItem): String {
    val reason = i.deletionReasonCode?.let { code ->
        ArchiveReasonOption.entries.find { it.code == code }?.labelUa ?: code
    }
    return buildString {
        appendLine("Інцидент ${incidentTypeUa(i.type)}")
        appendLine("Статус: ${incidentStatusUa(i.status)}")
        if (i.isModified) appendLine("Змінений (є версії в БД)")
        appendLine("ID: ${i.id}")
        appendLine("Дорожній лист: ${i.waybillId}")
        i.reportedAt?.let { appendLine("Час події: $it") }
        if (i.stopLabel.isNotBlank()) appendLine("Зупинка: ${i.stopLabel}")
        appendLine("Можна рухатися самостійно: ${if (i.canMoveIndependently) "так" else "ні"}")
        appendLine("Опис: ${i.description}")
        appendLine("Координати: ${"%.5f".format(i.lat)}, ${"%.5f".format(i.lng)}")
        i.photoUrl?.let { appendLine("Фото: $it") }
        i.deletedAt?.let { appendLine("Архівовано: $it") }
        reason?.let { appendLine("Причина архіву: $it") }
        i.deletionReasonNote?.takeIf { it.isNotBlank() }?.let { appendLine("Примітка: $it") }
    }.trim()
}

@Composable
fun IncidentsListScreen(
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onEditIncident: (String) -> Unit,
    viewModel: IncidentsListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var archiveIncidentTarget by remember { mutableStateOf<IncidentItem?>(null) }
    var copyIncidentTarget by remember { mutableStateOf<IncidentItem?>(null) }
    var photoViewerUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(state.feedbackMessage) {
        if (state.feedbackMessage != null) {
            delay(2800)
            viewModel.consumeFeedback()
        }
    }

    archiveIncidentTarget?.let { target ->
        ArchiveReasonDialog(
            title = "Архівувати інцидент?",
            confirmLabel = "Архівувати",
            onDismiss = { archiveIncidentTarget = null },
            onConfirm = { code, note ->
                viewModel.archiveIncident(target.id, code, note)
                archiveIncidentTarget = null
            }
        )
    }

    photoViewerUrl?.let { url ->
        Dialog(onDismissRequest = { photoViewerUrl = null }) {
            Surface(shape = RoundedCornerShape(12.dp)) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 480.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    copyIncidentTarget?.let { inc ->
        AlertDialog(
            onDismissRequest = { copyIncidentTarget = null },
            title = { Text("Копіювати запис?") },
            text = { Text("Текст інциденту буде в буфері обміну.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(formatIncidentForCopy(inc)))
                        Toast.makeText(context, "Скопійовано", Toast.LENGTH_SHORT).show()
                        copyIncidentTarget = null
                    }
                ) {
                    Text("Копіювати")
                }
            },
            dismissButton = {
                TextButton(onClick = { copyIncidentTarget = null }) {
                    Text("Скасувати")
                }
            }
        )
    }

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
                    text = "Лише ваші записи.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            state.feedbackMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(msg, Modifier.padding(12.dp))
                    }
                }
            }

            if (state.error != null) {
                item { Text(state.error!!, color = MaterialTheme.colorScheme.error) }
            }

            item {
                Text("Активні", style = MaterialTheme.typography.titleMedium)
            }

            items(state.incidents, key = { it.id }) { incident ->
                IncidentCard(
                    incident = incident,
                    isArchived = false,
                    onArchivedClick = null,
                    onRequestArchive = { archiveIncidentTarget = incident },
                    onEdit = { onEditIncident(incident.id) },
                    onPhotoClick = { photoViewerUrl = it }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Неактивні (архів)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Натисніть картку, щоб скопіювати дані.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            if (state.archivedIncidents.isEmpty()) {
                item {
                    Text(
                        "Архів порожній",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.archivedIncidents, key = { "arch_${it.id}" }) { incident ->
                    IncidentCard(
                        incident = incident,
                        isArchived = true,
                        onArchivedClick = { copyIncidentTarget = incident },
                        onRequestArchive = null,
                        onEdit = null,
                        onPhotoClick = { photoViewerUrl = it }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun IncidentCard(
    incident: IncidentItem,
    isArchived: Boolean,
    onArchivedClick: (() -> Unit)?,
    onRequestArchive: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onPhotoClick: ((String) -> Unit)? = null
) {
    val mutedColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val normalColors = CardDefaults.cardColors()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isArchived && onArchivedClick != null) {
                    Modifier.clickable(onClick = onArchivedClick)
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isArchived) 0.dp else 2.dp),
        colors = if (isArchived) mutedColors else normalColors
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${incidentTypeUa(incident.type)} · ${incidentStatusUa(incident.status)}",
                style = MaterialTheme.typography.titleMedium
            )
            if (incident.isModified) {
                Text(
                    "Змінений",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            incident.reportedAt?.let {
                Text("Час: $it", style = MaterialTheme.typography.bodySmall)
            }
            incident.deletedAt?.let {
                Text("Архівовано: $it", style = MaterialTheme.typography.bodySmall)
            }
            incident.deletionReasonCode?.let { code ->
                val label = ArchiveReasonOption.entries.find { it.code == code }?.labelUa ?: code
                Text("Причина архіву: $label", style = MaterialTheme.typography.bodySmall)
            }
            incident.deletionReasonNote?.takeIf { it.isNotBlank() }?.let {
                Text("Примітка: $it", style = MaterialTheme.typography.bodySmall)
            }
            Text("Дорожній лист: ${incident.waybillId}", style = MaterialTheme.typography.bodySmall)
            if (incident.stopLabel.isNotBlank()) {
                Text("Зупинка: ${incident.stopLabel}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Самостійний рух: ${if (incident.canMoveIndependently) "так" else "ні"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(incident.description.ifBlank { "— немає опису —" })
            Text(
                "Координати: ${"%.5f".format(incident.lat)}, ${"%.5f".format(incident.lng)}",
                style = MaterialTheme.typography.bodySmall
            )
            incident.photoUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .then(
                            if (onPhotoClick != null) {
                                Modifier.clickable { onPhotoClick(url) }
                            } else {
                                Modifier
                            }
                        ),
                    contentScale = ContentScale.Crop
                )
                if (onPhotoClick != null) {
                    Text(
                        "Торкніться фото для збільшення",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isArchived && onEdit != null) {
                TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Редагувати")
                }
            }
            if (!isArchived && onRequestArchive != null) {
                TextButton(onClick = onRequestArchive, modifier = Modifier.fillMaxWidth()) {
                    Text("В архів…", color = MaterialTheme.colorScheme.error)
                }
            }
            if (isArchived) {
                Text(
                    "Натисніть картку для копіювання",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
