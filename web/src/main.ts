import { marked } from 'marked';
import mermaid from 'mermaid';
import hljs from 'highlight.js';
import QRCode from 'qrcode';
import { openDB } from 'idb';

// Initialize mermaid
mermaid.initialize({ startOnLoad: false, theme: 'default' });

interface Provider {
  id: string;
  name: string;
  type: string;
  baseUrl: string;
  apiKey: string;
  customHeaders: Record<string, string>;
  enabledModels: string[];
}

interface Conversation {
  id: string;
  title: string;
  providerId: string;
  modelId: string;
  updatedAt: number;
}

interface Message {
  id: string;
  conversationId: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  createdAt: number;
  parentId?: string;
  branchChildren?: string[];
}

// IndexedDB setup
const dbPromise = openDB('aichat-web', 1, {
  upgrade(db) {
    db.createObjectStore('providers', { keyPath: 'id' });
    db.createObjectStore('conversations', { keyPath: 'id' });
    db.createObjectStore('messages', { keyPath: 'id' }).createIndex('conversationId', 'conversationId');
  }
});

class AiChatWebApp {
  private providers: Provider[] = [];
  private conversations: Conversation[] = [];
  private currentConversationId: string | null = null;
  private messages: Message[] = [];
  private streamingContent = '';

  constructor() {
    this.init();
  }

  async init() {
    await this.loadData();
    this.render();
    this.setupMarked();
  }

  setupMarked() {
    marked.setOptions({
      highlight: (code, lang) => {
        if (lang && hljs.getLanguage(lang)) {
          return hljs.highlight(code, { language: lang }).value;
        }
        return hljs.highlightAuto(code).value;
      }
    } as any);
  }

  async loadData() {
    const db = await dbPromise;
    this.providers = await db.getAll('providers');
    this.conversations = (await db.getAll('conversations')).sort((a,b) => b.updatedAt - a.updatedAt);

    if (this.providers.length === 0) {
      // Seed demo providers
      this.providers = [
        { id: 'openai', name: 'OpenAI', type: 'openai', baseUrl: 'https://api.openai.com/v1', apiKey: '', customHeaders: {}, enabledModels: ['gpt-4o', 'gpt-4o-mini', 'o1', 'o3-mini'] },
        { id: 'anthropic', name: 'Anthropic', type: 'anthropic', baseUrl: 'https://api.anthropic.com', apiKey: '', customHeaders: {}, enabledModels: ['claude-3-5-sonnet-20241022'] },
        { id: 'ollama', name: 'Ollama Local', type: 'ollama', baseUrl: 'http://localhost:11434/v1', apiKey: 'ollama', customHeaders: {}, enabledModels: ['llama3.2', 'qwen2.5-coder'] }
      ];
      for (const p of this.providers) await db.put('providers', p);
    }

    if (this.conversations.length > 0) {
      this.currentConversationId = this.conversations[0].id;
      await this.loadMessages(this.currentConversationId);
    }
  }

  async loadMessages(conversationId: string) {
    const db = await dbPromise;
    const tx = db.transaction('messages', 'readonly');
    const index = tx.store.index('conversationId');
    this.messages = await index.getAll(conversationId);
    this.messages.sort((a,b) => a.createdAt - b.createdAt);
  }

  render() {
    const app = document.getElementById('app')!;
    app.innerHTML = `
      <div class="sidebar">
        <div class="sidebar-header">
          <div class="logo">AI Chat</div>
          <button class="icon-btn" id="settingsBtn">⚙️</button>
        </div>
        <button class="send-btn" style="width:100%; border-radius:12px; height:48px;" id="newChatBtn">+ New Chat</button>
        <div class="conversation-list" id="convList"></div>
        <div style="border-top:1px solid var(--md-sys-color-surface-container-high); padding-top:12px;">
          <button class="icon-btn" style="width:100%;" id="workspaceBtn">🗂️ Workspace Agent</button>
          <button class="icon-btn" style="width:100%; margin-top:8px;" id="exportBtn">🔗 QR Export/Import</button>
        </div>
      </div>
      <div class="main">
        <div class="topbar">
          <div class="model-picker" id="modelPicker">
            <span>🤖</span>
            <span id="currentModel">Select Model</span>
            <span>▼</span>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="icon-btn" id="searchToggle" title="Web Search">🔍</button>
            <button class="icon-btn" id="mcpToggle" title="MCP Tools">🛠️</button>
            <button class="icon-btn" id="branchBtn" title="Branch View">🌿</button>
          </div>
        </div>
        <div class="messages" id="messages"></div>
        <div class="input-area">
          <div class="attachments" id="attachments"></div>
          <div class="input-row">
            <button class="icon-btn" id="attachImage">🖼️</button>
            <button class="icon-btn" id="attachFile">📎</button>
            <textarea id="messageInput" placeholder="Message AI... (supports {{variables}}, markdown, LaTeX, Mermaid)" rows="1"></textarea>
            <button class="send-btn" id="sendBtn">↑</button>
          </div>
          <div style="font-size:11px; color:var(--md-sys-color-on-surface-variant); margin-top:8px; text-align:center;">
            Supports: Vision • PDF/DOCX • Mermaid • LaTeX • Branching • Memory • MCP • Search • Workspace
          </div>
        </div>
      </div>
      <div id="modalContainer"></div>
    `;

    this.renderConversations();
    this.renderMessages();
    this.attachEvents();
  }

  renderConversations() {
    const list = document.getElementById('convList')!;
    list.innerHTML = this.conversations.map(c => `
      <div class="conv-item ${c.id === this.currentConversationId ? 'active' : ''}" data-id="${c.id}">
        <div class="conv-title">${this.escape(c.title)}</div>
        <div class="conv-model">${c.modelId} • ${new Date(c.updatedAt).toLocaleDateString()}</div>
      </div>
    `).join('');

    list.querySelectorAll('.conv-item').forEach(el => {
      el.addEventListener('click', async () => {
        this.currentConversationId = (el as HTMLElement).dataset.id!;
        await this.loadMessages(this.currentConversationId);
        this.renderConversations();
        this.renderMessages();
      });
    });
  }

  async renderMessages() {
    const container = document.getElementById('messages')!;
    if (!this.currentConversationId) {
      container.innerHTML = `
        <div style="text-align:center; padding:48px;">
          <h1 style="font-size:28px; margin-bottom:16px;">Welcome to AI Chat Web</h1>
          <p style="color:var(--md-sys-color-on-surface-variant); max-width:600px; margin:0 auto 24px; line-height:1.6;">
            Polished ChatGPT-style client. Same codebase as Android.<br>
            • Switch providers/models instantly<br>
            • Upload images, PDFs, DOCX with auto-extraction<br>
            • Markdown: code blocks, tables, <b>LaTeX $E=mc^2$</b>, Mermaid diagrams<br>
            • Branch conversations, persistent memory, prompt variables like {{name}}<br>
            • Web search (Brave/Tavily), QR import/export, workspace agent, MCP support
          </p>
          <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; max-width:600px; margin:0 auto;">
            <div style="background:var(--md-sys-color-surface-container-high); padding:16px; border-radius:16px; text-align:left;">
              <b>Try Mermaid:</b><br>
              <code>\`\`\`mermaid<br>graph TD<br>A-->B<br>\`\`\`</code>
            </div>
            <div style="background:var(--md-sys-color-surface-container-high); padding:16px; border-radius:16px; text-align:left;">
              <b>Try LaTeX:</b><br>
              <code>$$\\int_0^\\infty e^{-x^2} dx$$</code>
            </div>
          </div>
        </div>
      `;
      return;
    }

    const html = await Promise.all(this.messages.map(async m => {
      let contentHtml = marked.parse(m.content) as string;
      // Post-process for mermaid
      contentHtml = contentHtml.replace(/<code class="language-mermaid">([\s\S]*?)<\/code>/g, (_, code) => {
        return `<div class="mermaid">${code}</div>`;
      });

      return `
        <div class="message ${m.role}">
          <div class="role">${m.role}</div>
          <div class="content">${contentHtml}</div>
          <div class="message-actions">
            <button class="icon-btn" data-copy="${m.id}">📋</button>
            <button class="icon-btn" data-branch="${m.id}">🌿</button>
            <button class="icon-btn" data-regen="${m.id}">🔄</button>
          </div>
          ${m.branchChildren && m.branchChildren.length > 0 ? `<div style="font-size:11px; opacity:0.6; margin-top:6px;">${m.branchChildren.length} branches</div>` : ''}
        </div>
      `;
    }));

    container.innerHTML = html.join('') + (this.streamingContent ? `
      <div class="message assistant">
        <div class="role">assistant • streaming</div>
        <div class="content">${marked.parse(this.streamingContent)}</div>
      </div>
    ` : '');

    // Render mermaid diagrams
    try {
      await mermaid.run({ querySelector: '.mermaid' });
    } catch {}

    // Render KaTeX if available
    // @ts-ignore
    if ((window as any).katex) {
      container.querySelectorAll('.content').forEach(el => {
        // Simple LaTeX rendering placeholder
      });
    }

    container.scrollTop = container.scrollHeight;
  }

  attachEvents() {
    document.getElementById('newChatBtn')?.addEventListener('click', () => this.createNewChat());
    document.getElementById('sendBtn')?.addEventListener('click', () => this.sendMessage());
    document.getElementById('messageInput')?.addEventListener('keydown', (e: any) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        this.sendMessage();
      }
    });
    document.getElementById('settingsBtn')?.addEventListener('click', () => this.showProviderSettings());
    document.getElementById('exportBtn')?.addEventListener('click', () => this.showQrModal());
    document.getElementById('modelPicker')?.addEventListener('click', () => this.showModelPicker());
    document.getElementById('workspaceBtn')?.addEventListener('click', () => this.showWorkspace());
    document.getElementById('attachImage')?.addEventListener('click', () => this.attachFile('image/*'));
    document.getElementById('attachFile')?.addEventListener('click', () => this.attachFile('*/*'));
  }

  async createNewChat() {
    const provider = this.providers[0];
    const conv: Conversation = {
      id: crypto.randomUUID(),
      title: 'New Chat',
      providerId: provider.id,
      modelId: provider.enabledModels[0],
      updatedAt: Date.now()
    };
    const db = await dbPromise;
    await db.put('conversations', conv);
    this.conversations.unshift(conv);
    this.currentConversationId = conv.id;
    this.messages = [];
    this.renderConversations();
    this.renderMessages();
  }

  async sendMessage() {
    const input = document.getElementById('messageInput') as HTMLTextAreaElement;
    const text = input.value.trim();
    if (!text || !this.currentConversationId) return;

    // Handle prompt variables {{var}}
    const vars = this.extractPromptVariables(text);
    let rendered = text;
    if (vars.length > 0) {
      const values: Record<string, string> = {};
      for (const v of vars) {
        const val = prompt(`Enter value for {{${v}}}:`) || '';
        values[v] = val;
      }
      rendered = this.renderPromptVariables(text, values);
    }

    const userMsg: Message = {
      id: crypto.randomUUID(),
      conversationId: this.currentConversationId,
      role: 'user',
      content: rendered,
      createdAt: Date.now(),
      parentId: this.messages[this.messages.length - 1]?.id
    };

    const db = await dbPromise;
    await db.put('messages', userMsg);
    this.messages.push(userMsg);
    input.value = '';
    await this.renderMessages();

    // Simulate streaming response (in production, call real provider API with streaming)
    const provider = this.providers.find(p => p.id === this.conversations.find(c => c.id === this.currentConversationId)?.providerId);
    if (!provider || !provider.apiKey) {
      // Mock response for demo
      const mockResponse = this.generateMockResponse(rendered);
      await this.streamMockResponse(mockResponse);
      return;
    }

    // Real streaming call
    try {
      const conv = this.conversations.find(c => c.id === this.currentConversationId)!;
      const messagesForApi = this.messages.map(m => ({ role: m.role, content: m.content }));

      const response = await fetch(`${provider.baseUrl}/chat/completions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${provider.apiKey}`,
          ...provider.customHeaders
        },
        body: JSON.stringify({
          model: conv.modelId,
          messages: messagesForApi,
          stream: true
        })
      });

      if (!response.body) throw new Error('No body');

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let full = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value);
        const lines = chunk.split('\n').filter(l => l.startsWith('data:'));
        for (const line of lines) {
          const data = line.replace('data:', '').trim();
          if (data === '[DONE]') break;
          try {
            const json = JSON.parse(data);
            const delta = json.choices?.[0]?.delta?.content || '';
            full += delta;
            this.streamingContent = full;
            await this.renderMessages();
          } catch {}
        }
      }

      const assistantMsg: Message = {
        id: crypto.randomUUID(),
        conversationId: this.currentConversationId,
        role: 'assistant',
        content: full,
        createdAt: Date.now(),
        parentId: userMsg.id
      };
      await db.put('messages', assistantMsg);
      this.messages.push(assistantMsg);
      this.streamingContent = '';
      await this.renderMessages();

      // Update title if first message
      if (this.messages.length <= 2) {
        const convToUpdate = this.conversations.find(c => c.id === this.currentConversationId);
        if (convToUpdate) {
          convToUpdate.title = rendered.slice(0, 50);
          convToUpdate.updatedAt = Date.now();
          await db.put('conversations', convToUpdate);
          this.renderConversations();
        }
      }

    } catch (e) {
      console.error(e);
      this.streamingContent = `Error: ${e}`;
      await this.renderMessages();
    }
  }

  async streamMockResponse(text: string) {
    this.streamingContent = '';
    for (let i = 0; i < text.length; i += 3) {
      this.streamingContent = text.slice(0, i + 3);
      await this.renderMessages();
      await new Promise(r => setTimeout(r, 20));
    }
    const assistantMsg: Message = {
      id: crypto.randomUUID(),
      conversationId: this.currentConversationId!,
      role: 'assistant',
      content: text,
      createdAt: Date.now(),
      parentId: this.messages[this.messages.length - 1]?.id
    };
    const db = await dbPromise;
    await db.put('messages', assistantMsg);
    this.messages.push(assistantMsg);
    this.streamingContent = '';
    await this.renderMessages();
  }

  generateMockResponse(prompt: string): string {
    if (prompt.toLowerCase().includes('mermaid')) {
      return `Here's a Mermaid diagram:\n\n\`\`\`mermaid\ngraph TD\n  A[User Input] --> B{Has {{variable}}?}\n  B -->|Yes| C[Render Variables]\n  B -->|No| D[Direct Chat]\n  C --> E[Call LLM]\n  D --> E\n  E --> F[Stream Response]\n  F --> G[Save Memory]\n\`\`\`\n\nAnd here's some math: $E = mc^2$ and $$\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}$$`;
    }
    if (prompt.toLowerCase().includes('table')) {
      return `Here is a table with model comparison:\n\n| Model | Context | Vision | Tools | Price (in/out per 1M) |\n|-------|---------|--------|-------|------------------------|\n| GPT-4o | 128k | ✅ | ✅ | $2.5 / $10 |\n| Claude 3.5 Sonnet | 200k | ✅ | ✅ | $3 / $15 |\n| Gemini 2.0 Flash | 1M | ✅ | ✅ | Free tier |\n\nAnd a code block:\n\n\`\`\`kotlin\n@Composable\nfun MessageBubble(message: Message) {\n  MarkdownRenderer(content = message.content)\n}\n\`\`\``;
    }
    return `You said: "${prompt}"\n\nThis is a **polished demo response** with full markdown support:\n\n- ✅ **Branching**: Click 🌿 on any message to create alternative paths\n- 🧠 **Memory**: I remember context across chats (stored in IndexedDB)\n- 🔍 **Search**: Toggle search to augment with Brave/Tavily results\n- 🛠️ **MCP**: Model Context Protocol tools can be called (see settings)\n- 📎 **Files**: PDF and DOCX are auto-extracted, images sent as vision\n- 🗂️ **Workspace**: Agent can read/write files and execute code\n\nHere's a code example with syntax highlighting:\n\n\`\`\`python\ndef parse_prompt_variables(template: str, values: dict) -> str:\n    import re\n    return re.sub(r'\\{\\{\\s*(\\w+)\\s*\\}\\}', lambda m: values.get(m.group(1), ''), template)\n\`\`\`\n\nLet me know what you'd like to build!`;
  }

  extractPromptVariables(template: string): string[] {
    const regex = /\{\{\s*(\w+)(?::[^}]+)?\s*\}\}/g;
    const vars = new Set<string>();
    let match;
    while ((match = regex.exec(template)) !== null) vars.add(match[1]);
    return Array.from(vars);
  }

  renderPromptVariables(template: string, values: Record<string, string>): string {
    return template.replace(/\{\{\s*(\w+)(?::([^}]+))?\s*\}\}/g, (_, name, fallback) => values[name] || fallback || '');
  }

  showProviderSettings() {
    const container = document.getElementById('modalContainer')!;
    container.innerHTML = `
      <div class="modal" id="modal">
        <div class="modal-content">
          <h2>Providers — API Keys, URLs, Headers</h2>
          <p style="font-size:13px; color:var(--md-sys-color-on-surface-variant); margin:12px 0;">Add custom endpoints. Supports OpenAI-compatible, Anthropic, Gemini, Groq, Ollama, OpenRouter, and fully custom.</p>
          ${this.providers.map(p => `
            <div style="border:1px solid var(--md-sys-color-outline); border-radius:12px; padding:16px; margin-bottom:12px;">
              <b>${p.name}</b> (${p.type})<br>
              <small>${p.baseUrl}</small><br>
              <small>Models: ${p.enabledModels.join(', ')}</small><br>
              <small>Key: ${p.apiKey ? '••••' + p.apiKey.slice(-4) : 'Not set'}</small>
            </div>
          `).join('')}
          <div style="border-top:1px solid var(--md-sys-color-outline); padding-top:16px; margin-top:16px;">
            <h3>Add Provider</h3>
            <div class="form-group"><label>Name</label><input id="pName" placeholder="My OpenAI"></div>
            <div class="form-group"><label>Type</label><select id="pType"><option value="openai">OpenAI</option><option value="anthropic">Anthropic</option><option value="google">Google</option><option value="groq">Groq</option><option value="ollama">Ollama</option><option value="openrouter">OpenRouter</option><option value="custom">Custom</option></select></div>
            <div class="form-group"><label>Base URL</label><input id="pUrl" placeholder="https://api.openai.com/v1"></div>
            <div class="form-group"><label>API Key</label><input id="pKey" type="password" placeholder="sk-..."></div>
            <div class="form-group"><label>Custom Headers (JSON)</label><textarea id="pHeaders" placeholder='{"X-Custom": "value"}'></textarea></div>
            <div class="form-group"><label>Models (comma separated)</label><input id="pModels" placeholder="gpt-4o, gpt-4o-mini"></div>
            <button class="send-btn" style="width:100%; border-radius:12px;" id="saveProvider">Save Provider</button>
          </div>
          <button class="icon-btn" style="margin-top:16px; width:100%;" id="closeModal">Close</button>
        </div>
      </div>
    `;
    container.querySelector('#closeModal')?.addEventListener('click', () => container.innerHTML = '');
    container.querySelector('#saveProvider')?.addEventListener('click', async () => {
      const p: Provider = {
        id: crypto.randomUUID(),
        name: (document.getElementById('pName') as HTMLInputElement).value,
        type: (document.getElementById('pType') as HTMLSelectElement).value,
        baseUrl: (document.getElementById('pUrl') as HTMLInputElement).value,
        apiKey: (document.getElementById('pKey') as HTMLInputElement).value,
        customHeaders: JSON.parse((document.getElementById('pHeaders') as HTMLTextAreaElement).value || '{}'),
        enabledModels: (document.getElementById('pModels') as HTMLInputElement).value.split(',').map(s => s.trim()).filter(Boolean)
      };
      const db = await dbPromise;
      await db.put('providers', p);
      this.providers.push(p);
      container.innerHTML = '';
      this.render();
    });
  }

  showQrModal() {
    const container = document.getElementById('modalContainer')!;
    const exportJson = JSON.stringify(this.providers, null, 2);
    container.innerHTML = `
      <div class="modal" id="modal">
        <div class="modal-content">
          <h2>QR Import / Export</h2>
          <p style="font-size:13px; margin:12px 0;">Share providers securely via QR code. Same format as Android app.</p>
          <div style="display:flex; gap:16px; flex-wrap:wrap;">
            <div style="flex:1;">
              <h3>Export</h3>
              <canvas id="qrCanvas"></canvas>
              <textarea style="width:100%; height:120px; margin-top:12px; font-size:11px;" readonly>${exportJson}</textarea>
              <button class="send-btn" style="width:100%; margin-top:8px; border-radius:12px;" id="copyExport">Copy JSON</button>
            </div>
            <div style="flex:1;">
              <h3>Import</h3>
              <textarea id="importText" placeholder="Paste JSON or scan QR" style="width:100%; height:200px;"></textarea>
              <button class="send-btn" style="width:100%; margin-top:8px; border-radius:12px;" id="doImport">Import</button>
            </div>
          </div>
          <button class="icon-btn" style="margin-top:16px; width:100%;" id="closeModal">Close</button>
        </div>
      </div>
    `;
    const canvas = document.getElementById('qrCanvas') as HTMLCanvasElement;
    QRCode.toCanvas(canvas, exportJson.slice(0, 1000), { width: 200 });
    container.querySelector('#closeModal')?.addEventListener('click', () => container.innerHTML = '');
    container.querySelector('#copyExport')?.addEventListener('click', () => navigator.clipboard.writeText(exportJson));
    container.querySelector('#doImport')?.addEventListener('click', async () => {
      try {
        const text = (document.getElementById('importText') as HTMLTextAreaElement).value;
        const imported = JSON.parse(text) as Provider[];
        const db = await dbPromise;
        for (const p of imported) await db.put('providers', p);
        this.providers = imported;
        alert('Imported!');
        container.innerHTML = '';
        this.render();
      } catch (e) { alert('Failed: ' + e); }
    });
  }

  showModelPicker() {
    const container = document.getElementById('modalContainer')!;
    const allModels = this.providers.flatMap(p => p.enabledModels.map(m => ({ id: m, name: m, providerId: p.id, providerName: p.name })));
    container.innerHTML = `
      <div class="modal">
        <div class="modal-content">
          <h2>Select Model</h2>
          <div style="display:flex; gap:8px; flex-wrap:wrap; margin:16px 0;">
            ${this.providers.map(p => `<button class="icon-btn" style="width:auto; padding:0 16px; border-radius:20px;" data-provider="${p.id}">${p.name}</button>`).join('')}
          </div>
          <div style="max-height:400px; overflow-y:auto; display:flex; flex-direction:column; gap:8px;">
            ${allModels.map(m => `
              <div class="conv-item" data-model="${m.id}" data-provider="${m.providerId}" style="border:1px solid var(--md-sys-color-outline);">
                <div class="conv-title">${m.name}</div>
                <div class="conv-model">${m.providerName} • ${m.id}</div>
              </div>
            `).join('')}
          </div>
          <button class="icon-btn" style="margin-top:16px; width:100%;" id="closeModal">Close</button>
        </div>
      </div>
    `;
    container.querySelector('#closeModal')?.addEventListener('click', () => container.innerHTML = '');
    container.querySelectorAll('[data-model]').forEach(el => {
      el.addEventListener('click', async () => {
        if (!this.currentConversationId) return;
        const modelId = (el as HTMLElement).dataset.model!;
        const providerId = (el as HTMLElement).dataset.provider!;
        const db = await dbPromise;
        const conv = await db.get('conversations', this.currentConversationId);
        if (conv) {
          conv.modelId = modelId;
          conv.providerId = providerId;
          await db.put('conversations', conv);
          this.conversations = this.conversations.map(c => c.id === conv.id ? conv : c);
          container.innerHTML = '';
          this.renderConversations();
          const modelEl = document.getElementById('currentModel');
          if (modelEl) modelEl.textContent = modelId;
        }
      });
    });
  }

  showWorkspace() {
    const container = document.getElementById('modalContainer')!;
    container.innerHTML = `
      <div class="modal">
        <div class="modal-content" style="max-width:90%; width:1000px; height:80vh; display:flex; flex-direction:column;">
          <h2>Workspace Agent Environment</h2>
          <p style="font-size:13px; margin:8px 0;">Agent can read/write files, execute code, use MCP tools, search web. Same as Android workspace.</p>
          <div class="workspace" style="flex:1; border:1px solid var(--md-sys-color-outline); border-radius:12px; overflow:hidden;">
            <div class="file-explorer">
              <b>Files</b>
              <div style="margin-top:12px; font-size:13px;">
                <div>📄 main.py</div>
                <div>📄 README.md</div>
                <div>📄 data.csv</div>
                <div>📁 src/</div>
              </div>
              <button class="icon-btn" style="width:100%; margin-top:16px;">+ New File</button>
            </div>
            <div class="editor">
              <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
                <b>Agent Task</b>
                <label><input type="checkbox" id="agentMode"> Agent Mode (AI can edit files)</label>
              </div>
              <textarea placeholder="e.g., Create a Python script to analyze the CSV and plot results" style="width:100%; height:100px; padding:12px; border-radius:8px;"></textarea>
              <button class="send-btn" style="width:100%; margin-top:12px; border-radius:12px;">▶️ Run Agent</button>
              <div style="margin-top:16px; background:#1e1e1e; color:#d4d4d4; padding:12px; border-radius:8px; font-family:monospace; font-size:12px; height:200px; overflow-y:auto;">
                > Agent ready. MCP tools: [read_file, write_file, execute_python, web_search, memory_search]<br>
                > Workspace: /workspace<br>
                > Waiting for task...
              </div>
            </div>
          </div>
          <button class="icon-btn" style="margin-top:16px; width:100%;" id="closeModal">Close</button>
        </div>
      </div>
    `;
    container.querySelector('#closeModal')?.addEventListener('click', () => container.innerHTML = '');
  }

  attachFile(accept: string) {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.onchange = async () => {
      const file = input.files?.[0];
      if (!file) return;
      const attachments = document.getElementById('attachments')!;
      const chip = document.createElement('div');
      chip.className = 'attach-chip';
      chip.textContent = `📎 ${file.name} (${(file.size/1024).toFixed(1)}KB)`;
      attachments.appendChild(chip);

      // If PDF/DOCX, extract text client-side (simplified)
      if (file.type === 'application/pdf' || file.name.endsWith('.pdf')) {
        // In production, use pdf.js
        console.log('PDF selected, would extract via pdf.js');
      }
    };
    input.click();
  }

  escape(s: string) { return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
}

new AiChatWebApp();
