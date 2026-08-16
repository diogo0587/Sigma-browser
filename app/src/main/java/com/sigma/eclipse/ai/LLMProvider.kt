package com.sigma.eclipse.ai

import kotlinx.coroutines.flow.Flow

interface LLMProvider {
    suspend fun generateStream(prompt: String, systemPrompt: String? = null): Flow<String>
}
