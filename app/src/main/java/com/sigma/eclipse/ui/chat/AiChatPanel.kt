package com.sigma.eclipse.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AiChatPanel(
    viewModel: AiChatViewModel,
    onClose: () -> Unit,
    onSendMessage: (String, Boolean, Boolean) -> Unit,
    onPageAction: (PageAction) -> Unit = {},
    hasPageContext: Boolean = false,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    var includeContext by remember { mutableStateOf(hasPageContext) }
    var isDeepResearch by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Sigma AI", style = MaterialTheme.typography.titleLarge)
                    Text("Seu copiloto para a web", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Fechar") }
            }
            HorizontalDivider()

            if (hasPageContext) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(onClick = { onPageAction(PageAction.SUMMARIZE) }, label = { Text("Resumir") })
                    AssistChip(onClick = { onPageAction(PageAction.EXPLAIN) }, label = { Text("Explicar") })
                    AssistChip(onClick = { onPageAction(PageAction.TRANSLATE) }, label = { Text("Traduzir") })
                }
            }

            LazyColumn(
                Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            if (hasPageContext) "Pergunte sobre a página ou toque em Resumir para começar."
                            else "Abra uma página e use o Sigma AI para perguntar, explicar ou resumir.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                items(messages) { msg -> MessageBubble(message = msg) }
            }

            Surface(tonalElevation = 3.dp) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected = includeContext, onClick = { includeContext = !includeContext }, label = { Text("Página") })
                        FilterChip(selected = isDeepResearch, onClick = { isDeepResearch = !isDeepResearch }, label = { Text("Research") })
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            Modifier.weight(1f),
                            placeholder = { Text("Pergunte à Sigma AI…") },
                            shape = RoundedCornerShape(22.dp),
                            maxLines = 4
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText, includeContext, isDeepResearch)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier.size(50.dp)
                        ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar") }
                    }
                }
            }
        }
    }
}

enum class PageAction { SUMMARIZE, EXPLAIN, TRANSLATE }

@Composable
fun MessageBubble(message: ChatMessage) {
    val container = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    Box(Modifier.fillMaxWidth(), contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(color = container, shape = RoundedCornerShape(18.dp), modifier = Modifier.widthIn(max = 330.dp)) {
            Text(message.content + if (message.isGenerating) " ▍" else "", Modifier.padding(14.dp))
        }
    }
}
