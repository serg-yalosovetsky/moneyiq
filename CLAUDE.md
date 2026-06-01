# onemoney — Project Instructions

This is a native Android personal finance app (Kotlin + Jetpack Compose + Room + Hilt).
App source lives under `onemoney/app/src/main/java/org/syalosovetskyi/onemoney/`.

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
cd onemoney; .\gradlew :app:compileDebugKotlin

# Unit tests
cd onemoney; .\gradlew :app:testDebugUnitTest

# Full debug build
cd onemoney; .\gradlew :app:assembleDebug
```

If compile fails with a cache error, add `--rerun-tasks`.

If compile fails with R.jar file lock error, run `.\gradlew --stop` to kill all Gradle daemons, then retry.

## Shell Environment

Default shell: Windows PowerShell.

Before generating commands, always identify the target environment:
- Windows PowerShell
- WSL Bash
- Remote Linux Bash

Do not mix PowerShell and Bash syntax.

PowerShell specifics:
- Use `$env:VAR` for environment variables
- Use backtick `` ` `` for line continuation
- Use PowerShell here-strings (`@'...'@`) for multiline strings passed to git

Avoid Bash heredocs (`<< EOF`) in PowerShell — they cause parse errors.

For complex scripts, write a script file first, then execute it.

## Editing Discipline

For files larger than 200 lines or structurally complex files:

1. Read the target file first.
2. Output a numbered plan before editing: file path, exact intended change, verification command.
3. Prefer a single clean rewrite over many partial edits.
4. After risky edits, run `.\gradlew :app:compileDebugKotlin` before continuing.

If two consecutive edits produce unexpected results: STOP. Do not continue patching. Ask the user before proceeding.

### File Lock / Busy File Protocol

If an edit fails with "file modified since read", "file is locked", or a similar conflict error — do NOT retry immediately. Use exponential backoff:

1. Wait ~3 seconds, retry once.
2. If still locked, wait ~5 seconds, retry once.
3. If still locked, wait ~10 seconds, retry once.
4. If still locked after 3 attempts, report to the user and stop.

Re-read the file before each retry attempt — another process (linter, Gradle daemon, IDE) may have changed the content.

This mirrors CSMA/CD: detect the collision, back off, then retransmit.

## Bug Log

Before fixing any compile/runtime error:
1. Check `.context/bug-log.md` for similar symptoms.
2. Avoid known-bad approaches listed there.

After every non-trivial fix, append to `.context/bug-log.md`:
- Symptom, Root cause, Fix, Files changed, Verification command, Date.

## Risky Operations

For SSH, networking, router, firewall, credentials, or deploy changes — before executing anything, output:
1. Exact sequence of commands.
2. What could break access.
3. Rollback path.
4. How to verify success.

Wait for explicit user approval before executing.
