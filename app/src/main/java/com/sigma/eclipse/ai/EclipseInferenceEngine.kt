package com.arm.aichat.internal

import android.content.Context
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thin Android wrapper around the JNI symbols exposed by the upstream
 * llama.cpp Android implementation.
 *
 * The native runtime stays inside the app process. No localhost server and no
 * executable is launched from app storage.
 */
internal class InferenceEngineImpl private constructor(context: Context) {
    private val nativeLibDir = context.applicationInfo.nativeLibraryDir
    private val initialized = AtomicBoolean(false)
    private var modelPath: String? = null

    companion object {
        @Volatile
        private var instance: InferenceEngineImpl? = null

        fun getInstance(context: Context): InferenceEngineImpl =
            instance ?: synchronized(this) {
                instance ?: InferenceEngineImpl(context.applicationContext).also { instance = it }
            }
    }

    init {
        System.loadLibrary("ai-chat")
        check(nativeLibDir.isNotBlank()) { "Native library directory is unavailable" }
        init(nativeLibDir)
        initialized.set(true)
    }

    @Synchronized
    fun loadModel(path: String) {
        check(initialized.get()) { "Native runtime is not initialized" }
        val file = File(path)
        require(file.isFile && file.canRead()) { "Model is not readable: $path" }
        unloadModel()
        check(load(path) == 0) { "llama.cpp failed to load model: $path" }
        check(prepare() == 0) { "llama.cpp failed to prepare model context" }
        modelPath = path
    }

    @Synchronized
    fun setSystemPrompt(prompt: String) {
        check(modelPath != null) { "Load a model before setting the system prompt" }
        require(prompt.isNotBlank())
        check(processSystemPrompt(prompt) == 0) { "Failed to process system prompt" }
    }

    @Synchronized
    fun generate(prompt: String, predictLength: Int = 512, onToken: (String) -> Unit) {
        check(modelPath != null) { "Load a model before generating" }
        require(prompt.isNotBlank())
        check(processUserPrompt(prompt, predictLength) == 0) {
            "Failed to process user prompt"
        }
        while (true) {
            val token = generateNextToken() ?: break
            if (token.isNotEmpty()) onToken(token)
        }
    }

    @Synchronized
    fun unloadModel() {
        if (modelPath != null) {
            unload()
            modelPath = null
        }
    }

    @Synchronized
    fun close() {
        if (initialized.compareAndSet(true, false)) {
            unloadModel()
            shutdown()
        }
    }

    private external fun init(nativeLibDir: String)
    private external fun load(modelPath: String): Int
    private external fun prepare(): Int
    private external fun processSystemPrompt(systemPrompt: String): Int
    private external fun processUserPrompt(userPrompt: String, predictLength: Int): Int
    private external fun generateNextToken(): String?
    private external fun unload()
    private external fun shutdown()
}
