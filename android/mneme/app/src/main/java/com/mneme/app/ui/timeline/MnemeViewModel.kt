package com.mneme.app.ui.timeline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mneme.app.ai.AskResult
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.MemoryAsk
import com.mneme.app.data.NoteEntity
import com.mneme.app.data.NoteRepository
import com.mneme.app.media.MediaIngestPipeline
import com.mneme.app.media.MnemePowerGates
import com.mneme.app.notif.MnemeNotificationListener
import com.mneme.app.pulse.PulsePipeline
import com.mneme.app.pulse.PulseResult
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

enum class MnemeTab { Memory, Ask }

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
        refreshNotifAccess()
    }

    fun refreshNotifAccess() {
        _ui.value = _ui.value.copy(
            notifAccess = MnemeNotificationListener.isEnabled(getApplication()),
        )
    }

    fun openNotifAccessSettings() {
        MnemeNotificationListener.openSettings(getApplication())
    }

    fun setTab(tab: MnemeTab) {
        if (_ui.value.listening) stopListening(submit = false)
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

    fun deleteNote(id: Long) {
        viewModelScope.launch { notes.delete(id) }
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
