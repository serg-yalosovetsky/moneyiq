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

Root category icon keys must be semantically specific — not generic fallbacks. Correct keys (current, after migration 12):

| Category       | Icon key      | Material icon           | Color      |
|----------------|---------------|-------------------------|------------|
| Продукти       | `grocery`     | `ShoppingBasket`        | `#03A9F4`  |
| Ресторація     | `restaurant`  | `Restaurant`            | `#5C6BC0`  |
| Дозвілля       | `theater`     | `TheaterComedy`         | `#7B1FA2`  |
| Транспорт      | `car`         | `DirectionsCar`         | `#00897B`  |
| Здоров'я       | `volunteer`   | `VolunteerActivism`     | `#4CAF50`  |
| Подарунки      | `gift`        | `CardGiftcard`          | `#F44336`  |
| Сім'я          | `family`      | `FamilyRestroom`        | `#673AB7`  |
| Покупки        | `shopping`    | `ShoppingCart`          | `#795548`  |
| Робота         | `work`        | `Work`                  | `#1565C0`  |
| Таксі          | `taxi`        | `LocalTaxi`             | `#FDD835`  |
| АЗС/Пальне     | `gas_station` | `LocalGasStation`       | `#FF8F00`  |
| Кіно (child)   | `movie`       | `Movie`                 | `#9C27B0`  |
| Gaming (child) | `gaming`      | `SportsEsports`         | `#607D8B`  |
| Зв'язок        | `phone`       | `PhoneAndroid`          | `#3F51B5`  |
| Інтернет       | `wifi`        | `Wifi`                  | `#00BCD4`  |
| Фінанси        | `money`       | `AccountBalance`        | `#F9A825`  |
| Комуналка      | `home`        | `Home`                  | `#546E7A`  |
| Food delivery  | `delivery`    | `LocalShipping`         | `#FF6F00`  |
| Ресторани      | `restaurant`  | `Restaurant`            | `#E53935`  |
| Кафе           | `coffee`      | `LocalCafe`             | `#795548`  |

Available leisure sub-icons: `theater` (Дозвілля), `movie` (Кіно), `gaming`, `celebration`, `spa`, `ticket`.

These are registered in `CategoryIcons.kt` (`CATEGORY_ICONS_LIST`) and mapped in `CategoryStyleUtil.kt` (`iconColorMap`). Data migrations 5→13 backfill existing DB rows. Do not reuse old generic keys (`music`/`health`) for broad root categories.

## ADR-017: Large Screen Files Split Into Companion Files

Screen files that exceeded ~600 lines were split into a main file + companion file(s) in the same package (2026-05-29). Each companion holds composables that the main screen delegates to but that do not need to be top-level nav destinations.

Current companion file map:

| Package | Main file | Companion file(s) |
|---|---|---|
| `ui.accounts` | `AccountSheets.kt` | `AccountPickerSheets.kt` |
| `ui.budget` | `BudgetScreen.kt` | `BudgetSheets.kt` |
| `ui.categories` | `CategoriesScreen.kt` | `CategoriesWidgets.kt` |
| `ui.categories` | `CategorySheets.kt` | `CategoryFormSheets.kt` |
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

Also update: DB `version` in `@Database`, `addMigrations(...)` in `DatabaseModule.kt`, seed in `CategoryRepository`, auto-suggest rule in `CategoryStyleUtil`, and the icon table in `UI_CONTRACTS.md`.

## ADR-019: No `graphicsLayer { clip = false }` On Dialog Siblings

`graphicsLayer { clip = false }` forces a hardware layer on the composable. Android composites such layers above `Dialog` windows regardless of Compose z-ordering — any composable using this modifier will render visually above `CategoryActionSheet` or similar dialogs, breaking the scrim.

**Rule:** Do not apply `graphicsLayer { clip = false }` (or any `graphicsLayer { ... }` block that sets `clip = false`) to composables that are siblings of, or ancestors of composables that launch, `Dialog` calls. If content genuinely needs to overflow its clip boundary alongside dialogs, place it in a full-screen root-level overlay `Box` instead.
