# System Overview

MoneyIQ is a native Android personal finance app built to recreate a 1Money-style experience with accounts, categories, transactions, budgets, reports, overview screens, and widgets.

## Runtime Boundary

- Runtime app source lives under `moneyiq/app`.
- Reverse-engineered/reference APK artifacts live at the repository root and extraction folders. They are not runtime source.
- The Android app is package `org.pixelrush.moneyiq`.
- There is one app module: `:app`.

## Main Modules

- `data/db` - Room v15, entities (accounts, categories, transactions), DAOs, migrations 1→15, type converters.
- `data/repository` - AccountRepository, CategoryRepository, TransactionRepository, SelectedMonthRepository (shared period state), SettingsRepository (DataStore).
- `di` - Hilt wiring for DAOs, database, and workers.
- `ui/main` - `MainScreen` (app shell, HorizontalPager, bottom nav, drawer, shared top bar), `SharedMonthNavPill`, `MainViewModel`.
- `ui/accounts` - account list, `AccountsScreen`, `IconColorPickerScreen`. Sheets split: `AccountSheets.kt` (CurrencyInfo data, type helpers, `NewAccountTypeSheet`, `AccountFormSheet`), `AccountPickerSheets.kt` (`CurrencyPickerSheet`, `TypePickerSheet`, `ColorPickerSheet`, `DescEditorDialog`, `BalanceInputDialog`, form helpers, `AccountActionSheet`).
- `ui/categories` - `CategoriesScreen` (donut + chip grid), `CategoriesViewModel`. Companion files: `CategoriesWidgets.kt` (`CategoryChip`, `SideSubcategoryPanel`, `ExpandedCategoryStrip`, `AddCategoryChip`, `DonutChart`, `dashedCircleBorder`), `CategorySheets.kt` (`CategoryActionSheet`, `QuickExpenseSheet`), `CategoryFormSheets.kt` (`CategoryFormSheet`, `ColorIconPickerSheet`, `EditCategoriesScreen`), `CategoryIcons.kt` (icon/color lists, `categoryIconFor()`).
- `ui/components/calculator` - shared cross-screen components: `CalcState.kt` (`CalcStateHolder`, `rememberCalcState`), `CalcKeypad.kt` (`SharedCalcKeypad`, `AmountCalculatorSheet`), `CalcDateSheet.kt` (`CalcDateSheet`, `FullDatePickerDialog`, `AccountPickerSheet`, repeat/reminder dialogs).
- `ui/components/dialogs` - generic reusable AlertDialog composables: `TextInputDialog.kt` (single-line text input with optional enforce-fill mode), `ConfirmationDialog.kt` (destructive/neutral confirmation with optional icon). Both `internal`.
- `ui/transactions` - `TransactionsListScreen`, `AddTransactionScreen`, `TransactionViewModel`, `TransactionsListViewModel`. Sheet/dialog composables in dedicated files: `TxSearchScreen.kt` (`TxSearchScreen`, `SearchSectionHeader`, `TypeFilterCard`, `ColoredFilterChip`), `CategoryPickerSheet.kt` (`CategoryPickerSheet`, `CategoryPickerCell`, `AccountPickerRow`), `TransferQuickSheet.kt`, `TransactionDetailSheet.kt`.
- `ui/budget` - `BudgetScreen` (main + `BudgetTopBar`, `resolvedCatIcon`, chip/card composables), `BudgetSheets.kt` (`BudgetInputSheet`, `BudgetSettingsSheet`), `BudgetViewModel`.
- `ui/overview` - `OverviewScreen` (main + all chart/stats composables), `OverviewSheets.kt` (`CategoryDetailSheet`), `OverviewViewModel`.
- `ui/reports` - `ReportsScreen`, `ReportsViewModel`.
- `ui/settings` - `SettingsScreen` (enum + `SettingsScreen` composable + `MainSettingsContent`), `SettingsSubScreens.kt` (`ThemePageContent`, `ColorPalette`, `CurrencyPageContent`, `AboutPageContent`, shared helpers, dialogs), `SettingsViewModel`. Static data in `ui/settings/data/`: `CurrencyData.kt` (`CurrencyDef`, `CURRENCIES_MAIN/OTHER/CRYPTO/ALL`) and `SettingsData.kt` (`ACCENT_COLORS`, `LANGUAGES`, `DAYS_OF_WEEK`, `CURRENCY_FORMAT_EXAMPLES`, `formatMoneyWithSettings`).
- `ui/data` - `DataScreen` (main screen only), `DataWidgets.kt` (`MonoFlowSyncCard`, `DataSectionHeader`, `DataActionItem`, `DriveBackupItem`, `LocalBackupItem`, `pluralUk`, `ResetDataDialog`), `DataViewModel` (JSON import/export, backup; injects DAOs directly — TODO: migrate to repositories).
- `ui/theme` - `Theme.kt` (colors incl. `BudgetExpenseColor`, `BudgetIncomeColor`), `Spacing.kt` (design tokens xs–xxl).
- `ui/widget` - `BalanceWidget`, `ExpenseWidget` (Glance).
- `workers` - `NotificationWorker` (daily notification, self-reschedules), `DriveBackupWorker`, `MonoFlowSyncWorker`.
- `util` - `BackupSerializer`, `CsvExporter`, `CategoryStyleUtil` (keyword→icon/color auto-suggest).

## Main Flows

- App startup seeds default categories if the category table is empty.
- Users manage accounts and categories, then record transactions against them.
- Transaction add/update/delete mutates account balances in `TransactionRepository`.
- Period-aware screens read monthly or selected-period aggregates from DAOs/repositories.
- The main UI is a Compose `Scaffold` with bottom tabs and embedded feature screens.
- Settings (theme, accent color, biometric login, notifications, budget visibility, currency) are persisted via DataStore in `SettingsRepository` and applied in `MainActivity`.

## Test Suite

Unit tests live in `app/src/test/`, instrumented tests in `app/src/androidTest/`. ~196 tests, 0 failures (as of 2026-05-29).

Covered:
- Repositories: TransactionRepository, AccountRepository, CategoryRepository, SelectedMonthRepository
- ViewModels: TransactionViewModel, AccountsViewModel, CategoriesViewModel, MainViewModel, ReportsViewModel
- Utils: CategoryStyleUtil, BackupSerializer, CalcState
- Room (instrumented): AccountDao, CategoryDao, TransactionDao, AppDatabaseSchemaTest

Commands:
- Unit tests: `gradlew :app:testDebugUnitTest`
- Instrumented: `gradlew connectedAndroidTest` (requires device/emulator)

## CI/CD

Two GitHub Actions workflows live in `.github/workflows/`:

| File | Trigger | What it does |
|---|---|---|
| `build.yml` | push to `main`, PRs, tags `v*.*.*` | Unit tests → debug APK (on push) → signed release APK + GitHub Release (on tags) |
| `ci.yml` | push to `main`, PRs | Compile check + unit tests (fast path) |

Release signing uses secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. `SENTRY_AUTH_TOKEN` is required for release mapping uploads. All secrets live in GitHub Actions — never committed.

`gradlew` must have the executable bit set in git (`git update-index --chmod=+x moneyiq/gradlew`); otherwise CI fails with `Permission denied`.

## Non-Runtime Context

- `.context` docs explain expected architecture and implementation constraints.
- `README.md` is public-facing project documentation.
