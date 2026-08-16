package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ServerStatus {
    STOPPED, STARTING, RUNNING, STOPPING, ERROR
}

data class AppState(
    val status: ServerStatus = ServerStatus.STOPPED,
    val logs: List<String> = emptyList(),
    val baseModel: String = "Llama-3-8B-Instruct-Q4_K_M.gguf",
    val isUncensored: Boolean = false,
    val ctxSize: Int = 30000,
    val gpuLayers: Int = 0,
    val isDownloadingLlama: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val isBusy: Boolean = false
)

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun startServer() {
        if (_state.value.status != ServerStatus.STOPPED) return
        viewModelScope.launch {
            _state.update { it.copy(status = ServerStatus.STARTING, isBusy = true) }
            addLog("Starting llama-server...")
            delay(1000)
            addLog("Loading model: ${_state.value.baseModel}")
            delay(1500)
            addLog("llama server listening at http://127.0.0.1:8080")
            _state.update { it.copy(status = ServerStatus.RUNNING, isBusy = false) }
        }
    }

    fun stopServer() {
        if (_state.value.status != ServerStatus.RUNNING) return
        viewModelScope.launch {
            _state.update { it.copy(status = ServerStatus.STOPPING, isBusy = true) }
            addLog("Stopping server...")
            delay(1000)
            addLog("Server stopped.")
            _state.update { it.copy(status = ServerStatus.STOPPED, isBusy = false) }
        }
    }

    fun updateCtxSize(size: Int) {
        _state.update { it.copy(ctxSize = size) }
    }

    fun updateGpuLayers(layers: Int) {
        _state.update { it.copy(gpuLayers = layers) }
    }

    fun toggleUncensored(checked: Boolean) {
        _state.update { it.copy(isUncensored = checked) }
    }

    fun downloadLlama() {
        viewModelScope.launch {
            _state.update { it.copy(isDownloadingLlama = true) }
            addLog("Downloading llama.cpp...")
            delay(2000)
            addLog("Download complete.")
            _state.update { it.copy(isDownloadingLlama = false) }
        }
    }

    fun downloadModel() {
        viewModelScope.launch {
            _state.update { it.copy(isDownloadingModel = true) }
            addLog("Downloading model ${_state.value.baseModel}...")
            delay(3000)
            addLog("Model download complete.")
            _state.update { it.copy(isDownloadingModel = false) }
        }
    }

    fun restoreDefaults() {
        _state.update { it.copy(
            ctxSize = 30000,
            gpuLayers = 0,
            isUncensored = false
        ) }
    }

    fun clearAllData() {
        _state.update { it.copy(logs = emptyList()) }
        addLog("All downloaded data cleared.")
    }

    private fun addLog(message: String) {
        _state.update { it.copy(logs = it.logs + message) }
    }
}
