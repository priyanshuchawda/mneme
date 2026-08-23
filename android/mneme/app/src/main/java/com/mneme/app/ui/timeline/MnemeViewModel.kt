package com.mneme.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mneme.app.ai.AskResult
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.MemoryAsk
import com.mneme.app.data.NoteEntity
import com.mneme.app.data.NoteRepository
import com.mneme.app.pulse.PulsePipeline
import com.mneme.app.pulse.PulseResult
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
    val searchQuery: String = "",
    val searchResults: List<NoteEntity>? = null,
    val askQuery: String = "",
    val askBusy: Boolean = false,
    val askAnswer: String? = null,
    val askCitations: List<NoteEntity> = emptyList(),
)

class MnemeViewModel(
    private val notes: NoteRepository,
    private val pulse: PulsePipeline,
    private val asker: MemoryAsk,
    private val llm: LlmBridge,
) : ViewModel() {
    val timeline = notes.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(MnemeUiState())
    val ui: StateFlow<MnemeUiState> = _ui.asStateFlow()

    fun setTab(tab: MnemeTab) {
        _ui.value = _ui.value.copy(tab = tab)
        if (tab != MnemeTab.Ask) {
            viewModelScope.launch { llm.unload() }
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
        viewModelScope.launch {
            _ui.value = _ui.value.copy(askBusy = true, status = "Searching memory…", askAnswer = null)
            val result: AskResult = try {
                asker.ask(q)
            } catch (e: Exception) {
                AskResult("Ask failed: ${e.message}", emptyList())
            }
            _ui.value = _ui.value.copy(
                askBusy = false,
                askAnswer = result.answer,
                askCitations = result.citations,
                status = if (result.citations.isEmpty()) "No matches" else "Answered from ${result.citations.size} notes",
            )
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

    fun deleteNote(id: Long) {
        viewModelScope.launch { notes.delete(id) }
    }

    companion object {
        fun factory(
            notes: NoteRepository,
            pulse: PulsePipeline,
            asker: MemoryAsk,
            llm: LlmBridge,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MnemeViewModel(notes, pulse, asker, llm) as T
        }
    }
}
