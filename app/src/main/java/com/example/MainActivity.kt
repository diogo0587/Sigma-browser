package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            state = state,
                            onSettingsClick = { navController.navigate("settings") },
                            onStartServer = { viewModel.startServer() },
                            onStopServer = { viewModel.stopServer() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            state = state,
                            onClose = { navController.popBackStack() },
                            onDownloadLlama = { viewModel.downloadLlama() },
                            onDownloadModel = { viewModel.downloadModel() },
                            onUncensoredChange = { viewModel.toggleUncensored(it) },
                            onCtxSizeChange = { viewModel.updateCtxSize(it) },
                            onGpuLayersChange = { viewModel.updateGpuLayers(it) },
                            onRestoreDefaults = { viewModel.restoreDefaults() },
                            onClearAllData = { viewModel.clearAllData() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AppState,
    onSettingsClick: () -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sigma Eclipse LLM") },
                actions = {
                    IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("settings_button")) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusPanel(
                status = state.status,
                isBusy = state.isBusy,
                onStartServer = onStartServer,
                onStopServer = onStopServer
            )
            
            LogsSection(logs = state.logs, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatusPanel(
    status: ServerStatus,
    isBusy: Boolean,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Status: ${status.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (status == ServerStatus.STOPPED || status == ServerStatus.ERROR) {
                    Button(
                        onClick = onStartServer,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("start_server_button")
                    ) {
                        Text("Start Server")
                    }
                } else {
                    Button(
                        onClick = onStopServer,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("stop_server_button")
                    ) {
                        Text("Stop Server")
                    }
                }
            }
        }
    }
}

@Composable
fun LogsSection(logs: List<String>, modifier: Modifier = Modifier) {
    Text("Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = false
        ) {
            items(logs) { log ->
                Text(text = "> $log", color = Color.Green, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppState,
    onClose: () -> Unit,
    onDownloadLlama: () -> Unit,
    onDownloadModel: () -> Unit,
    onUncensoredChange: (Boolean) -> Unit,
    onCtxSizeChange: (Int) -> Unit,
    onGpuLayersChange: (Int) -> Unit,
    onRestoreDefaults: () -> Unit,
    onClearAllData: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Confirm Clear All Data") },
            text = { Text("Are you sure you want to clear all downloaded data? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("close_settings_button")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Setup Section
            SettingsSection("Setup") {
                Button(
                    onClick = onDownloadLlama,
                    enabled = !state.isDownloadingLlama,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isDownloadingLlama) "Downloading..." else "Download llama.cpp")
                }
                
                OutlinedTextField(
                    value = state.baseModel,
                    onValueChange = {},
                    label = { Text("Base Model") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Auto-selected based on system RAM", style = MaterialTheme.typography.bodySmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.isUncensored,
                        onCheckedChange = onUncensoredChange,
                        enabled = !state.isDownloadingModel
                    )
                    Text("Uncensored Model")
                }
                Text("⚠️ Uncensored model may produce unfiltered content. Use with caution.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)

                Button(
                    onClick = onDownloadModel,
                    enabled = !state.isDownloadingModel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isDownloadingModel) "Downloading..." else "Download Current Model")
                }
            }

            // Server Configuration Section
            SettingsSection("Server Configuration") {
                Text("Context Size: ${state.ctxSize}")
                Slider(
                    value = state.ctxSize.toFloat(),
                    onValueChange = { onCtxSizeChange(it.toInt()) },
                    valueRange = 6000f..100000f,
                    steps = 94
                )

                Text("GPU Layers: ${state.gpuLayers}")
                Slider(
                    value = state.gpuLayers.toFloat(),
                    onValueChange = { onGpuLayersChange(it.toInt()) },
                    valueRange = 0f..41f,
                    steps = 41
                )

                OutlinedButton(
                    onClick = onRestoreDefaults,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore Defaults")
                }
            }

            // Maintenance Section
            SettingsSection("Maintenance") {
                Button(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Data")
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}
