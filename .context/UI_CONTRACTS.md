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
- **Budget row** (position 2, above icon):
  - If `budgetAmount > 0`: shows **remaining budget** (`budgetAmount - spending`). When over budget (`remainingBudget < 0`): remaining shown in a colored pill (category color bg, white text Bold). When within budget: remaining shown dimmed (alpha 0.42, SemiBold).
  - If `budgetAmount == 0`: shows "0 ₴" dimmed (alpha 0.30). No spacer.
  - `budgetOverride: Double?` parameter overrides `category.budgetAmount` (used by Budget screen to pass its own budget value).
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

**Subcategory mode** (`showSubcategories = true`): **same orbital layout as category mode** (donut in centre, chips surrounding). `showChildBadge = !showSubcategories` — badges hidden because subcategories have no children. `topRow/midLeft/midRight/extCats` are computed from `display` unconditionally (no `showSubcategories` guard on those variables).

### Donut Subcategory Focus

When a root category with children is double-tapped and `hasExpandedStrip == true` (expansion strip is visible), the DonutChart **switches to showing only the subcategory breakdown**:

```kotlin
val hasExpandedStrip = expandedCat != null && expandedChildren.isNotEmpty()
val donutCats    = if (hasExpandedStrip) expandedChildren else categories
val donutExpense = if (hasExpandedStrip && selectedTab == 0)
    expandedChildren.sumOf { spending[it.id] ?: 0.0 } else totalExpense
val donutIncome  = if (hasExpandedStrip && selectedTab == 1)
    expandedChildren.sumOf { spending[it.id] ?: 0.0 } else totalIncome
```

- `donutCats` = child categories (same-name-dedup already applied via `expandedChildren`)
- `donutExpense`/`donutIncome` = sum of child spending for the active tab; full total otherwise
- The `spending` map is shared — no extra data fetch required
- When the expansion strip is dismissed, the donut reverts to showing all root categories

**Rule:** Do not pass `categories` (all roots) to DonutChart when `hasExpandedStrip == true`. The parent category's large slice would dominate the ring and the individual subcategory proportions would be invisible.

### Expanded Subcategory Strip (`ExpandedCategoryStrip`)

Triggered by double-click on a chip. The chip row and its strip are **one LazyColumn item** — wrapped in a `Column` so no `CATEGORY_VERTICAL_GAP` appears between them.

**All zones** (`topRow`, `midLeft`, `midRight`, `extCats`) now use `inline = true` mode:
- Strip appears directly below the row that contains the expanded chip, fused into the same `Column`
- `showParentHeader = false` — strip shows only children (no parent header needed; chip above is the visual anchor)
- `showChildBadge = true` on the expanded chip; chip bottom corners are **flat** (`flatBottom = true` on `CategoryChip`) so the chip and strip merge seamlessly

**`CategoryChip` `flatBottom` parameter** (added 2026-05-31):
- When `isExpanded = true` AND `flatBottom = true`: clip shape is `RoundedCornerShape(topStart=12, topEnd=12, bottomStart=0, bottomEnd=0)` — bottom corners are flat
- When `isExpanded = true` AND `flatBottom = false` (default): all corners 12dp rounded
- `inlineStripShown` flows from `CategoriesGridContent` → `CategoryGridRow` → `CategoryGridSlot` → `CategoryChip` as `flatBottom`

**`ExpandedCategoryStrip` `inline` parameter** (added 2026-05-31):
- `inline = false` (default, legacy): wrapped in `Card(RoundedCornerShape(16dp), padding=12dp/4dp)`
- `inline = true` (current usage in `CategoriesGridContent`): `HorizontalDivider(parentColor 18% alpha)` + `Column(background=parentColor 7% alpha)` — no Card, no margins, flush with chip row

**Strip visual spec (inline mode):**
- Top border: `HorizontalDivider` in parent color at 18% alpha
- Background: parent color 7% alpha
- Children: distributed with `weight(1f)` per child (max 4), sorted by spending desc
- Only children with `spending > 0` OR `budgetAmount > 0` appear
- Child icon circles: 44dp; icon 22dp; name 10sp; spending 9sp SemiBold

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

`CATEGORY_ICONS_LIST` in `CategoryIcons.kt` is the canonical set of valid icon keys. `suggestCategoryStyle` in `CategoryStyleUtil.kt` has 38 rules checked top-to-bottom — all rules and their colors:

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
| `coffee` | `#795548` brown | кафе, кав'ярня, кава, coffee |
| `restaurant` | `#4659BE` blue | ресторан, ресторація, їдальня, food, pizza |
| `grocery` | `#4AAFE8` light-blue | продукти, атб, сільпо, фора |
| `celebration` | `#FF6D00` orange | розваг, свят, party, вечірк, банкет |
| `theater` | `#F73579` pink | дозвілл, театр, концерт, шоу, entertainment, festival |
| `movie` | `#9C27B0` purple | кіно, cinema, фільм, netflix |
| `gaming` | `#607D8B` blue-grey | gaming, ігри, playstation, xbox, steam |
| `telegram` | `#2196F3` blue | telegram, телеграм, viber, messenger |
| `dating` | `#E91E63` pink | dating, tinder, bumble, знайомств |
| `ticket` | `#AD1457` dark-pink | квиток, квитки |
| `music` | `#AB47BC` purple | музик, spotify |
| `shopping` | `#7B5947` brown | покупки, магазин, shopping |
| `taxi` | `#FDD835` yellow | таксі, taxi, uklon, bolt, uber |
| `gas_station` | `#FF8F00` amber | азс, азц, заправк, wog, okko, socar |
| `bus` | `#FFA834` orange | **транспорт**, автобус, метро, маршрутк, transit |
| `car` | `#FF7043` deep-orange | авто, машин, автомоб, паркінг, бензин, пальне |
| `home` | `#546E7A` blue-grey | комунальн, квартир, оренда, ремонт |
| `work` | `#1565C0` dark-blue | зарплат, офіс, фриланс, дохід |
| `school` | `#FF9800` orange | освіт, навчан, школа, курс |
| `volunteer` | `#48B456` green | здоров, самопочутт |
| `pharmacy` | `#43A047` dark-green | аптека, ліки, medication, таблетк, препарат |
| `doctor` | `#D81B60` pink-red | медицин, лікар, клінік, стоматолог, hospital |
| `flight` | `#03A9F4` light-blue | відпочин, туризм, готель, booking |
| `money` | `#F9A825` amber-dark | **фінанс**, інвестиц, банк, крипто, депозит |
| `pets` | `#8D6E63` brown-light | тварин, кіт, собак, ветеринар |
| `gift` | `#F34B4D` red | подарун, birthday |
| `sports` | `#F44336` red | спорт, фітнес, gym, тренув |
| `gavel` | `#BF360C` deep-orange | штраф, пеня, санкц, fine, penalty |
| `percent` | `#F9A825` amber | процент, відсоток, податок, пдв, interest, tax |

**Fallback**: unrecognised name → `category` key, color `#4CAF50` for INCOME or `#78909C` for EXPENSE.

**Notes on recent rule changes:**
- `health` key **removed** from rules. Replaced by two specific rules: `pharmacy` (аптека/ліки) and `doctor` (медицина/лікар/стоматолог). `health` is kept as a legacy entry in `iconColorMap` only for DB backward compatibility.
- `celebration` **added** (2026-05-31): matches "розваг", "свят", "party", "вечірк" — must appear **before** `theater` (which no longer matches "розваг").
- `gavel` and `percent` **added** (2026-05-31): штрафи/пені and проценти/податки.
- `movie` color corrected to `#9C27B0` (purple), not pink.

**Critical rule orderings** (do not reorder these pairs):
- `wifi` before `phone` — both match "інтернет"-related terms
- `coffee` before `restaurant` — кафе more specific
- `grocery` before `shopping` — продукти more specific
- `receipt` before `home` — "рахунки/оплат" more specific than generic home
- `celebration` before `theater` — "розваг" and "свят" go to celebration, not broad leisure
- `volunteer` before `pharmacy` before `doctor` — wellness → pills → medical (specificity chain)
- `clothes` before `shopping` — одяг more specific
- `theater` → `movie` → `gaming`/`telegram`/`dating` → `ticket` — leisure specificity chain
- `taxi` → `gas_station` → `bus` → `car` — transport specificity chain (`bus`=public, `car`=personal vehicle)
- `gavel` and `percent` after `sports` (bottom of list) — narrow keyword sets, low false-positive risk

Seeder defaults: "Дозвілля" root → `theater` (`#F73579`); "Таксі" child → `taxi` (`#FDD835`).

### CategoryFormSheet — Title Logic

The `TopAppBar` title in `CategoryFormSheet` is context-sensitive:

| Condition | Title |
|---|---|
| `existing == null && forParentId != null` | "Нова субкатегорія" |
| `existing == null` | "Нова категорія" |
| `existing.parentId != null` | "Субкатегорія" |
| `existing.parentId == null` | "Категорія" |

`forParentId: Long?` is a parameter passed when the form is opened to create a child under a known parent.

### CategoryFormSheet — Subcategories Section

The "Підкатегорії" section is **only rendered for root categories** (`existing != null && existing.parentId == null`):

1. Existing children are displayed as icon rows (color circle + name) via `items(children)`.
2. Each child row has a **trailing `LinkOff` icon button** (shown only when `onDetachSubcategory != null`). Tapping it calls `onDetachSubcategory(child)` → caller sets `child.parentId = null`, promoting the subcategory to a root category. The category is **not deleted** — it becomes available to be added to any other parent.
3. **"Додати підкатегорію"** button shown only when `onAddSubcategory != null` (callback provided by caller).
4. Clicking "Додати підкатегорію" invokes `onAddSubcategory()` → caller opens a new `CategoryFormSheet` with `forParentId = parent.id`.
5. New subcategory is saved with `parentId` set, enforcing single-parent uniqueness at the DB level.

**`onDetachSubcategory` wiring:**
- `CategoriesScreen.kt`: `viewModel.update(child.copy(parentId = null))`
- `CategoryFormSheets.kt` / `EditCategoriesScreen.kt`: delegate through `onSave(child.name, …, child.copy(parentId = null))` so `MainScreen` calls `categoriesViewModel.update(existing.copy(…))` — `parentId = null` is preserved because `existing` already has it set to null.

Child categories (`existing.parentId != null`) do **not** show the "Підкатегорії" section at all — subcategories cannot have sub-subcategories.

**Rule**: Do not add "Підкатегорії" to child category forms. One level of hierarchy is the maximum.

### CategoryFormSheet — Name Input

Category name is edited via an **inline `BasicTextField`** directly inside the form header (not a separate dialog). Behaviour:
- On new category creation (`existing == null`): `FocusRequester.requestFocus()` is called in `LaunchedEffect(Unit)` — keyboard appears immediately.
- Placeholder text "Введіть назву" is shown when blank (via `decorationBox`).
- `ImeAction.Done` → `focusManager.clearFocus()` (dismisses keyboard).
- `TextInputDialog` is **not** used for category name input — it was removed from `CategoryFormSheets.kt`.

`TextInputDialog` remains available in `ui/components/dialogs` and is still used in other screens (e.g., account name editing).

### EditCategoriesScreen (added 2026-05-31)

`EditCategoriesScreen` lives in its own file `EditCategoriesScreen.kt` (not `CategoryFormSheets.kt`). It is opened via the pencil icon in `SharedTopBar` on the Categories tab and is a full-screen `Box` overlay inside `MainScreen` (same pattern as `SettingsScreen`).

**Layout:**
- Stateful top bar: ← back button + context-sensitive title (`"Редагувати субкатегорії"` when `showSubcategories = true`, `"Редагувати категорії"` otherwise) + "Субкатегорії" `TextButton`
- `TabRow` with two tabs: ↓ Витрати / ⊕ Доходи (icons `ArrowCircleDown` / `AddCircleOutline`)
- `CategoriesGridContent` below — same donut chart and category colors as the main Categories screen

**Key differences from `CategoriesScreen`:**
- `childCounts = emptyMap()` — suppresses all +N subcategory badges
- `onChipClick` and `onChipLongClick` both open `CategoryFormSheet` for editing (not `QuickExpenseSheet`)
- "+" (`onAdd`) opens `CategoryFormSheet` with `existing = null` to create a new top-level category
- "Субкатегорії" button toggles `showSubcategories` state → passed to `CategoriesGridContent` (same flag as the main screen's subcategory toggle)

**Add subcategory flow (from within `CategoryFormSheet`):**
When the user taps "Додати підкатегорію" inside an edit form, `editCategory` is cleared and `addSubcategoryFor` is set. A second `CategoryFormSheet(forParentId = parent.id)` opens, and on save calls `onAddSubcategory(...)`.

**Data flow:**
`EditCategoriesScreen` receives its data as parameters from `MainScreen` (using `categoriesViewModel.state`). It does not own a ViewModel — all mutations go through callbacks (`onSave`, `onDelete`, `onAddSubcategory`) that call `categoriesViewModel` directly in `MainScreen`.

## SharedCalcKeypad (`ui/components/calculator/CalcKeypad.kt`)

`SharedCalcKeypad` is the shared 5-column calculator layout used across budget, transaction, transfer, and amount-picker screens.

### Key Layout

```
Row 1: ÷  |  7  |  8  |  9  |  ⌫  (backspace — Outlined icon)
Row 2: ×  |  4  |  5  |  6  |  [row2ExtraKey or C]
Row 3: −  |  1  |  2  |  3  |  [= or ✓] (rows 3-4 share weight=2f)
Row 4: +  |  ₴  |  0  |  ,  |
```

### `row2ExtraKey` parameter

`row2ExtraKey: (@Composable RowScope.() -> Unit)?` — optional 5th key in row 2.

| Caller | row2ExtraKey value |
|---|---|
| `AddTransactionScreen` | tab-to-date / date chip |
| `TransactionDetailSheet` | date chip |
| `TransferQuickSheet` | "to account" button |
| `CategorySheets` (quick expense) | account picker |
| `BudgetSheets` | not provided → **C button** |
| `AmountCalculatorSheet` | not provided → **C button** |

When `row2ExtraKey == null`, a **"C" (Clear) button** is shown in red. Tapping "C" calls `calc.onKey("C")` which resets `currentStr = "0"`, `pendingOp = null`, `pendingVal = 0.0` — clearing the full expression including any pending arithmetic operation.

**Rule**: Do not pass `row2ExtraKey` unless a genuine context-specific action is needed in that slot. The default "C" clear button is the correct behaviour for standalone amount/budget pickers.

### CalcStateHolder key handling

| Key | Effect |
|---|---|
| `0-9` | Append digit (max 12 chars; max 2 decimal places) |
| `,` | Append decimal separator if not already present |
| `+` `−` `×` `÷` | Store pending op and current value; start new operand |
| `=` | Evaluate pending op; clear `pendingOp` |
| `⌫` | Drop last character; falls back to "0" when single digit |
| `C` | Full reset: `currentStr="0"`, `pendingOp=null`, `pendingVal=0.0` |

### Backspace Icon

Backspace uses `Icons.AutoMirrored.Outlined.Backspace` (outlined, not filled). Do not revert to `Filled.Backspace`.

## Budget Screen

`BudgetScreen` (`Бюджет` tab) is a `Column { LazyColumn(weight=1f) + IncomeBudgetBar }` layout.

### SavingsSectionCard — Formula and Display

`SavingsSectionCard` in `BudgetScreen.kt` computes savings with this priority:

```
realSavings   = incomeTotal - expenseTotal         // actual cash flow
actualSavings = if (incomeBudget > 0) incomeBudget - expenseTotal
                else realSavings                   // header amount

daysPassed  = BudgetUiState.daysPassed (computed in BudgetViewModel)
              - past month: = daysInMonth
              - current month: = Calendar.DAY_OF_MONTH
              - future month: = 0
hasForecast = daysPassed > 0 && daysInMonth > daysPassed && expenseTotal > 0
projectedExpenses = expenseTotal / daysPassed * daysInMonth   (linear extrapolation)
projectedSavings  = if (incomeBudget > 0) incomeBudget - projectedExpenses
                    else realSavings - (projectedExpenses - expenseTotal)
```

Display logic:
- **Header right** (large): `projectedSavings` when `hasForecast`; otherwise `actualSavings` (= incomeBudget − expenses when budget set)
- **"прогноз"** label below header when forecast is active
- **"пройшло X з Y днів"** below title when forecast is active
- **"збережено X ₴"** (subtitle left): shown only when `incomeTotal > 0` (hidden when no income received yet)
- **"витрати до кінця ~X ₴"** in subtitle right when hasForecast; otherwise **"в бюджеті X ₴"** (incomeBudget)
- Color: green (`#26A69A`) when ≥ 0, pink/red (`#D81B60`) when negative

**Rule**: The header shows budget-based savings (incomeBudget − expenses), not cash-flow savings. This matches "Доступно в бюджеті" in `IncomeBudgetBar`.

### IncomeBudgetBar Layout (CRITICAL)

`IncomeBudgetBar` is placed **outside** the `LazyColumn` — it is a fixed element pinned to the bottom of the screen, not a scrollable list item.

```
Column(fillMaxSize) {
    LazyColumn(Modifier.weight(1f)) { … expense budget rows … }
    IncomeBudgetBar(modifier = Modifier.padding(bottom = bottomPadding))
}
```

Do **not** move `IncomeBudgetBar` back inside the `LazyColumn`. It was moved out because:
- When income categories are few, the bar would scroll out of view.
- The income total summary must always be visible alongside the expense list.

`IncomeBudgetBar` takes a `modifier: Modifier = Modifier` parameter for bottom padding injection.

## Overview Screen

`OverviewScreen` is the `Огляд` tab. It shows monthly totals, a daily bar chart, stats row, and a list section below the stats.

### Mode Toggle

Expense/Income toggle switches `OverviewMode` (EXPENSE / INCOME). All amounts, chart bars, and the list section reflect only the active mode.

### List Section Priority

The lower list section renders in this priority order:

1. **Category rows** — if `state.categories` is non-empty (categories with spending > 0 for the active mode and period)
2. **Transaction rows** — if `state.categories` is empty but `state.transactions` is non-empty (renders each `TransactionWithDetails` via `TransactionListItem`)
3. **Empty state** — "Немає категорій" icon + label if both are empty

This ensures that income/expense amounts shown in the toggle header always correspond to visible line items. Transactions that belong to a deleted category or have `categoryId = null` still contribute to the header total and appear in the fallback list.

`OverviewUiState.transactions` holds the mode-filtered transaction list (`monoTx` in `OverviewViewModel.buildState`). Tapping a transaction row in fallback mode is a no-op (`onClick = {}`).

### Category Detail Sheet

Tapping a category row opens `CategoryDetailSheet` (`OverviewSheets.kt`) as a `ModalBottomSheet` with the category color as container. Not available from transaction rows.

### Category Row Icons

Category rows in the list section call `categoryIconFor(row.icon)` from `org.pixelrush.moneyiq.ui.categories.CategoryIcons`. This is the same mapping used by `CategoriesScreen` and `CategoryFormSheet` — 48 icon keys total. Do not add a local icon mapper in `OverviewScreen.kt` (see ADR-034).

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

### Back Navigation To Home Tab

`BackHandler(enabled = currentPage != homeTabIndex)` — pressing Back from any tab navigates to the **home screen tab** configured in Settings (`settings.homeScreen: HomeScreenTab`). Pressing Back while already on the home tab is handled by the system → closes the app.

```kotlin
val homeTabIndex = activeTabs.indexOfFirst { it.label == settings.homeScreen.label }
    .takeIf { it >= 0 } ?: 0
```

Right-edge swipe (`onRightEdge = goBack`) follows the same logic. If `homeScreen` is set to `BUDGET` but Budget tab is hidden, index falls back to `0` (Accounts).

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
| Total balance | `titleLarge` + Bold override | 22sp | Bold 700 |

### SharedMonthNavPill

Navigation arrows: single `Icons.Default.KeyboardDoubleArrowLeft` / `KeyboardDoubleArrowRight` (32dp icon, 4dp padding = 24dp visual). Previously two `KeyboardArrowLeft/Right` side-by-side — replaced because `spacedBy(2.dp)` left a visible gap.

| Element | Style | Size | Weight |
|---|---|---|---|
| Day-count badge (red circle) | `labelMedium` + Bold | 12sp | Bold 700, White |
| Period label ("13 – 29 ТРАВНЯ") | `titleSmall` + Bold | 14sp | Bold 700, PILL_ACCENT |

### CategoryChip — normal (`isCompact = false`)

Icon circle 48dp, icon 26dp. Name box `heightIn(min=28dp, max=40dp)`. Name is single-line (`maxLines=1, softWrap=false`).

| Element | Size | Weight |
|---|---|---|
| Category name | 13sp, lineHeight 16sp | SemiBold 600 |
| Budget row (position 2) | 11sp, lineHeight 13sp | SemiBold or Bold (overbudget) |
| Spending amount (bottom) | 13sp, lineHeight 15sp | **Bold 700** |
| +N child badge | 8sp | — white on primary |

### CategoryChip — compact (`isCompact = true`)

Icon circle 40dp, icon 22dp. Name box `heightIn(min=24dp, max=34dp)`. Name is single-line (`maxLines=1, softWrap=false`).

| Element | Size | Weight |
|---|---|---|
| Category name | 12sp, lineHeight 14sp | SemiBold 600 |
| Budget row (position 2) | 10sp, lineHeight 12sp | SemiBold or Bold (overbudget) |
| Spending amount | 12sp, lineHeight 14sp | **Bold 700** |
| +N child badge | 7sp | — white on primary |

### DonutChart center

| Element | Style | Size | Weight |
|---|---|---|---|
| "Витрати" / "Доходи" label | `labelSmall.copy(fontSize=14sp)` | 14sp | Medium, alpha 55% |
| Expense total | `titleSmall.copy(fontSize=20sp)` + Bold | 20sp | Bold 700, error color |
| Income total | `bodySmall.copy(fontSize=15sp)` + Medium | 15sp | Medium 500, teal #26A69A |
