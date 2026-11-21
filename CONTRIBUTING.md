# Contributing to MovieTracker

Thanks for your interest in improving the app! This project is a university course
team project, so the workflow below is what the team actually uses.

## Getting set up

1. Install Android Studio and sync the project (see the README for requirements).
2. Build and run once before making changes: `./gradlew assembleDebug`.
3. Optional: add your own TMDB v3 key to `local.properties` to test online lookup.

## Branch and pull request workflow

- The default branch is `dev`. All pull requests target `dev`; `main` only receives
  reviewed release merges.
- Never push directly to `dev` or `main`; both are protected.
- Start every branch from the latest `dev`:
  ```bash
  git checkout dev && git pull
  git checkout -b feature/<area>-<short-name>    # e.g. feature/library-search
  ```
- One change per PR. Keep PRs small and focused.
- PR title: `<area>: <what changed>`, e.g. `G7: add offline search hint`.
- A PR merges only when CI (the Gradle build workflow) is green **and** one teammate
  has approved it. The PR author performs the merge.

## Code conventions

- **Java 11 only.** No Kotlin sources, no Jetpack Compose. UI is XML layouts +
  Material Components on a Material 3 theme.
- **MVVM.** Activities and Fragments only display and receive input; data flows
  through a per-feature `ViewModel` into the shared `Repository`. Never touch DAOs
  or the database directly from UI code, and never run I/O on the main thread.
- **Package layout:** `ui/<feature>/`, `data/db/`, `data/repository/`, `util/`
  under `hk.hkucs.group_6_android_app_dev`.
- **English-only UI.** Every user-visible string lives in `res/values/strings.xml`
  in English — no hardcoded text in layouts or Java, no other locales. Format dates
  and numbers with `Locale.US`.
- Use theme attributes and `colors.xml` / `dimens.xml` resources; no hardcoded
  colors or sizes in layouts.
- Class names are `UpperCamelCase`; resource names are `lowercase_with_underscores`.
- Do not add third-party dependencies or change version catalog entries without
  discussing it with the maintainers first.

## Shared files

These files are maintained centrally — database entities and DAOs (`data/db/`),
`AndroidManifest.xml`, navigation structure, `MainActivity`, theme files, and all
Gradle build scripts. Changes to them need explicit maintainer approval in the PR.

## Privacy and content rules

- Never commit real server addresses, accounts, tokens or API keys. The TMDB key
  belongs in `local.properties` only.
- Movie/show metadata (titles, cast, posters) may only come from TMDB, and only via
  the sample-data generation script or the in-app lookup. Do not manually copy
  content from other sites.
- Wherever TMDB data is shown, keep the attribution visible (About dialog, search
  screen footer).

## Before opening a PR

1. The project compiles and runs on an emulator or device.
2. Test your change end to end and attach screenshots for UI changes.
3. Fill in the PR template.
