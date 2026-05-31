# UI Contracts — Index

UI contracts are split across focused files. Start here for shell/navigation; follow links for feature-specific contracts.

| File | Contents |
|---|---|
| `UI_CONTRACTS.md` | App shell, navigation, shared month state, settings, text/locale |
| `UI_ACCOUNTS.md` | Accounts screen, icon badges, adaptive tint, credit limit, CurrencyPickerSheet |
| `UI_CATEGORIES.md` | Categories screen, chip layout, donut, QuickExpenseSheet, CategoryFormSheet, EditCategoriesScreen |
| `UI_CATEGORY_ICONS.md` | Icon auto-suggest rules table (55 rules), critical orderings |
| `UI_CALCULATOR.md` | SharedCalcKeypad signature, layout, key handling, hardware keyboard |
| `UI_BUDGET.md` | Budget screen, BudgetInputSheet, SavingsSectionCard, IncomeBudgetBar, BudgetUiState |
| `UI_TRANSACTIONS.md` | AddTransactionScreen, CategoryPickerSheet, overview screen |
| `UI_TYPOGRAPHY.md` | Typography tokens, SharedTopBar/NavPill/CategoryChip/DonutChart sizing |

---

## App Shell

`MainScreen` owns the main `Scaffold`, bottom navigation, drawer, shared top bar, and embedded tab screens.

Bottom tabs: `Рахунки`, `Категорії`, `Операції`, `Бюджет`, `Огляд`. Budget tab may be hidden (`budgetVisible` in `SettingsRepository`).

`SettingsScreen` and `EditCategoriesScreen` are **not** nav destinations — they overlay as full-screen Compose `Box` layers inside `MainScreen`. The `NavGraph` has only two routes: `Main` and `AddTx`.

## Shared Top Bar

`SharedTopBar` displays total balance and page-specific action buttons:
- Accounts: add account
- Categories: toggle category compactness
- Transactions: search
- Budget: budget settings
- Overview/other: settings (⚙)

## Settings Screen

Full-screen Compose overlay. Internal pages: `MAIN`, `THEME`, `CURRENCY`, `ABOUT`.

Settings persisted via DataStore (`SettingsRepository` → `AppSettings`):
- `themeMode`: `SYSTEM` / `LIGHT` / `DARK`
- `accentColor`: hex string or empty (system default)
- `homeScreen`: `HomeScreenTab` enum
- `budgetVisible`: Boolean
- `loginProtectionEnabled`: Boolean → `BiometricPrompt` after 30s background
- `notificationsEnabled`: Boolean → `NotificationWorker` via WorkManager
- `currency`, `numberFormat`: String

`formatMoneyWithSettings()` is in `SettingsSubScreens.kt`. Not yet applied globally — `formatMoney()` in `MainScreen.kt` remains the default.

## Navigation And Gestures

Main paging is controlled programmatically via `HorizontalPager`. Horizontal swipes change month/period (left = next, right = prev). `BackHandler` inside `MainScreen` closes embedded overlays before system back.

### Back Navigation — Handler Priority

Handlers are registered in this order (last registered = highest priority):

```kotlin
BackHandler(enabled = showEditCategories) { showEditCategories = false }  // highest priority
BackHandler(enabled = currentPage != homeTabIndex) { goBack() }
```

- **`showEditCategories = true`** → back closes the Edit Categories overlay (does not exit the app, does not navigate tabs).
- **On any non-home tab** → back navigates to the home tab.
- **Already on home tab, no overlays** → system handles (app exits or goes to launcher).

**Rule:** Any new full-screen overlay added to `MainScreen` must register its own `BackHandler` *before* the tab-navigation handler so it takes priority. Pattern: `BackHandler(enabled = showXyz) { showXyz = false }`.

```kotlin
val homeTabIndex = activeTabs.indexOfFirst { it.label == settings.homeScreen.label }.takeIf { it >= 0 } ?: 0
```

Right-edge swipe (`onRightEdge = goBack`) follows the same logic. Falls back to `0` if the configured home tab is hidden.

### Swipe Sensitivity

```
SWIPE_THRESHOLD = 130f px   // deliberately high to avoid accidental flips
```

Fires only when `|deltaX| > |deltaY| * 1.7` (predominantly horizontal).

## Shared Month State

`SelectedMonthRepository` — single source of truth for selected period (`StateFlow<AppMonth>`). All period-aware screens observe it. `SharedMonthNavPill` and `SharedTopBar` display and modify it.

`AppMonth` modes: `MONTH`, `TODAY`, `WEEK`, `YEAR`, `ALL`, `DAY`, `RANGE`. `computeRange(AppMonth)` → `Pair<Long, Long>`.

**Rule:** Do not give individual screens their own month state. All navigation goes through this repository.

## Text And Locale

Visible app labels are primarily Ukrainian with some Russian in comments/code. Do not casually rename user-visible labels without product intent.
