package com.sigma.eclipse.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigma.eclipse.ai.LLMProvider
import com.sigma.eclipse.ai.LocalEclipseProvider
import com.sigma.eclipse.research.DeepResearchEngine
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

class AiChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLocalMode = MutableStateFlow(true)
    val isLocalMode: StateFlow<Boolean> = _isLocalMode.asStateFlow()

    private val localProvider: LLMProvider = LocalEclipseProvider()

    fun setLocalMode(enabled: Boolean) {
        _isLocalMode.value = enabled
    }

    fun sendMessage(text: String, contextText: String? = null) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(content = text, isUser = true)
        val aiMsg = ChatMessage(content = "", isUser = false, isGenerating = true)
        
        _messages.update { it + userMsg + aiMsg }

        viewModelScope.launch {
            val prompt = if (contextText != null) {
                "Contexto:\n$contextText\n\nPergunta: $text"
            } else {
                text
            }
            
            try {
                val provider = localProvider 
                var currentText = ""
                
                provider.generateStream(prompt).collect { chunk ->
                    currentText += chunk
                    _messages.update { list ->
                        list.map { 
                            if (it.id == aiMsg.id) it.copy(content = currentText) else it 
                        }
                    }
                }
                
                _messages.update { list ->
                    list.map { 
                        if (it.id == aiMsg.id) it.copy(isGenerating = false) else it 
                    }
                }
            } catch (e: Exception) {
                _messages.update { list ->
                    list.map { 
                        if (it.id == aiMsg.id) it.copy(content = "Erro: ${e.message}", isGenerating = false) else it 
                    }
                }
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
                var currentText = ""
                DeepResearchEngine.conductResearch(topic).collect { chunk ->
                    currentText += chunk
                    _messages.update { list ->
                        list.map { 
                            if (it.id == aiMsg.id) it.copy(content = currentText) else it 
                        }
                    }
                }
                _messages.update { list ->
                    list.map { 
                        if (it.id == aiMsg.id) it.copy(isGenerating = false) else it 
                    }
                }
            } catch (e: Exception) {
                _messages.update { list ->
                    list.map { 
                        if (it.id == aiMsg.id) it.copy(content = "Erro no Deep Research: ${e.message}", isGenerating = false) else it 
                    }
                }
            }
        }
    }
}

