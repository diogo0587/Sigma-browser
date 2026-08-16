package com.sigma.eclipse.ai

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

internal class LocalModelManager(private val context: Context) {
    private val client = OkHttpClient.Builder().build()

    data class ModelSpec(
        val filename: String,
        val url: String,
        val sha256: String,
    )

    private val largeModel = ModelSpec(
        filename = "Qwen3.5-4B.Q6_K.gguf",
        url = "https://releases.sigmabrowser.com/dev/secure-llm/model_jackrong_qwen35_4b_opus_reasoning_q6k.zip",
        sha256 = "faaf1c53d696ed804fdafc2210012adcae8df6c3003c59c8bb6057d7c7599ffc",
    )

    private val smallModel = ModelSpec(
        filename = "model-s-v1.0.gguf",
        url = "https://releases.sigmabrowser.com/dev/secure-llm/model_s.zip",
        sha256 = "e5b0282323ebc54db43d0a8b91e5869555f9a6ee6811a893fa1adc47a9382fcd",
    )

    fun selectModel(): ModelSpec {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClassMb = activityManager.memoryClass
        return if (memoryClassMb >= 8192) largeModel else smallModel
    }

    suspend fun ensureModel(): File = withContext(Dispatchers.IO) {
        val spec = selectModel()
        val modelsDir = File(context.filesDir, "eclipse-models").apply { mkdirs() }
        val model = File(modelsDir, spec.filename)
        if (model.isFile && verifySha256(model, spec.sha256)) return@withContext model

        if (model.exists()) model.delete()

        val archive = File(modelsDir, "${spec.filename}.zip")
        download(spec.url, archive)
        extractModel(archive, model)
        archive.delete()

        check(verifySha256(model, spec.sha256)) {
            "SHA-256 verification failed for ${spec.filename}"
        }
        model
    }

    private fun download(url: String, destination: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Model download failed: HTTP ${response.code}" }
            val body = response.body ?: error("Model download returned an empty body")
            body.byteStream().use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output, 1024 * 1024) }
            }
        }
    }

    private fun extractModel(archive: File, destination: File) {
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.substringAfterLast('/') == destination.name) {
                    FileOutputStream(destination).use { output -> zip.copyTo(output, 1024 * 1024) }
                    return
                }
                entry = zip.nextEntry
            }
        }
        error("${destination.name} was not found inside model archive")
    }

    private fun verifySha256(file: File, expected: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }.equals(expected, ignoreCase = true)
    }
}
