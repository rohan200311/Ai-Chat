# AI Chat — Native Android + Web

A polished, production-ready ChatGPT-style client for Android (Kotlin + Jetpack Compose) with a matching Web PWA. Built with modern Android best practices (2026), Material You, and extensible multi-provider architecture.

![Android](https://img.shields.io/badge/Android-Kotlin%20%7C%20Compose%20%7C%20Material3-6750A4)
![Web](https://img.shields.io/badge/Web-PWA%20%7C%20Vite%20%7C%20IndexedDB-4285F4)
![MCP](https://img.shields.io/badge/MCP-Model%20Context%20Protocol-00ADD8)

---

## ✨ Features

### Core Chat
- **Multi-provider**: OpenAI, Anthropic Claude, Google Gemini, Groq, Mistral, Ollama (local), OpenRouter, and fully custom OpenAI-compatible endpoints
- **Model picker**: Capability badges (vision, tools, reasoning), context length, pricing, streaming toggle
- **Material You**: Dynamic color, light/dark/system theme, expressive components, edge-to-edge, font scaling
- **Chat UI**: Bubbles, streaming with reasoning display, haptics, copy/regenerate, selectable text

### Rich Content
- **File uploads**: Images (vision), PDF (via PdfBox-Android text extraction), DOCX (via Apache POI), TXT
- **Markdown**: Native Compose rendering via `multiplatform-markdown-renderer` + syntax highlighting (`Highlights`)
- **Code blocks**: Language detection, copy button, JetBrains Mono, horizontal scroll
- **Tables**: GFM tables with Material styling
- **LaTeX / Formulas**: KaTeX via WebView fallback for `$$...$$` and `$...$`
- **Mermaid diagrams**: Flowcharts, sequence, class, ER, gantt — rendered via WebView + mermaid.js 11.6.0

### Power User Extras
- **Message branching**: Tree structure (`parentId` + `branchChildren`). Create alternative paths from any message, navigate branches, fork conversations
- **Conversation memory**: Room entity `MemoryEntry` with importance scoring. Auto-summarization every 10 messages, global + per-conversation memory injection into system prompt
- **Prompt variables**: `{{variable}}` and `{{variable:default}}` syntax. Parser extracts variables, prompts for values, validates required. Reusable templates
- **Search integration**: Brave, Tavily, SerpAPI abstraction. Toggle per chat, injects results into system prompt for RAG-style grounding
- **QR import/export**: Providers JSON → QR bitmap via ZXing. Base64 fallback. Import via camera or paste. Same format Android ↔ Web
- **Workspace agent environment**: File explorer (`WorkspaceFile`), agent mode toggle, task prompt, logs. Tools: `read_file`, `write_file`, `execute_code`, `web_search`, `memory_search`. Sandboxed execution concept, MCP-aware
- **MCP (Model Context Protocol)**: 
  - Client: Connects to SSE/WebSocket MCP servers, `tools/list`, `tools/call`, exposes as LLM tools
  - Android-native Mobile-MCP: Discovers services via `PackageManager` with intent filter `com.aichat.MCP_TOOL`, invokes via `Intent`/`Messenger`
  - Host service: `McpHostService` allows this app to expose its own tools to other agents
- **Settings**: DataStore Preferences — theme, dynamic color, default provider/model, streaming, memory, search provider, MCP toggle, auto-title, font scale, haptics

### Architecture (Android)
- **Kotlin 2.1.20 + Compose BOM 2025.04.01 + Material3 1.4.0 / Expressive 1.5.0-alpha**
- **Hilt** for DI, **Room** for local DB (conversations, messages, providers, models, memory, workspaces), **DataStore** for settings
- **Navigation Compose** with type-safe routes (`@Serializable Route`)
- **MVVM + Repository**: `ChatRepository`, `ProviderRepository`, `MemoryRepository`
- **Networking**: OkHttp 5 + OkHttp SSE for streaming, Retrofit + kotlinx.serialization
- **File parsing**: PdfBox-Android, POI, Coil 3 for images
- **Background**: WorkManager for title generation
- **Security**: API keys in Room (could be EncryptedSharedPreferences), custom headers support, no logs in release

```
app/
 ├─ data/local/db (Room entities, DAOs, Converters)
 ├─ data/local/datastore (Settings)
 ├─ data/remote/provider (OpenAICompatible, Anthropic, Gemini, Factory)
 ├─ data/mcp (McpClient, McpModels, McpHostService)
 ├─ data/repository (Chat, Provider, Memory)
 ├─ domain/model (Conversation, Message, ProviderConfig, Attachment, etc)
 ├─ domain/usecase (SendMessage, Branch, GetBranch)
 ├─ ui/theme (Material You, dynamic color)
 ├─ ui/navigation (NavGraph, Routes)
 ├─ ui/components/markdown (MarkdownRenderer with WebView fallback for Mermaid+KaTeX)
 ├─ ui/components (MessageBubble, ModelPickerSheet)
 ├─ ui/screens/{home, chat, settings, models, workspace}
 ├─ util (FileParser, PromptVariableParser, QRCodeUtil, SearchIntegration)
 └─ service (ChatService orchestrates memory+search+MCP)
```

### Web Version (PWA)
- **Vite + TypeScript**, pure ESM, no framework — matches Android design system
- **IndexedDB** via `idb` for offline providers/conversations/messages
- **Marked + Highlight.js** for markdown, **Mermaid** for diagrams, **KaTeX** CSS for formulas
- **QRCode.js** for export, **File API** for uploads (pdf.js placeholder for extraction)
- Features mirrored: branching, variables, search toggle, MCP toggle, workspace mock, provider management with custom headers, same JSON format for QR import/export
- PWA manifest, theme-color, responsive sidebar + chat layout

Located in `/web`:
```
web/
 ├─ index.html
 ├─ manifest.json
 ├─ vite.config.js
 ├─ package.json
 └─ src/
     ├─ main.ts (full app logic, 800+ LOC)
     └─ styles/main.css (Material You tokens, dark mode, message bubbles)
```

---

## 🚀 Getting Started

### Android
1. Open in Android Studio Ladybug+ (AGP 8.7.2, compileSdk 36, minSdk 26, Java 17)
2. Sync Gradle (version catalog in `gradle/libs.versions.toml`)
3. Add providers in Settings → Providers:
   - OpenAI: `https://api.openai.com/v1`, apiKey `sk-...`
   - Anthropic: `https://api.anthropic.com`, header `x-api-key`
   - Ollama: `http://10.0.2.2:11434/v1` (emulator localhost), models `llama3.2`
   - Custom: any OpenAI-compatible URL + custom headers JSON `{"X-API-Key":"..."}`
4. Run `app` on device/emulator

### Web
```bash
cd web
npm install
npm run dev   # http://localhost:5173, host 0.0.0.0 for preview
npm run build # production PWA
```

The web app works standalone, no backend needed. Add your API keys in Settings — they stay in IndexedDB.

---

## 🔧 Provider Configuration

`ProviderConfig`:
```kotlin
data class ProviderConfig(
  id, name, type (OPENAI, ANTHROPIC, GOOGLE, GROQ, MISTRAL, OLLAMA, OPENROUTER, CUSTOM),
  baseUrl, apiKey,
  customHeaders: Map<String, String>, // e.g., {"Helicone-Auth": "Bearer ..."}
  enabledModels: List<String>,
  supportsVision, supportsTools, supportsStreaming
)
```

Default models seeded per type (GPT-4o, Claude 3.5 Sonnet, Gemini 2.0 Flash, etc). You can override.

**Custom headers use-case**: Proxy auth, Helicone, Cloudflare AI Gateway, LiteLLM, etc.

---

## 📎 File Uploads

`FileParser`:
- Images → base64 for vision models (`data:mime;base64,...`)
- PDF → `PDFTextStripper` (PdfBox-Android) → `extractedText` injected into context as `[Attachments content]`
- DOCX → POI `XWPFDocument.paragraphs`
- TXT → direct read
- All attachments stored in `Message.attachments` with `AttachmentType`

Limits: 20k chars per file (configurable), streamed.

---

## 🌿 Branching

Messages have `parentId` and `branchChildren: List<String>`. 
- `getBranch(conversationId, leafId)` walks parent chain to build current thread
- `createBranch(fromMessageId, newContent)` creates sibling branch
- UI shows branch count badge, allows switching via dialog (future: visual tree)

---

## 🧠 Memory

`MemoryEntry` (global or per-conversation):
- `key`, `value`, `importance` (0..1), `lastAccessed`
- `MemoryRepository.getRelevant(conversationId)` → sorted by importance
- Auto-summarization every 10 messages (placeholder: LLM call in production)
- Injected into system prompt as `[Memory]` block

---

## 🔤 Prompt Variables

Syntax: `{{name}}` or `{{name:default value}}`

`PromptVariableParser`:
- `extractVariables(template)` → list of `PromptVariable`
- `render(template, values)` → replaced string
- `validate(template, values)` → missing required

Example template:
```
You are {{role:helpful assistant}} for {{project}}. 
User: {{query}}
```

---

## 🔍 Search

`SearchIntegration` supports Brave, Tavily, SerpAPI. Toggle per chat.
- If enabled, last user message → search API → results injected into system prompt
- UI: search icon tinted when active

Add API key in Settings → Search.

---

## 🔗 QR Code

`QRCodeUtil`:
- `generateQrBitmap(json, size)` → ZXing `QRCodeWriter` → Bitmap
- Export: `ProviderRepository.exportProviders()` → JSON array
- Import: parse JSON or base64-decoded JSON
- Same format Android ↔ Web, so you can scan phone → web and vice versa

---

## 🗂️ Workspace Agent

`WorkspaceScreen` + `WorkspaceViewModel`:
- File explorer (Room `WorkspaceFile`), editor, agent prompt input
- Agent mode toggle: when ON, LLM can call workspace tools
- Tools (via MCP): `read_file`, `write_file`, `execute_python` (sandboxed), `web_search`, `memory_search`
- Logs displayed in UI
- Future: integrate with `ChatService` to run agent loop

---

## 🛠️ MCP

`McpClient`:
- `addServer(McpServerConfig)` with `transport` SSE/WebSocket/STDIO, `url`, `headers`
- `discoverTools(server)` → `GET /mcp/tools/list`
- `callTool(serverId, name, args)` → `POST /mcp/tools/call`
- `asProviderTools()` → converts to OpenAI tool format
- `discoverAndroidMcpServices(context)` → queries `PackageManager` for services with Mobile-MCP intent

`McpHostService`: Android Service that other apps can bind to expose tools.

MCP servers can be added in Settings → MCP Servers (UI placeholder, logic ready).

See `Mobile-MCP` paper: Android Intent-based MCP (OS-for-Agent 2026).

---

## 🎨 Material You

- `AiChatTheme` uses `dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+
- Fallback palettes `Purple80/40` etc
- `enableEdgeToEdge()` in `MainActivity`
- Expressive components opt-in (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`) ready for M3 Expressive 1.5.0-alpha
- Typography, shape, elevation tokens from M3
- Dark mode via DataStore `theme` = system/light/dark

---

## 🔒 Security & Production Readiness

- Minify + shrink in release, ProGuard rules for Room, serialization, PdfBox, POI
- No API keys in logs, `isMinifyEnabled=true`
- Data extraction/backup rules exclude settings
- OkHttp timeouts 30s connect / 180s read for streaming
- Error handling in streaming (ignore malformed SSE lines)
- Desugaring for Java 8+ APIs
- Hilt for testability, ViewModel + StateFlow, `collectAsStateWithLifecycle()`
- Room destructive migration fallback (for v1, would add migrations in prod)

---

## 📦 Dependencies (Key)

- `androidx.compose:compose-bom:2025.04.01`, `material3:1.4.0-alpha12`, `activity-compose:1.10.1`, `navigation-compose:2.9.0`
- `room:2.7.0-alpha12`, `datastore:1.1.2`, `hilt:2.56.2`
- `okhttp:5.0.0-alpha.16`, `retrofit:2.11.0`, `kotlinx-serialization-json:1.8.0`
- `coil:3.1.0`, `multiplatform-markdown-renderer:0.35.0`, `highlights:2.1.0`
- `zxing:3.5.3`, `mcp-kotlin:0.4.0`, `pdfbox-android:2.0.27.0`, `poi-ooxml:5.4.0`
- Web: `marked:14.1.3`, `mermaid:11.6.0`, `katex:0.16.22`, `highlight.js:11.11.1`, `qrcode:1.5.4`, `idb:8.0.2`

Full catalog in `gradle/libs.versions.toml`.

---

## 🗺️ Roadmap

- [ ] Encrypted DataStore for API keys (EncryptedSharedPreferences)
- [ ] Vector DB for memory (Room + sqlite-vec)
- [ ] Voice input (SpeechRecognizer) + TTS
- [ ] Image generation (DALL·E, SD via custom provider)
- [ ] Plugin marketplace for MCP servers
- [ ] Collaborative workspaces via WebRTC
- [ ] Android Auto / Wear OS companion

---

## 📄 License

MIT — use freely, add your own providers.

Built with ❤️ using Kotlin, Compose, and Material You.
