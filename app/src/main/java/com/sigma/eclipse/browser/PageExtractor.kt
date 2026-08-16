package com.sigma.eclipse.browser

import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PageExtractor {
    suspend fun extractCleanText(webView: WebView): String = suspendCancellableCoroutine { continuation ->
        val script = """
            (function() {
                var clone = document.body.cloneNode(true);
                var removeTags = ['script', 'style', 'nav', 'header', 'footer', 'noscript', 'iframe'];
                for (var i=0; i<removeTags.length; i++) {
                    var elements = clone.getElementsByTagName(removeTags[i]);
                    for (var j=elements.length-1; j>=0; j--) {
                        elements[j].parentNode.removeChild(elements[j]);
                    }
                }
                return clone.innerText || clone.textContent;
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            val text = result?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\t", "\t") 
                ?: ""
            continuation.resume(text)
        }
    }
}
