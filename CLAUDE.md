# CLAUDE.md — Nat20 (Android)

Native Kotlin/Jetpack Compose port of the shipping iOS app **Nat20**, a D&D character
manager with pluggable rulesets. This file is the working contract for any agent (local or
Claude-on-web) building this port. Read it before each task.

## Sources of truth (in priority order)

1. **`../nat20-ios/README.md`** — the canonical feature spec. **26 shipped build-steps across
   three rulesets** (D&D 5e 2014, D&D 5e 2024, Pathfinder 2e Remaster) + a detailed "5e rules —
   coverage gaps" list. When unsure *what a mechanic should do*, this wins.
2. **`../nat20-ios` source** — the behavioural spec. The Swift `Packages/Domain` and
   `Packages/DnD5e` are the reference implementation. Read the actual Swift when porting logic.
3. **`README.md` (this repo)** — the execution backlog. Steps **A1–A17** with build order and
   "what we reuse vs. reimplement". Work the backlog top-to-bottom.

This is a **parallel native port, not a shared binary** (KMP was considered and deferred). iOS
stays Swift; Android reimplements the same domain + 5e engine idiomatically in Kotlin.

## Module layout & dependency rules

iOS uses a **shared-core + thin-version-package** split for its rulesets (`DnD5eCore` /
`DnD5e` / `DnD5e2024`; `PathfinderCore` / `Pathfinder`). **We mirror that seam from the start**
(decided up front — see README "Module layout"):

```
:domain               pure Kotlin/JVM, NO Android deps  — Ruleset/Character/Payload/Intent/Event + Campaign.apply
:ruleset-dnd5e-core   pure Kotlin/JVM, deps :domain     — edition-agnostic 5e machinery (abilities, modifier math,
                                                          proficiency-by-level, slot tables, effects)   (= DnD5eCore)
:ruleset-dnd5e-2014   pure Kotlin/JVM, deps core        — 2014 payload, catalogues, intents, level-up   (= DnD5e)
:ruleset-dnd5e-2024   pure Kotlin/JVM, deps core        — 2024 edition         (= DnD5e2024)   [created at its step]
:ruleset-pf2e-core    pure Kotlin/JVM, deps :domain     — PF2e maths (proficiency ladder, degrees)  [created at its step]
:ruleset-pf2e         pure Kotlin/JVM, deps pf2e-core   — PF2e payload/catalogues/flows             [created at its step]
:data                 Android library                   — Room entities + DAOs + CharacterRepository (the sync seam)
:app                  Android app (Compose)             — UI, ViewModels, navigation, entry point
```

Keep `:domain` and every `:ruleset-*` module free of Android imports so they stay
JVM-unit-testable (mirrors iOS, where the packages run `swift test` on the host). UI lives
**only** in `:app` (shared parchment chrome — iOS's `DnD5eUICore` — lives under an `:app`
`theme`/`codex` package; extract a `:ui-core` library only when a second ruleset's UI justifies
it). Package root is `au.com.evonet.nat20.*`. **Only `-core` + `-2014` exist now** — the 2024 /
PF2e modules are scaffolded when their steps land, not as empty modules ahead of content.

## Stack (decided — do not re-litigate)

- **Language/UI:** Kotlin 2.0 + Jetpack Compose (Material3, BOM-managed).
- **Persistence:** Room (SQLite), **local-only**, behind `CharacterRepository`. No CloudKit equivalent.
- **Serialization:** `kotlinx.serialization` for the payload/event JSON blobs.
- **State:** ViewModel + `StateFlow`. (iOS uses `@Observable`; there is no `ObservableObject`.)
- **SDK:** `minSdk = 26`, `target/compileSdk = 35`, JDK 17. *(Reconcile the README's "min SDK
  TBD / API 33+" line to 26 — the scaffold already pins 26.)*
- **On-device AI:** Gemini Nano (AICore / ML Kit GenAI), availability-gated. **Off by default**
  on unsupported devices — the affordance simply doesn't appear (one flag, reused everywhere).
- **Billing:** Google Play Billing (one free character + one-time IAP unlock). Step A13.

## iOS → Android translation map

| iOS | Android | Notes |
|---|---|---|
| SwiftData `@Model` (thin JSON envelopes, `payloadData: Data`) | Room entity + `kotlinx.serialization` blob | persistence is already envelope-style — clean map |
| Codecs `CharacterCodec` / `CampaignCodec` | same idea: domain ↔ Room row translators | keep the seam |
| `@Observable` store (`CharacterStore`) | ViewModel + `StateFlow` | `store.apply(intent)` → `Campaign.apply` → persist + journal |
| Intent structs (33 of them) | sealed-class hierarchy of intents | pure logic, voluminous, correctness-critical |
| `NavigationStack` + closure routing | Navigation Compose | sheet/picker enums → routes or bottom-sheet state |
| 6-tab paged `TabView` (CodexShell) | `HorizontalPager` + tab row | |
| StoreKit 2 (`PatronStore`) | Play Billing | A13 |
| Apple FoundationModels (`@Generable`) | Gemini Nano + JSON-schema prompting | gated; degrade gracefully |
| `ImageRenderer`→PDF export | `PdfDocument`/Canvas hand-built layout | no Compose equivalent to SwiftUI-view PDF |
| 15 JSON catalogues + 8 TTF fonts | copied verbatim (see below) | zero rework |

## Where the reused assets live

- **SRD JSON catalogues** → `ruleset-dnd5e-2014/src/main/resources/catalogues/` (subdir structure
  preserved from iOS: `Spells/`, `Classes/`, `Races/`, `Monsters/`, `Inventory/`, etc.). Load via
  classloader resource streams so the same code works in JVM tests and on Android.
- **Fonts** → `app/src/main/res/font/`, renamed to Android-safe names:
  `cinzel.ttf`, `cormorant_garamond[/_italic].ttf`, `eb_garamond[/_italic].ttf`,
  `im_fell_english_regular.ttf`, `im_fell_english_italic.ttf`, `im_fell_english_sc_regular.ttf`.
- **Licensing — carry attribution verbatim** (build a Credits screen, A9): SRD 5.1 +
  5e-bits/5e-database are **CC BY 4.0**; the five typefaces are **SIL OFL 1.1**. Tasha's /
  Xanathar's spell content is **not** openly licensed — mirror the iOS stance (no broad reuse).

## Key simplification vs iOS

iOS carries ~1200 lines of **tolerant `Codable`** with legacy-field migration branches
(years of shipped schema changes for existing user data). **Android is greenfield with no
users**, so port only the *current* payload schema — skip the legacy-migration branches. This
removes the single biggest porting risk. (Keep the schema versioned in Room so future
migrations are clean.)

## How to work the backlog

- One **A-step per branch/PR** (A1, A2, …). Keep changes scoped to that step.
- Follow the iOS build order — it was proven out shipping the iOS app abstraction-first.
- For each step: port the domain/logic first **with JUnit5 tests**, then the UI.
- Mirror the iOS test discipline: `:domain` and `:ruleset-dnd5e` get real unit-test coverage
  (modifier math, slot tables, each intent, full mini character builds).

## Definition of done (every step)

1. `./gradlew build` is green.
2. `./gradlew test` passes (JVM unit tests for `:domain` + `:ruleset-dnd5e`).
3. `./gradlew :app:assembleDebug` succeeds. *(Actual app-launch verification is local — the web sandbox has no emulator; see below.)*
4. New logic has tests. Behaviour matches the iOS reference (cross-check the Swift).
5. No Android imports leaked into `:domain` / `:ruleset-dnd5e`.

## Build & test commands

```bash
./gradlew build                 # full build
./gradlew test                  # JVM unit tests (domain + ruleset-dnd5e)
./gradlew :ruleset-dnd5e:test   # just the 5e engine tests
./gradlew :app:assembleDebug    # build the debug APK
./gradlew lint                  # Android lint
```

## Running on Claude Code on the web

Each web session clones only this repo, so:

- The **iOS reference** (`../nat20-ios`) is cloned as a sibling by `scripts/web-setup.sh` — set
  that as the cloud environment's setup script (`bash scripts/web-setup.sh`). Then all the
  `../nat20-ios` references above resolve.
- **Pre-installed:** JDK 21 + Gradle. **A1–A3** (`:domain` + `:ruleset-dnd5e`, pure Kotlin)
  build and test on the JDK alone — **no Android SDK needed**, so the port can start immediately.
- **Android SDK** is installed by the same setup script (needed from **A4**, `:app`/`:data`).
- **No emulator** in the sandbox (no nested virtualization). Instrumented/UI tests and actual
  app-launch checks run **locally** (pull the branch or `claude --teleport <session-id>`).
  In-sandbox "done" = build green + `:app:assembleDebug` compiles + JVM unit tests pass.
- Use **Auto accept edits** mode; **one backlog step per session/PR**.

## Out of scope / explicitly deferred

- **CloudKit-style sync** — Android is local-only; the `CharacterRepository` seam is where a
  backend slots in later (Open decisions in README).
- **KMP shared domain** — deferred; revisit only if logic drift gets painful.
- Anything under the iOS README's "open follow-ups" that's unstarted there (e.g. party-merged
  chronicles, step 16) — do not get ahead of the reference app.
