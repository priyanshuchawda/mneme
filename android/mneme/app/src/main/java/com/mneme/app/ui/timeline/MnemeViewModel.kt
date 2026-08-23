package com.mneme.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

data class MnemeUiState(
    val status: String = "Ready",
    val pulsing: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<NoteEntity>? = null,
)

class MnemeViewModel(
    private val notes: NoteRepository,
    private val pulse: PulsePipeline,
) : ViewModel() {
    val timeline = notes.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(MnemeUiState())
    val ui: StateFlow<MnemeUiState> = _ui.asStateFlow()

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
        fun factory(notes: NoteRepository, pulse: PulsePipeline) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MnemeViewModel(notes, pulse) as T
            }
    }
}
