package com.mneme.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mneme.app.data.CardStatus
import com.mneme.app.data.NoteEntity
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemeHomeScreen(
    viewModel: MnemeViewModel,
    onPulse: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestMedia: () -> Unit,
    onRequestMicPermission: () -> Unit,
) {
    val notes by viewModel.timeline.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val shown = ui.searchResults ?: notes

    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (ui.tab == MnemeTab.Memory) {
                FloatingActionButton(
                    onClick = onPulse,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = "Pulse",
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
                NavigationBarItem(
                    selected = ui.tab == MnemeTab.Memory,
                    onClick = { viewModel.setTab(MnemeTab.Memory) },
                    icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                    label = { Text("Memory") },
                )
                NavigationBarItem(
                    selected = ui.tab == MnemeTab.Ask,
                    onClick = { viewModel.setTab(MnemeTab.Ask) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                    label = { Text("Ask") },
                )
                NavigationBarItem(
                    selected = ui.tab == MnemeTab.Settings,
                    onClick = { viewModel.setTab(MnemeTab.Settings) },
                    icon = {
                        Icon(
                            if (ui.tab == MnemeTab.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Settings") },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Retenqo",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = when (ui.tab) {
                    MnemeTab.Memory -> "Your private memory stays on this phone."
                    MnemeTab.Ask -> "Ask only from what you saved."
                    MnemeTab.Settings -> "Permissions and capture setup."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = ui.status,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (ui.tab == MnemeTab.Memory) {
                    TextButton(onClick = viewModel::showWhatsDue) {
                        Text("Due")
                    }
                    if (ui.showSetupBanner) {
                        TextButton(onClick = { viewModel.setTab(MnemeTab.Settings) }) {
                            Text("Setup")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (ui.tab) {
                MnemeTab.Memory -> MemoryPane(
                    shown = shown,
                    query = ui.searchQuery,
                    onQuery = viewModel::setSearch,
                    onDelete = viewModel::deleteNote,
                    onDone = viewModel::markDone,
                    onSnooze = viewModel::snoozeOneDay,
                    onReopen = viewModel::reopenCard,
                )
                MnemeTab.Ask -> AskPane(
                    query = ui.askQuery,
                    busy = ui.askBusy,
                    listening = ui.listening,
                    answer = ui.askAnswer,
                    citations = ui.askCitations,
                    onQuery = viewModel::setAskQuery,
                    onAsk = viewModel::submitAsk,
                    onMicDown = onRequestMic,
                    onMicUp = { viewModel.stopListening(submit = true) },
                )
                MnemeTab.Settings -> SettingsPane(
                    setup = ui.setup,
                    mediaBusy = ui.mediaBusy,
                    showBanner = ui.showSetupBanner,
                    onDismissBanner = viewModel::dismissSetupBanner,
                    onRequestPostNotifications = onRequestPostNotifications,
                    onOpenNotifListener = viewModel::openNotifAccessSettings,
                    onRequestMedia = onRequestMedia,
                    onOpenAllFiles = viewModel::openAllFilesAccess,
                    onRequestMic = onRequestMicPermission,
                    onOpenExactAlarm = viewModel::openExactAlarmSettings,
                    onScanWhatsApp = viewModel::scanWhatsAppMedia,
                    onOpenAppDetails = viewModel::openAppDetails,
                )
            }
        }
    }
}

@Composable
private fun MemoryPane(
    shown: List<NoteEntity>,
    query: String,
    onQuery: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onDone: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onReopen: (Long) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(text = "Search memory") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (shown.isEmpty()) {
        EmptyMemory(modifier = Modifier.fillMaxSize())
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shown, key = { it.id }) { note ->
                NoteRow(
                    note = note,
                    onDelete = { onDelete(note.id) },
                    onDone = { onDone(note.id) },
                    onSnooze = { onSnooze(note.id) },
                    onReopen = { onReopen(note.id) },
                )
            }
        }
    }
}

@Composable
private fun AskPane(
    query: String,
    busy: Boolean,
    listening: Boolean,
    answer: String?,
    citations: List<NoteEntity>,
    onQuery: (String) -> Unit,
    onAsk: () -> Unit,
    onMicDown: () -> Unit,
    onMicUp: () -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !busy && !listening,
        placeholder = { Text(text = "When is chapter 11 due?") },
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            onClick = onAsk,
            enabled = !busy && !listening && query.isNotBlank(),
        ) {
            Text(
                when {
                    listening -> "Listening…"
                    busy -> "Thinking on-device…"
                    else -> "Ask memory"
                },
            )
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (listening) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    shape = CircleShape,
                )
                .pointerInput(busy) {
                    detectTapGestures(
                        onPress = {
                            if (busy) return@detectTapGestures
                            onMicDown()
                            try {
                                tryAwaitRelease()
                            } finally {
                                onMicUp()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Hold to talk",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
    Text(
        text = "Hold mic to ask by voice",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (busy) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!answer.isNullOrBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            RoundedCornerShape(18.dp),
                        )
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Answer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = answer, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (citations.isNotEmpty()) {
            item {
                Text(
                    text = "Cited notes",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(citations, key = { "c-${it.id}" }) { note ->
                NoteRow(note = note, showActions = false)
            }
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

@Composable
private fun EmptyMemory(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Nothing saved yet", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap Pulse on anything worth remembering.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoteRow(
    note: NoteEntity,
    showActions: Boolean = true,
    onDelete: () -> Unit = {},
    onDone: () -> Unit = {},
    onSnooze: () -> Unit = {},
    onReopen: () -> Unit = {},
) {
    val type = note.type()
    val status = note.status()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    if (status != CardStatus.OPEN) {
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(note.source)
                        note.who?.takeIf { it.isNotBlank() }?.let {
                            append(" · ")
                            append(it)
                        }
                        append(" · ")
                        append(
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(note.createdAt)),
                        )
                        note.dueAt?.let { due ->
                            append(" · due ")
                            append(
                                DateFormat.getDateInstance(DateFormat.MEDIUM)
                                    .format(Date(due)),
                            )
                        }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showActions) {
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = note.body,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (showActions) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (status) {
                    CardStatus.DONE -> {
                        TextButton(onClick = onReopen) { Text("Reopen") }
                    }
                    else -> {
                        TextButton(onClick = onDone) { Text("Done") }
                        TextButton(onClick = onSnooze) { Text("Snooze 1d") }
                    }
                }
            }
        }
    }
}
