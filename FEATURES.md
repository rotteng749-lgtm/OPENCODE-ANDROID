# Feature Roadmap — OpenCode Android Extended

## 1. MCP Servers (All)
- [x] MCP server registry (9 servers: filesystem, memory, sequential-thinking, fetch, time, git, puppeteer, sqlite, everything)
- [x] MCP client implementation (stdio JSON-RPC 2.0)
- [x] Server lifecycle management (start/stop/isRunning)
- [ ] Tool discovery & invocation in chat
- [x] UI: MCP server list, enable/disable, setup hints

## 2. Claude Plugins
- [x] Plugin registry (5 official/community plugins)
- [x] Plugin installer (from registry / GitHub URL / JSON)
- [x] Slash command execution in chat
- [x] UI: Plugin browser, install, configure

## 3. Skill Management
- [x] GitHub skill installer (clone + parse SKILL.md)
- [x] Manual skill add (paste markdown)
- [x] Skill executor (auto-activate by trigger words)
- [x] UI: Skill library, add from URL, add manual

## 4. Root Shell Access
- [x] Root detection & executor (su)
- [x] Package manager (pm list, install, uninstall)
- [x] File system access (read/write any path)
- [x] Process manager (ps, kill)
- [x] UI: Root tools panel

## 5. Browser Automation (Root)
- [x] UI Automator wrapper (dump, tap, swipe, input)
- [x] Screenshot capture
- [x] App launcher (am start)
- [x] Node tree parser + tap by text

## 6. 9Router AI Routing
- [x] Multi-provider client (OpenAI-compatible)
- [x] Combo fallback (ordered model list)
- [x] Round-robin strategy
- [x] Cooldown on 401/429 with exponential backoff
- [x] Tier list (Subscription/Cheap/Free)
- [x] Pre-built combos: Auto, Fast, Smart
- [ ] Fusion mode (parallel + judge)
- [ ] Token savers (compression, prompt bias)
- [x] UI: Router combo editor

## 7. Integration
- [x] Wire router into ChatViewModel direct-API mode
- [x] Wire skills into chat (auto-activate)
- [x] Wire plugins into chat (slash commands)
- [x] Labs screen in Settings
- [ ] Wire MCP tools into chat (tool calls)
- [ ] Root tools accessible from chat
