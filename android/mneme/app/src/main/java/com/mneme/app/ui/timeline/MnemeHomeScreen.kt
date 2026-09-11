package com.mneme.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mneme.app.data.CardStatus
import com.mneme.app.data.NoteEntity
import java.text.DateFormat
import java.util.Date

private fun pitchSubtitle(ui: MnemeUiState): String {
    if (ui.pitchOverlay == PitchOverlay.Pulse) return "Reading the screen you pointed at."
    if (ui.pitchOverlay == PitchOverlay.Focus) return "Timed live screen + voice → cards."
    return when (ui.tab) {
        MnemeTab.Ask -> "Ask your personal assistant — every answer is cited."
        MnemeTab.Settings -> "Control what Retenqo remembers."
        MnemeTab.Memory -> "Your personal assistant, grounded in what you already know."
    }
}

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
    val shown = ui.pinnedNotes ?: ui.searchResults ?: notes
    val motion = rememberInfiniteTransition(label = "ret-enqo-motion")
    val statusAlpha by motion.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "status-pulse",
    )

    if (ui.pitchOverlay == PitchOverlay.Messy) {
        PitchMessyScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Gallery) {
        PitchGalleryScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Action) {
        var done by remember { mutableStateOf(false) }
        PitchActionScreen(done = done, onDone = { done = true })
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Live) {
        PitchLiveFlowScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Meeting) {
        PitchMeetingScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Brief) {
        PitchDailyBriefScreen()
        return
    }
    if (ui.pitchOverlay == PitchOverlay.Chat) {
        PitchAssistantChatScreen()
        return
    }

    val gradient = Brush.verticalGradient(
        0f to Color(0xFF101817),
        0.48f to MaterialTheme.colorScheme.background,
        1f to Color(0xFF090D0E),
    )

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (ui.tab == MnemeTab.Memory && ui.pinnedNotes == null && ui.pitchOverlay == PitchOverlay.None) {
                val fabScale by animateFloatAsState(
                    targetValue = if (ui.pulsing) 0.92f else 1f,
                    animationSpec = tween(180),
                    label = "pulse-scale",
                )
                FloatingActionButton(
                    onClick = onPulse,
                    modifier = Modifier.graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    },
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
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp)),
                containerColor = Color(0xFF111A17),
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = ui.tab == MnemeTab.Memory,
                    onClick = { viewModel.setTab(MnemeTab.Memory) },
                    icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                    label = { Text("Memory") },
                    colors = navItemColors(),
                )
                NavigationBarItem(
                    selected = ui.tab == MnemeTab.Ask,
                    onClick = { viewModel.setTab(MnemeTab.Ask) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                    label = { Text("Ask") },
                    colors = navItemColors(),
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
                    colors = navItemColors(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("R", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Column {
                        Text(
                            text = "RETENQO",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "PERSONAL MEMORY ASSISTANT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .graphicsLayer { alpha = statusAlpha }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f), CircleShape),
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = when (ui.tab) {
                    MnemeTab.Ask -> "Ask anything."
                    MnemeTab.Settings -> "Tune your memory."
                    MnemeTab.Memory -> "Your day, remembered."
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = pitchSubtitle(ui),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
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
                if (ui.tab == MnemeTab.Memory && ui.pinnedNotes == null) {
                    TextButton(onClick = viewModel::showBrief, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Brief")
                    }
                    TextButton(onClick = viewModel::showWhatsDue, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Due")
                    }
                    if (ui.showSetupBanner) {
                        TextButton(onClick = { viewModel.setTab(MnemeTab.Settings) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
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
                    showBrief = ui.showBrief,
                    briefHeadline = ui.briefHeadline,
                    briefBody = ui.briefBody,
                    onDismissBrief = viewModel::dismissBrief,
                    hideSearch = ui.pinnedNotes != null,
                    lead = {
                        when (ui.pitchOverlay) {
                            PitchOverlay.Pulse -> PitchPulseBody()
                            PitchOverlay.Focus -> PitchFocusBody()
                            else -> {}
                        }
                    },
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
                    onOpenAccessibility = viewModel::openAccessibilitySettings,
                    onRequestMic = onRequestMicPermission,
                    onOpenExactAlarm = viewModel::openExactAlarmSettings,
                    onScanWhatsApp = viewModel::scanWhatsAppMedia,
                    onDemoSample = viewModel::ingestDemoSample,
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
    showBrief: Boolean,
    briefHeadline: String,
    briefBody: String,
    onDismissBrief: () -> Unit,
    hideSearch: Boolean = false,
    lead: @Composable () -> Unit = {},
) {
    lead()
    if (!hideSearch) {
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
    } else {
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showBrief) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF10261F), RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("New since last open", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(briefHeadline, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = briefBody.replace("New since last open:", "").trim().replace("\n", " · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onDismissBrief, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Dismiss") }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }

    if (shown.isEmpty()) {
        EmptyMemory(
            modifier = Modifier.fillMaxWidth().padding(vertical = 52.dp),
            title = if (showBrief) "No saved cards yet" else "Nothing saved yet",
            body = if (showBrief) "New activity is shown above. Save a note or use Pulse to build memory." else "Tap Pulse on anything worth remembering.",
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 148.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(if (hideSearch) "Due now" else "All memories", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (hideSearch) "Needs your attention" else "Your private context, organized",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("${shown.size} ${if (shown.size == 1) "card" else "cards"}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            items(shown, key = { it.id }) { note ->
                NoteRow(
                    note = note,
                    showActions = !hideSearch,
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
    val micScale by animateFloatAsState(
        targetValue = if (listening) 1.08f else 1f,
        animationSpec = tween(220),
        label = "mic-scale",
    )
    if (answer.isNullOrBlank() && !busy) {
        AssistantHero(listening = listening)
        Spacer(modifier = Modifier.height(14.dp))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f), RoundedCornerShape(22.dp))
            .padding(10.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !busy && !listening,
            placeholder = { Text(text = "Ask your memory anything…") },
            shape = RoundedCornerShape(16.dp),
            colors = composerFieldColors(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickPrompt("What do I owe?", onClick = { onQuery("What do I owe?") })
            QuickPrompt("Notifications", onClick = { onQuery("Read my notifications") })
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onAsk,
                enabled = !busy && !listening && query.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = micScale
                        scaleY = micScale
                    }
                    .background(
                        color = if (listening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
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
                Icon(Icons.Default.Mic, contentDescription = "Hold to talk", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
    Text(
        text = when {
            listening -> "Listening… release to send"
            busy -> "Retenqo is thinking…"
            else -> "Type or hold the mic to talk"
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (busy) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 148.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!answer.isNullOrBlank()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 4 } + expandVertically(tween(260)),
                    exit = fadeOut(tween(120)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10261F), RoundedCornerShape(18.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f), RoundedCornerShape(18.dp))
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
private fun AssistantHero(listening: Boolean) {
    val orbScale by animateFloatAsState(
        targetValue = if (listening) 1.12f else 1f,
        animationSpec = tween(420),
        label = "assistant-orb",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(146.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFF183E35), Color(0xFF10191A), Color(0xFF10151D))),
                RoundedCornerShape(24.dp),
            )
            .border(1.dp, Color(0xFF4BAE91).copy(alpha = .26f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .graphicsLayer { scaleX = orbScale; scaleY = orbScale }
                .background(
                    Brush.radialGradient(listOf(Color(0xFFD5FFF0), Color(0xFF55D9B2), Color(0xFF1A5C4B))),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF092019).copy(alpha = .92f), CircleShape),
            )
        }
        Spacer(modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (listening) "Listening" else "Private memory online",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (listening) "Release the mic when you’re done." else "Ask by text or voice. Answers stay grounded in your data.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB7D6CC),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("GEMINI  ·  CITED", style = MaterialTheme.typography.labelMedium, color = Color(0xFF72E0BE))
        }
    }
}

@Composable
private fun MemoryHero(memoryCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFF182A27), Color(0xFF11191A))),
                RoundedCornerShape(22.dp),
            )
            .border(1.dp, Color(0xFF3B6659).copy(alpha = .45f), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Memory at a glance", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Your private context, organized.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9DB9B0))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(memoryCount.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("cards", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickPrompt(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        shape = RoundedCornerShape(50),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

@Composable
private fun composerFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun EmptyMemory(
    modifier: Modifier = Modifier,
    title: String = "Nothing saved yet",
    body: String = "Tap Pulse on anything worth remembering.",
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
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
    val bodyText = note.body.trim()
    val titleText = note.title.removePrefix("Task:").trim()
    val cleanedBody = bodyText
        .removePrefix(note.source)
        .trimStart(' ', '\n', '·', ':')
        .trim()
    val showBody = cleanedBody.isNotBlank() &&
        cleanedBody != titleText &&
        !note.title.contains(cleanedBody, ignoreCase = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                shape = RoundedCornerShape(18.dp),
            )
        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                    text = titleText,
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
        if (showBody) {
            Text(
                text = cleanedBody,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
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
