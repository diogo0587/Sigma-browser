package com.sigma.eclipse.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalEclipseProvider : LLMProvider {
    override suspend fun generateStream(prompt: String, systemPrompt: String?): Flow<String> = flow {
        val simulatedResponse = "Eu sou o Eclipse Local LLM. " +
            "Esta é uma simulação de inferência local rodando no dispositivo, " +
            "preparada para ser substituída pela execução JNI do llama.cpp " +
            "ou por modelos via NDK nas próximas fases de integração.\n\n" +
            "Você disse:\n\"$prompt\"\n\n" +
            "Tudo funcionará inteiramente offline."
        
        val words = simulatedResponse.split(" ")
        for (word in words) {
            emit("$word ")
            delay(60) // Simulating generation speed ~16 tokens/second
        }
    }
}
