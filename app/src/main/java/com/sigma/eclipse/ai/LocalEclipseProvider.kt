package com.sigma.eclipse.ai

import android.content.Context
import com.arm.aichat.internal.InferenceEngineImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class LocalEclipseProvider(context: Context) : LLMProvider {
    private val appContext = context.applicationContext
    private val modelManager = LocalModelManager(appContext)
    private val engine = InferenceEngineImpl.getInstance(appContext)
    private var systemPromptConfigured = false

    override suspend fun generateStream(prompt: String, systemPrompt: String?): Flow<String> = flow {
        val model = modelManager.ensureModel()
        engine.loadModel(model.absolutePath)

        if (!systemPromptConfigured) {
            engine.setSystemPrompt(
                systemPrompt ?: """
                    Você é o Eclipse, o assistente local privado do Sigma Browser.
                    Responda em português brasileiro quando o usuário escrever em português.
                    Seja objetivo, correto e útil. Use o contexto fornecido pelo navegador
                    quando ele estiver disponível. Não invente fatos quando não houver dados.
                """.trimIndent()
            )
            systemPromptConfigured = true
        }

        engine.generate(prompt, predictLength = 768) { token ->
            kotlinx.coroutines.runBlocking { emit(token) }
        }
    }.flowOn(Dispatchers.IO)
}
