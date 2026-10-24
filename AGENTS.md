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
| `core/` (plain Kotlin/JVM) | `Models.kt` (mirror of the server's `shared/api.d.ts`), `ApiClient` (Origin + CSRF like a browser, no redirects), `EventStream` (SSE), `IgText` (links/@usernames), `Format`, `vnc/` (RFB client), `Authentik.kt` (authentik forward auth, `docs/authentik.md`) |
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
- `core/Authentik.kt` is the same file in wa_logger's client, web-ip-phone and RWR (only the package differs): change
  them together. authentik's introspection accepts the app's token for a proxy provider only if the app's OAuth2
  provider lists that proxy provider in `jwt_federation_providers` (not the other way round).
- media3 APIs marked `UnstableApi` need `@androidx.annotation.OptIn(UnstableApi::class)` or lint fails.

## Multi-user (0.5.0, matches the server's Admin UI)
- The admin (the first account) sees an extra **Users** tab: users, ports and the admin log (`AdminScreen.kt`,
  `ApiClient.admin*`). The phone rail keeps 7 items; admins open it from Settings.
- A sign-in with a temporary password returns `mustChangePassword`; the login screen then asks for a new password
  (`AuthActions.claim` → `POST /api/auth/claim`), plus the recovery key after an admin reset.
- `AppState.role` decides whether the Users tab exists. Never rely on that for security: the server checks the admin
  role itself and answers 401/403 to everyone else.

## Multiple accounts (0.6.0)
- `Prefs.accounts` is the list (id, server URL, allowHttp, user name, addedAt); `Prefs.activeAccountId` the open one.
  The single saved server of 0.5.0 migrates to `Account.MAIN` and keeps the old secret file names, so an upgrade
  never signs anyone out (`accounts_ready` makes the migration run once).
- **One jar and one authentik sign-in per account id**: `SecureCookieJar(context, id)` → `session-<id>.bin`,
  `KeystoreAuthentikStore(context, id)` → `authentik-<id>.bin`. `AppGraph.accountClient(id)` bundles them with the
  OkHttp client and caches it; `ServerSession` takes that client. Never put a cookie jar or an `Authentik` instance on
  `AppGraph` again — that is exactly what used to carry a session across servers.
- Trusted certificates are per **host** (`certs:<host>` in `wal_prefs`), and `ServerTrust.serverHost` reloads them, so
  trusting a self-signed certificate for one server does not widen another's.
- `AppController` owns the flows: `switchTo`, `beginAddAccount`/`cancelAddAccount` (pending id so an authentik sign-in
  during the probe is stored for the account about to exist), `connect` (dedupes by URL), `removeAccount` (server
  logout + delete secrets + open the next), `changeServer` (replaces a never-signed-in server, otherwise adds).
- The account menu (`ui/screens/AccountsScreen.kt`) is rendered by `MainShell` **and** `AppRoot`: the sign-in screens
  need it too, otherwise an account that is not signed in is a dead end. `AppRoot` wraps `MainShell` in `key(s.id)`.
- Adding a parameter after `onChangeServer` on `SignupScreen`/`LoginScreen` breaks trailing-lambda callers (the
  screenshot test passes `{}` last): keep `onAccounts` before `onChangeServer`.
- Tests: new Robolectric classes need `@Config(sdk = [35])` (with the default `compileSdk 37` Robolectric dies in
  `FileDescriptorInterceptor` before any test body), and one prefs directory is shared for the class, so clear it in
  `@Before`. `AccountsTest` covers migration, ordering, labels, file names and per-host certificates.
- Emulator check without a real server: `scripts/fake-logger-api.py` in the homelab skill answers `/api/state` as
  "not logged in" until `/api/auth/login` sets a cookie — switching away and back must keep that cookie.
