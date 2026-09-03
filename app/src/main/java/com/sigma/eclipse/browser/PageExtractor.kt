package com.sigma.eclipse.browser

import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PageExtractor {
    suspend fun extractCleanText(webView: WebView): String = suspendCancellableCoroutine { continuation ->
        val script = """
            (function() {
                function clean(s) {
                    return (s || '').replace(/\\u00a0/g, ' ').replace(/[ \\t]+/g, ' ').replace(/\\n{3,}/g, '\\n\\n').trim();
                }
                var root = document.body;
                if (!root) return JSON.stringify({title: document.title || '', url: location.href, text: ''});
                var clone = root.cloneNode(true);
                var removeTags = ['script','style','nav','header','footer','noscript','iframe','svg','canvas','form','button','input','textarea','select'];
                for (var i = 0; i < removeTags.length; i++) {
                    var elements = clone.getElementsByTagName(removeTags[i]);
                    for (var j = elements.length - 1; j >= 0; j--) elements[j].remove();
                }
                var preferred = clone.querySelector('article, main, [role="main"], .post-content, .entry-content, .article-content');
                var source = preferred || clone;
                var title = clean(document.title);
                var text = clean(source.innerText || source.textContent);
                if (!text) text = clean(clone.innerText || clone.textContent);
                return JSON.stringify({title: title, url: location.href, text: text});
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            try {
                val json = org.json.JSONObject(result ?: "{}")
                val title = json.optString("title")
                val pageUrl = json.optString("url")
                val text = json.optString("text")
                val combined = buildString {
                    if (title.isNotBlank()) append("Título: ").append(title).append("\n")
                    if (pageUrl.isNotBlank()) append("URL: ").append(pageUrl).append("\n")
                    if (text.isNotBlank()) append("\n").append(text)
                }
                continuation.resume(combined.take(MAX_CONTEXT_CHARS))
            } catch (_: Exception) {
                continuation.resume(result.orEmpty().take(MAX_CONTEXT_CHARS))
            }
        }
    }

    private const val MAX_CONTEXT_CHARS = 24000
}
