package com.sigma.eclipse.browser

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sigma.eclipse.ai.ContextManager
import com.sigma.eclipse.privacy.PrivacyEngine
import com.sigma.eclipse.ui.chat.AiChatPanel
import com.sigma.eclipse.ui.chat.AiChatViewModel
import com.sigma.eclipse.ui.chat.PageAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(onNavigateToSettings: () -> Unit) {
    var url by remember { mutableStateOf("https://www.google.com") }
    var pageTitle by remember { mutableStateOf("Google") }
    var webView: WebView? by remember { mutableStateOf(null) }
    var isChatOpen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val chatViewModel: AiChatViewModel = viewModel()
    val scope = rememberCoroutineScope()

    fun normalized(input: String): String = when {
        input.startsWith("http://") || input.startsWith("https://") -> input
        input.contains(".") && !input.contains(" ") -> "https://$input"
        else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(input, "UTF-8")}" 
    }

    fun extractContext(action: PageAction) {
        val view = webView ?: return
        scope.launch {
            val raw = PageExtractor.extractCleanText(view)
            val context = ContextManager.processPageContext(raw)
            when (action) {
                PageAction.SUMMARIZE -> chatViewModel.summarizePage(context)
                PageAction.EXPLAIN -> chatViewModel.explainPage(context)
                PageAction.TRANSLATE -> chatViewModel.translatePage(context)
            }
            isChatOpen = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth().height(54.dp).padding(end = 8.dp),
                            singleLine = true,
                            placeholder = { Text("Pesquisar ou digitar endereço") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { webView?.loadUrl(normalized(url)) }),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    },
                    actions = { IconButton(onClick = onNavigateToSettings) { Icon(Icons.Filled.Settings, contentDescription = "Configurações") } }
                )
                if (isLoading) LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
            }
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = { webView?.goBack() }, enabled = webView?.canGoBack() == true) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                IconButton(onClick = { webView?.goForward() }, enabled = webView?.canGoForward() == true) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Avançar") }
                IconButton(onClick = { webView?.reload() }) { Icon(Icons.Filled.Refresh, "Atualizar") }
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = { isChatOpen = true }) {
                    Icon(Icons.Filled.AutoAwesome, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Sigma AI")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onNavigateToSettings) { Icon(Icons.Filled.MoreVert, "Mais opções") }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                                isLoading = true
                                pageUrl?.let { url = it }
                            }
                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                isLoading = false
                                pageUrl?.let { url = it }
                                view?.title?.let { if (it.isNotBlank()) pageTitle = it }
                            }
                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                if (request != null && PrivacyEngine.shouldBlock(request)) return PrivacyEngine.createEmptyResponse()
                                return super.shouldInterceptRequest(view, request)
                            }
                            override fun doUpdateVisitedHistory(view: WebView?, urlStr: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, urlStr, isReload)
                                urlStr?.let { url = it }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                                isLoading = newProgress < 100
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false
                        loadUrl(url)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = isChatOpen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.86f)
            ) {
                AiChatPanel(
                    viewModel = chatViewModel,
                    onClose = { isChatOpen = false },
                    hasPageContext = webView != null,
                    onPageAction = ::extractContext,
                    onSendMessage = { text, includeContext, isDeepResearch ->
                        if (isDeepResearch) {
                            chatViewModel.sendDeepResearch(text)
                        } else if (includeContext && webView != null) {
                            scope.launch {
                                val raw = PageExtractor.extractCleanText(webView!!)
                                chatViewModel.sendMessage(text, ContextManager.processPageContext(raw))
                            }
                        } else chatViewModel.sendMessage(text)
                    }
                )
            }
        }
    }
}
