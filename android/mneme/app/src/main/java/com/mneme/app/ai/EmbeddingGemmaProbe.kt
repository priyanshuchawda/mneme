package com.mneme.app.ai

import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Optional EmbeddingGemma TFLite loader. Full inference needs SentencePiece tokenization
 * (model expects token ids). Until that lands, [available] is used for diagnostics only.
 */
class EmbeddingGemmaProbe {
    fun probe(): String {
        val model = File(MODEL_PATH)
        if (!model.isFile) return "missing model"
        return try {
            Interpreter(loadMapped(model)).use { interp ->
                interp.allocateTensors()
                val `in` = interp.getInputTensor(0).shape().contentToString()
                val out = interp.getOutputTensor(0).shape().contentToString()
                "ready in=$`in` out=$out"
            }
        } catch (e: Exception) {
            Log.w(TAG, "probe failed", e)
            "error: ${e.message}"
        }
    }

    private fun loadMapped(file: File): MappedByteBuffer =
        file.inputStream().channel.use { ch ->
            ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size())
        }

    companion object {
        private const val TAG = "EmbedGemma"
        const val MODEL_PATH =
            "/sdcard/Mneme/models/phone1/embed/litert/embeddinggemma-300M_seq512_mixed-precision.tflite"
    }
}
