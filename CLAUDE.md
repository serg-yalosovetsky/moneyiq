# MoneyIQ — Project Instructions

This is a native Android personal finance app (Kotlin + Jetpack Compose + Room + Hilt).
App source lives under `moneyiq/app/src/main/java/org/pixelrush/moneyiq/`.

## Before starting any task

Read the relevant `.context/` files first. They contain authoritative architecture decisions, DB schema, and UI contracts — do not guess or infer what is already documented there.

**Always read at the start of a session:**
- `.context/SYSTEM_OVERVIEW.md` — module map, main flows, test suite, CI
- `.context/DB_SCHEMA.md` — Room DB version, all entities and fields, migration history

**Read when working on UI:**
- `.context/ui/UI_CONTRACTS.md` — index; links to the right sub-file
- Pick the matching sub-file: `UI_ACCOUNTS.md`, `UI_CATEGORIES.md`, `UI_CALCULATOR.md`, `UI_BUDGET.md`, `UI_TRANSACTIONS.md`, `UI_TYPOGRAPHY.md`
- For category icon rules: `.context/ui/UI_CATEGORY_ICONS.md`

**Read when making architectural decisions:**
- `.context/ADR_LOG.md` — all past decisions and the rules they establish; check before introducing a new pattern

## Key rules (summary — full detail in context files)

- Any DB change requires a `MIGRATION_N_(N+1)` + bump `version` + add to `ALL_MIGRATIONS` + update `DB_SCHEMA.md`.
- Never hardcode `Color.White` on an icon/text whose background is `account.colorHex` — use `luminance()`.
- `SharedCalcKeypad` currency key: pass `onCurrencyClick` only when the caller manages a picker state.
- `QuickExpenseSheet.onSave` has 6 parameters (added `repeatMode`, `reminderMode` — see ADR-042).
- `RepeatTransactionWorker` runs nightly — do not duplicate its balance-update logic without syncing.
- Do not add a new local icon mapper in any screen — always call `categoryIconFor()` from `CategoryIcons.kt`.
- `.context` is documentation only — never import or bundle it.

## Build commands

```powershell
# Compile check (fast)
cd moneyiq; .\gradlew :app:compileDebugKotlin

# Unit tests
cd moneyiq; .\gradlew :app:testDebugUnitTest

# Full debug build
cd moneyiq; .\gradlew :app:assembleDebug
```

If compile fails with a cache error, add `--rerun-tasks`.
