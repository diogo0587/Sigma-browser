package com.sigma.eclipse.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sigma.eclipse.ai.LLMProvider
import com.sigma.eclipse.ai.OpenAiCompatibleProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val isGenerating: Boolean = false
)

class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLocalMode = MutableStateFlow(false)
    val isLocalMode: StateFlow<Boolean> = _isLocalMode.asStateFlow()

    private val apiProvider: LLMProvider = OpenAiCompatibleProvider(application)

    fun setLocalMode(enabled: Boolean) {
        // Kept for UI compatibility. Sigma now uses user-configured API inference only.
        _isLocalMode.value = false
    }

    fun sendMessage(text: String, contextText: String? = null) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(content = text, isUser = true)
        val aiMsg = ChatMessage(content = "", isUser = false, isGenerating = true)
        _messages.update { it + userMsg + aiMsg }

        viewModelScope.launch {
            val prompt = if (contextText != null) {
                "Contexto da página:\n$contextText\n\nPergunta do usuário: $text"
            } else {
                text
            }

            try {
                var currentText = ""
                apiProvider.generateStream(prompt).collect { chunk ->
                    currentText += chunk
                    _messages.update { list ->
                        list.map { if (it.id == aiMsg.id) it.copy(content = currentText) else it }
                    }
                }
                finishMessage(aiMsg.id, currentText)
            } catch (e: Exception) {
                failMessage(aiMsg.id, "Erro na IA por API: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun sendDeepResearch(topic: String) {
        if (topic.isBlank()) return

        val userMsg = ChatMessage(content = "Deep Research: $topic", isUser = true)
        val aiMsg = ChatMessage(content = "", isUser = false, isGenerating = true)
        _messages.update { it + userMsg + aiMsg }

        viewModelScope.launch {
            try {
                val system = "Você é o modo Deep Research do Sigma Browser. Produza uma análise estruturada, " +
                    "separe fatos de inferências, destaque limitações e indique que a resposta depende das fontes " +
                    "ou contexto fornecidos. Não invente fontes nem alegue ter navegado na web se isso não ocorreu."
                var currentText = ""
                apiProvider.generateStream(
                    prompt = "Faça uma análise aprofundada do seguinte tema:\n$topic",
                    systemPrompt = system
                ).collect { chunk ->
                    currentText += chunk
                    _messages.update { list ->
                        list.map { if (it.id == aiMsg.id) it.copy(content = currentText) else it }
                    }
                }
                finishMessage(aiMsg.id, currentText)
            } catch (e: Exception) {
                failMessage(aiMsg.id, "Erro no Deep Research por API: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun finishMessage(id: String, content: String) {
        _messages.update { list ->
            list.map { if (it.id == id) it.copy(content = content, isGenerating = false) else it }
        }
    }

    private fun failMessage(id: String, message: String) {
        _messages.update { list ->
            list.map { if (it.id == id) it.copy(content = message, isGenerating = false) else it }
        }
    }
}
