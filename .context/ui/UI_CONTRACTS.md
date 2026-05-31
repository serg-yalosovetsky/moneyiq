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

**Drawer:** `ModalNavigationDrawer` with `gesturesEnabled = drawerState.isOpen` — gestures disabled when closed (prevents accidental open), enabled when open (allows swipe-to-close and scrim tap to dismiss). Open via avatar tap or left-edge swipe (`edgeSwipe`). Close via scrim tap, back button (`BackHandler`), or `AppDrawerContent.onClose`.

`SettingsScreen` and `EditCategoriesScreen` are **not** nav destinations — they overlay as full-screen Compose `Box` layers inside `MainScreen`. The `NavGraph` has only two routes: `Main` and `AddTx`.

## Shared Top Bar

`SharedTopBar` (`MainScreen.kt`) — Row layout, left avatar / center balance / right action.

**Left:** 44dp circle (`surfaceVariant` bg), `Icons.Filled.Person` (22dp, `onSurfaceVariant`). Clickable → drawer.

**Center:** Column — "Всі рахунки" (`labelSmall`, `onSurface 55%`) + balance (`headlineMedium + Bold`, `onSurface`, ellipsis).

**Right action** (context-aware by page):
| Page | Icon | Action |
|---|---|---|
| 0 Accounts | `Add` | onPlusClick |
| 1 Categories | `Outlined.Edit` | onEditCategories |
| 2 Transactions | `Search` | onSearchTx |
| 3 Budget | `Outlined.Speed` | onBudgetSettings |
| other | `Outlined.Settings` | onSettings |

## Shared Month Nav Pill

`SharedMonthNavPill` (`SharedMonthPill.kt`) — Row with left/right arrows + center pill.

**Arrows:** `Icons.AutoMirrored.Filled.KeyboardArrowLeft/Right` (30dp), tint = `pillColor`. Row horizontal padding = 10dp.

**Pill colors:**
```kotlin
PILL_ACCENT  = Color(0xFFD81B60)   // crimson — past/future months
PILL_CURRENT = Color(0xFF4B6BEF)   // indigo-blue — current calendar month
pillColor = if (isCurrentMonth) PILL_CURRENT else PILL_ACCENT
```

**Pill surface:** `RoundedCornerShape(50dp)`, `color = pillColor.copy(alpha = 0.12f)`. Padding: `start=6dp, end=14dp, top=6dp, bottom=6dp`.

**Pill content:**
- Badge: `Surface(RoundedCornerShape(6dp), border=1.5dp pillColor)`, text `labelSmall + Bold`, `color = pillColor`, padding `horizontal=6dp, vertical=2dp`
- Label: `bodyMedium + SemiBold`, `color = pillColor`
- Dropdown arrow: `Icons.Default.KeyboardArrowDown` (26dp), `pillColor.copy(alpha=0.7f)`

**Month text:** `MONTH_NAMES_UA_FULL[month].uppercase()` for `PeriodMode.MONTH` → "ЧЕРВЕНЬ 2026". Other modes use Title Case from `MONTH_NAMES_UA_FULL` unchanged.

**Click:** opens `PeriodSelectorSheet` (7 period modes: Month, Today, Week, Year, All, Day, Range).

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
BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } } // highest priority
BackHandler(enabled = showEditCategories) { showEditCategories = false }
BackHandler(enabled = currentPage != homeTabIndex) { goBack() }
```

- **`drawerState.isOpen`** → back closes the app drawer (highest priority — nothing else should intercept back when drawer is open).
- **`showEditCategories = true`** → back closes the Edit Categories overlay.
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

## SharedMonthNavPill — Current Month Color

Two pill accent colors are defined in `SharedMonthPill.kt`:

```kotlin
private val PILL_ACCENT  = Color(0xFFD81B60)   // crimson — past/future months
private val PILL_CURRENT = Color(0xFF4B6BEF)   // indigo-blue — current calendar month
```

`isCurrentMonth = appMonth.mode == PeriodMode.MONTH && appMonth.month == today.month && appMonth.year == today.year`

When `isCurrentMonth` is true, `pillColor = PILL_CURRENT`; otherwise `pillColor = PILL_ACCENT`. The pill background (`pillColor.copy(alpha=0.12f)`), badge border+text, label text, and dropdown arrow all use `pillColor` — no element uses a hardcoded accent. The badge is a border-only rounded rect (not a filled circle).

**Rule:** Only `PeriodMode.MONTH` pointing at the current calendar month gets the blue color. All other modes (TODAY, WEEK, YEAR, ALL, DAY, RANGE) and past/future months keep the crimson `PILL_ACCENT`.

## Text And Locale

Visible app labels are primarily Ukrainian with some Russian in comments/code. Do not casually rename user-visible labels without product intent.
