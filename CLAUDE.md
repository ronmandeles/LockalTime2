# LockalTime

Android app (Kotlin, Jetpack Compose, Hilt) that blocks chosen apps during a focus session.
README.md explains how blocking and QR join work. This file defines the engineering baselines.

## Commands

```
./gradlew :app:installDebug                      # build and install (JDK 17, Android SDK 36)
./gradlew testDebugUnitTest                      # all unit tests, including pure-Kotlin modules
./gradlew :feature:home:impl:testDebugUnitTest   # one module
```

Run the unit tests before reporting a change as done.

## Skills and precedence

Load skills before reading a diff, for reviews as well as edits.

- Always, for any Kotlin or Android change: `claude-android-skill` (global), the structural
  baseline (module layout, Screen/ViewModel/repository shape, test doubles). Ignore its version
  table and `gradle-setup.md`: this project is on Kotlin 2.x with the Compose compiler Gradle
  plugin.
- `android-skills:*` (plugin) has a skill for every kind of Android task. Load every one whose
  topic the change touches, before reading the diff; missing a relevant one is the failure to
  avoid. The usual matches:
  - a file with `@Composable`: `android-skills:compose`
  - `stateIn`, `combine` or a `MutableStateFlow` in a ViewModel: `android-skills:kotlin-flows`
  - dispatchers, scopes, cancellation, executor bridges: `android-skills:kotlin-coroutines`
  - tests: `android-skills:android-testing`. Test-first and the two-schedulers trap
    (`MainDispatcherRule` vs `runTest`) apply here; its Compose UI, KMP and screenshot sections
    do not.
  - repositories: `android-data-layer`; DataStore: `datastore`; network: `android-retrofit`;
    platform boundaries: `kmp-boundaries`; visibility questions: `modularization`; convention
    plugins: `android-gradle-logic`; build speed: `gradle-build-performance`; Logcat, ANRs,
    crashes: `android-debugging`.
- Not by default: `android-skills:android-dev`. Its house defaults are already in this file and
  its greenfield UI convention is the one rejected below.
- Where a skill disagrees with this file, this file wins: no effects `Channel`, no `Actions`
  interface, no four-bucket UiState. See "State and screens" below.

## Module rules

- `app` depends on the feature api/impl modules, `blocking` and `core:designsystem`, and wires
  navigation. Features never depend on each other and never on `:blocking`; they reach the
  blocker only through the `BlockingServiceMonitor` interface in `core:data`.
- `feature:x:api` holds only `@Serializable` routes and `NavController` extensions.
  `feature:x:impl` holds Screen, ViewModel, UiState and the `NavGraphBuilder` extension.
- `core:model` and `core:invite` are pure Kotlin (JVM library plugin). No Android imports.
- `core:data` holds repository interfaces and `internal` implementations, bound with `@Binds`
  in `DataModule`. Swap implementations there, never at call sites.
- `core:domain` holds `SessionManager`, the only way to start, join or stop a session, and
  `BlockPolicy`. `AppBlockerService` depends on `SessionManager` alone (active session, clock,
  expiry cleanup) and never reads `SessionRepository`.
- New code declares the lowest visibility that compiles: start `private`, widen to `internal`
  only for another file in the same module, to public only for a real consumer in another
  module. Repository implementations and screens are `internal`. UiState and Action types are
  public today and may be narrowed.

## Three model layers, never merged

| Layer | Where | Rule |
|---|---|---|
| Domain | `core:model` | Free to change. |
| Storage entities | `core:datastore/StoredData.kt` | Property names are on-disk keys. Any change gets a `JsonSerializersTest` case. |
| Invite wire | `core:invite/InviteWire.kt` | Short keys, versioned. Bump `v` for breaking changes and add an `InviteCodecTest` case. |

When a network layer is added it is a fourth: DTOs in `core:network`, mapped at the repository.

## Storage

- Typed DataStore with kotlinx.serialization JSON for small config (saved sessions, active
  session). Keep `ignoreUnknownKeys`, `CorruptionException` in `readFrom`, and
  `ReplaceFileCorruptionHandler`.
- Anything that grows (history, block events, sync queue) goes to Room in a new `core:database`,
  not DataStore.

## State and screens

- UiState is a sealed interface with `Loading` and one content state (`Success` on Home,
  `Editing` on the editor), exposed as `StateFlow<XUiState>`.
- Derived state combines repository flows with `stateIn(viewModelScope, WhileSubscribed(5_000),
  Loading)` (Home). Form state is a private `MutableStateFlow` changed with `update {}` (editor).
- Dialogs are state: a `dialog: XDialog?` field on the content state. No one-shot effect
  channels. Navigation after an action is also state (`isSaved` plus `LaunchedEffect`).
- Input is `onAction(XAction)` with a sealed `XAction`, dispatched by one `when` in the ViewModel.
- Two overloads per screen. The stateful one takes `hiltViewModel()`, collects with
  `collectAsStateWithLifecycle()`, and owns Android calls (intents, permission launchers).
  The stateless one takes `uiState` plus lambdas and is what `@ThemePreviews` render.
- Parameterised ViewModels use `@HiltViewModel(assistedFactory = ...)` with `@AssistedInject`.

## Compose

- Material 3 only (`androidx.compose.material3`). Versions come from the Compose BOM; never pin
  a Compose artifact version.
- Icons come from `material-icons-core` through `LockalTimeIcons`. Do not add
  `material-icons-extended`.
- Theme is `LockalTimeTheme`. Previews wrap in `LockalTimeTheme(dynamicColor = false)`.
- Composables that lay out content take `modifier: Modifier = Modifier` as the first optional
  parameter and apply it to their root. Dialogs and stateful screen entries take none.
- Children below the stateful entry receive state and lambdas, never the ViewModel. Screen
  pieces (cards, rows, dialogs) are `private` in the screen file, or `internal` in their own
  file when they are large (`InviteScanner.kt`, `QrCode.kt`).
- `LazyColumn` items always have a `key`.
- User-facing text goes through `stringResource` / `pluralStringResource` from the module's
  `strings.xml`. No literals in `Text(...)` outside previews.
- Side effects: `LaunchedEffect` keyed on state for navigation, `DisposableEffect` for
  lifecycle-bound resources (camera), `produceState` for async loads, `rememberUpdatedState`
  for callbacks captured by long-lived effects. `AndroidView` only where Compose has no
  equivalent (`PreviewView`).
- Activities call `enableEdgeToEdge()`; `Scaffold` padding is applied to the content.
- `@OptIn(ExperimentalMaterial3Api::class)` is accepted for `TopAppBar`.

## Coroutines and time

- Dispatchers are injected through `@Dispatcher(IO)` / `@Dispatcher(Default)` and
  `@ApplicationScope`. Direct `Dispatchers.*` use is limited to `CommonModule`, the
  `AppBlockerService` scope (`Main.immediate`) and `AppIcon` (a composable with no injection).
- Time comes from the injected `Clock`. `System.currentTimeMillis()` appears only in
  `CommonModule`.

## Testing

- Hand-written doubles in `core:testing` (`TestSessionRepository`, `TestClock`, ...).
  No Mockito or MockK.
- ViewModel tests use `MainDispatcherRule`. For `stateIn` ViewModels, collect `uiState` in
  `backgroundScope` before asserting. `MutableStateFlow` ViewModels can read `.value` directly.
- Every module with logic has a `src/test`. `blocking` is the known gap.

## Style

- Kotlin official style, trailing commas in multi-line parameter lists.
- Comments explain why, not what, in the voice of the existing files. A comment the reader could
  infer from the code beside it is deleted.
- Widen an existing mechanism (a dialog field, an action, a repository method) before adding a
  parallel one beside it.
