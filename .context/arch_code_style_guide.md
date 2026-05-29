# Architecture And Code Style Guide

## Kotlin

- Prefer small composables and repository methods that match existing package ownership.
- Keep state in ViewModels or screen-local Compose state depending on lifetime.
- Use Flow from DAOs/repositories for reactive data.
- Avoid broad refactors while fixing a focused UI or accounting issue.
- ViewModels must live in their own file separate from the Screen file (e.g., `BudgetViewModel.kt` + `BudgetScreen.kt`).

## Compose UI

- Preserve existing Material 3 style and icon library.
- Use stable dimensions for chips, buttons, and chart regions when layout drift would be visible.
- Avoid text wrapping in compact controls unless explicitly designed.
- Prefer existing color/icon/category helpers.
- Keep Ukrainian user-facing labels unless changing copy is part of the task.
- Calculator and date-picker components live in `ui/components/calculator/` — import from there, not from `ui.categories`.
- `categoryIconFor()` remains in `ui/categories/CategoryIcons.kt` (internal).
- Transaction sheet composables are split into `TxSearchScreen.kt`, `CategoryPickerSheet.kt`, `TransferQuickSheet.kt`, `TransactionDetailSheet.kt` — do not re-merge into a single file. New transaction sheet composables go into the most relevant of these four files.
- Generic dialog composables live in `ui/components/dialogs/` — use `TextInputDialog` for single-line text input and `ConfirmationDialog` for destructive/confirm prompts. Do not re-inline `AlertDialog` copies for these patterns.

## Companion File Pattern

Large screen files (>600 lines) are split into a main file + one or more companion files in the same package:

| Main file | Companion(s) | What goes in companion |
|---|---|---|
| `BudgetScreen.kt` | `BudgetSheets.kt` | Input/settings bottom sheets |
| `CategoriesScreen.kt` | `CategoriesWidgets.kt` | Chip, panel, chart, border composables |
| `CategorySheets.kt` | `CategoryFormSheets.kt` | CategoryFormSheet, ColorIconPickerSheet, EditCategoriesScreen |
| `AccountSheets.kt` | `AccountPickerSheets.kt` | Picker sheets, dialogs, form helpers, AccountActionSheet |
| `OverviewScreen.kt` | `OverviewSheets.kt` | CategoryDetailSheet |
| `SettingsScreen.kt` | `SettingsSubScreens.kt` | Sub-pages, helpers, dialogs |
| `DataScreen.kt` | `DataWidgets.kt` | All private composables and helper functions |

**Visibility rule:** Composables/functions shared across files in the same package must be `internal`, not `private`. Use `private` only for helpers that are used exclusively within the same file.

**Data subpackage:** Pure data constants and pure utility functions (no Compose/Android dependencies except `Color`) go in a `data/` subpackage of the owning module. Example: `ui/settings/data/CurrencyData.kt` (`CurrencyDef`, currency lists) and `ui/settings/data/SettingsData.kt` (`ACCENT_COLORS`, `LANGUAGES`, `DAYS_OF_WEEK`, `CURRENCY_FORMAT_EXAMPLES`, `formatMoneyWithSettings`). Screen and companion files import via `import ...settings.data.*`.

## Persistence

- Add Room migrations for schema changes.
- Update `.context/DB_SCHEMA.md` after schema changes.
- Keep repositories as the boundary for multi-entity mutations.

## Testing

Unit test libraries are configured. Use these patterns:

```kotlin
@get:Rule val mainDispatcherRule = MainDispatcherRule()  // sets UnconfinedTestDispatcher
```

For Flow testing use Turbine:

```kotlin
vm.state.test {
    val state = awaitItem()
    assertEquals(expected, state.someField)
    cancelAndIgnoreRemainingEvents()
}
```

For ViewModel tests with lazy state (state updates only after all upstream flows emit):
- Use `emptyFlow()` for one upstream to keep `isLoading = true` observable.
- Use `flowOf(value)` to immediately trigger combine and set `isLoading = false`.

Test commands:
- Compile check: `gradlew :app:compileDebugKotlin`
- Unit tests: `gradlew :app:testDebugUnitTest`
- Instrumented tests (device required): `gradlew connectedAndroidTest`

When adding new ViewModels or repositories, add corresponding tests following patterns in existing test files.

## Comments

- Comments are acceptable for non-obvious accounting or layout decisions.
- Do not add comments that restate simple code.

## Parallel Agent Safety

Multiple Claude sessions may run against this repo simultaneously. Before editing a file:

1. Check `git status` from the repo root (`G:\code\one-money-clone`) — NOT from `moneyiq/`.
2. If a file has uncommitted changes, read the full diff before overwriting.
3. Scope each session's changes to clearly separated files to avoid merge conflicts.
4. Commit completed work promptly so other sessions see it via `git log`.
