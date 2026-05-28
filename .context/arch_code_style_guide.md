# Architecture And Code Style Guide

## Kotlin

- Prefer small composables and repository methods that match existing package ownership.
- Keep state in ViewModels or screen-local Compose state depending on lifetime.
- Use Flow from DAOs/repositories for reactive data.
- Avoid broad refactors while fixing a focused UI or accounting issue.

## Compose UI

- Preserve existing Material 3 style and icon library.
- Use stable dimensions for chips, buttons, and chart regions when layout drift would be visible.
- Avoid text wrapping in compact controls unless explicitly designed.
- Prefer existing color/icon/category helpers.
- Keep Ukrainian user-facing labels unless changing copy is part of the task.

## Persistence

- Add Room migrations for schema changes.
- Update `.context/DB_SCHEMA.md` after schema changes.
- Keep repositories as the boundary for multi-entity mutations.

## Testing And Verification

- Minimum check for Kotlin/Compose changes: `app:compileDebugKotlin`.
- For database/accounting changes, add focused unit or instrumentation coverage if the project has suitable test harnesses.
- For UI-sensitive screens, prefer emulator screenshot validation when available.

## Comments

- Comments are acceptable for non-obvious accounting or layout decisions.
- Do not add comments that restate simple code.

## Parallel Agent Safety

Multiple Claude sessions may run against this repo simultaneously. Before editing a file:

1. Check `git status` from the repo root (`G:\code\one-money-clone`) — NOT from `moneyiq/`.
2. If a file has uncommitted changes, read the full diff before overwriting.
3. Scope each session's changes to clearly separated files to avoid merge conflicts.
4. Commit completed work promptly so other sessions see it via `git log`.
