# LockalTime

Android app (Kotlin + Jetpack Compose) that blocks selected apps during a focus **session**.

## Run

Open the project in Android Studio, or from the command line:

```
./gradlew :app:installDebug
./gradlew testDebugUnitTest
```

Requires JDK 17 and Android SDK 36. On first launch, tap **Open settings** and enable
**LockalTime app blocker** under Accessibility.

> On Android 13+, if the APK was sideloaded (not installed via Android Studio/adb or a store),
> the toggle is greyed out. Go to *App info → ⋮ → Allow restricted settings* first.

## How blocking works

`AppBlockerService` is an `AccessibilityService` that only listens for window-state changes,
so it learns which app comes to the foreground without reading screen content. If that app is
in the active session, it opens `BlockedActivity` on top. Leaving that screen (the button or
Back) always goes to the home screen.

Sessions can be timed. A timed session is over once the clock passes its `endsAtMillis`, and
`BlockPolicy` checks this on every event, so blocking stops on time even if nothing has cleared
the stored session yet. Clearing it (`SessionManager.endIfExpired`) is just cleanup, done by the
service and the home screen. Known limitation: this uses the wall clock, so changing the system
time ends a session early. That's acceptable while stopping early is allowed anyway.

## Architecture

Multi-module, following Google's architecture guidance as implemented in
[Now in Android](https://github.com/android/nowinandroid). Dependencies are wired with Hilt.

```
app/                  Application, MainActivity, nav graph that connects the features
blocking/             Accessibility service, block screen, BlockingServiceMonitor impl
feature/home/         api: HomeRoute        impl: screen, ViewModel, UI state
feature/editor/       api: SessionEditorRoute + navigateToSessionEditor()
                      impl: screen, ViewModel, UI state
core/model/           Pure Kotlin domain models (no Android, no serialization)
core/domain/          SessionManager, BlockPolicy
core/invite/          Pure Kotlin: the QR invite format (wire model + InviteCodec)
core/data/            Repository interfaces + implementations, BlockingServiceMonitor
core/datastore/       On-disk JSON schema, serializers, DataStore instances
core/common/          Clock, coroutine dispatchers, application scope
core/designsystem/    Theme and icons
core/ui/              Shared composables (AppIcon), preview annotations
core/testing/         Test doubles for repositories, clock, and Dispatchers.Main
build-logic/          Convention plugins shared by all module build files
```

Feature modules never depend on each other. `app` wires their navigation together. Each screen
has a stateful entry point that collects the ViewModel's `StateFlow`, and a stateless overload
that takes a `UiState` plus an `onAction` callback (used by the `@ThemePreviews`).

The stored JSON lives in `core:datastore` as separate `*Entity` classes. Their property names are
the on-disk keys, so the domain models can change freely. `JsonSerializersTest` checks that files
written by earlier versions still load.

Tests use hand-written test doubles from `core:testing` instead of a mocking library.

The main design decision is the split between a **`BlockSession`** (a saved configuration)
and the **`ActiveSession`** (a snapshot of what is being enforced right now):

```
 local "Start" ─┐
                ├─► SessionManager ─► ActiveSessionRepository ─► AppBlockerService
 QR join ───────┘
```

The blocking service only observes `ActiveSession`, so it doesn't care where a session came
from. This is what makes new ways of entering a session additive.

### QR join

A running session can be shared: **Share** on the active session shows a QR code, and another
phone taps **Join** and scans it to block the same apps until the same end time.

- **It's a copy, not a live link.** `SessionManager.join(invite)` writes a new `ActiveSession` on
  the joining phone that starts now and ends at the host's absolute `endsAtMillis`. Stopping on
  either phone doesn't affect the other. Apps the joiner doesn't have are listed in the invite but
  have no effect.
- **Format** (`core:invite`): `lockaltime://join?d=` + base64url(zlib(JSON)), with short keys such
  as `{"v":1,"id":…,"n":…,"p":[…],"e":…}`. The wire model is separate from the domain
  `SessionInvite`, like `core:datastore`'s entities. Unknown keys are ignored, and a higher `v`
  shows "update the app" rather than "not a valid code". `InviteCodecTest` pins the v1 format.
  It's a URI so a deep link from the system camera can be added later without changing it.
- **Scanning** is in-app: a CameraX preview whose frames are decoded by zxing-core
  (`InviteScanner.kt`), so it works without Google Play services. It asks for the camera
  permission on the first Join. **QR drawing** also uses zxing-core.

### Next steps

- **Shared/live sessions** (host stops → everyone unblocks): back `ActiveSessionRepository`
  with a remote source (e.g. Firestore or a WebSocket) and bind it in `DataModule`. The UI and
  service stay unchanged.
- **Richer rules** (schedules, allow-lists): extend `BlockPolicy`, which is covered by unit tests.

### Things to decide before the multi-device features

- **Cross-platform app identity.** Sessions store Android package names. An iPhone joining
  the session can't use these. iOS also has no equivalent API: it needs a separate Swift app
  using the Screen Time / FamilyControls APIs, where the user picks apps themselves through an
  opaque system picker. A cross-platform shared session will probably need to share *categories*
  or a mapping table, not raw app IDs.
- **Play Store policy.** Google restricts use of the Accessibility API. Publishing will need a
  prominent disclosure and a policy declaration. A `UsageStatsManager` + overlay implementation
  could replace the service behind the same `ActiveSession` contract if needed.
