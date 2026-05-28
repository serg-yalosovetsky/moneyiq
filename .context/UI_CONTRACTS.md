# UI Contracts

## App Shell

`MainScreen` owns the main `Scaffold`, bottom navigation, drawer, shared top bar, and embedded tab screens.

Bottom tabs:

- `Рахунки`
- `Категорії`
- `Операції`
- `Бюджет`
- `Огляд`

Budget tab may be hidden by settings (`budgetVisible` in `SettingsRepository`).

`SettingsScreen` and `EditCategoriesScreen` are NOT nav destinations; they overlay as full-screen Compose `Box` layers inside `MainScreen`.

## Shared Top Bar

The shared top bar (`SharedTopBar`) displays total balance and page-specific action buttons.

- Accounts: add account
- Categories: toggle category compactness
- Transactions: search
- Budget: budget settings
- Overview/other: settings (⚙)

Settings icon appears on tabs 2-4 (Categories, Transactions, Budget). Accounts tab has its own add-account action.

## Categories Screen

The categories screen (`CategoriesScreen.kt`) is a high-sensitivity 1Money-like surface.

### Chip Dimensions (CRITICAL — do not change without audit)

```
CHIP_WIDTH           = 82.dp
CHIP_HEIGHT          = 124.dp   // must stay ≥120dp; clipping hides spending text
CHIP_CIRCLE_SIZE     = 48.dp
CHIP_WIDTH_COMPACT   = 70.dp
CHIP_HEIGHT_COMPACT  = 108.dp
CHIP_CIRCLE_COMPACT  = 40.dp
```

Chip content height budget: 28dp (name) + 11dp (budget or spacer) + 3dp + 48dp (icon circle) + 3dp + 12dp (spending) = 105dp. CHIP_HEIGHT=124dp gives 124-4(padding)=120dp available, with 15dp margin for sp-based text scaling.

If you reduce CHIP_HEIGHT below 120dp: the spending amount text at the bottom is clipped at any font scale > 1.0.

### Chip Visual Logic

- Icon circle is solid (white icon): when `spending > 0`
- Icon circle is tinted (light alpha 0.13, colored icon): when `spending == 0`
- Budget text shows only when `category.budgetAmount > 0`; otherwise a Spacer of equal height preserves alignment
- Spending text color: category color if `spending > 0`, grey if `spending == 0`

### Category Aggregation (`effectiveSpending`)

When `showSubcategories = false` (default), spending of child categories is summed into their parent IDs:

```kotlin
val result = spending.toMutableMap()
allCategoriesForTab.filter { it.parentId != null }.forEach { child ->
    child.parentId?.let { pid ->
        result[pid] = (result[pid] ?: 0.0) + (spending[child.id] ?: 0.0)
    }
}
```

This happens inside `CategoriesGridContent`, NOT in the ViewModel.

### Donut Chart

- Always shows both expense (crimson) and income (teal) rings simultaneously
- Donut center taps toggle `selectedTab` (EXPENSE/INCOME), which filters the chip grid below
- `DONUT_SECTION_HEIGHT = 360.dp`; `SIDE_COLUMN_WIDTH = 90.dp`
- Side columns contain categories 5-8; grid top row shows categories 1-4; categories 9+ appear below

### Auto-Suggest Icons

When creating a new category (existing == null), `CategoryFormSheet` runs a `LaunchedEffect(name)` that calls `suggestCategoryStyle(name, type)` if name ≥ 3 chars AND user hasn't manually picked an icon (iconKey == "category"). Once the user touches the icon picker, auto-suggest stops firing.

## Navigation And Gestures

- Main horizontal paging is controlled programmatically via `HorizontalPager`.
- Horizontal swipes inside feature screens change month/period (swipe left = next month, swipe right = prev month).
- Edge swipes handled by `MainScreen` for drawer/back behavior.
- `BackHandler` inside `MainScreen` handles closing embedded overlays (Settings, EditCategories) before system back.

## Shared Month State

`SelectedMonthRepository` holds the selected period as a `StateFlow<AppMonth>`. All period-aware screens (Categories, Budget, Overview, Transactions) observe this shared flow. `SharedMonthNavPill` and `SharedTopBar` display and modify it. The pill shows month label and a "days" badge.

## Text And Locale

Visible app labels are primarily Ukrainian with some existing Russian comments/code text. Do not casually rename user-visible labels without product intent.
