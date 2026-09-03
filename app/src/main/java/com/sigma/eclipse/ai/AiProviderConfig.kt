package com.sigma.eclipse.ai

import android.content.Context

/** User-controlled AI provider configuration. Secrets are stored separately. */
data class AiProviderConfig(
    val provider: String = PROVIDER_NVIDIA,
    val endpoint: String = NVIDIA_ENDPOINT,
    val model: String = DEFAULT_NVIDIA_MODEL
) {
    companion object {
        const val PROVIDER_NVIDIA = "NVIDIA"
        const val PROVIDER_OPENAI_COMPATIBLE = "OpenAI-compatible"
        const val NVIDIA_ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions"
        const val DEFAULT_NVIDIA_MODEL = "nvidia/nemotron-3-super-120b-a12b"
        private const val PREFS = "ai_provider_config"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_ENDPOINT = "endpoint"
        private const val KEY_MODEL = "model"

        fun load(context: Context): AiProviderConfig {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val provider = prefs.getString(KEY_PROVIDER, PROVIDER_NVIDIA) ?: PROVIDER_NVIDIA
            val endpoint = prefs.getString(KEY_ENDPOINT, NVIDIA_ENDPOINT) ?: NVIDIA_ENDPOINT
            val model = prefs.getString(KEY_MODEL, DEFAULT_NVIDIA_MODEL) ?: DEFAULT_NVIDIA_MODEL
            return AiProviderConfig(provider, endpoint, model)
        }

        fun save(context: Context, config: AiProviderConfig) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PROVIDER, config.provider)
                .putString(KEY_ENDPOINT, config.endpoint.trim())
                .putString(KEY_MODEL, config.model.trim())
                .apply()
        }
    }
}
