# AGENTS.md — notes for AI coding agents

All agent notes (rules, commands, gotchas) are in [CLAUDE.md](CLAUDE.md); the design is in
[ARCHITECTURE.md](ARCHITECTURE.md). Quick start:

- Build/test: `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug` (JDK 21, Android SDK 37).
- Where things live: `core/` (API models + client, SSE, text parser, VNC; plain JVM), `app/` (Compose UI, data
  layer, media). The server contract is insta_logger's `shared/api.d.ts`.
- Don't touch: release signing files (never committed), the cookie/Keystore code without tests.
