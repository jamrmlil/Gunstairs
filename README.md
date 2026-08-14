# Gun Stairs

A small 2D arcade game for Android: aim a swinging gun barrel and shoot the
enemy standing one stair above you before it shoots first.

> Work in progress — this README is being filled in incrementally as the
> game is built. See commit history / CI for current status.

## Status

Project scaffold: Gradle wrapper, `:app` (Compose UI) and `:domain` (pure
Kotlin game logic) modules, CI workflow. Gameplay, persistence, and tests are
being added in follow-up commits.

## Build

```
./gradlew assembleDebug assembleRelease test lint
```

Local sandboxes without access to `dl.google.com` cannot resolve the Android
Gradle Plugin / AndroidX artifacts; use GitHub Actions CI (`.github/workflows/build.yml`)
to build and verify.
