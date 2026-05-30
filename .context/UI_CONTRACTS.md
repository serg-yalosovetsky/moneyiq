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
CHIP_WIDTH              = 116.dp
CHIP_HEIGHT             = 136.dp   // must stay ≥136dp; reducing clips spending text
CHIP_CIRCLE_SIZE        = 60.dp
CHIP_WIDTH_COMPACT      = 82.dp
CHIP_HEIGHT_COMPACT     = 112.dp
CHIP_CIRCLE_COMPACT     = 40.dp
CATEGORY_VERTICAL_GAP   = 8.dp
DONUT_SECTION_HEIGHT     = CHIP_HEIGHT * 2 + CATEGORY_VERTICAL_GAP  // = 280dp (2 chips + gap)
SUBCATEGORY_PANEL_WIDTH  = 150.dp
SUBCATEGORY_PANEL_HEIGHT = 76.dp
```

**CRITICAL spacing rule**: Never use `Modifier.height(N).padding(bottom = K)` on chip rows — padding inside a height constraint subtracts from content area and clips the spending text. Use `LazyColumn(verticalArrangement = Arrangement.spacedBy(CATEGORY_VERTICAL_GAP))` for inter-row spacing and `Modifier.height(chipHeight)` on rows without bottom padding.

The chip name `Box` uses `heightIn(min=28.dp, max=40.dp)` (compact: `min=22.dp, max=32.dp`), not a fixed height.

### Chip Visual Logic

- **All categories are always shown**, including those with 0 spending. Zero-spending chips appear "pale".
- Icon circle is solid (white icon): when `spending > 0`
- Icon circle is tinted (light alpha 0.13, colored icon): when `spending == 0`
- **Budget text** (above icon, position 2): shown whenever `budgetAmount > 0`, regardless of spending; dimmed (alpha 0.42), 8sp Normal; otherwise a fixed-height `Spacer` preserves layout
- **+N badge** (small circle top-right of icon): shown when `showChildBadge && childCount > 0`; displays `"+N"` (N = child count after same-name dedup); 18dp normal / 16dp compact circle, `primary` bg, 8sp / 7sp compact
- Spending text color: category color if `spending > 0`, grey (onSurface 35%) if `spending == 0`

`display` (in `CategoriesGridContent`) = `sorted` (all root categories sorted by spending desc). Do NOT filter `display` to active-only — zero-spending categories must remain visible as pale chips.

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

### Donut Chart Layout (1Money-style, as of 2026-05-30)

- Always shows both expense (crimson) and income (teal) rings simultaneously
- Donut center taps toggle `selectedTab` (EXPENSE/INCOME), which filters the chip grid
- **All chips are the same size** — side column chips use the same `isCompact` value as top/bottom rows. No separate compact sizing for mid-section chips.
- **Category slot assignment** (collapsed mode, `showSubcategories = false`, sorted by spending desc):

```
[0]     [1]      [2]      [3]     ← topRow (4 chips, SpaceBetween)
[4]  [ DONUT (280dp) ]  [6]       ← mid rows 1-2: chipW col | donut | chipW col
[5]  [               ]  [7]
[8]  [      +        ]  [9]       ← mid row 3: midLeft3 | AddCategoryChip | midRight3
[10]   [11]   [12]   [13]         ← extCats = display.drop(10), rows of 4
...
```

- `topRow    = display.take(4)`
- `midLeft   = [display[4], display[5]]` — left column beside donut, `Column(width=chipW, spacedBy=CATEGORY_VERTICAL_GAP)`
- `midRight  = [display[6], display[7]]` — right column beside donut
- `midLeft3  = display.getOrNull(8)` — 3rd left chip (Дозвілля-level), in `item(key="mid3_row")`
- `midRight3 = display.getOrNull(9)` — 3rd right chip (Транспорт-level), same row as midLeft3
- `extCats   = display.drop(10)` — remaining in rows of 4
- Mid section: `Row { Column(chipW){ midLeft } + DonutChart(weight=1f, h=DONUT_SECTION_HEIGHT) + Column(chipW){ midRight } }`
- Mid row 3: `Row(SpaceBetween, height=chipHeight, paddingTop=4dp) { Box(chipW){midLeft3} + Box(weight=1f, Center){AddCategoryChip} + Box(chipW){midRight3} }`
- `DONUT_SECTION_HEIGHT = CHIP_HEIGHT * 2 + CATEGORY_VERTICAL_GAP` = **280dp**
- `CATEGORY_VERTICAL_GAP = 8dp` — spacing between chips in side columns and between all grid rows

**Subcategory mode** (`showSubcategories = true`): full-width donut at top, all categories in rows of 4 below.
### Expanded Subcategory Strip (`ExpandedCategoryStrip`)

Shown below the row that contains the double-clicked parent chip.

- **No parent chip** shown in the strip — only children
- Children distributed evenly across full card width using `weight(1f)` per child (max 4)
- Only children with `spending > 0` appear (zero-amount subcategories filtered out)
- Child icon circles: 44dp; icon 22dp; name 10sp; card background: parent color 8% alpha

### Inline Subcategory Panel (`SideSubcategoryPanel`) — Dead Code

`SideSubcategoryPanel` (in `CategoriesWidgets.kt`) and the wrapper functions `LocalSubcategoryPanel`/`TopSubcategoryPanelRow` (in `CategoriesScreen.kt`) exist in the codebase but `TopSubcategoryPanelRow` is never called — this code path is inactive. Active subcategory expansion uses `ExpandedCategoryStrip` instead.

The mid-section **chip columns** (`midLeft`/`midRight`) flanking the donut are the current design and remain active.

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

**Important:** `childCounts` (used for the "+N" badge) applies the same exclusion, so the badge count and the expansion strip are always consistent. A child excluded from expansion does not increment the badge.

### graphicsLayer Prohibition In Category Chips

Category chip containers in `CategoriesGridContent` must **not** use `.graphicsLayer { clip = false }`.
That modifier creates a hardware layer whose z-level is above `Dialog` windows — causing chips to render above `CategoryActionSheet` (the long-press dialog), bleeding through the scrim.

The expansion ring drawn by `drawBehind` (in `CategoriesWidgets.kt`) fits within chip bounds without the modifier:
- compact: ring radius 24dp < half chip-width 37dp
- normal: ring radius 28dp < half chip-width 43dp

If content genuinely needs to escape clip bounds and coexist with Dialogs, use a full-screen overlay `Box` at the root composition level.
### Auto-Suggest Icons

When creating a new category (existing == null), `CategoryFormSheet` runs a `LaunchedEffect(name)` that calls `suggestCategoryStyle(name, type)` if name ≥ 3 chars AND user hasn't manually picked an icon (iconKey == "category"). Once the user touches the icon picker, auto-suggest stops firing.

`CATEGORY_ICONS_LIST` in `CategoryIcons.kt` is the canonical set of valid icon keys. `suggestCategoryStyle` in `CategoryStyleUtil.kt` has 36 rules checked top-to-bottom — all rules and their colors:

| Key | Color | Top matching keywords |
|---|---|---|
| `ai` | `#6200EA` deep-purple | ai, chatgpt, openai, claude, gemini, gpt |
| `aliexpress` | `#FF6D00` orange | aliexpress, ali, temu, shein |
| `cloud` | `#0288D1` sky-blue | cloud, хмар, icloud, dropbox, хостинг |
| `transfer` | `#00897B` teal | переказ, transfer, відправк |
| `delivery` | `#FF6F00` amber | кур'єр, доставка, нова пошта |
| `devices` | `#607D8B` blue-grey | електрон, техніка, ноутбук, гаджет |
| `wifi` | `#00BCD4` cyan | інтернет, wifi, провайдер |
| `phone` | `#3F51B5` indigo | зв'язок, мобільн, lifecell, kyivstar |
| `beauty` | `#AD1457` dark-pink | краса, салон, манікюр, спа |
| `clothes` | `#00838F` dark-cyan | одяг, взуття, fashion |
| `family` | `#7A48F2` purple | сім'я, дітям, дитяч |
| `receipt` | `#546E7A` blue-grey | рахунки, bills, платіж, оплат |
| `coffee` | `#7B5947` brown | кафе, кав'ярня, кава, coffee |
| `restaurant` | `#4659BE` blue | ресторан, ресторація, їдальня, food, pizza |
| `grocery` | `#4AAFE8` light-blue | продукти, атб, сільпо, фора |
| `theater` | `#F73579` pink | дозвілл, розваг, театр, концерт, entertainment |
| `movie` | `#F73579` pink | кіно, cinema, фільм, netflix |
| `gaming` | `#607D8B` blue-grey | gaming, ігри, playstation, xbox, steam |
| `telegram` | `#2196F3` blue | telegram, телеграм, viber, messenger |
| `dating` | `#E91E63` pink | dating, tinder, bumble, знайомств |
| `ticket` | `#AD1457` dark-pink | квиток, квитки |
| `music` | `#AB47BC` purple | музик, spotify |
| `shopping` | `#7B5947` brown | покупки, магазин, shopping |
| `taxi` | `#FDD835` yellow | таксі, taxi, uklon, bolt, uber |
| `gas_station` | `#FF8F00` amber | азс, азц, заправк, wog, okko, socar |
| `car` | `#FFA834` orange | транспорт, авто, машин, автобус, паркінг |
| `home` | `#546E7A` blue-grey | комунальн, квартир, оренда, ремонт |
| `work` | `#1565C0` dark-blue | зарплат, офіс, фриланс, дохід |
| `school` | `#FF9800` orange | освіт, навчан, школа, курс |
| `volunteer` | `#48B456` green | здоров, самопочутт |
| `health` | `#43A047` dark-green | медицин, аптека, лікар, стоматолог |
| `flight` | `#03A9F4` light-blue | відпочин, туризм, готель, booking |
| `money` | `#F9A825` amber-dark | **фінанс**, інвестиц, банк, крипто, депозит |
| `pets` | `#8D6E63` brown-light | тварин, кіт, собак, ветеринар |
| `gift` | `#F34B4D` red | подарун, свят, birthday |
| `sports` | `#F44336` red | спорт, фітнес, gym, тренув |

**Fallback**: unrecognised name → `category` key, color `#4CAF50` for INCOME or `#78909C` for EXPENSE.

**Critical rule orderings** (do not reorder these pairs):
- `wifi` before `phone` — both match "інтернет"-related terms
- `coffee` before `restaurant` — кафе more specific
- `grocery` before `shopping` — продукти more specific
- `receipt` before `home` — "рахунки/оплат" more specific than generic home; "комунальн" was moved from `receipt` → `home` (2026-05-29), backed by migration 10→11
- Ресторація subcategory icons (migration 12→13): "food delivery"/"glovo" → `delivery` `#FF6F00`; "ресторани" → `restaurant` `#E53935`; "кафе" → `coffee` `#795548` — all three had inherited the parent's orange restaurant icon
- `clothes` before `shopping` — одяг more specific
- `theater` → `movie` → `gaming`/`telegram`/`dating` → `ticket` — leisure specificity chain
- `taxi` → `gas_station` → `car` — transport specificity chain

Seeder defaults: "Дозвілля" root → `theater` (`#F73579`); "Таксі" child → `taxi` (`#FDD835`).

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

`SettingsScreen` is a full-screen Compose overlay (not a NavGraph route). Internal pages: `MAIN`, `THEME`, `CURRENCY`, `ABOUT`. `AboutPageContent` (in `SettingsSubScreens.kt`) shows the launcher icon (96dp), app name, `BuildConfig.VERSION_NAME`, a short description, and "© 2025 PixelRush" footer.

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

## Typography

Material 3 token defaults used throughout (no custom font family — system default):

| Token | Default size | Default weight |
|---|---|---|
| `labelSmall` | 11sp | Medium 500 |
| `labelMedium` | 12sp | Medium 500 |
| `bodySmall` | 12sp | Regular 400 |
| `bodyMedium` | 14sp | Regular 400 |
| `titleSmall` | 14sp | Medium 500 |
| `titleMedium` | 16sp | Medium 500 |

### SharedTopBar

| Element | Style | Size | Weight |
|---|---|---|---|
| "Всі рахунки" subtitle | `labelSmall` | 11sp | Medium, alpha 55% |
| Total balance | `titleSmall` + Bold override | 14sp | Bold 700 |

### SharedMonthNavPill

| Element | Style | Size | Weight |
|---|---|---|---|
| Day-count badge (red circle) | `labelMedium` + Bold | 12sp | Bold 700, White |
| Period label ("13 – 29 ТРАВНЯ") | `titleSmall` + Bold | 14sp | Bold 700, PILL_ACCENT |

### CategoryChip — normal (`isCompact = false`)

Icon circle 48dp, icon 26dp. Name box `heightIn(min=28dp, max=40dp)`.

| Element | Size | Weight |
|---|---|---|
| Category name | 11sp, lineHeight 13sp | SemiBold 600 |
| Budget text (above icon, when `budgetAmount > 0`) | 8sp, lineHeight 11sp | Normal 400, alpha 42% |
| Spending amount (bottom) | 10sp, lineHeight 12sp | SemiBold 600 |
| +N child badge | 8sp | — white on primary |

### CategoryChip — compact (`isCompact = true`)

Icon circle 40dp, icon 22dp. Name box `heightIn(min=22dp, max=32dp)`.

| Element | Size | Weight |
|---|---|---|
| Category name | 10sp, lineHeight 12sp | SemiBold 600 |
| Budget text | 8sp, lineHeight 10sp | Normal 400, alpha 42% |
| Spending amount | 9sp, lineHeight 11sp | SemiBold 600 |
| +N child badge | 7sp | — white on primary |

### DonutChart center

| Element | Style | Size | Weight |
|---|---|---|---|
| "Витрати" / "Доходи" label | `labelSmall` | 11sp | Medium, alpha 55% |
| Expense total | `titleSmall` + Bold | 14sp | Bold 700, error color |
| Income total | `bodySmall.copy(fontSize=12sp)` + Medium | 12sp | Medium 500, teal #26A69A |
