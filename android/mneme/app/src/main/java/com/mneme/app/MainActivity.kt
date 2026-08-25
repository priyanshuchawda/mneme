package com.mneme.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mneme.app.ai.LlmBridge
import com.mneme.app.ai.MemoryAsk
import com.mneme.app.ai.MnemeEmbedder
import com.mneme.app.data.MnemeDatabase
import com.mneme.app.data.NoteRepository
import com.mneme.app.media.MediaIngestPipeline
import com.mneme.app.pulse.ProjectionService
import com.mneme.app.pulse.PulsePipeline
import com.mneme.app.pulse.ScreenCapture
import com.mneme.app.ui.theme.MnemeTheme
import com.mneme.app.ui.timeline.MnemeHomeScreen
import com.mneme.app.ui.timeline.MnemeViewModel
import com.mneme.app.voice.AlarmScheduler
import com.mneme.app.voice.TtsSpeaker
import com.mneme.app.voice.WhisperBridge
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val llm by lazy { LlmBridge(this) }
    private val embedder by lazy { MnemeEmbedder(this, llm) }
    private val notes by lazy { NoteRepository(MnemeDatabase.get(this).notes(), embedder) }
    private val whisper by lazy { WhisperBridge(this) }
    private val tts by lazy { TtsSpeaker(this) }
    private val alarms by lazy { AlarmScheduler(this) }
    private val pulse by lazy { PulsePipeline(this, notes, llm) }
    private val media by lazy { MediaIngestPipeline(this, pulse, llm) }
    private val asker by lazy { MemoryAsk(notes, llm) }
    private val capture by lazy { ScreenCapture(this) }

    private val viewModel: MnemeViewModel by viewModels {
        MnemeViewModel.factory(
            application,
            notes,
            pulse,
            media,
            asker,
            llm,
            whisper,
            tts,
            alarms,
        )
    }

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Toast.makeText(this, "Hold the mic to talk", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission needed for voice Ask", Toast.LENGTH_SHORT).show()
            }
        }

    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) {
                Toast.makeText(this, "Screen capture permission needed for Pulse", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val data = result.data!!
            lifecycleScope.launch {
                try {
                    startForegroundService(Intent(this@MainActivity, ProjectionService::class.java))
                    // Hide Mneme so MediaProjection mirrors the previous app (e.g. WhatsApp).
                    moveTaskToBack(true)
                    kotlinx.coroutines.delay(450)
                    val bitmap = capture.capture(result.resultCode, data)
                    // Return to Mneme for OCR / save status.
                    startActivity(
                        Intent(this@MainActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    )
                    viewModel.onPulseBitmap(bitmap)
                } catch (e: Exception) {
                    startActivity(
                        Intent(this@MainActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    )
                    Toast.makeText(this@MainActivity, "Pulse failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    stopService(Intent(this@MainActivity, ProjectionService::class.java))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MnemeTheme {
                val ui by viewModel.ui.collectAsStateWithLifecycle()
                MnemeHomeScreen(
                    viewModel = viewModel,
                    onPulse = {
                        if (ui.pulsing) return@MnemeHomeScreen
                        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        projectionLauncher.launch(mgr.createScreenCaptureIntent())
                    },
                    onRequestMic = {
                        when {
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED -> viewModel.startListening()
                            else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                )
            }
        }
    }
}
