# AGENTS.md — notes for AI coding agents

insta_logger for Android is a native Kotlin/Compose client of the self-hosted insta_logger server
(github.com/revocx35/insta_logger). It shows the server's encrypted DM log (unsent messages kept), sends through the
server, plays Reels and shows logged Stories. **The phone never talks to Instagram** — only to the owner's server.

Read first: [ARCHITECTURE.md](ARCHITECTURE.md) (components, data flow, threat model), [README.md](README.md) (features,
install, build, tests), [CLAUDE.md](CLAUDE.md) (the same rules for Claude sessions), and the server's
`shared/api.d.ts` + ARCHITECTURE.md §8 (the API this app mirrors).

## Layout

| Path | What |
|---|---|
| `core/` (plain Kotlin/JVM) | `Models.kt` (mirror of the server's `shared/api.d.ts`), `ApiClient` (Origin + CSRF like a browser, no redirects), `EventStream` (SSE), `IgText` (links/@usernames), `Format`, `vnc/` (RFB client) |
| `app/.../ui/` | `AppRoot` (screen routing), `MainShell` (tabs), `screens/` (Chats, Reels, Stories, Unsent, Search, Instagram, Settings, auth), `components/` (bubbles, media, inputs), `theme/` (web CSS tokens, icons) |
| `app/.../data/` | `AppGraph`, `AppController`, `SecureCookieJar` (Keystore-encrypted session cookie), `ServerTrust` (TLS trust), `ServerSender` |
| `app/.../media/` | ExoPlayer setup, media viewer, save/open, voice recorder, gallery auto-save |
| `app/src/test/` | Robolectric screenshot/unit tests with `SampleData` |

## Commands

```bash
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug   # JDK 21, Android SDK 37
./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true                     # screenshots → app/build/screenshots/
IGL_IT_BASE=… IGL_IT_USER=… IGL_IT_PASSWORD=… IGL_IT_CA=root.crt ./gradlew :core:test --tests '*LiveServerTest*'
```
Releases are signed by hand (key outside the repo, never in CI): bump `versionCode`/`versionName`, build
`:app:assembleRelease` with `keystore.properties`, `gh release create vX.Y.Z <apk>`.

## Conventions

- Server API change → update `core/.../Models.kt` and `ApiClient` with it; unknown enum values must fall back (`ApiJson`).
- New screen → a Robolectric screenshot test where it can run without a live session; check light/dark and phone width.
- Bottom bar has 7 tabs: keep labels short; anything more goes into a screen, not the bar.
- Update README, ARCHITECTURE.md and this file with every feature; commits end with the `Co-Authored-By` trailer;
  run gitleaks before pushing (no LAN addresses, home paths or keys in the repo).

## Never

- Contact Instagram or its CDN directly, or add analytics/third-party SDKs.
- Log message content, names, cookies or tokens; render message text as markup; cache media on disk.
- Trust user-installed CAs or turn off hostname verification; commit keystores or `keystore.properties`.

## Gotchas

- Many UI-kit names start with `Wa` (`Wa.colors`, `WaIcons`, `WaButton`, `WaTextView`): inherited from the wa_logger
  client, they are just the component names.
- Compose trailing lambdas bind to the last parameter: keep `onClick` last in small composables.
- Own bubbles are white text on blue (`bubbleOutText`): anything inside a bubble must use the bubble's colors.
- media3 APIs marked `UnstableApi` need `@androidx.annotation.OptIn(UnstableApi::class)` or lint fails.
