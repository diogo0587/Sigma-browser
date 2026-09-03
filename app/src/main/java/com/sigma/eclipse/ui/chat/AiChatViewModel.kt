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

    private val apiProvider: LLMProvider = OpenAiCompatibleProvider(application)

    fun sendMessage(text: String, contextText: String? = null) {
        if (text.isBlank()) return
        val prompt = if (contextText.isNullOrBlank()) text else buildContextPrompt(contextText, text)
        sendApiMessage(userText = text, prompt = prompt)
    }

    fun summarizePage(contextText: String) {
        if (contextText.isBlank()) {
            showError("Não consegui extrair o conteúdo desta página.")
            return
        }
        val system = "Você é o Sigma AI, assistente de leitura do navegador. Resuma apenas o conteúdo fornecido. " +
            "Não invente informações. Responda em português do Brasil. Comece com um resumo curto e depois liste " +
            "os principais pontos. Se houver título e URL, use-os apenas como metadados."
        sendApiMessage(
            userText = "Resumir esta página",
            prompt = "Crie um resumo útil e fiel da página abaixo. Preserve nomes, números e fatos importantes.\n\n$contextText",
            systemPrompt = system
        )
    }

    fun explainPage(contextText: String) {
        if (contextText.isBlank()) {
            showError("Não consegui extrair o conteúdo desta página.")
            return
        }
        sendApiMessage(
            userText = "Explicar esta página",
            prompt = "Explique de forma simples e estruturada o conteúdo desta página. Identifique a ideia central e os pontos mais importantes.\n\n$contextText",
            systemPrompt = "Você é o Sigma AI. Explique somente o conteúdo fornecido, sem inventar fatos. Responda em português do Brasil."
        )
    }

    fun translatePage(contextText: String) {
        if (contextText.isBlank()) {
            showError("Não consegui extrair o conteúdo desta página.")
            return
        }
        sendApiMessage(
            userText = "Traduzir esta página",
            prompt = "Traduza o conteúdo principal da página para português do Brasil. Preserve estrutura, nomes próprios e números.\n\n$contextText",
            systemPrompt = "Você é um tradutor preciso integrado ao Sigma Browser. Traduza somente o conteúdo fornecido."
        )
    }

    fun sendDeepResearch(topic: String) {
        if (topic.isBlank()) return
        sendApiMessage(
            userText = "Deep Research: $topic",
            prompt = "Faça uma análise aprofundada do seguinte tema:\n$topic",
            systemPrompt = "Você é o modo Deep Research do Sigma Browser. Produza uma análise estruturada, separe fatos de inferências, destaque limitações e não invente fontes nem alegue ter navegado na web se isso não ocorreu."
        )
    }

    private fun sendApiMessage(userText: String, prompt: String, systemPrompt: String? = null) {
        val userMsg = ChatMessage(content = userText, isUser = true)
        val aiMsg = ChatMessage(content = "", isUser = false, isGenerating = true)
        _messages.update { it + userMsg + aiMsg }
        viewModelScope.launch {
            try {
                var currentText = ""
                apiProvider.generateStream(prompt, systemPrompt).collect { chunk ->
                    currentText += chunk
                    _messages.update { list -> list.map { if (it.id == aiMsg.id) it.copy(content = currentText) else it } }
                }
                finishMessage(aiMsg.id, currentText)
            } catch (e: Exception) {
                failMessage(aiMsg.id, "Erro na IA por API: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun buildContextPrompt(context: String, question: String): String =
        "Você está respondendo sobre uma página aberta no Sigma Browser. Use o contexto abaixo como fonte principal. " +
            "Se a informação não estiver presente, diga isso claramente. Não invente.\n\n" +
            "CONTEXTO DA PÁGINA:\n$context\n\nPERGUNTA:\n$question"

    private fun finishMessage(id: String, content: String) {
        _messages.update { list -> list.map { if (it.id == id) it.copy(content = content, isGenerating = false) else it } }
    }

    private fun failMessage(id: String, message: String) {
        _messages.update { list -> list.map { if (it.id == id) it.copy(content = message, isGenerating = false) else it } }
    }

    private fun showError(message: String) {
        _messages.update { it + ChatMessage(content = message, isUser = false) }
    }
}
