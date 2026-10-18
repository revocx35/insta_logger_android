# insta_logger for Android — notes for Claude sessions

Native client of the insta_logger server (github.com/revocx35/insta_logger). Read `AGENTS.md` and `ARCHITECTURE.md` first, and the
server's `shared/api.d.ts` (the contract `core/.../Models.kt` mirrors) and `ARCHITECTURE.md` §8 (routes).

## Rules
- Documentation is part of every change: keep `AGENTS.md`, `ARCHITECTURE.md` and `README.md` up to date in the same commit.
- The phone never talks to Instagram: everything goes through the server's API. Don't add direct Instagram calls.
- Security: never log message content, names, cookies or tokens; never render message text as markup; only http(s)
  links; media never on disk (memory cache only); the session cookie stays in `SecureCookieJar` (Keystore).
- Keep `Models.kt` in step with the server's `shared/api.d.ts` (unknown enum values fall back, see `ApiJson`).
- Never commit `keystore.properties` / keystores. The release key and its properties file live outside the repo (on
  the maintainer's build machine); copy the properties file in as `keystore.properties` for signed builds.
- Commits end with the Co-Authored-By trailer. Bump `versionCode` for every release.

## Commands
- `./gradlew --offline :core:test :app:testDebugUnitTest` (unit + screenshot render), `:app:assembleDebug`.
- Screenshots: `./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true` → `app/build/screenshots/`.
- Live API test against a server: see README "Tests" (`IGL_IT_*` env vars, `LiveServerTest`).

## Gotchas
- Derived from the wa_logger client: some internal names still start with `Wa` (theme `Wa.colors`, `WaIcons`,
  `WaButton`, `WaTextView`); they are just the UI kit's names.
- Compose trailing lambdas bind to the *last* parameter: keep `onClick` last in small composables.
- Own messages are white on blue (`bubbleOutText`); anything drawn inside a bubble must use the bubble's colors.
