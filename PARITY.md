# iOS ↔ Android screen parity audit

Goal: every Android screen looks and is structured like its iOS counterpart.
Process per screen: code-level diff → apply fixes → Bartek eyeballs both devices → tick.
Started 2026-07-13. Walk order = navigation order.

Legend: `[ ]` not audited · `[~]` audited, fixes in progress · `[x]` fixed + visually confirmed

## App shell & home

- [~] 1. Splash — iOS `Nat20App.swift` (`Nat20SplashView`, ~1.2s fade) ↔ `ui/theme/Brand.kt` (`SplashView`)
  - Built 2026-07-13: ported `Nat20MarkD20Iso` / `Nat20Wordmark` / `Nat20Lockup` to Compose (`Brand.kt`), splash overlay in `MainActivity` (1.2s hold, 0.4s ease-out fade, onboarding/roster ready behind it). Light = texture + fox-marks, dark = candle-glow. Awaiting visual confirm.
- [~] 2. Roster / home — `CharacterIndexView.swift` ↔ `ui/roster/RosterScreen.kt`
  - Fixed 2026-07-13: dashed CTA border, pinned-bar 3-stop gradient + top hairline, DropCap 34sp + cream inner-glow ring, chevron 18dp top-aligned, empty-tile 40dp top gap, gear 20dp, list bottom padding. Awaiting visual confirm.
  - Deferred: portrait-in-card (iOS `PortraitFrame`; Android has no portrait data yet — do with #23 Identity sheet). Android-only swipe-to-delete stays until #40 (Character Settings) provides the iOS delete path.
- [~] 3. Onboarding (4 pages) — `Onboarding/OnboardingView.swift` ↔ `ui/onboarding/OnboardingScreen.kt`
  - Restyled 2026-07-13: was generic Material (Button/headlineSmall/circle dots); now parchment chrome — SKIP pill, ruled Cinzel label, Cormorant italic titles, EB Garamond body, diamond dots, `PrimaryActionButton` (new shared port in `theme/Controls.kt`), and iOS vignettes (Welcome lockup, tinted die.png copied from iOS assets, Session-II journal plate w/ burst+sparkle glyphs, sealed sheet + wax seal). Copy was already identical. Awaiting visual confirm.

## Settings & friends

- [~] 4. Settings — `Settings/SettingsView.swift` ↔ `ui/settings/SettingsScreen.kt`
  - Fixed 2026-07-13: shared `SectionLabel`/pills to iOS caption2 sizing (11sp, pill v-padding 7), tile chevrons 16dp, patron-badge icon 20dp, bottom padding 24. Removed Android-only "Replay introduction" tile; added iOS-parity DEBUG-only Developer section ("Always Show Onboarding" toggle + `AppSettings.alwaysShowOnboarding` + per-launch gate in `MainActivity`). Awaiting visual confirm.
  - Deferred: "Custom Creatures" reference tile lands with #44 (needs the whole homebrew feature).
  - Resolved 2026-07-13: Android-only "Actions (PF2e)" reference tile + `PathfinderActionsScreen` **removed** per Bartek (exact parity; recoverable from git history). The `PfActions` catalogue + tests stay in `:ruleset-pf2e` (inert data, harmless).
- [~] 5. Patron paywall — `Patron/PatronView.swift` ↔ `ui/patron/PatronScreen.kt`
  - Rewritten 2026-07-13: was generic Material (TopAppBar/Card/Button); now iOS chrome — CLOSE/UNLOCK top nav, ornamental divider, d20 crest ("PATRON" eyebrow, Cormorant display title, IM Fell tagline), parchment perk tile w/ drawn three-person glyph, accent pledge button (Cinzel small-caps, accentDeep outline, in-flight spinner) + RESTORE PURCHASE + footnote. Auto-dismiss on unlock kept. "Google account" wording kept (platform-correct vs iOS "Apple Account"). Awaiting visual confirm.
- [~] 6. Credits — `Settings/CreditsView.swift` ↔ `ui/settings/CreditsScreen.kt`
  - Fixed 2026-07-13: section labels now use shared `SectionLabel` (fading rule, iOS `PickerSection` paddings), card titles 18sp, back chevron 18dp. Attribution copy verified verbatim. "↗" text glyph stands in for iOS `arrow.up.right.square`. Awaiting visual confirm.

## Character creation

- [~] 7. Creation entry — iOS: unified wizard w/ Ruleset step (`CharacterCreationWizard.swift`) · Android: EditionChooser AlertDialog in `NatApp.kt` — **structural**
  - Built 2026-07-13: unified `CreationWizardScreen` (Ruleset step w/ selection-dot edition tiles, indicator previews 1→8 steps once an edition is picked) + shared `EditorShell` chrome in new `ui/editor/WizardShell.kt` (back circle + kicker/title nav row, diamond step rail w/ tap-back jumps, hairline divider, pinned gradient footer w/ capsule Cancel + Continue/Create/Save buttons, `WizardStepSection`). EditionChooser AlertDialog + the three per-edition create routes removed; single "create" route. All three wizards re-hosted on `EditorShell` with `stepOffset = 1` + `onExitFirstStep` (back/jump from their first step returns to the Ruleset step); 2014 edit route unchanged (stepOffset 0). Step bodies untouched — that's items 8–10. Awaiting visual confirm.
  - Deferred: Content Sources disclosure under the selected 2014 row (needs the SourceCatalog port); per-edition draft retention when switching editions mid-flow (iOS keeps three drafts — Android re-entering an edition resets it); AI-describe card seam (A20).
- [ ] 8. 2014 wizard — `DnD5eEditorView.swift` + steps ↔ `ui/editor/DnD5eWizardScreen.kt` (iOS extra steps: Equipment, Manner, AI-describe card)
- [ ] 9. 2024 wizard — `DnD5e2024EditorView.swift` ↔ `ui/editor/DnD5e2024WizardScreen.kt` (iOS extra steps: Weapon Mastery, Fighting Style, Armor, Subclass, Spells)
- [ ] 10. PF2e wizard — `PathfinderEditorView.swift` ↔ `ui/editor/PathfinderWizardScreen.kt` (iOS extra steps: Subclass, Spells, Feats, Advancement, Equipment)
- [ ] 11. Custom Race form — iOS `CustomRaceForm.swift` · Android: **missing**

## Character sheet — 5e 2014 codex

- [ ] 12. Codex shell/chrome (hero row, campaign region, tab bar) — `CodexShellView.swift` ↔ `ui/codex/CodexShellView.kt`
- [ ] 13. Stats tab — `Pages/StatsPage.swift` ↔ `CodexPages.kt`
- [ ] 14. Skills tab — `Pages/SkillsPage.swift` ↔ `CodexPages.kt`
- [ ] 15. Combat tab — `Pages/CombatPage.swift` ↔ `CodexPages.kt`
- [ ] 16. Spells tab — `Pages/SpellsPage.swift` ↔ `SpellsPage.kt`
- [ ] 17. Items tab — `Pages/ItemsPage.swift` ↔ `ItemsPage.kt`
- [ ] 18. Lore tab — `Pages/LorePage.swift` ↔ `CodexPages.kt`
- [ ] 19. Actions sheet + pickers — iOS tile grid + ~40 full-screen pickers (`ActionsSheet/`, `ActionPickers/`) · Android: single 6-tile bottom sheet (`ui/actions/DnD5eActionsSheet.kt`) — **structural**
- [ ] 20. Level Up — `LevelUp/LevelUpView.swift` ↔ `ui/codex/LevelUpWizard.kt`
- [ ] 21. Attack flow — iOS `AttackPicker` ↔ `ui/codex/AttackSheet.kt`
- [ ] 22. Add/Edit item sheets — `AddItemSheet.swift`/`EditItemSheet.swift` ↔ dialogs in `ItemsPage.kt`
- [ ] 23. Identity sheet — iOS `Identity/IdentitySheet.swift` · Android: **missing**

## Character sheet — 5e 2024 codex

- [ ] 24. Codex shell/chrome — `Codex2024ShellView.swift` ↔ `ui/sheet/Codex2024ShellView.kt`
- [ ] 25–30. Six tabs (Stats/Skills/Combat/Spells/Items/Lore) — `Codex2024Pages.swift` ↔ `Codex2024ShellView.kt`
- [ ] 31. 2024 actions sheet + pickers — `Actions2024SheetView.swift` + `Pickers2024.swift` · Android: shares 2014 bottom sheet — **structural**
- [ ] 32. 2024 Level Up — `LevelUpSheet2024.swift` ↔ `ui/sheet/LevelUp2024Wizard.kt`
- [ ] 33. 2024 attack flow — `AttackPicker2024` ↔ `ui/sheet/AttackSheet2024.kt`

## Character sheet — PF2e

- [ ] 34. PF2e sheet — iOS single-scroll read-only `CodexPathfinderShellView.swift` · Android 6-tab pager `ui/sheet/PathfinderSheetView.kt` — **structural rebuild**
- [ ] 35. PF2e actions sheet — `PathfinderActionsView.swift` · Android: none — **missing**
- [ ] 36. PF2e Level Up — `PathfinderLevelUpView.swift` ↔ `PfLevelUpDialog` (in `PathfinderSheetView.kt`)

## Campaign

- [ ] 37. Campaign setup/settings — iOS `CampaignSetupView.swift` + `CampaignSettingsView.swift` (party roster, leave campaign, 2014 daily-spells) · Android: start/end dialogs only — **structural**
- [ ] 38. Journal — `CampaignJournalView.swift` (day-session pager, Log/Chronicle toggle, Act button, cog) ↔ `ui/journal/JournalScreen.kt`
- [ ] 39. Past adventures — `PastAdventuresView.swift` ↔ `ui/past/PastAdventuresScreen.kt`
- [ ] 40. Character settings (PDF export, delete) — iOS `CharacterSettingsView.swift` · Android: **missing** (delete = roster swipe)

## Reference (Settings → …)

- [ ] 41. Spell library shell + 3 ruleset tabs — `ReferenceTabShell.swift` + per-ruleset views ↔ `ui/reference/SpellLibraryShell.kt`
- [ ] 42. Item catalog + details — per-ruleset ↔ `ItemCatalogShell.kt` (iOS pushes detail screens; Android uses dialogs)
- [ ] 43. Monster codex + statblocks — per-ruleset ↔ `MonsterCodexShell.kt`
- [ ] 44. Custom Creatures list + form — iOS only · Android: **missing**
- [x] 45. PF2e Actions reference — removed 2026-07-13 (Android-only, no iOS counterpart; see #4)

## Out of scope for this audit

- AI character creation (backlog A20, unstarted on Android by design)
- PDF export pages (follows from #40 if built)
- iPad/tablet layouts (A13 descoped — Android is phone-only)
