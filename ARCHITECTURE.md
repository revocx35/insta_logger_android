# Architecture — insta_logger for Android

## Components
- **core/** (Kotlin/JVM, no Android): `Models.kt` (DTOs mirroring insta_logger's `shared/api.d.ts`), `ApiClient`
  (OkHttp; JSON; `Origin` + `X-CSRF-Token` like the browser; no redirects followed), `EventStream` (SSE `/api/events`),
  `IgText` (links + @usernames, port of the web's `igText.tsx`), `Format` (times/sizes), `vnc/` (RFB client for the
  server's VNC bridge `/api/vnc`), `Authentik` (authentik forward-auth support; same file as in wa_logger, web-ip-phone
  and RWR, design + setup in `docs/authentik.md`).
- **app/** (Compose): `AppRoot` routes server → signup → login/recover → recovery key → Instagram login → main;
  `MainShell` tabs: Chats, Reels, Stories, Unsent, Search, Instagram (VNC), Settings. `data/` holds `AppGraph` (OkHttp with
  pinned trust), `SecureCookieJar` (session cookie encrypted with an Android Keystore AES-GCM key), `ServerTrust`
  (system CAs + per-server trusted certs/fingerprints), `AppController` (state + events while in foreground),
  `AuthentikSignIn` (Custom Tab login, `AuthentikRedirectActivity` for `<applicationId>:/authentik`, Keystore-encrypted
  token store; UI in `ui/screens/AuthentikScreens.kt`).
  `media/` plays video/voice (Media3 over the authenticated OkHttp client), views and saves media.

## Data flow
Phone ⇄ insta_logger server only. The server talks to Instagram through its own browser; the app never contacts
Instagram or its CDN. Media URLs are server-relative (`/api/media/<id>`, `/api/reels/media/<ref>`) and fetched with
the session; nothing is written to disk except files the owner saves.

## Reels
`ReelsScreen`: a `VerticalPager` over `GET /api/reels` pages; one shared ExoPlayer plays the current page (posters
for the rest; the next 3 reels are warmed with a `Range: bytes=0-1` request so the server caches them); like/unlike `POST /api/reels/:id/like`, comments `GET/POST /api/reels/:id/comments` (`replyTo` for
replies); "Send" posts the reel link (`https://www.instagram.com/reel/<code>/`) into chosen chats via the normal
send route.

## Stories
`StoriesScreen`: `GET /api/stories/status|authors` and `GET /api/stories?user=&before=` (newest first); account rings
filter the `LazyVerticalGrid`; the full-screen viewer plays videos with one ExoPlayer and moves on when one ends.
*Check now* = `POST /api/stories/check`. Story settings (on/off, retention days, check interval, everyone / chosen
accounts with a picker from `GET /api/stories/accounts`) live in Settings → Logging. The server does the logging;
the app only shows what it saved.

## User management (0.5.0)

The client mirrors the server's `/api/admin/*` (admin only): `ApiClient.adminOverview`, `adminCreateUser`,
`adminSetUserPort`, `adminSetUserDisabled`, `adminResetUser` (recovery/wipe), `adminDeleteUser`, `adminPutPort`,
`adminDeletePort`, `adminAudit`. Bodies are built with `buildJsonObject` because the server expects explicit `null` for
"default port" / "no fixed origin", which `ApiJson` would drop. The admin's password (and 2FA code, when on) is asked
again for reset and delete.

`AdminScreen` shows names, status, the user's Instagram *state*, ports and the admin log — never content. The temporary
password is displayed once, with the address of the port the user signs in on. `AppState.role` (server) decides whether
the tab exists; the server enforces the role regardless.

The claim flow lives in `AuthScreens.LoginScreen`: `LoginResponse.mustChangePassword` → choose-own-password form →
`claim()` → recovery key shown (`SetupRecoveryScreen`) → normal app.

## authentik (0.4.0)
A server behind authentik forward auth answers with authentik's sign-in redirect; the interceptors in `core/Authentik.kt`
turn that into "Sign in with authentik" (OAuth 2 + PKCE in a Custom Tab, public client `instalogger-android`), then send
`Authorization: Bearer` (refreshed automatically; refresh token Keystore-encrypted, revoked when the server is forgotten).
The outpost validates it by token introspection. See `docs/authentik.md`.

## Threat model (client side)
Lost/stolen phone: the cookie is Keystore-encrypted, screenshots blocked by default, no disk media cache; server
session timeouts apply. Hostile message content: rendered as text only, only http(s) links. Network attacker: TLS
with system CAs or explicitly trusted server certs only (no user CAs), hostname verification on; plain HTTP only on
local addresses or after confirmation. Malicious server: the app trusts its own server by design.

## Platform constraints
minSdk 26, target 37 (local network permission for private addresses on Android 17+). No push notifications
(foreground event stream only). Voice recording needs Android 10+ (Ogg/Opus).
