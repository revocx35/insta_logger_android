# Architecture — insta_logger for Android

## Components
- **core/** (Kotlin/JVM, no Android): `Models.kt` (DTOs mirroring insta_logger's `shared/api.d.ts`), `ApiClient`
  (OkHttp; JSON; `Origin` + `X-CSRF-Token` like the browser; no redirects followed), `EventStream` (SSE `/api/events`),
  `IgText` (links + @usernames, port of the web's `igText.tsx`), `Format` (times/sizes), `vnc/` (RFB client for the
  server's VNC bridge `/api/vnc`).
- **app/** (Compose): `AppRoot` routes server → signup → login/recover → recovery key → Instagram login → main;
  `MainShell` tabs: Chats, Reels, Unsent, Search, Instagram (VNC), Settings. `data/` holds `AppGraph` (OkHttp with
  pinned trust), `SecureCookieJar` (session cookie encrypted with an Android Keystore AES-GCM key), `ServerTrust`
  (system CAs + per-server trusted certs/fingerprints), `AppController` (state + events while in foreground).
  `media/` plays video/voice (Media3 over the authenticated OkHttp client), views and saves media.

## Data flow
Phone ⇄ insta_logger server only. The server talks to Instagram through its own browser; the app never contacts
Instagram or its CDN. Media URLs are server-relative (`/api/media/<id>`, `/api/reels/media/<ref>`) and fetched with
the session; nothing is written to disk except files the owner saves.

## Reels
`ReelsScreen`: a `VerticalPager` over `GET /api/reels` pages; one shared ExoPlayer plays the current page (posters
for the rest); like/unlike `POST /api/reels/:id/like`, comments `GET/POST /api/reels/:id/comments` (`replyTo` for
replies); "Send" posts the reel link (`https://www.instagram.com/reel/<code>/`) into chosen chats via the normal
send route.

## Threat model (client side)
Lost/stolen phone: the cookie is Keystore-encrypted, screenshots blocked by default, no disk media cache; server
session timeouts apply. Hostile message content: rendered as text only, only http(s) links. Network attacker: TLS
with system CAs or explicitly trusted server certs only (no user CAs), hostname verification on; plain HTTP only on
local addresses or after confirmation. Malicious server: the app trusts its own server by design.

## Platform constraints
minSdk 26, target 37 (local network permission for private addresses on Android 17+). No push notifications
(foreground event stream only). Voice recording needs Android 10+ (Ogg/Opus).
