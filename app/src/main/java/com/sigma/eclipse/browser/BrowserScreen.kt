package com.sigma.eclipse.browser

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateToSettings: () -> Unit
) {
    var url by remember { mutableStateOf("https://www.google.com") }
    var webView: WebView? by remember { mutableStateOf(null) }
    var isDrawerOpen by remember { mutableStateOf(false) }
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
                onOpenAi = { /* Open AI Chat */ }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
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
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        IconButton(onClick = onForward) {
            Icon(Icons.Filled.ArrowForward, contentDescription = "Forward")
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
