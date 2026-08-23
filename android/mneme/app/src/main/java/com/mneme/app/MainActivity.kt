package com.mneme.app

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mneme.app.data.MnemeDatabase
import com.mneme.app.data.NoteRepository
import com.mneme.app.pulse.ProjectionService
import com.mneme.app.pulse.PulsePipeline
import com.mneme.app.pulse.ScreenCapture
import com.mneme.app.ui.theme.MnemeTheme
import com.mneme.app.ui.timeline.MnemeHomeScreen
import com.mneme.app.ui.timeline.MnemeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notes by lazy { NoteRepository(MnemeDatabase.get(this).notes()) }
    private val pulse by lazy { PulsePipeline(this, notes) }
    private val capture by lazy { ScreenCapture(this) }

    private val viewModel: MnemeViewModel by viewModels {
        MnemeViewModel.factory(notes, pulse)
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
                    val bitmap = capture.capture(result.resultCode, data)
                    viewModel.onPulseBitmap(bitmap)
                } catch (e: Exception) {
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
                )
            }
        }
    }
}
