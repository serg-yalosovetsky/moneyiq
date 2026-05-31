# ADR Log

## ADR-001: Native Android + Compose

The app is implemented as a native Android app using Kotlin and Jetpack Compose. Continue feature work in Compose unless a platform API requires XML or manifest-level configuration.

## ADR-002: Local-First Persistence

Room is the source of truth for accounts, categories, and transactions. Data access should go through DAOs and repositories rather than ad hoc storage.

## ADR-003: Repositories Own Balance Side Effects

`TransactionRepository` applies and rolls back balance changes when transactions are added, deleted, or updated. UI code must not independently adjust account balances.

## ADR-004: Categories Are Hierarchical

Categories support `parentId`; root categories drive the broad category view and children may be shown or aggregated depending on the screen state.

## ADR-005: 1Money-Like UX Is A Product Constraint

Several layouts intentionally mimic 1Money behavior, especially categories, month navigation, and bottom tabs. Preserve visual similarity when changing these surfaces.

## ADR-006: Context Docs Are Not Runtime Code

`.context` is documentation only. Do not import it, bundle it, or make runtime behavior depend on it.

## ADR-007: Shared Month State Via Repository

`SelectedMonthRepository` is the single source of truth for the selected period. All feature screens observe its `StateFlow<AppMonth>`. Do not give individual screens their own month state. Month navigation (prev/next/jump) must go through this repository.

## ADR-008: CategoryStyleUtil For Icon/Color Suggestion

`util/CategoryStyleUtil.kt` owns the keyword→icon/color mapping. When adding new categories programmatically (seeder, import), always use `suggestCategoryStyle(name, type)` if the icon key is unknown or invalid. Do not hard-code icon key strings outside this utility and `CategoryRepository.seedDefaults()`.

## ADR-009: Settings Are Overlaid, Not Navigated

`SettingsScreen` and `EditCategoriesScreen` are rendered as full-screen Compose overlays inside `MainScreen`, not as separate navigation destinations. This keeps the `NavGraph` minimal (only `Main` and `AddTx` routes). Do not add routes for settings or category editing.

## ADR-010: Shared Calculator/Date Components Live In `ui/components/calculator`

`CalcStateHolder`, `SharedCalcKeypad`, `AmountCalculatorSheet`, `CalcDateSheet`, `AccountPickerSheet`, and related helpers were extracted from `CategorySheets.kt` into `ui/components/calculator/`. Import calculator and date-picker components from `ui.components.calculator`, NOT from `ui.categories`.

## ADR-011: ViewModels Live In Separate Files From Screens

Each screen file owns only composable functions. The `@HiltViewModel` class, `UiState` data class, and supporting data classes live in a dedicated `*ViewModel.kt` file in the same package. This applies to: `BudgetViewModel`, `OverviewViewModel`, `TransactionsListViewModel`, `DataViewModel`.

## ADR-012: Transaction Sheet Composables Split Into Dedicated Files

`TransactionSheets.kt` (deleted 2026-05-29) was split into four focused files in `ui.transactions`:

- `TxSearchScreen.kt` — `TxSearchScreen`, `SearchSectionHeader`, `TypeFilterCard`, `ColoredFilterChip`
- `CategoryPickerSheet.kt` — `CategoryPickerSheet`, `CategoryPickerCell`, `AccountPickerRow`
- `TransferQuickSheet.kt` — `TransferQuickSheet`
- `TransactionDetailSheet.kt` — `TransactionDetailSheet`

All composables use `internal` visibility (same module, same package). They must NOT be declared `private` — `TransactionsListScreen.kt` calls them across file boundaries within the package.

## ADR-018: Reusable Dialog Composables Live In `ui/components/dialogs`

`TextInputDialog` and `ConfirmationDialog` (added 2026-05-29) replace inline `AlertDialog` blocks that were copied across `CategoryFormSheets.kt`, `TransactionDetailSheet.kt`, and similar files.

- **`TextInputDialog`** — `title`, `label`, `initialValue`, `allowDismiss` (when `false`: OK enabled only after typing, Cancel hidden, back press blocked), `confirmText`, `dismissText`, `onConfirm(value)`, `onDismiss`. State lives inside the composable.
- **`ConfirmationDialog`** — `title`, `message`, `icon?` (drawn in error tint), `confirmText`, `dismissText`, `destructive` (error-colored TextButton when `true`), `onConfirm`, `onDismiss`.

Both are `internal` (module-visible). Import from `org.pixelrush.moneyiq.ui.components.dialogs`. Do not re-inline new copies of these patterns.

## ADR-019: Sentry For Crash Reporting

Sentry Android SDK (`sentry-android 7.20.0`) is initialized in `MoneyIQApp.onCreate` before any coroutine work starts. The Sentry Gradle plugin (`io.sentry.android.gradle 4.14.1`) uploads ProGuard mappings and source context automatically on release builds.

- DSN is hardcoded in `MoneyIQApp.kt` (not a secret — it is the public ingest endpoint).
- Auth token for the Gradle plugin lives in `local.properties` (gitignored) and is never committed. CI uses the `SENTRY_AUTH_TOKEN` env var.
- `buildConfig = true` is enabled in `app/build.gradle.kts` so `BuildConfig.DEBUG` and `BuildConfig.VERSION_NAME` are available.
- `environment` is set to `"debug"` or `"production"` based on `BuildConfig.DEBUG`.
- `release` is set to `"moneyiq@${BuildConfig.VERSION_NAME}"`.
- Screenshots, view hierarchy, and user interaction tracing are enabled (`isAttachScreenshot`, `isAttachViewHierarchy`, `isEnableUserInteractionTracing`).
- `tracesSampleRate = 1.0` during development — reduce to `0.2` or lower before high-traffic release.

## ADR-013: Comprehensive Unit Test Suite Exists

The project has a full JUnit4/MockK/Turbine/Robolectric test suite (~196 tests). When adding or changing a repository, ViewModel, or utility class, add tests covering the changed paths. Follow existing patterns: `MainDispatcherRule`, `runTest`, Turbine `.test {}`. Testing dependencies are already configured in `libs.versions.toml` and `app/build.gradle.kts`.

## ADR-014: DataViewModel Injects DAOs Directly (Known Debt)

`DataViewModel` injects `TransactionDao`, `AccountDao`, `CategoryDao` directly rather than through repositories. This is a known gap: `// TODO: Migrate direct DAO access to repository layer`. Do not copy this pattern for new ViewModels.

## ADR-015: Same-Name Subcategory Deduplication In Category Expansion

When computing `expandedChildren` for the double-click expansion strip and inline panel, any child category whose `name.trim().lowercase()` equals the parent's is excluded. This prevents a subcategory "Продукти" from appearing inside its parent "Продукти".

The deduplication is applied at the **display layer only** (`CategoriesGridContent`) — the database retains the child record. It is intentional and silent; no warning is shown to the user.

If two semantically different categories genuinely need the same name under the same parent, they should be distinguished by icon/color rather than name.

## ADR-016: Specific Icon Keys For Root Categories

Root category icon keys must be semantically specific — not generic fallbacks. Correct keys (current, after migration 25):

| Category       | Icon key      | Material icon           | Color      |
|----------------|---------------|-------------------------|------------|
| Продукти       | `grocery`     | `ShoppingBasket`        | `#4AAFE8`  |
| Ресторація     | `restaurant`  | `Restaurant`            | `#4659BE`  |
| Дозвілля       | `theater`     | `TheaterComedy`         | `#F73579`  |
| Транспорт      | `bus`         | `DirectionsBus`         | `#FFA834`  |
| Здоров'я       | `volunteer`   | `HealthAndSafety`       | `#48B456`  |
| Подарунки      | `gift`        | `CardGiftcard`          | `#F34B4D`  |
| Сім'я          | `family`      | `FamilyRestroom`        | `#7A48F2`  |
| Покупки        | `shopping`    | `ShoppingCart`          | `#7B5947`  |
| Робота         | `work`        | `Work`                  | `#1565C0`  |
| Таксі          | `taxi`        | `LocalTaxi`             | `#FDD835`  |
| Авто           | `car`         | `DirectionsCar`         | `#FF7043`  |
| Паркінг        | `parking`     | `LocalParking`          | `#78909C`  |
| АЗС/Пальне     | `gas_station` | `LocalGasStation`       | `#FF8F00`  |
| Оренда         | `key`         | `Key`                   | `#9C27B0`  |
| Фриланс        | `laptop`      | `Laptop`                | `#26A69A`  |
| Аптека         | `pharmacy`    | `Medication`            | `#43A047`  |
| Лікар/Медицина | `doctor`      | `MedicalServices`       | `#D81B60`  |
| Спорт          | `sports`      | `DirectionsRun`         | `#F44336`  |
| Кіно (child)   | `movie`       | `Movie`                 | `#9C27B0`  |
| Gaming (child) | `gaming`      | `SportsEsports`         | `#607D8B`  |
| Зв'язок        | `phone`       | `PhoneAndroid`          | `#3F51B5`  |
| Інтернет       | `wifi`        | `Wifi`                  | `#00BCD4`  |
| Фінанси        | `money`       | `AttachMoney`           | `#F9A825`  |
| Комуналка      | `home`        | `Home`                  | `#546E7A`  |
| Food delivery  | `delivery`    | `LocalShipping`         | `#FF6F00`  |
| Ресторани      | `restaurant`  | `Restaurant`            | `#E53935`  |
| Кафе           | `coffee`      | `LocalCafe`             | `#795548`  |
| Штрафи         | `gavel`       | `Gavel`                 | `#BF360C`  |
| Проценти/ПДВ   | `percent`     | `Percent`               | `#F9A825`  |

**Icon distinctness rule** (migration 19→20, 2026-05-31): Every commonly-used subcategory has a distinct icon. Аптека→`pharmacy` (Medication pill), Лікар→`doctor` (MedicalServices cross), Спорт→`sports` (DirectionsRun — running figure). Паркінг→`parking` separate from Авто (`car`). Оренда→`key`. Фриланс→`laptop`.

**Volunteer icon** (2026-05-31): `volunteer` key uses `Icons.Outlined.HealthAndSafety` (shield with cross/heart — clearly "health & safety"). Changed from `FavoriteBorder` (generic heart) and before that from `VolunteerActivism`.

**Sports icon** (2026-05-31): `sports` key uses `Icons.Outlined.DirectionsRun` (running figure). Changed from `FitnessCenter` (dumbbell) to better distinguish from "gym" and to be more universally recognisable as physical activity.

**Transport icon split** (migration 17→18): `bus`/`DirectionsBus` for public transport vs `car`/`DirectionsCar` for personal vehicle.

Available leisure sub-icons: `theater` (Дозвілля), `movie` (Кіно), `gaming`, `celebration`, `spa`, `ticket`.

These are registered in `CategoryIcons.kt` (`CATEGORY_ICONS_LIST`) and mapped in `CategoryStyleUtil.kt` (`iconColorMap`). Data migrations 5→25 backfill existing DB rows. The old `health` key is kept as a legacy entry in `iconColorMap` only — do not use it for new categories or seeds.

Migration 12→13: Deletes EXPENSE categories named "Фінанс*".
Migration 13→14: Root category color palette refresh.
Migration 14→15: Subcategory icon fix (delivery/coffee/restaurant, parentId guard, TRIM+LIKE).
Migration 15→16: `money` icon safety net for remaining "Фінанс*".
Migration 16→17: Subcategory icons via exact name matching.
Migration 17→18: `bus` for root Транспорт.
Migration 18→19: Fixes placeholder `category` icons for money/celebration/theater.
Migration 19→20: Distinct icons for pharmacy/doctor/parking/gas_station/key/laptop/sports.
Migration 20→21: Structural — adds `currencyCode TEXT NOT NULL DEFAULT 'UAH'` to categories.
Migration 21→22: Bulk icon fixes for existing rows: delivery/clothes/school/devices/doctor/sports/parking/percent/gavel.
Migration 22→23: Utilities icons: home/phone/wifi.
Migration 23→24: Здоров'я root `volunteer`/`#48B456`; Спорт→`sports`; remaining `health` roots→`volunteer`.
Migration 24→25: **Unconditional** fix for Дозвілля (`theater`/`#F73579`) and Розваги (`celebration`/`#FF6D00`) — no `icon = 'category'` guard because imported data via REPLACE strategy can silently overwrite prior migration results.

## ADR-017: Large Screen Files Split Into Companion Files

Screen files that exceeded ~600 lines were split into a main file + companion file(s) in the same package (2026-05-29). Each companion holds composables that the main screen delegates to but that do not need to be top-level nav destinations.

Current companion file map:

| Package | Main file | Companion file(s) |
|---|---|---|
| `ui.accounts` | `AccountSheets.kt` | `AccountPickerSheets.kt` |
| `ui.budget` | `BudgetScreen.kt` | `BudgetSheets.kt` |
| `ui.categories` | `CategoriesScreen.kt` | `CategoriesWidgets.kt` |
| `ui.categories` | `CategorySheets.kt` | `CategoryFormSheets.kt` |
| `ui.categories` | `CategoriesScreen.kt` | `EditCategoriesScreen.kt` |
| `ui.data` | `DataScreen.kt` | `DataWidgets.kt` |
| `ui.overview` | `OverviewScreen.kt` | `OverviewSheets.kt` |
| `ui.settings` | `SettingsScreen.kt` | `SettingsSubScreens.kt` |
| `ui.transactions` | `TransactionsListScreen.kt` | `TxSearchScreen.kt`, `CategoryPickerSheet.kt`, `TransferQuickSheet.kt`, `TransactionDetailSheet.kt` |

**Rule:** Composables shared across files in the same package must be `internal`, not `private`. `private` is reserved for helpers used exclusively within the same file. Constants (`val`) shared across companion files must also be `internal`.

## ADR-020: Icon/Color Fixes For Existing Categories Go In Room Migrations

`CategoryStyleUtil` auto-suggest and `CategoryRepository.seedDefaults()` only affect NEW or freshly installed categories. To fix icon/color on categories already stored in the user's DB, add a `MIGRATION_N_(N+1)` in `AppDatabase.kt` with a targeted SQL `UPDATE`.

Pattern:
```kotlin
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'home', colorHex = '#546E7A' WHERE name IN ('Комунальні', 'Комуналка')")
    }
}
```

Also update: DB `version` in `@Database`, `ALL_MIGRATIONS` array in `AppDatabase.kt`, seed in `CategoryRepository`, auto-suggest rule in `CategoryStyleUtil`, and the icon table in `UI_CONTRACTS.md`.

**REPLACE strategy risk:** `insertCategories` uses `@Insert(onConflict = OnConflictStrategy.REPLACE)`. Importing a backup or syncing data that has `icon = 'category'` silently overwrites correctly migrated icons — subsequent app launches do NOT re-run already-completed migrations.

**Rule for import-resilient fixes:** When a named category could be re-imported with a wrong icon, **omit the `AND icon = 'category'` guard**. Use an unconditional `UPDATE ... WHERE LOWER(TRIM(name)) = 'xxx'` (see migration 24→25: Дозвілля/Розваги).

## ADR-021: LazyColumn Spacing Via `verticalArrangement`, Not Item Padding

Never use `Modifier.height(N).padding(bottom = K)` on chip row items inside a `LazyColumn`. In Compose, `padding` inside a `height` constraint subtracts from the content area — 12dp of bottom padding on a 136dp row leaves only 124dp for content, clipping the spending text at the bottom of each chip.

**Rule:** Use `LazyColumn(verticalArrangement = Arrangement.spacedBy(CATEGORY_VERTICAL_GAP))` for inter-row spacing. Individual row items use only `Modifier.height(chipHeight)` — no bottom padding.

This applies to `CategoriesGridContent` and any future screen that places fixed-height chip rows in a `LazyColumn`.

## ADR-022: Category Filter In Transactions Includes Children

When a root category is selected as a filter in `TransactionsListScreen`, the filter expands to include all direct child categories. This means selecting "Ресторація" also shows transactions from "Glovo", "Ресторани", "Кафе".

Implementation in `filteredTransactions` inside `TransactionsListScreen.kt`:
```kotlin
val allCats = state.expenseCategories + state.incomeCategories
val expandedCatIds = if (filterCategoryIds.isEmpty()) emptySet<Long>() else {
    filterCategoryIds + allCats.filter { it.parentId in filterCategoryIds }.map { it.id }.toSet()
}
```
`remember` keys include `state.expenseCategories` and `state.incomeCategories` to recompute when categories change.

**Rule:** Do not change this to a DAO-level filter without ensuring child IDs are also fetched. The expansion is one level deep (grandchildren are not included).

## ADR-023: No `graphicsLayer { clip = false }` On Dialog Siblings

`graphicsLayer { clip = false }` forces a hardware layer on the composable. Android composites such layers above `Dialog` windows regardless of Compose z-ordering — any composable using this modifier will render visually above `CategoryActionSheet` or similar dialogs, breaking the scrim.

**Rule:** Do not apply `graphicsLayer { clip = false }` (or any `graphicsLayer { ... }` block that sets `clip = false`) to composables that are siblings of, or ancestors of composables that launch, `Dialog` calls. If content genuinely needs to overflow its clip boundary alongside dialogs, place it in a full-screen root-level overlay `Box` instead.

## ADR-024: AddCategoryChip Is Inside DonutChart

The `+` (add category) chip is placed **inside** the `DonutChart` composable, positioned at `Alignment.BottomCenter` with `padding(bottom=28.dp)`. It is NOT a standalone `LazyColumn` item.

`DonutChart` signature: `onAdd: (() -> Unit)? = null` — when non-null, the chip is rendered inside the chart's `Box` overlay.

Current dimensions: `Column(width=64dp)`, circle `Box(44dp)`, icon `18dp`.

`DONUT_SECTION_HEIGHT = CHIP_HEIGHT * 2 + CATEGORY_VERTICAL_GAP` ensures the donut + chip fit exactly in the space flanked by 2 mid-column chips on each side.

## ADR-026: BudgetViewModel `totalAmount` Counts All Spending, Not Just Budgeted

`BudgetSectionData.totalAmount` is the sum of **all** actual spending for the period (`expRows.sumOf { it.amount }`), regardless of whether a category has a budget set.

Previously this was filtered to `expRows.filter { it.category.budgetAmount > 0 }.sumOf { ... }`, which caused "витрачено 0.00" on the Budget screen whenever no category had a budget — even with real transactions.

`totalAmount` drives:
- "витрачено N" label in `BudgetSectionCard`
- `expenseTotal` passed to `IncomeBudgetBar` for "Доступно в бюджеті" calculation

**Rule:** Do not re-add the `budgetAmount > 0` filter. The "в бюджеті" figure uses `totalBudget` (sum of budget amounts); the "витрачено" figure uses `totalAmount` (sum of actual spending). These are intentionally separate.

## ADR-027: MonoFlow Sync Uses BackupSerializer JSON, Not CSV

`MonoFlowSyncWorker` calls `GET $url/api/sync?since=$lastSyncMs` (Bearer token auth) and deserializes the response with `BackupSerializer.deserialize(json)`. The `/export/flow.csv` endpoint is a separate export for the Flow app and is not consumed by MoneyIQ.

The JSON format is the same as the manual backup:
```json
{
  "version": 1, "exportDate": ..., "app": "MoneyIQ",
  "accounts":     [ { "id", "name", "type", "balance", "currency", ... } ],
  "categories":   [ { "id", "name", "type", "colorHex", "icon", "budgetAmount", "parentId", ... } ],
  "transactions": [ { "id", "type", "amount", "accountId", "toAccountId", "categoryId", "note", "date" } ]
}
```

`type` on transactions is `EXPENSE | INCOME | TRANSFER | BORROW | LEND | REPAY`. `toAccountId` is a valid `Long` for transfers, `null` otherwise. The server is responsible for detecting PayPal/Revolut/ATM rows and setting `type=TRANSFER` with the appropriate `toAccountId`.

Worker inserts via `insertAccounts` / `insertCategories` / `insertTransactions` (REPLACE strategy — MERGE by `id`). Existing data is not deleted.

## ADR-028: Expanded Subcategory Strip Is Fused With Its Parent Row (2026-05-31)

`ExpandedCategoryStrip` (double-click expansion) is always rendered **in the same LazyColumn item** as the row that contains the expanded chip. The chip row and its strip are wrapped in a `Column` inside one `item { }` block — this eliminates the 8dp `Arrangement.spacedBy` gap that would otherwise appear between them.

`ExpandedCategoryStrip` has an `inline: Boolean = false` parameter:
- `inline = true`: no Card wrapper; thin `HorizontalDivider` (parent color 18% alpha) + plain `Column` with parent color 7% alpha background — appears flush with the chip row above
- `inline = false` (legacy / non-fused): wrapped in a `Card(RoundedCornerShape(16dp))`

The expanded chip uses `flatBottom = true` on `CategoryChip` (bottom corners 0dp instead of 12dp) so the chip and strip form one seamless visual block.

**Rule:** All three strip insertion points (`topRow`, `mid_section`, `extCats`) use `inline = true`. Do not add a separate `LazyColumn` item for the strip — doing so reintroduces the gap.

## ADR-029: Budget Savings Forecast Uses Linear Day Extrapolation (2026-05-31)

`BudgetViewModel` exposes `daysPassed: Int` in `BudgetUiState`. `SavingsSectionCard` uses it to project savings:

```
projectedExpenses = actualExpenses / daysPassed * daysInMonth
projectedSavings  = incomeBudget - projectedExpenses
```

Forecast is shown only when `daysPassed > 0 && daysInMonth > daysPassed && expenseTotal > 0` (i.e., current month with some spend data). Past/future months show only actual savings. This is intentionally simple linear extrapolation — do not replace with complex models without a product decision.

## ADR-030: EditCategoriesScreen Reuses CategoriesGridContent Without Badges (2026-05-31, updated 2026-05-31)

`EditCategoriesScreen` lives in `CategoryFormSheets.kt` (not a separate file). It is the full-screen "Редагувати категорії / Редагувати субкатегорії" overlay and reuses `CategoriesGridContent` directly.

Key constraints:
- `childCounts = emptyMap()` is always passed — suppresses all +N subcategory badges.
- Chip tap and long-press both open `CategoryFormSheet` for editing (not `QuickExpenseSheet`).
- The "Субкатегорії" `TextButton` in the top bar toggles `showSubcategories`. **Title is context-sensitive**: `"Редагувати субкатегорії"` when `showSubcategories = true`, `"Редагувати категорії"` otherwise.
- Subcategory mode uses **the same orbital layout** as category mode (donut centred, chips surrounding). `showChildBadge = !showSubcategories` throughout `CategoriesGridContent` — badges hidden for subcategories.
- `topRow/midLeft/midRight/extCats` in `CategoriesGridContent` are now computed unconditionally from `display` (no `if (!showSubcategories)` guard). The if/else branch that rendered a full-width donut for subcategory mode was removed.
- The screen receives all data as parameters from `MainScreen` (no own ViewModel); mutations go through callbacks.

## ADR-031: Budget Savings Header Shows Budget-Based Savings, Not Cash-Flow Savings

`SavingsSectionCard` header shows `incomeBudget − expenseTotal` (when a budget is set), not `incomeTotal − expenseTotal`.

**Reason:** `incomeTotal` is often 0 for most of the month (salary arrives once), making `incomeTotal − expenseTotal` a large misleading negative. `incomeBudget − expenseTotal` ("how much of your planned income remains after expenses") is the number users expect to see — it matches the "Доступно в бюджеті" bar at the bottom.

**Internal name split:**
- `realSavings = incomeTotal − expenseTotal` — used only for the "збережено" subtitle label, and only when `incomeTotal > 0`
- `actualSavings = if (incomeBudget > 0) incomeBudget − expenseTotal else realSavings` — drives the header

**Rule:** Do not revert to `incomeTotal − expenseTotal` as the header without also replacing the bottom bar "Доступно в бюджеті" (which shows the same budget-based number). They must agree.

## ADR-032: CategoryFormSheet Title And Subcategory Section Are Context-Sensitive

`CategoryFormSheet` title adapts to category kind:

| Condition | Title |
|---|---|
| `existing == null && forParentId != null` | "Нова субкатегорія" |
| `existing == null` | "Нова категорія" |
| `existing.parentId != null` | "Субкатегорія" |
| `existing.parentId == null` | "Категорія" |

"Підкатегорії" section is rendered **only for root categories** (`existing != null && existing.parentId == null`). It shows existing children as icon rows and a "Додати підкатегорію" button (only when `onAddSubcategory != null`). Child categories never show this section.

New subcategory creation flow: caller sets `addSubcategoryTo = parent`, opens a new `CategoryFormSheet(forParentId = parent.id)`, on save calls `viewModel.add(..., parentId = parent.id)`. Single-parent uniqueness is enforced at the DB level by the `parentId` field — no UI-level deduplication needed.

**Rule:** Do not add a nested subcategory level (subcategory of a subcategory). One level of hierarchy is the product constraint.

**Rule:** Do not add nested subcategory levels (subcategory of a subcategory). One level of hierarchy is the product constraint.

## ADR-025: Overview List Falls Back To Transactions When No Categories

`OverviewScreen` renders the list below the stats row with this priority:

1. Category rows (spending > 0 for mode + period)
2. Transaction rows via `TransactionListItem` (when categories list is empty but transactions exist)
3. "Немає категорій" empty state (both empty)

**Reason:** Income/expense header totals are computed from all transactions of that type, regardless of category assignment. Without the fallback, a non-zero total header with an empty list is contradictory and confusing to the user.

**`OverviewUiState`** carries `transactions: List<TransactionWithDetails>` populated from `monoTx` (mode-filtered transactions for the period).

**Rule:** Do not remove the transaction fallback without also fixing the header total to exclude uncategorised transactions. Tapping a fallback transaction row is intentionally a no-op — if navigation to `TransactionDetailSheet` is added later, ensure the CategoryDetailSheet bottom sheet state is not inadvertently triggered.

## ADR-033: DonutChart Switches To Subcategory View When Parent Is Expanded (2026-05-31)

When a root category with children is double-tapped (subcategory expansion strip is shown), the `DonutChart` in the mid-section must show only the **subcategory spending breakdown** instead of the full category list.

**Implementation** (in `CategoriesGridContent`, just before the DonutChart call):

```kotlin
val hasExpandedStrip = expandedCat != null && expandedChildren.isNotEmpty()
val donutCats    = if (hasExpandedStrip) expandedChildren else categories
val donutExpense = if (hasExpandedStrip && selectedTab == 0)
    expandedChildren.sumOf { spending[it.id] ?: 0.0 } else totalExpense
val donutIncome  = if (hasExpandedStrip && selectedTab == 1)
    expandedChildren.sumOf { spending[it.id] ?: 0.0 } else totalIncome
DonutChart(
    categories   = donutCats,
    spending     = spending,
    totalExpense = donutExpense,
    totalIncome  = donutIncome,
    ...
)
```

`expandedChildren` is already computed for the expansion strip (same-name-dedup applied). The donut uses the same `spending` map as the chips — no separate data fetch needed.

**Visual contract:** When subcategory strip is visible, the donut ring shows slices for each child category proportional to their individual spending. The center label totals reflect only child spending for the active tab. When the strip is dismissed, the donut reverts to showing all root categories.

**Rule:** Do not pass the full `categories` list to DonutChart when `hasExpandedStrip == true`. Passing all categories while showing a strip makes the donut misleading — the expanded parent's slice stays dominant and the children are invisible in the ring.

## ADR-034: Overview Screen Uses `categoryIconFor` From `CategoryIcons.kt` (2026-05-31)

`OverviewScreen.kt` previously had its own local `iconVectorFor()` function with only 13 icon mappings. Any icon name not in that list fell back to `Icons.Default.Category` — causing wrong icons for "Переказ" (transfer), "Доставка" (delivery), "AliExpress" (aliexpress), "Електроніка" (devices), and others.

**Fix:** Removed `iconVectorFor()` from `OverviewScreen.kt`. Now calls `categoryIconFor(iconName)` from `org.pixelrush.moneyiq.ui.categories.CategoryIcons`, which covers all 48 icon keys. `categoryIconFor` was changed from `internal` to `public` to allow cross-package access.

**Rule:** Do not add a new local icon mapper in any screen file. Always call `categoryIconFor()` from `CategoryIcons.kt`. If a new icon key is added to the app, add it to `CATEGORY_ICONS_LIST` in `CategoryIcons.kt` — that is the single source of truth for all icon name → vector mappings.
