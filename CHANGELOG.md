# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-11-04

### Added
- Direct-API fallback chat with 26 free providers and 70+ models (OpenRouter, Groq,
  Google Gemini, Mistral AI, LLM7.io, Kilo Code, Hugging Face, Z AI, Cerebras,
  Cloudflare Workers AI, Cohere, OpenCode Zen, SiliconFlow, SambaNova, xAI,
  AI21 Labs, DeepSeek, Nscale, Nebius, ModelScope, OVHcloud AI, Aion Labs,
  Agnes AI, Alibaba Cloud, Chutes.ai, Glhf.chat) from awesome-freellm-apis.
  When the engine binary is missing, chat streams directly from the provider.
- System prompt editor in Settings → Free providers; applied to every direct-API
  request.
- 9Router-style AI router with combos (Auto/Fast/Smart), fallback, round-robin,
  and per-provider cooldown on 401/429.
- MCP server registry with 9 servers (filesystem, memory, sequential-thinking,
  fetch, time, git, puppeteer, sqlite, everything) and stdio JSON-RPC client.
- Claude plugin manager: registry, install from GitHub/JSON, slash commands in chat.
- Skill manager: built-in + GitHub SKILL.md + manual markdown, auto-activates by trigger.
- Root shell executor (su) with package manager, file access, process tools.
- Browser/app automation via root: UI Automator dump, tap, swipe, screenshot.
- Labs screen in Settings for MCP, plugins, skills, router, and root tools.
- ChatGPT-style model picker in the chat composer: routing combos (Auto/Fast/Smart)
  or a fixed provider + model, persisted across restarts.
- Real embedded Termux-style terminal with multiple sessions, PTY, proot, and bootstrap download.
- Glassmorphic UI across all screens (dark/light, translucent panes, mesh background).
- `terminal-emulator` and `terminal-view` modules vendored from Termux (Apache-2.0).
- `TerminalInstaller` downloads and verifies the official Termux bootstrap zip with SHA-256.
- `TerminalSessionManager` runs each session inside `proot` so the Termux userland sees
  `/data/data/com.termux/files`.
- Extra-keys bar, clipboard copy/paste, and session tabs in the terminal screen.
- Indonesian translations for the terminal screen.

### Changed
- `minSdk` and `targetSdk` are now both 28. Android 10+ blocks `execve` from writable app
  storage for `targetSdk >= 29`; the Termux userland must run from `filesDir`, so the app
  opts into the legacy behaviour. `compileSdk` remains 35.
- Version bumped to `0.2.0` / code `3`.
- `core:design` gained `Glass.kt` primitives and a static mesh background instead of
  per-card backdrop blur.

### Fixed
- `TerminalSessionManager.ensureReady()` fast-path now emits `State.Installed` so
  the progress panel dismisses when the bootstrap is already present.
- `TerminalSessionManager.createSession()` creates `files/home` before starting
  `proot` so the `can't chdir` warning is gone.
- Validated on a Samsung A15 (Android 16, arm64-v8a) over adb: bootstrap
  downloads, extracts, proot + bash run, and `uname -a` prints inside the
  Termux userland.

### Known issues
- The extra-keys bar does not latch `CTRL`/`ALT` modifiers yet.
- Engine binaries are not built in this environment, so AI chat requires a
  release built with the engine artifacts present.
- See [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for details.

## [0.1.0] - 2025-01-01

### Added
- Initial release.
