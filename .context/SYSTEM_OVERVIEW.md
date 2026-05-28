# System Overview

MoneyIQ is a native Android personal finance app built to recreate a 1Money-style experience with accounts, categories, transactions, budgets, reports, overview screens, and widgets.

## Runtime Boundary

- Runtime app source lives under `moneyiq/app`.
- Reverse-engineered/reference APK artifacts live at the repository root and extraction folders. They are not runtime source.
- The Android app is package `org.pixelrush.moneyiq`.
- There is one app module: `:app`.

## Main Modules

- `data/db` - Room v5, entities (accounts, categories, transactions), DAOs, migrations 1→5, type converters.
- `data/repository` - AccountRepository, CategoryRepository, TransactionRepository, SelectedMonthRepository (shared period state), SettingsRepository (DataStore).
- `di` - Hilt wiring for DAOs, database, and workers.
- `ui/main` - `MainScreen` (app shell, HorizontalPager, bottom nav, drawer, shared top bar), `SharedMonthNavPill`, `MainViewModel`.
- `ui/accounts` - account list, `AccountFormSheet`, `NewAccountTypeSheet`, `IconColorPickerScreen`.
- `ui/categories` - `CategoriesScreen` (donut + chip grid), `CategorySheets` (QuickExpenseSheet, CategoryFormSheet), `EditCategoriesScreen`, `CategoriesViewModel`.
- `ui/transactions` - `TransactionsListScreen`, `AddTransactionScreen`, `TransactionViewModel`, `TransactionsListViewModel`.
- `ui/budget` - `BudgetScreen`, `BudgetViewModel`.
- `ui/overview` - `OverviewScreen`, `OverviewViewModel`.
- `ui/reports` - `ReportsScreen`, `ReportsViewModel`.
- `ui/settings` - `SettingsScreen` (all settings dialogs inline), `SettingsViewModel`.
- `ui/data` - `DataScreen`, `DataViewModel` (JSON import/export, backup).
- `ui/widget` - `BalanceWidget`, `ExpenseWidget` (Glance).
- `workers` - `NotificationWorker` (daily notification, self-reschedules), `DriveBackupWorker`, `MonoFlowSyncWorker`.
- `util` - `BackupSerializer`, `CsvExporter`, `CategoryStyleUtil` (keyword→icon/color auto-suggest).

## Main Flows

- App startup seeds default categories if the category table is empty.
- Users manage accounts and categories, then record transactions against them.
- Transaction add/update/delete mutates account balances in `TransactionRepository`.
- Period-aware screens read monthly or selected-period aggregates from DAOs/repositories.
- The main UI is a Compose `Scaffold` with bottom tabs and embedded feature screens.

## Non-Runtime Context

- `.context` docs explain expected architecture and implementation constraints.
- `README.md` is public-facing project documentation.
