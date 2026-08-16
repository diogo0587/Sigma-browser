# Análise do Repositório Sigma Eclipse LLM e Plano de Portabilidade

## 1. Arquitetura Encontrada
O projeto `sigma-eclipse-llm` é uma aplicação desktop baseada em Tauri (Rust + React). 
A arquitetura é dividida em:
- **Frontend (React + Vite):** Interface de usuário com chat, configurações de modelo, monitoramento de status e logs.
- **Backend (Tauri / Rust):** Gerencia o ciclo de vida do servidor local. Ele faz o download de binários pré-compilados do `llama.cpp` (`llama-server`) e de arquivos de modelo `.gguf`, dependendo do sistema operacional e arquitetura (Windows, macOS, Linux).
- **Comunicação:** O frontend se comunica com o processo Tauri via IPC (Inter-Process Communication) para iniciar/parar o servidor, e faz requisições HTTP REST diretas para `localhost:8080` (onde o `llama-server` está rodando) para inferência.

## 2. Runtime do LLM
O runtime utilizado é o **llama.cpp** (especificamente o binário `llama-server`, versão `b8611`). O processamento de LLM acontece através de requisições HTTP locais para a API compatível com OpenAI fornecida pelo `llama-server`.

## 3. Modelos Utilizados
A aplicação utiliza modelos no formato **GGUF**. As configurações em `versions.json` apontam para:
- **Model (Padrão):** `Qwen3.5-4B.Q6_K.gguf` (Qwen3.5-4B Quantizado em Q6_K)
- **Model Uncensored:** `Qwen3.5-4B-Uncensored-HauhauCS-Aggressive-Q6_K.gguf`
- **Model S (Low RAM):** `model-s-v1.0.gguf` (versão menor para sistemas com pouca memória)

## 4. Requisitos de Hardware
O backend em Rust implementa uma detecção de memória e VRAM (GPU):
- **Modelos Grandes (Contexto de até 28k-30k):** Exigem 24GB+ ou 16GB+ de RAM dependendo do OS.
- **Modelos Pequenos (Contexto 6k-12k):** Para sistemas com menos de 16GB de RAM.
- **GPU:** Detecta NVIDIA via `wmic` / `nvidia-smi` no Windows, ou Apple Silicon no macOS para definir as camadas de GPU offload (ex: 35 ou 41 camadas).

## 5. Possibilidade Real de Portar para Android
**Desafios:**
- Executar um binário web server (`llama-server`) como subprocesso no Android é problemático devido a restrições de permissão de rede local, ciclo de vida do Android e restrições de execução (W^X).
- O download dinâmico de binários executáveis violaria as políticas de segurança modernas do Android (SELinux não permite executar binários na pasta de dados do app facilmente nas versões mais novas).

**Solução para Android (Plano Realista):**
A melhor forma de executar `llama.cpp` no Android é compilar a biblioteca via **JNI/NDK** (criando um `libllama.so`) e consumi-la nativamente em Kotlin, em vez de iniciar um servidor HTTP local.
Como estamos focados em gerar um MVP funcional rapidamente neste ambiente, criaremos uma arquitetura robusta baseada em interfaces (`LLMProvider`), com:
- `LocalEclipseProvider`: Implementação preparada para consumir JNI (llama.cpp) ou simular inferência local offline até que as bibliotecas NDK sejam integradas ao build final.
- **Formato:** O app Android gerenciará arquivos GGUF em seu armazenamento privado.

## 6. Dependências Incompatíveis
- **Tauri / Rust:** Não pode ser usado diretamente. A lógica de gerência de servidor (Rust) e UI (React) precisa ser inteiramente reescrita em Kotlin (ViewModels) e Jetpack Compose.
- **llama-server (binário):** Deve ser substituído por chamadas JNI ou simulação de execução local para viabilidade do MVP no Android Studio.

## 7. Plano de Implementação (Fases)

O projeto seguirá uma abordagem modular (Clean Architecture/MVVM) com as seguintes fases de desenvolvimento iterativo:

1. **FASE 1 (Concluída):** Análise da arquitetura do Eclipse.
2. **FASE 2:** Criação do "shell" Android (Estrutura de diretórios, dependências Gradle, Compose, Room, permissões).
3. **FASE 3:** Desenvolvimento do Navegador Web (`WebView` com controles, histórico, abas).
4. **FASE 4:** Integração da infraestrutura Eclipse (Definição de `LLMProvider`, gerenciamento de memória, detecção de HW).
5. **FASE 5:** Criação do AI Chat (UI do assistente sobreposta, suporte a Markdown).
6. **FASE 6:** Chat with Page (Extração de texto via injeção JS na WebView, chunking).
7. **FASE 7:** Privacy Engine (Ad blocker rudimentar bloqueando hosts conhecidos, modo privado).
8. **FASE 8:** AI Agent experimental (Mecanismo para o LLM sugerir ações na WebView).
9. **FASE 9:** Deep Research (Agrupamento iterativo e síntese local).
10. **FASE 10-12:** Otimização, Testes e Geração do APK.

O foco inicial será um app coeso que junte Navegação Web, AI Assistant Integrado e Preparação Local.
