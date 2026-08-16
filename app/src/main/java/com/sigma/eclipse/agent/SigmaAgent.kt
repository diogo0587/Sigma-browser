package com.sigma.eclipse.agent

import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AgentAction(
    val description: String,
    val requiresConfirmation: Boolean = false,
    val execute: (WebView) -> Unit
)

class SigmaAgent(private val webView: WebView) {
    private val _agentState = MutableStateFlow<String>("Idle")
    val agentState: StateFlow<String> = _agentState.asStateFlow()

    private val _pendingAction = MutableStateFlow<AgentAction?>(null)
    val pendingAction: StateFlow<AgentAction?> = _pendingAction.asStateFlow()

    suspend fun executeGoal(goal: String) {
        _agentState.value = "Planejando: $goal..."
        delay(1500) // Simulate LLM planner
        
        _agentState.value = "Navegando para o motor de busca..."
        withContext(Dispatchers.Main) {
            webView.loadUrl("https://duckduckgo.com/?q=${goal.replace(" ", "+")}")
        }
        delay(2000)

        // Proposal of a destructive/interactive action
        val action = AgentAction(
            description = "Clicar no primeiro resultado da busca",
            requiresConfirmation = true,
            execute = { view ->
                view.evaluateJavascript(
                    "document.querySelectorAll('a[data-testid=\"result-title-a\"]')[0].click();",
                    null
                )
            }
        )
        
        _agentState.value = "Aguardando confirmação do usuário..."
        _pendingAction.value = action
    }

    fun confirmAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null
        _agentState.value = "Executando ação..."
        action.execute(webView)
        _agentState.value = "Ação concluída."
    }

    fun cancelAction() {
        _pendingAction.value = null
        _agentState.value = "Ação cancelada pelo usuário."
    }
}
