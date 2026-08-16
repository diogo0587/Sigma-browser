package com.sigma.eclipse.ai

object ContextManager {
    fun processPageContext(rawText: String, maxTokens: Int = 4000): String {
        // Simple word-based chunking simulation
        // In a real scenario, we'd use the model's actual Tokenizer
        val words = rawText.split("\\s+".toRegex())
        val estimatedTokens = words.size * 1.3 // Rough heuristic
        
        return if (estimatedTokens > maxTokens) {
            val allowedWords = (maxTokens / 1.3).toInt()
            words.take(allowedWords).joinToString(" ") + "\n\n...[Conteúdo truncado para caber na memória RAM local]..."
        } else {
            rawText
        }
    }
}
