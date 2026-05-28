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

`SettingsScreen` and `EditCategoriesScreen` are NOT nav destinations; they overlay as full-screen Compose `Box` layers inside `MainScreen`. The `NavGraph` has only two routes: `Main` and `AddTx`.

Budget tab visibility is driven by `budgetVisible` from `SettingsRepository`; `activeTabs` is recomputed and the pager is rebuilt immediately.

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

Chip content height budget: name (28–40dp, wrap) + 11dp (budget or spacer) + 3dp + 48dp (icon circle) + 3dp + 12dp (spending) ≈ 105–117dp. CHIP_HEIGHT=124dp gives 120dp available after 4dp padding — sufficient for 2-line names at normal font scale.

The chip name `Box` uses `heightIn(min=28.dp, max=40.dp)` (compact: `min=22.dp, max=32.dp`), not a fixed height. This allows long names ("Ресторації") to wrap to 2 lines without truncation. For 1-line names the box collapses to its natural text height, keeping the layout compact.

If you reduce CHIP_HEIGHT below 120dp: the spending amount text at the bottom is clipped at any font scale > 1.0.

### Chip Visual Logic

- Icon circle is solid (white icon): when `spending > 0`
- Icon circle is tinted (light alpha 0.13, colored icon): when `spending == 0`
- **Budget text** (above icon): shown only when `budgetAmount > 0 && spending == 0`; otherwise Spacer of equal height preserves alignment
- **Budget badge** (pill on icon top-right): shown when `showChildBadge && budgetAmount > 0 && spending > 0`; displays `"${budgetAmount.toInt()} ₴"` in a `RoundedCornerShape(50)` pill with `primary` background — replaces the old `+childCount` badge
- Spending text color: category color if `spending > 0`, grey if `spending == 0`

### CategoryActionSheet Data

The action sheet uses **effective spending** (children aggregated into parent), not raw category spending:

```kotlin
val catSpending = effectiveSpending[cat.id] ?: 0.0
val catTxCount  = allCategoriesForTab
    .filter { it.id == cat.id || it.parentId == cat.id }
    .sumOf { state.monthTxCounts[it.id] ?: 0 }
```

This ensures the sheet's spending amount and operation count match what the chip displays.

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

### Donut Chart Layout

- Always shows both expense (crimson) and income (teal) rings simultaneously
- Donut center taps toggle `selectedTab` (EXPENSE/INCOME), which filters the chip grid below
- `DONUT_SECTION_HEIGHT = 360.dp`; `SIDE_COLUMN_WIDTH = 90.dp`
- **Category slot assignment** (collapsed mode, sorted by spending desc):
  - Top row: positions 1–4 (`display[0..3]`), `Arrangement.SpaceBetween` — outer chips align above side columns
  - Left column: positions 5, 6, 9 (`display[4]`, `display[5]`, `display[8]`) — always `isCompact = true`
  - Right column: positions 7, 8, 10 (`display[6]`, `display[7]`, `display[9]`) — always `isCompact = true`
  - Categories 11+ appear in rows of 4 below the donut section
- With 3 compact chips per side column: `3 × 108dp + 2 × 16dp = 356dp < 360dp` — fits without overflow

### Expanded Subcategory Strip (`ExpandedCategoryStrip`)

Shown below the row that contains the double-clicked parent chip.

- **No parent chip** shown in the strip — only children
- Children distributed evenly across full card width using `weight(1f)` per child (max 4)
- Only children with `spending > 0` appear (zero-amount subcategories filtered out)
- Child icon circles: 44dp; icon 22dp; name 10sp; card background: parent color 8% alpha

### Inline Subcategory Panel (`SideSubcategoryPanel`)

When a category in the left or right side column is double-clicked, the donut row switches to inline expansion: the expanded chip stays in its column, a panel of non-empty subcategories occupies 60% of the row, and the donut shrinks to 40%.

- Left expanded: `[chip col | panel (0.6f) | donut (0.4f)]`
- Right expanded: `[donut (0.4f) | panel (0.6f) | chip col]`
- Panel shows only subcategories with `spending > 0`, sorted descending
- Each row: 32dp circle icon + name + amount; card background: parent color 8% alpha
- **Panel height is wrap-content** — uses `Modifier.weight(0.6f).wrapContentHeight(Alignment.CenterVertically)`. The `wrapContentHeight()` is critical: Compose `Row` with `height(360.dp)` passes `minHeight = 360` to ALL children (including weighted), forcing them to the full row height. `wrapContentHeight()` overrides that minimum to 0, lets the Card shrink to content size, and centers it vertically.
- **Implementation lives in `CategoriesWidgets.kt`**, not `CategoriesScreen.kt`. Call sites are in `CategoriesScreen.kt` mid-row section.
- **`strip_mid` does not exist** — when the inline panel is showing, there is no second expansion strip below the donut. Rendering both would duplicate children. `showInlinePanel` is computed at the `CategoriesGridContent` level (outside `item {}`) so the strip condition can reference it.
- **Pre-filtered before call**: `CategoriesGridContent` computes `expandedChildrenWithSpending = expandedChildren.filter { (spending[it.id] ?: 0.0) > 0.0 }` and passes it to `SideSubcategoryPanel` instead of the raw `expandedChildren`. `showInlinePanel` checks `expandedChildrenWithSpending.isNotEmpty()` — if all children have zero spending, the panel does not appear at all. The DAO returns 0.0 (not null) for categories without transactions (LEFT JOIN + COALESCE), so the pre-filter is the reliable guard.

### Same-Name Subcategory Deduplication

When computing `expandedChildren`, any child whose `name.trim().lowercase()` matches the parent's name is excluded:

```kotlin
val parentName = cat.name.trim().lowercase()
allCategoriesForTab.filter { c ->
    c.parentId == cat.id && !c.archived &&
    c.name.trim().lowercase() != parentName
}
```

This prevents a subcategory "Продукти" from appearing inside its parent "Продукти". The two are semantically the same and should be treated as one leaf.

### Auto-Suggest Icons

When creating a new category (existing == null), `CategoryFormSheet` runs a `LaunchedEffect(name)` that calls `suggestCategoryStyle(name, type)` if name ≥ 3 chars AND user hasn't manually picked an icon (iconKey == "category"). Once the user touches the icon picker, auto-suggest stops firing.

`CATEGORY_ICONS_LIST` in `CategoryIcons.kt` is the canonical set of valid icon keys. Notable keys and their icons:

| Key | Icon | Color | Matches (auto-suggest) |
|---|---|---|---|
| `taxi` | `LocalTaxi` | `#FDD835` yellow | таксі, taxi, uklon, bolt, uber |
| `gas_station` | `LocalGasStation` | `#FF8F00` amber | азс, азц, заправк, wog, okko, socar |
| `car` | `DirectionsCar` | `#FF9800` orange | транспорт, авто, машин, паркінг, бензин |
| `movie` | `Movie` | `#7B1FA2` deep-purple | дозвілл, розваг, кіно, фільм, netflix |
| `gaming` | `SportsEsports` | `#607D8B` blue-grey | gaming, ігри, playstation, xbox, steam |
| `telegram` | `Send` | `#2196F3` blue | telegram, телеграм, viber, messenger |
| `dating` | `Favorite` | `#E91E63` pink | tinder, bumble, знайомств |
| `ticket` | `ConfirmationNumber` | `#AD1457` dark-pink | концерт, театр, квитки, festival |
| `grocery` | `ShoppingBasket` | `#03A9F4` | продукти, атб, сільпо, фора |
| `volunteer` | `VolunteerActivism` | `#4CAF50` | здоров, самопочутт |

Rule ordering in `CategoryStyleUtil.kt` (most specific first): `taxi`/`gas_station` before `car`; `movie`/`gaming`/`telegram`/`dating` before `ticket`. Do not reorder. The seeder default for "Таксі" uses `taxi` (yellow `#FDD835`); "АЗС" is a user-created category migrated to `gas_station` via migration 6→7.

## Transactions Screen

Sheet/dialog composables for the transactions tab were split from `TransactionSheets.kt` (deleted) into four dedicated files in `ui/transactions`:

| File | Contents |
|---|---|
| `TxSearchScreen.kt` | `TxSearchScreen`, `SearchSectionHeader`, `TypeFilterCard`, `ColoredFilterChip` |
| `CategoryPickerSheet.kt` | `CategoryPickerSheet`, `CategoryPickerCell`, `AccountPickerRow` |
| `TransferQuickSheet.kt` | `TransferQuickSheet` |
| `TransactionDetailSheet.kt` | `TransactionDetailSheet` |

All composables are `internal` and remain in the same package — `TransactionsListScreen.kt` calls them unchanged. Do not mark them `private`.

## Settings Screen

`SettingsScreen` is a full-screen Compose overlay (not a NavGraph route). Internal pages: `MAIN`, `THEME`, `CURRENCY`.

Settings persisted via DataStore (`SettingsRepository` → `AppSettings`):
- `themeMode`: `SYSTEM` / `LIGHT` / `DARK`
- `accentColor`: hex string or empty (system default)
- `homeScreen`: `HomeScreenTab` enum
- `budgetVisible`: Boolean
- `loginProtectionEnabled`: Boolean → `BiometricPrompt` in `MainActivity` after 30s background
- `notificationsEnabled`: Boolean → `NotificationWorker` via WorkManager
- `currency`, `numberFormat`: String

`formatMoneyWithSettings()` is defined in `SettingsSubScreens.kt` (same `ui.settings` package as `SettingsScreen`). It is not yet applied globally — `formatMoney()` in `MainScreen.kt` remains the default for transaction display.

## Navigation And Gestures

- Main horizontal paging is controlled programmatically via `HorizontalPager`.
- Horizontal swipes inside feature screens change month/period (swipe left = next month, swipe right = prev month).
- Edge swipes handled by `MainScreen` for drawer/back behavior.
- `BackHandler` inside `MainScreen` handles closing embedded overlays (Settings, EditCategories) before system back.

### Swipe Sensitivity (`horizontalSwipe` modifier in `MainScreen.kt`)

```
SWIPE_THRESHOLD = 130f px   // deliberately high to avoid accidental flips
```

Swipe only fires when movement is **predominantly horizontal**: `|deltaX| > |deltaY| * 1.7`. This prevents vertical LazyColumn scrolling from triggering month changes. Y-position is tracked from `awaitFirstDown` to finger lift.

### Month Flip Animation (`SharedMonthPill.kt`)

`SharedMonthNavPill` wraps its inner label Row in `AnimatedContent(targetState = pillLabel to pillBadge)`:

- Forward (next month): content slides left-in / left-out
- Backward (prev month): content slides right-in / right-out
- Duration: 220ms enter slide + 180ms fade-in; 180ms exit slide + 120ms fade-out

Direction is tracked via a plain `Ref<Boolean>` (not `MutableState`) to avoid extra recompositions. The outer `Surface` capsule stays static; only the text content inside slides.

## Shared Month State

`SelectedMonthRepository` holds the selected period as a `StateFlow<AppMonth>`. All period-aware screens (Categories, Budget, Overview, Transactions) observe this shared flow. `SharedMonthNavPill` and `SharedTopBar` display and modify it. The pill shows month label and a "days" badge.

`AppMonth` modes: `MONTH`, `TODAY`, `WEEK`, `YEAR`, `ALL`, `DAY`, `RANGE`. `computeRange(AppMonth)` returns the corresponding `Pair<Long, Long>` timestamp range.

## Text And Locale

Visible app labels are primarily Ukrainian with some existing Russian comments/code text. Do not casually rename user-visible labels without product intent.
