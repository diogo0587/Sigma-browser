package com.sigma.eclipse.browser

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sigma.eclipse.ui.chat.AiChatPanel
import com.sigma.eclipse.ui.chat.AiChatViewModel
import com.sigma.eclipse.privacy.PrivacyEngine
import com.sigma.eclipse.ai.ContextManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateToSettings: () -> Unit
) {
    var url by remember { mutableStateOf("https://www.google.com") }
    var webView: WebView? by remember { mutableStateOf(null) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isChatOpen by remember { mutableStateOf(false) }
    
    val chatViewModel: AiChatViewModel = viewModel()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            BrowserTopBar(
                currentUrl = url,
                onUrlSubmit = { newUrl ->
                    url = if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                        "https://$newUrl"
                    } else {
                        newUrl
                    }
                    webView?.loadUrl(url)
                },
                onOpenDrawer = {
                    isDrawerOpen = true
                }
            )
        },
        bottomBar = {
            BrowserBottomBar(
                onBack = { webView?.goBack() },
                onForward = { webView?.goForward() },
                onRefresh = { webView?.reload() },
                onOpenAi = { isChatOpen = true }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                if (request != null && PrivacyEngine.shouldBlock(request)) {
                                    return PrivacyEngine.createEmptyResponse()
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun doUpdateVisitedHistory(
                                view: WebView?,
                                urlStr: String?,
                                isReload: Boolean
                            ) {
                                super.doUpdateVisitedHistory(view, urlStr, isReload)
                                urlStr?.let { url = it }
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(url)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // AI Chat Overlay Panel
            AnimatedVisibility(
                visible = isChatOpen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxHeight(0.8f) // Ocupa 80% da tela para o Chat
            ) {
                AiChatPanel(
                    viewModel = chatViewModel,
                    onClose = { isChatOpen = false },
                    onSendMessage = { text, includeContext, isDeepResearch ->
                        if (isDeepResearch) {
                            chatViewModel.sendDeepResearch(text)
                        } else if (includeContext && webView != null) {
                            scope.launch {
                                val rawText = PageExtractor.extractCleanText(webView!!)
                                val cleanText = ContextManager.processPageContext(rawText)
                                chatViewModel.sendMessage(text, cleanText)
                            }
                        } else {
                            chatViewModel.sendMessage(text)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopBar(
    currentUrl: String,
    onUrlSubmit: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    var text by remember(currentUrl) { mutableStateOf(currentUrl) }

    TopAppBar(
        title = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(end = 8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = { onUrlSubmit(text) }
                ),
                shape = MaterialTheme.shapes.extraLarge,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        },
        actions = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        }
    )
}

@Composable
fun BrowserBottomBar(
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAi: () -> Unit
) {
    BottomAppBar {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        IconButton(onClick = onForward) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
        }
        Spacer(Modifier.weight(1f))
        FilledTonalButton(onClick = onOpenAi) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI")
            Spacer(Modifier.width(8.dp))
            Text("Ask AI")
        }
        Spacer(Modifier.width(16.dp))
    }
}
