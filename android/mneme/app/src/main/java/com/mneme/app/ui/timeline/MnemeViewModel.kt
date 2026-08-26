package com.mneme.app.ui.timeline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mneme.app.ai.AskResult
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.MemoryAsk
import com.mneme.app.data.IngestSources
import com.mneme.app.data.NoteEntity
import com.mneme.app.data.NoteRepository
import com.mneme.app.media.MediaIngestPipeline
import com.mneme.app.media.MnemePowerGates
import com.mneme.app.notif.MnemeNotificationListener
import com.mneme.app.pulse.PulsePipeline
import com.mneme.app.pulse.PulseResult
import com.mneme.app.remind.DueReminderScheduler
import com.mneme.app.settings.AppSetup
import com.mneme.app.settings.AppSetupStatus
import com.mneme.app.voice.AlarmScheduler
import com.mneme.app.voice.MicRecorder
import com.mneme.app.voice.TtsSpeaker
import com.mneme.app.voice.VoiceIntent
import com.mneme.app.voice.VoiceIntentRouter
import com.mneme.app.voice.WhisperBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MnemeTab { Memory, Ask, Settings }

data class MnemeUiState(
    val tab: MnemeTab = MnemeTab.Memory,
    val status: String = "Ready",
    val pulsing: Boolean = false,
    val mediaBusy: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<NoteEntity>? = null,
    val askQuery: String = "",
    val askBusy: Boolean = false,
    val askAnswer: String? = null,
    val askCitations: List<NoteEntity> = emptyList(),
    val listening: Boolean = false,
    val notifAccess: Boolean = false,
    val setup: AppSetupStatus = AppSetupStatus(
        postNotifications = false,
        notifListener = false,
        mediaImages = false,
        microphone = false,
        exactAlarms = false,
        allFilesAccess = false,
    ),
    val showSetupBanner: Boolean = false,
)

class MnemeViewModel(
    app: Application,
    private val notes: NoteRepository,
    private val pulse: PulsePipeline,
    private val media: MediaIngestPipeline,
    private val asker: MemoryAsk,
    private val llm: LlmBridge,
    private val whisper: WhisperBridge,
    private val tts: TtsSpeaker,
    private val alarms: AlarmScheduler,
) : AndroidViewModel(app) {
    val timeline = notes.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(MnemeUiState())
    val ui: StateFlow<MnemeUiState> = _ui.asStateFlow()

    private val mic = MicRecorder()
    private var pumpJob: Job? = null

    init {
        refreshSetup(openSettingsIfNeeded = true)
    }

    fun refreshSetup(openSettingsIfNeeded: Boolean = false) {
        val app = getApplication<Application>()
        val setup = AppSetup.snapshot(app)
        val unseen = !AppSetup.hasSeenSetup(app)
        val openSetup = openSettingsIfNeeded && unseen && setup.missingCritical
        _ui.value = _ui.value.copy(
            notifAccess = setup.notifListener,
            setup = setup,
            showSetupBanner = setup.missingCritical || !setup.readyForDailyUse,
            tab = if (openSetup) MnemeTab.Settings else _ui.value.tab,
            status = when {
                openSetup -> "Finish setup so Retenqo can capture what matters"
                setup.readyForDailyUse -> _ui.value.status.ifBlank { "Ready" }
                else -> _ui.value.status
            },
        )
        if (openSetup) AppSetup.markSetupSeen(app)
    }

    /** @deprecated use [refreshSetup] */
    fun refreshNotifAccess() = refreshSetup()

    fun openNotifAccessSettings() {
        MnemeNotificationListener.openSettings(getApplication())
    }

    fun openExactAlarmSettings() {
        AppSetup.openExactAlarmSettings(getApplication())
    }

    fun openAllFilesAccess() {
        AppSetup.openAllFilesAccess(getApplication())
    }

    fun openAppDetails() {
        AppSetup.openAppDetails(getApplication())
    }

    fun dismissSetupBanner() {
        AppSetup.markSetupSeen(getApplication())
        _ui.value = _ui.value.copy(showSetupBanner = false)
    }

    fun setTab(tab: MnemeTab) {
        if (_ui.value.listening) stopListening(submit = false)
        if (tab == MnemeTab.Settings) {
            AppSetup.markSetupSeen(getApplication())
            refreshSetup()
        }
        _ui.value = _ui.value.copy(tab = tab)
        if (tab != MnemeTab.Ask) {
            viewModelScope.launch {
                llm.unload()
                whisper.unload()
            }
        }
    }

    fun setSearch(query: String) {
        _ui.value = _ui.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _ui.value = _ui.value.copy(searchResults = null)
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(searchResults = notes.search(query))
        }
    }

    fun setAskQuery(query: String) {
        _ui.value = _ui.value.copy(askQuery = query)
    }

    fun submitAsk() {
        val q = _ui.value.askQuery.trim()
        if (q.isBlank() || _ui.value.askBusy) return
        handleUtterance(q)
    }

    fun startListening() {
        if (_ui.value.listening || _ui.value.askBusy) return
        try {
            mic.start()
            _ui.value = _ui.value.copy(listening = true, status = "Listening… hold mic")
            pumpJob = viewModelScope.launch { mic.pump() }
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(status = "Mic failed: ${e.message}")
        }
    }

    fun stopListening(submit: Boolean = true) {
        if (!_ui.value.listening) return
        pumpJob?.cancel()
        pumpJob = null
        val samples = mic.stopAndGetSamples()
        _ui.value = _ui.value.copy(listening = false)
        if (!submit) return
        if (samples.size < MicRecorder.SAMPLE_RATE / 2) {
            _ui.value = _ui.value.copy(status = "Too short — hold longer")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(askBusy = true, status = "Transcribing…")
            val text = try {
                whisper.transcribe(samples)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(askBusy = false, status = "Whisper failed: ${e.message}")
                return@launch
            }
            if (text.isBlank()) {
                _ui.value = _ui.value.copy(askBusy = false, status = "Heard nothing")
                return@launch
            }
            _ui.value = _ui.value.copy(askQuery = text)
            handleUtterance(text)
        }
    }

    private fun handleUtterance(text: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(askBusy = true, askAnswer = null, askQuery = text)
            when (val intent = VoiceIntentRouter.route(text)) {
                is VoiceIntent.WhatsDue -> {
                    _ui.value = _ui.value.copy(status = "Checking due dates…")
                    val result: AskResult = try {
                        asker.ask("what's due")
                    } catch (e: Exception) {
                        AskResult("Due check failed: ${e.message}", emptyList())
                    }
                    _ui.value = _ui.value.copy(
                        askBusy = false,
                        askAnswer = result.answer,
                        askCitations = result.citations,
                        status = if (result.citations.isEmpty()) "No dues" else "${result.citations.size} with due dates",
                    )
                    tts.speak(result.answer.take(160))
                }
                is VoiceIntent.SetAlarm -> {
                    val msg = try {
                        alarms.schedule(intent.hour, intent.minute, intent.label)
                    } catch (e: Exception) {
                        "Alarm failed: ${e.message}"
                    }
                    _ui.value = _ui.value.copy(
                        askBusy = false,
                        askAnswer = msg,
                        askCitations = emptyList(),
                        status = msg,
                    )
                    tts.speak(msg)
                }
                is VoiceIntent.Remember -> {
                    notes.saveIfNew(
                        title = intent.text.take(80),
                        body = intent.text,
                        source = "Voice",
                    )
                    val msg = "Saved to memory."
                    _ui.value = _ui.value.copy(
                        askBusy = false,
                        askAnswer = msg,
                        askCitations = emptyList(),
                        status = msg,
                    )
                    tts.speak(msg)
                }
                is VoiceIntent.Query -> {
                    _ui.value = _ui.value.copy(status = "Searching memory…")
                    val result: AskResult = try {
                        asker.ask(intent.text)
                    } catch (e: Exception) {
                        AskResult("Ask failed: ${e.message}", emptyList())
                    }
                    _ui.value = _ui.value.copy(
                        askBusy = false,
                        askAnswer = result.answer,
                        askCitations = result.citations,
                        status = if (result.citations.isEmpty()) {
                            "No matches"
                        } else {
                            "Answered from ${result.citations.size} notes"
                        },
                    )
                    tts.speak(result.answer)
                }
            }
        }
    }

    fun onPulseBitmap(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(pulsing = true, status = "Reading screen…")
            val result: PulseResult = try {
                pulse.runFromBitmap(bitmap)
            } catch (e: Exception) {
                PulseResult(null, "Pulse failed: ${e.message}", false)
            }
            _ui.value = _ui.value.copy(pulsing = false, status = result.preview)
        }
    }

    fun scanWhatsAppMedia() {
        if (_ui.value.mediaBusy || _ui.value.pulsing) return
        viewModelScope.launch {
            val gate = MnemePowerGates.canRunHeavyWork(getApplication(), userInitiated = true)
            if (!gate.ok && gate.reason.contains("RAM", ignoreCase = true)) {
                _ui.value = _ui.value.copy(status = gate.reason)
                return@launch
            }
            _ui.value = _ui.value.copy(mediaBusy = true, status = "Scanning WhatsApp media…")
            val result = try {
                media.scanWhatsAppRecent(limit = 4)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    mediaBusy = false,
                    status = "Media scan failed: ${e.message}",
                )
                return@launch
            }
            _ui.value = _ui.value.copy(mediaBusy = false, status = result.message)
        }
    }

    /** On-device E2E without real WA: sample chat → extract → card. */
    fun ingestDemoSample() {
        if (_ui.value.mediaBusy || _ui.value.pulsing) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(mediaBusy = true, status = "Saving demo card…")
            val sample = """
                From Rahul:
                Pay me ₹500 by Friday for dinner last week
            """.trimIndent()
            val result = try {
                pulse.ingestText(sample, sourceLabel = IngestSources.DEMO)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    mediaBusy = false,
                    status = "Demo ingest failed: ${e.message}",
                )
                return@launch
            }
            _ui.value = _ui.value.copy(
                mediaBusy = false,
                status = result.preview,
                tab = MnemeTab.Memory,
            )
        }
    }

    fun showWhatsDue() {
        if (_ui.value.tab != MnemeTab.Ask) {
            _ui.value = _ui.value.copy(tab = MnemeTab.Ask)
        }
        handleUtterance("what's due today")
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            DueReminderScheduler.cancel(getApplication(), id)
            notes.delete(id)
        }
    }

    fun markDone(id: Long) {
        viewModelScope.launch {
            DueReminderScheduler.cancel(getApplication(), id)
            notes.markDone(id)
            _ui.value = _ui.value.copy(status = "Marked done")
        }
    }

    fun snoozeOneDay(id: Long) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
            notes.snooze(id, until)
            _ui.value = _ui.value.copy(status = "Snoozed 1 day")
        }
    }

    fun reopenCard(id: Long) {
        viewModelScope.launch {
            notes.markOpen(id)
            _ui.value = _ui.value.copy(status = "Reopened")
        }
    }

    override fun onCleared() {
        if (_ui.value.listening) stopListening(submit = false)
        tts.shutdown()
        super.onCleared()
    }

    companion object {
        fun factory(
            app: Application,
            notes: NoteRepository,
            pulse: PulsePipeline,
            media: MediaIngestPipeline,
            asker: MemoryAsk,
            llm: LlmBridge,
            whisper: WhisperBridge,
            tts: TtsSpeaker,
            alarms: AlarmScheduler,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MnemeViewModel(app, notes, pulse, media, asker, llm, whisper, tts, alarms) as T
        }
    }
}
