package com.sigma.eclipse.ai

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Calls OpenAI-compatible chat-completions APIs directly from the device.
 * The user owns and supplies the credential.
 */
class OpenAiCompatibleProvider(context: Context) : LLMProvider {
    private val appContext = context.applicationContext
    private val keyStore = SecureApiKeyStore(appContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun generateStream(prompt: String, systemPrompt: String?): Flow<String> = flow {
        val config = AiProviderConfig.load(appContext)
        val apiKey = keyStore.get()
            ?: throw IllegalStateException("Configure sua API Key em Configurações > Inteligência Artificial.")
        require(config.endpoint.startsWith("https://")) { "O endpoint da IA deve usar HTTPS." }
        require(config.model.isNotBlank()) { "Informe um modelo de IA." }

        val messages = JSONArray()
        if (!systemPrompt.isNullOrBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        messages.put(JSONObject().put("role", "user").put("content", prompt))

        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("temperature", 0.4)
            .put("stream", false)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(config.endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .post(body)
            .build()

        val responseText = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = runCatching { JSONObject(responseBody).optString("detail") }
                        .getOrDefault("")
                    val suffix = if (detail.isNotBlank()) ": $detail" else ""
                    throw IllegalStateException("API ${response.code}$suffix")
                }
                responseBody
            }
        }

        val json = JSONObject(responseText)
        val choices = json.optJSONArray("choices")
        val first = choices?.optJSONObject(0)
        val message = first?.optJSONObject("message")
        val content = message?.optString("content").orEmpty()
        if (content.isBlank()) {
            throw IllegalStateException("A API retornou uma resposta vazia.")
        }
        emit(content)
    }

    suspend fun testConnection(): Result<String> = runCatching {
        generateStream("Responda somente: OK").collect { }
        "Conexão funcionando"
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
