# Nat20 (Android)

The Android port of Nat20 — an app for managing D&D characters with pluggable rulesets.

Each character has a change log so you can see what shifted over time — level-ups, equipment, etc. The character sheet, spell list, and available options reflect the ruleset you've picked. The shipping iOS app (`../nat20-ios`) is the reference implementation; this README tracks bringing the same experience to Android.

## Stack

| | |
|---|---|
| **Platform** | Android (phone + tablet), min SDK TBD (target a modern baseline — Android 13 / API 33+) |
| **Language / UI** | Kotlin + Jetpack Compose |
| **Persistence** | Room (SQLite), local-only to start — behind a repository interface so sync can slot in later |
| **First ruleset** | D&D 5e (2014) |
| **On-device AI** | Gemini Nano (AICore / ML Kit GenAI) where supported, availability-gated like iOS gates FoundationModels |
| **Application ID** | `au.com.evonet.nat20` (proposed — mirrors the iOS bundle ID) |

## Relationship to the iOS app

This is a **parallel native Kotlin port**, not a shared binary. iOS keeps its Swift `Domain` / `DnD5e` / `Persistence` packages; Android reimplements the same domain model and 5e engine idiomatically in Kotlin. Two codebases, each native to its platform.

**What we reuse directly from `../nat20-ios`:**

- **SRD 5.1 JSON catalogues** — `Packages/DnD5e/Sources/DnD5e/Resources/` (spells, races, classes, backgrounds, feats, fighting styles, metamagic, invocations, pact boons, armor/weapons/gear, monsters, plus Tasha's/Xanathar's spells). These are platform-agnostic JSON; copy them into Android assets/resources verbatim. **Licensing:** SRD 5.1 + 5e-bits/5e-database are CC BY 4.0 — carry the same attribution (see the iOS Credits screen). Tasha's/Xanathar's spell content is **not** openly licensed; mirror the iOS stance on it.
- **Domain shape** — `Ruleset` / `Character` / `CharacterPayload` / `CharacterEvent` / `CharacterIntent` and the intent-logged-mutation pattern (`Campaign.apply`). The iOS source under `Packages/Domain` and `Packages/DnD5e` is the spec.
- **The full roadmap + rules-coverage notes** — `../nat20-ios/README.md` is the canonical feature definition. When in doubt about how a mechanic should behave, that's the source of truth.
- **Visual design language** — the ink-on-parchment look (Cinzel / Cormorant Garamond / EB Garamond / IM Fell English typefaces under SIL OFL 1.1; parchment ground; gold accents; codex-shell tab layout; the d20 app icon). Match it on Android with Compose theming.

**Risk to manage:** parallel codebases can drift. When a 5e rule is fixed or refined on one platform, mirror it on the other. (Kotlin Multiplatform for the shared domain was considered and deferred — see Open decisions.)

## Build order

The iOS app was built abstraction-first and shipped in a working order that proved out well; Android follows the same spine. Everything below is unstarted.

### Foundation

- [x] **A1. Project skeleton** — Android Studio project, Gradle (Kotlin DSL) setup, git repo + `.gitignore`, module structure (`:app`, `:domain`, `:ruleset-dnd5e`, `:data` — mirrors the iOS package split so the domain stays ruleset-agnostic and the 5e engine is isolated). SRD catalogues + fonts copied in. *(Builds with Android Studio's bundled JBR — no standalone JDK on the dev machine; set `JAVA_HOME` to it.)*
- [x] **A2. Core domain abstractions** — `Ruleset`, `Character`, `CharacterPayload`, `CharacterEvent`, `CharacterIntent` as Kotlin interfaces + value types (data classes / sealed classes), plus `LoggedEvent`, `NoteKind`, and the `Summon`/`Creature` value types (shape only; A15 behaviour later). Intents validate-then-return via `CharacterIntent.applyTo → IntentResult` (Kotlin has no `inout`; the campaign-logged path arrives with `Campaign.apply` at A7a). Codec surface uses JSON `String` (kotlinx.serialization). 12 JUnit5 tests: creation, rename, validation failures, codec round-trips. *(`Campaign` itself is A7a per build order.)*
- [x] **A3. D&D 5e ruleset, minimal slice** — `DnD5eRuleset` + `DnD5ePayload` (race, classes/`ClassEntry`, derived level, six abilities via `AbilityScores`, HP). `Ability` enum + modifier math; proficiency-bonus-by-level. First handful of intents (`TakeDamage`/`Heal`/`GainTempHp`/`LevelUp`) + their events, with the JSON codec wired per type-id. `LevelUpMath` (HP) + a stand-in class hit-die map (full `ClassCatalog` JSON loading deferred to the content steps). 36 JUnit5 tests: ruleset/payload defaults, proficiency + modifier tables, each intent, codec round-trips, full mini character build. *(Simpler than shipped iOS — no resistances/feats/slots/subclass yet; shapes match so logic grows in place.)*

### UI shell + persistence

- [ ] **A4. UI shell** — character list → character sheet (read-only), driven by a `CharacterStore` (ViewModel + `StateFlow`) seeded with demo 5e characters. Per-ruleset sheet composable dispatched by `rulesetID`. Identity / Vitals / Abilities sections; the change log as a separate Journal screen (Navigation Compose).
- [ ] **A5. Local persistence (Room)** — `:data` module. Room entities + DAOs for characters and campaign logs; a `CharacterCodec` bridges value-type domain `Character`s to/from Room rows (store the payload + events as serialized JSON, mirroring the iOS codec). **Repository interface** (`CharacterRepository`) wraps the data layer so a future sync backend slots in without touching the domain or UI. Demo seed on first launch. App relaunch preserves data.
  - *Note:* no CloudKit equivalent — Android is **local-only** for now. Cross-device (and eventual cross-platform) sync is an explicit later phase; the repository seam is what keeps that cheap.
- [ ] **A6. Create / edit / delete flows** — repository `add` / `update` / `delete`. Create screen from a `+` action; edit from the sheet; swipe-to-delete with confirmation. Sheet looks up by ID so updates reflect immediately.

### Campaigns + in-play

- [ ] **A7a. Campaign data model + lifecycle** — logs live on `Campaign`, not the character. `CharacterPhase` (`building` / `inCampaign(id)`) gates whether intents are allowed. `Campaign` value type + `Campaign.apply`; Room entities + codec; repository `startCampaign` / `endCampaign` / `apply(intent, toCampaign:)`.
- [ ] **A7b. In-campaign UI** — phase-aware sheet chrome (building → Edit + Start Campaign; in-campaign → Journal + Actions). Active-campaign banner. Start-campaign flow, paged journal with empty state, rename/end.
- [ ] **A7c. Past Adventures** — index of ended campaigns (build phase only), each row showing name, level arc, primary class, party, ended date; tapping opens the journal read-only.
- [ ] **A7d. AI chronicles** — on-device narrative summaries via **Gemini Nano** (AICore / ML Kit GenAI). One paragraph per session, third-person past-tense, every event reflected, no invented content. Stored on `Campaign`. Availability-gated: on devices without on-device LLM support the affordance simply doesn't appear (single availability flag, reused everywhere — same discipline as iOS).
- [ ] **A7e. Codex shell** — the ink-on-parchment in-character experience: a persistent shell with the 6 tabbed pages (Stats / Skills / Combat / Spells / Items / Lore), richer Actions sheet, and campaign journal. This is the big design-heavy build; match the iOS codex.
- [ ] **A7f. Play mechanics** — wire every action to a real intent + journal entry: damage/heal/temp HP, death saves, inspiration, concentration, conditions, coins, hit dice, short/long rest, attack rolls, cast spell (slot consumption + upcast + ritual), prepare/unprepare. Spell-slot accuracy (full / half / warlock pact progressions). Campaign opening line.

### Content + character creation

- [ ] **A8. Multi-step character creation wizard** — the parchment-themed multi-step builder (Ruleset → Name → Race → Class → Manner → Abilities → Skills → Spells → Advancements → Review). Ruleset is per-character. Materialises a draft into a `Character`.
- [ ] **A9. Settings** — narration style, light/dark/system appearance, Reference section, About (Credits & Licenses — carry the SRD + data-source + typeface attributions verbatim).
- [ ] **A10. Expand 5e content** — equipment + inventory, item usage (potions/scrolls/wondrous charges), class features incl. pool-resource counters, spell prep per-class buckets, realistic slot tables, third-caster casting (EK/AT), hit-die sizing, real AC computation. (See the iOS README's "5e rules — coverage gaps" for the long tail.)
- [ ] **A11. Level-up wizard + high-level creation** — multi-step in-campaign level-up plus the "Advancements" step for characters created above level 1. Shared choice catalogues (feats, fighting styles, metamagic, invocations, pact boons, ASIs, expertise, spell picks).

### Platform polish + later phases

- [ ] **A12. Tablet / large-screen layouts** — width-aware readable caps; multi-pane where it adds value (codex + actions side-by-side in landscape). Mirrors iOS 12a/12b.
- [ ] **A13. Free tier + paywall** — Google Play Billing. One character free; additional characters via a one-time IAP. Mirror the iOS gating once the iOS monetisation model is finalised.
- [ ] **A14. Active effects** — unified in-play modifier model (spells/items/features as typed effects with durations + sources, surfaced as coloured chips with concrete impact). Auto-apply on cast/use/feature, auto-cancel on concentration-end / long-rest.
- [ ] **A15. Familiars + summoned creatures** — secondary entities tied to a character (Find Familiar/Steed, Conjure Animals, Summon X family, Beast Master companion), surfaced as statblock cards.
- [ ] **A16. Die-roll animation primitive** — the Compose equivalent of `RollResultView`: tumbling dice, bonus chips streaming in, total ticker, crit flourish, haptics. Consumed by attack / save / skill / ability / initiative / hit-die / death-save / HP-roll pickers.
- [ ] **A17. AI-assisted character creation** — "describe your character" → on-device LLM produces a *concept*, deterministic Kotlin fills the mechanics. Gated behind the same on-device-AI availability flag as chronicles.

## Open decisions

- **Kotlin Multiplatform (deferred).** We chose a parallel Kotlin port over a KMP shared domain to keep the shipping iOS app untouched and Android idiomatic. Revisit if logic drift between platforms becomes painful — the natural seam would be extracting the domain + 5e engine into a KMP module consumed by both.
- **Sync backend (deferred).** Local-only via Room for now, behind `CharacterRepository`. When cross-device (or cross-platform iOS↔Android) sync is wanted, decide between Firebase/Firestore, a Supabase/custom backend that both apps target, or per-platform native sync. The repository interface is the insertion point.
- **Min SDK + on-device-AI device coverage.** Pin the min SDK and confirm which devices actually expose Gemini Nano / AICore; the AI features are availability-gated, so unsupported devices fall back gracefully (no chronicle / no AI creation, never a broken affordance).
- **Monetisation parity.** Keep the Play Billing model aligned with the iOS StoreKit model (one free character + IAP unlock) once iOS step 13 lands.

## Reference

- iOS reference app + canonical roadmap: [`../nat20-ios/README.md`](../nat20-ios/README.md)
- SRD JSON to copy: `../nat20-ios/Packages/DnD5e/Sources/DnD5e/Resources/`
