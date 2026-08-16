package com.sigma.eclipse.privacy

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object PrivacyEngine {
    private val blockedDomains = setOf(
        "google-analytics.com",
        "doubleclick.net",
        "facebook.net",
        "connect.facebook.net",
        "googleadservices.com",
        "googlesyndication.com",
        "ads.twitter.com",
        "scorecardresearch.com",
        "tracker",
        "telemetry"
    )

    fun shouldBlock(request: WebResourceRequest): Boolean {
        val url = request.url ?: return false
        val host = url.host ?: return false
        
        return blockedDomains.any { host.contains(it) }
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
