# Gun Stairs

A small 2D arcade game for Android, built with Kotlin and Jetpack Compose.
Aim a gun barrel that swings back and forth, tap to shoot the enemy standing
one stair above you, and climb as high as you can before you miss.

## Gameplay

- **Scene**: a staircase rising from the bottom of the screen. You stand on
  the current stair; an enemy stands one stair up, aiming at you.
- **Controls**: tap anywhere to shoot. That's it.
- **Aiming**: your gun's barrel swings continuously between a low and a high
  angle. Fire while it's aimed close enough to the enemy and you hit; fire
  while it's off and you miss.
- **Hit**: the enemy falls, you move up one stair, your score goes up by one,
  and a new enemy appears one stair above you.
- **Miss**: the enemy shoots back — game over.
- **Difficulty**: every stair climbed swings the barrel a little faster and
  narrows the angle you have to hit, ramping up over the first ~18 stairs
  until it caps out at its hardest.
- **Score**: your current run's stair count, plus a persisted best score
  shown on the menu and the game-over screen.

## Screens

Start/menu (title, best score, Start, Settings) → Game (score in the corner,
tap to shoot) → Game Over (score, best score, Restart, back to menu). Settings
is reachable from the menu and covers sound on/off and resetting the best
score.

## Architecture

- **`:domain`** — a plain Kotlin Gradle module with **no `android.*`
  dependency at all** (enforced by the module having no Android plugin
  applied, not just by convention). It owns the actual game rules:
  - `BarrelOscillator` — the barrel's angle as a triangle-wave sweep over time.
  - `DifficultyProgression` — how oscillation speed and hit tolerance ramp
    with stairs climbed.
  - `ShotResolver` — hit/miss decision from barrel angle vs. the ideal aim
    angle.
  - `GameEngine` / `GameState` — drives one round (start, advance time,
    shoot, return to menu) and tracks stair/score/best score.

  This is what's unit tested (JUnit, no Android dependencies needed to run
  it) — angle-over-time math, difficulty ramping, hit/miss boundaries, and
  the game engine's state transitions.

- **`:app`** — Compose UI and Android glue:
  - `ui/` — `GameViewModel` (state holder; owns a `GameEngine` and drives it
    from per-frame ticks via Compose's `withFrameNanos`) plus one composable
    per screen. The game screen itself is a `Canvas` — the staircase,
    stick-figure player/enemy, rotating gun barrel, and muzzle-flash/miss
    feedback are all drawn there rather than pulled from a game engine
    library, since a few `drawLine`/`drawCircle`/`rotate` calls are all this
    scene needs.
  - `data/` — `BestScoreRepository` and `SettingsRepository`, both backed by
    **Jetpack DataStore** and hidden behind plain Kotlin interfaces so
    nothing else in the app knows DataStore exists. **Room is deliberately
    not used**: the app persists exactly one integer (best score) and one
    boolean (sound on/off), and a database plus its schema/migration
    machinery would be pure overhead for that.

Compose only renders state and forwards input (taps, frame ticks); all game
logic lives in `GameViewModel` and `:domain`.

## Testing

- **Unit tests** (`:domain`, plain JUnit) cover the angle-over-time math,
  difficulty progression, hit/miss boundary conditions, and the game
  engine's state machine (start / shoot / miss / return to menu / best-score
  tracking).
- **Compose UI test** (`:app`, `src/testDebug`, Robolectric) exercises the
  critical path through the real UI: Start → shoot (hit) → shoot (miss) →
  Game Over → Restart. It runs as a JVM unit test rather than an
  instrumented `androidTest`, because CI has no Android emulator — see
  `.github/workflows/build.yml`, which only ever runs `./gradlew test`, not
  `connectedAndroidTest`. It lives in the debug-only `testDebug` source set
  (rather than the shared `test`) because Robolectric's Compose host activity
  comes from `ui-test-manifest`, a `debugImplementation` dependency whose
  manifest only merges into the debug variant. The barrel's angle is a pure
  function of elapsed time, so the test drives it deterministically through
  `GameViewModel.onFrame(...)` (the same entry point the real per-frame loop
  uses) instead of racing Compose's frame clock for an exact hit.

Run everything with:

```
./gradlew assembleDebug assembleRelease test lint
```

## Build & signing

- `applicationId` is fixed at `cz.novotny.gunstairs` and is not expected to
  change (changing it would make the system treat it as a different app,
  breaking existing installs).
- The repo is **public**, so the release keystore is never committed.
  Instead, CI decodes it from repository secrets
  (`GUNSTAIRS_KEYSTORE_BASE64`, `GUNSTAIRS_KEYSTORE_PASSWORD`,
  `GUNSTAIRS_KEY_ALIAS`, `GUNSTAIRS_KEY_PASSWORD`) at build time. Without
  those secrets set, `assembleRelease` still works by falling back to the
  debug keystore, so the build never breaks — release builds simply aren't
  reproducibly signed until the secrets are configured.
- `versionCode` is bumped on every installable release; `versionName`
  follows semver.

## CI

`.github/workflows/build.yml` runs on every push: JDK 17, Gradle
dependency caching, `assembleDebug`, `assembleRelease`, `test`, and `lint`.
Both `app-debug.apk` and `app-release.apk` are uploaded as workflow
artifacts, downloadable from the run's summary page.

This project was developed inside a sandbox without access to Google's
Maven repository (`dl.google.com`), so the Android Gradle Plugin and
AndroidX dependencies can't resolve locally — the domain module's plain-JUnit
tests were verified locally against Maven Central only, and everything
involving the Android Gradle Plugin was verified through this CI workflow.

## Scope

- No runtime permissions are requested — the game doesn't touch location,
  sensors, storage, network, or any other permission-gated API.
- Dark theme only, by deliberate choice: a night shooting-range staircase
  suits the game better than a light variant, and a small arcade game
  doesn't need effort split across two palettes.
- Sound is a toggle in Settings and persists via DataStore, but no audio
  assets are bundled yet — the hook is wired up so a missing asset can never
  break the build, and effects can be dropped in later without touching the
  settings plumbing.
- Gameplay is intentionally abstract and stylized (stick figures, no
  realistic violence, no sensitive capabilities like location or tracking).
