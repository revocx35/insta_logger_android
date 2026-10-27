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

## Multiple accounts (0.6.0)

`Prefs.accounts` is the list of accounts (id, server URL, HTTP choice, user name), `Prefs.activeAccountId` the one
being shown; `AppController` owns the switch:

- `open(account)` closes the current `ServerSession` and builds the next one; `ServerSession` gets its own
  `AccountClient` (`AppGraph.accountClient(id)` = encrypted cookie jar + `Authentik` instance + OkHttp client per
  account), so a switch never carries a cookie or bearer token across servers. Clients are cached, so switching back is
  instant and still signed in.
- `beginAddAccount()` sets an "adding" flag and a pending id; `ServerScreen` shows on top of the open account and probes
  with that id's client (`pendingAccountId()`), so an authentik sign-in during the check is stored for the account that
  is about to exist. `connect()` switches to a server already in the list, otherwise saves the account and opens it;
  `cancelAddAccount()` deletes the pending secrets.
- `removeAccount()` signs out on the server (best effort), deletes the cookie and authentik files, drops the entry, and
  opens the next account — or the server screen when none is left. `sweepOrphanSecrets()` removes files of accounts
  that no longer exist at startup.
- Trusted certificates moved from one global set to per-host keys (`certs:<host>`); `ServerTrust.serverHost` reloads
  the anchors of the account (or server being added) whenever it changes.
- The UI is `ui/screens/AccountsScreen.kt`: `AccountSwitcherDialog` (opened from Settings → Account, More →
  Switch account, and after adding a server) and `AccountCard` for the Settings header. `MainShell` keys the whole shell on the
  session id so tab state and view models never leak between accounts.
- Migration: the single saved `server_url`/`allow_http`/`trusted_certs` of 0.5.0 become `Account.MAIN`, which keeps the
  old secret file names; `accounts_ready` makes it run once.

## Look (0.7.0)

Styled after Instagram's app, matching the server's web UI (its `styles.css` tokens are mirrored in `theme/Theme.kt`):
white/black surfaces, `#efefef`/`#262626` secondary, `#0095f6` primary, `#ff3040` likes, 14 sp base type, flat 8 dp
buttons, outlined chips. `WaIcons` are the web's outline icons (`Icon.tsx`) built as stroked `ImageVector`s. The
wordmark uses Grand Hotel (`res/font`, SIL OFL in `third_party/`).

- `MainShell`: phone tab bar with icons only (Messages, Search, Reels, Stories, More; black on Reels; red dot when
  Instagram is not connected) and a `ModalBottomSheet` "More" (other pages, switch account, log out); the tablet rail
  is the narrow icon sidebar with More at the bottom.
- Chats: the account's Instagram username as title, "Messages · Requests", chips, 56 dp avatars, "preview · 2h"
  (`formatAgo`). Bubbles: 22 dp, runs joined with 4 dp corners (`joinTop`/`joinBottom`), own bubbles paint a slice
  of one screen-tall purple→blue gradient (position read in the draw phase only), bare photos/videos; pill composer
  with a text "Send".
- Reels: "Reels" header, filled red heart, double-tap heart pop, audio thumbnail. Stories: gradient-ring tray with
  counts, archive grid with date boxes, viewer with progress bars, tap zones (back / pause / next), photos 5 s.

## Threat model (client side)
Lost/stolen phone: the cookie is Keystore-encrypted, screenshots blocked by default, no disk media cache; server
session timeouts apply. Hostile message content: rendered as text only, only http(s) links. Network attacker: TLS
with system CAs or explicitly trusted server certs only (no user CAs), hostname verification on; plain HTTP only on
local addresses or after confirmation. Malicious server: the app trusts its own server by design.

## Platform constraints
minSdk 26, target 37 (local network permission for private addresses on Android 17+). No push notifications
(foreground event stream only). Voice recording needs Android 10+ (Ogg/Opus).
