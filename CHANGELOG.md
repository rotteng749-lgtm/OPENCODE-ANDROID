# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-11-04

### Added
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
- None.

### Known issues
- No runtime emulator validation was possible in the build environment.
- The extra-keys bar does not latch `CTRL`/`ALT` modifiers yet.
- Engine binaries are not built in this environment, so AI chat requires a
  release built with the engine artifacts present.
- See [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for details.

## [0.1.0] - 2025-01-01

### Added
- Initial release.
