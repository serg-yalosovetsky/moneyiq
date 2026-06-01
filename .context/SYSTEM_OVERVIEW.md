# System Overview

onemoney is a native Android personal finance app built to recreate a 1Money-style experience with accounts, categories, transactions, budgets, reports, overview screens, and widgets.

## Runtime Boundary

- Runtime app source lives under `onemoney/app`.
- Reverse-engineered/reference APK artifacts live at the repository root and extraction folders. They are not runtime source.
- The Android app is package `org.syalosovetskyi.onemoney`.
- There is one app module: `:app`.

## Main Modules

- `data/db` - Room v29, entities (accounts `+creditLimit`, categories, transactions `+repeatMode+reminderMode+nextRepeatDate`), DAOs (`CategoryDao.updateCategories` for batch reorder; `TransactionDao.getDueRepeatTransactions`, `clearNextRepeatDate`, `getTransactionsWithReminder`), migrations 1→29, type converters.
- `data/repository` - AccountRepository, CategoryRepository (`updateAll`, `repairIconKeys`), TransactionRepository, SelectedMonthRepository (shared period state), SettingsRepository (DataStore).
- `di` - Hilt wiring for DAOs, database, and workers.
- `ui/main` - `MainScreen` (app shell, HorizontalPager, bottom nav, drawer, shared top bar), `SharedMonthNavPill`, `MainViewModel`.
- `ui/accounts` - account list, `AccountsScreen`, `IconColorPickerScreen`. Sheets split: `AccountSheets.kt` (CurrencyInfo data, type helpers, `NewAccountTypeSheet`, `AccountFormSheet`), `AccountPickerSheets.kt` (`TypePickerSheet`, `ColorPickerSheet`, `DescEditorDialog`, `BalanceInputDialog`, form helpers, `AccountActionSheet`). `CurrencyPickerSheet` moved to `ui/components/currency`.
- `ui/categories` - `CategoriesScreen` (donut + chip grid), `CategoriesViewModel`. Companion files: `CategoriesWidgets.kt` (`CategoryChip`, `SideSubcategoryPanel`, `ExpandedCategoryStrip`, `AddCategoryChip`, `DonutChart`, `dashedCircleBorder`), `CategorySheets.kt` (`CategoryActionSheet`, `QuickExpenseSheet`), `CategoryFormSheets.kt` (`CategoryFormSheet`, `ColorIconPickerSheet`), `EditCategoriesScreen.kt` (`EditCategoriesScreen` — full-screen edit overlay, reuses `CategoriesGridContent`), `CategoryIcons.kt` (icon/color lists, `categoryIconFor()`).
- `ui/components/calculator` - shared cross-screen components: `CalcState.kt` (`CalcStateHolder`, `rememberCalcState`), `CalcKeypad.kt` (`SharedCalcKeypad`, `AmountCalculatorSheet`), `CalcDateSheet.kt` (`CalcDateSheet`, `FullDatePickerDialog`, `AccountPickerSheet`, repeat/reminder dialogs).
- `ui/components/dialogs` - generic reusable AlertDialog composables: `TextInputDialog.kt` (single-line text input with optional enforce-fill mode), `ConfirmationDialog.kt` (destructive/neutral confirmation with optional icon). Both `internal`.
- `ui/components/currency` - `CurrencyPicker.kt`: `CurrencyPickerSheet` — full-screen `Dialog` with tabs (Main / Other / Crypto). Moved here from `ui/accounts/AccountPickerSheets.kt`. Import: `org.syalosovetskyi.onemoney.ui.components.currency.CurrencyPickerSheet`.
- `ui/components/form` - `FormComponents.kt`: `FormSectionHeader` (labeled divider row), `FormNavRow` (icon + label + value + clickable). Both `internal`.
- `ui/components/icons` - `IconBox.kt`: `CircleIconBox`, `RoundedIconBox`. Both `internal`. Replaces ad-hoc `Box + clip + Icon` patterns.
- `ui/transactions` - `TransactionsListScreen` (`initialAccountFilter` for deep-link from Accounts tab), `AddTransactionScreen` (currency picker, category/transfer via `CategoryPickerSheet`), `TransactionViewModel`, `TransactionsListViewModel`. Sheet/dialog composables in dedicated files: `TxSearchScreen.kt`, `CategoryPickerSheet.kt` (`CategoryPickerSheet` — supports `currentType` simplified mode + `initialTab`), `TransferQuickSheet.kt`, `TransactionDetailSheet.kt`.
- `ui/budget` - `BudgetScreen` (main + `BudgetTopBar`, `resolvedCatIcon`, chip/card composables), `BudgetSheets.kt` (`BudgetInputSheet`, `IncomeBudgetInputSheet`, `BudgetSettingsSheet`), `BudgetViewModel` (injects `SettingsRepository` for global income budget).
- `ui/overview` - `OverviewScreen` (main + all chart/stats composables), `OverviewSheets.kt` (`CategoryDetailSheet`), `OverviewViewModel`.
- `ui/reports` - `ReportsScreen`, `ReportsViewModel`.
- `ui/settings` - `SettingsScreen` (enum + `SettingsScreen` composable + `MainSettingsContent`), `SettingsSubScreens.kt` (`ThemePageContent`, `ColorPalette`, `CurrencyPageContent`, `AboutPageContent`, shared helpers, dialogs), `SettingsViewModel`. Static data in `ui/settings/data/`: `CurrencyData.kt` (`CurrencyDef`, `CURRENCIES_MAIN/OTHER/CRYPTO/ALL`) and `SettingsData.kt` (`ACCENT_COLORS`, `LANGUAGES`, `DAYS_OF_WEEK`, `CURRENCY_FORMAT_EXAMPLES`, `formatMoneyWithSettings`).
- `ui/data` - `DataScreen` (main screen only), `DataWidgets.kt` (`MonoFlowSyncCard`, `DataSectionHeader`, `DataActionItem`, `DriveBackupItem`, `LocalBackupItem`, `pluralUk`, `ResetDataDialog`), `DataViewModel` (JSON import/export, backup; injects DAOs directly — TODO: migrate to repositories).
- `ui/theme` - `Theme.kt` (colors incl. `BudgetExpenseColor`, `BudgetIncomeColor`), `Spacing.kt` (design tokens xs–xxl).
- `ui/widget` - `BalanceWidget`, `ExpenseWidget` (Glance).
- `workers` - `NotificationWorker` (daily notification, self-reschedules), `DriveBackupWorker`, `MonoFlowSyncWorker`, `RepeatTransactionWorker` (nightly at 00:01 — creates due repeat transactions, fires reminder notifications, reschedules itself; also triggered once on app start via `onemoneyApp`).
- `util` - `BackupSerializer` (JSON import/export; fields: all account/category/transaction columns incl. `creditLimit`, `repeatMode`, `reminderMode`, `nextRepeatDate`; uses `has()+isNull()` guard for nullable longs for backward compat with old backups), `CsvExporter`, `CategoryStyleUtil` (keyword→icon/color auto-suggest), `RepeatUtil.kt` (`calculateNextRepeatDate`, `startOfDay`, `reminderOffsetDays`).

## Main Flows

- App startup seeds default categories if the category table is empty. `repairIconKeys()` and `repairDefaultColors()` run on every startup to fix icon/color drift from imports or old seed data. `repairIconKeys()` applies name-based overrides first, then runs `suggestCategoryStyle()` for any category whose stored icon is a generic placeholder (`"category"` or `"family"`) that doesn't match the category name, and finally re-derives icons for any key that isn't in the valid key set.
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

Two GitHub Actions workflows:

**`.github/workflows/ci.yml`**

| Trigger | Jobs |
|---|---|
| push to `main` or PR | `test` — unit tests + coverage report (artifacts uploaded 14 days) |
| push to `main` | `build` (needs `test`) — debug APK (artifact 30 days) |

**`.github/workflows/build.yml`**

| Trigger | Jobs |
|---|---|
| push to `main` or PR | `test` — unit tests |
| tag `v*.*.*` | `test` → `release` — signed APK + GitHub Release |

Release signing uses secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. `SENTRY_AUTH_TOKEN` is optional — when absent, `includeSourceContext` is disabled and the build still succeeds. All secrets live in GitHub Actions — never committed.

`gradlew` already has the executable bit in git (`100755`). Each job also runs `chmod +x gradlew` as a safety step.

Node.js runtime: GitHub Actions runners use Node.js 24 by default (Node.js 20 deprecated since 2025-09-19). No `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24` override needed.

## Non-Runtime Context

- `.context` docs explain expected architecture and implementation constraints.
- `.context/ui/` — UI contracts split into 8 files; `UI_CONTRACTS.md` is the index.
- `README.md` is public-facing project documentation.
