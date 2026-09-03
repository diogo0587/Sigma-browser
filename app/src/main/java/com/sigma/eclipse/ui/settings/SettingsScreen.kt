package com.sigma.eclipse.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.sigma.eclipse.ai.AiProviderConfig
import com.sigma.eclipse.ai.OpenAiCompatibleProvider
import com.sigma.eclipse.ai.SecureApiKeyStore
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val keyStore = remember { SecureApiKeyStore(context) }
    var provider by remember { mutableStateOf(AiProviderConfig.PROVIDER_NVIDIA) }
    var endpoint by remember { mutableStateOf(AiProviderConfig.NVIDIA_ENDPOINT) }
    var model by remember { mutableStateOf(AiProviderConfig.DEFAULT_NVIDIA_MODEL) }
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val config = AiProviderConfig.load(context)
        provider = config.provider
        endpoint = config.endpoint
        model = config.model
        apiKey = keyStore.get().orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inteligência Artificial") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text("A IA é executada por API usando sua própria chave. Nenhuma chave do desenvolvedor é necessária.")
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = provider,
                onValueChange = { provider = it },
                label = { Text("Provedor") },
                supportingText = { Text("NVIDIA ou qualquer API compatível com OpenAI") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text("Endpoint Chat Completions") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Modelo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; saved = false; status = null },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showKey) "Ocultar chave" else "Mostrar chave"
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    status = null
                    AiProviderConfig.save(context, AiProviderConfig(provider, endpoint, model))
                    keyStore.save(apiKey)
                    saved = true
                    status = "Configuração salva com segurança."
                }) {
                    Text("Salvar")
                }
                Button(onClick = {
                    scope.launch {
                        AiProviderConfig.save(context, AiProviderConfig(provider, endpoint, model))
                        keyStore.save(apiKey)
                        status = "Testando..."
                        val result = OpenAiCompatibleProvider(context).testConnection()
                        status = result.fold(
                            onSuccess = { "Conexão funcionando." },
                            onFailure = { "Falha: ${it.message ?: "erro desconhecido"}" }
                        )
                    }
                }) {
                    Text("Testar conexão")
                }
            }

            if (saved || status != null) {
                Spacer(Modifier.height(16.dp))
                Text(status ?: "Configuração salva.")
            }

            Spacer(Modifier.height(24.dp))
            Text("A chave fica criptografada no armazenamento privado do Android e não é gravada no código-fonte.")
        }
    }
}
