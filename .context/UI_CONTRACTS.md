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

## Accounts Screen

`AccountsScreen` (tab 0 — "Рахунки") is embedded inside the `HorizontalPager` in `MainScreen`. It receives action callbacks that are wired in `MainScreen`:

| Callback | Wired action |
|---|---|
| `onViewTx(acc)` | Sets `filterByAccountId = acc.id`, then `animateScrollToPage(txTabIndex)` — switches to Transactions tab **pre-filtered by that account** |
| `onAddIncome(acc)` | `onAddTransaction()` — opens AddTransaction screen |
| `onAddExpense(acc)` | `onAddTransaction()` — opens AddTransaction screen |
| `onAddTransfer(acc)` | `onAddTransaction()` — opens AddTransaction screen |

**"Операції" → account filter flow:**  
`MainScreen` holds `filterByAccountId: Long?`. When `onViewTx(acc)` fires, it sets `filterByAccountId = acc.id` before navigating. `TransactionsListScreen` receives `initialAccountFilter = filterByAccountId` and clears it via `onInitialAccountFilterApplied`. Internally, `LaunchedEffect(initialAccountFilter)` sets `filterAccountIds = setOf(accId)` — the same chip-filter mechanism used for category filtering.

**Limitation**: Income / expense / transfer forms open without a pre-selected account — the user picks the account manually inside the form.

**AccountActionSheet** (long-press on an account row) exposes six actions: Редагувати, Баланс, Операції, Поповнення, Списати, Переказ. The action sheet itself lives in `AccountPickerSheets.kt`. All callbacks are `() -> Unit` at the sheet level; `AccountsScreen` adapts them to `(AccountEntity) -> Unit` before forwarding to `MainScreen`.

### Account Icon Currency Badge

Every account icon displays a small **currency symbol badge** at the bottom-right corner:

- **`AccountIconBox`** (`AccountsScreen.kt`): 20dp surface circle, badge at `Alignment.BottomEnd` with `offset(2dp, 2dp)`. Currency symbol text: 8sp Bold, color = `badgeColor` (see Adaptive Tint below).
- **`AccountPickerSheet`** (`CalcDateSheet.kt`): 16dp circle, same position, 7sp Bold.
- Symbol is resolved via `CURRENCIES_ALL.find { it.code == account.currency }?.symbol?.take(2)`. Falls back to the first 2 chars of the currency code if the code is not in the list.
- Does not conflict with the star badge (default account indicator) which sits at `Alignment.BottomStart`.
- No DB migration needed — uses the existing `AccountEntity.currency` field.

### Adaptive Icon Tint For Light-Colored Accounts

Account icons use `luminance()` to pick readable content colors when the account background is light:

```kotlin
val isLightBg  = accentColor.luminance() > 0.5f
val iconTint   = if (isLightBg) Color(0xFF1C1B1F) else Color.White
val badgeColor = if (isLightBg) Color(0xFF1C1B1F) else accentColor
```

**Applied in:**
- `AccountIconBox` (`AccountsScreen.kt`) — icon tint + currency badge text color
- `AccountActionSheet` card (`AccountPickerSheets.kt`) — icon, name, balance, star all use `onCard = if (isLightCard) dark else white`; icon box bg uses `onCard.copy(alpha=0.15f)` instead of `Color.White.copy(alpha=0.2f)`
- `AccountFormSheet` icon preview (`AccountSheets.kt`) — `iconTint` derived from `accentColor.luminance()`
**Rule:** Never hardcode `tint = Color.White` on an icon whose background is an account's `colorHex`. Always derive tint from `luminance()`. Threshold: `> 0.5f` → light background → use `Color(0xFF1C1B1F)`.

### BudgetInputSheet — Icon Click + Currency Picker

**Signature:**
```kotlin
BudgetInputSheet(
    catRow:      BudgetCatRow,
    monthLabel:  String,
    accentColor: Color,
    amountLabel: String = "витрачено",
    onIconClick: (() -> Unit)? = null,
    onDismiss:   () -> Unit,
    onConfirm:   (Double, String) -> Unit   // amount + currency code
)
```

- The floating category icon circle (top-right) is clickable when `onIconClick != null`. Income budget entry passes `onIconClick = { incomeCatToEdit = null; showIncomeBudgetSheet = true }` to return to the income category picker.
- `pickedCurrency` initialised from `catRow.category.currencyCode`; synced via `LaunchedEffect(catRow.category.id)`.
- ₴ key in keypad → `showCurrencyPicker = true` → `ModalBottomSheet` listing `CURRENCIES_MAIN`.
- `onConfirm(calc.result(), pickedCurrency)` — currency is always returned with the amount.
- `BudgetViewModel.updateCategoryBudget(category, newBudget, currency)` persists both fields.

### AccountFormSheet — Credit Limit Field

`AccountFormSheet` (`AccountSheets.kt`) contains a "Кредитний ліміт" row in the "Баланс" section. Tapping it opens `AmountCalculatorSheet` (same component as "Баланс рахунку") and stores the result in local `creditLimit: Double` state.

**`onSave` signature (9 parameters):**
```kotlin
onSave: (name: String, type: AccountType, balance: Double, color: String,
         currency: String, description: String, includeInTotal: Boolean,
         icon: String, creditLimit: Double) -> Unit
```

**Data flow:**
- Edit path (`AccountsScreen.kt`): `acc.copy(creditLimit = creditLimit)` → `viewModel.update()`
- Create path (`MainScreen.kt`): `accountsViewModel.add(..., creditLimit = creditLimit)`
- `AccountsViewModel.add()` has `creditLimit: Double = 0.0` defaulted parameter — callers that don't pass it get 0.

**DB:** `accounts.creditLimit REAL NOT NULL DEFAULT 0.0`, added in migration 26→27.

**Display in form:** shows `"0 $sym"` when zero; otherwise `"${value} $sym"` (stripped trailing zeros).

**Rule:** Do not pass a hardcoded `0` for `creditLimit` in any `AccountFormSheet.onSave` lambda. Always destructure the parameter and forward it so existing credit limits survive re-saves.

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

`display` (in `CategoriesGridContent`) = `sorted` = `categories.sortedByDescending { spending[it.id] }`. The `sorted` algorithm is a simple sort with no grouping — it does NOT interleave parent categories with their children. Zero-spending chips appear pale but are always included. Do NOT filter `display` to active-only.

### QuickExpenseSheet — Panel Layout

`QuickExpenseSheet` (`CategorySheets.kt`) opens on single-tap of a category chip. It has a two-panel header row (80dp height) whose left/right layout depends on transaction type:

| Type | Left panel | Right panel |
|---|---|---|
| **EXPENSE** | Account (indigo `#3949AB` bg) — "З рахунку" — account name — tappable | Category (category color bg) — "До категорії" — category name |
| **INCOME** | Category (category color bg) — "З категорії" — category name | Account (indigo bg) — "На рахунок" — account name — tappable |

Both cases: account panel is tappable only when `accounts.size > 1` (opens account picker). The **category panel is always tappable — clicking it calls `onDismiss()`**, returning the user to the categories grid to pick a different category.

**Currency support:** `selectedCurrency` is initialised from `selectedAccount?.currency ?: "UAH"` and synced via `LaunchedEffect(selectedAccount?.currency)`. `currencySymbol` is resolved from `CURRENCIES_ALL`. The ₴ key in `SharedCalcKeypad` has `onCurrencyClick = { showCurrencyPicker = true }` — tapping it opens `CurrencyPickerSheet`. The selected currency is used in `calc.displayExpr(currencySymbol)` for display.

**Rule:** Do not merge the two paths into a single layout that only changes labels. The left panel's `Alignment` anchors (BottomStart vs BottomEnd, TopStart vs TopEnd) differ between the expense and income variants — always use the `isIncome` branch.

**`onSave` signature (6 parameters):**
```kotlin
onSave: (accountId: Long, amount: Double, note: String, date: Long,
         repeatMode: String, reminderMode: String) -> Unit
```

**Zero-amount UX:** `onConfirm()` has an `if (amt > 0.0)` guard. When the amount is 0 and the user presses ✓, `amountError = true` triggers a 600ms red flash (`animateColorAsState`) on the label and amount text, then resets. Do not remove the guard — a 0-amount transaction is invalid.

**Repeat/reminder flow:** `repeatMode` and `reminderMode` are local state, selected via `CalcDateSheet → RepeatDialog / ReminderDialog`. They are forwarded through `onSave` to `CategoriesViewModel.recordTransaction` / `TransactionsListViewModel.recordTransaction`, which computes `nextRepeatDate = calculateNextRepeatDate(date, repeatMode)` and persists all three fields to `TransactionEntity`. Automation is handled nightly by `RepeatTransactionWorker`.

### CategoryActionSheet Data

The action sheet uses **effective spending** (children aggregated into parent), not raw category spending:

```kotlin
val catSpending = effectiveSpending[cat.id] ?: 0.0
val catTxCount  = allCategoriesForTab
    .filter { it.id == cat.id || it.parentId == cat.id }
    .sumOf { state.monthTxCounts[it.id] ?: 0 }
```

This ensures the sheet's spending amount and operation count match what the chip displays.

### Light-Background Contrast Rule (Category/Budget Sheets)

Several sheets use the **category or account color** as `containerColor` of a `ModalBottomSheet` or as a section background. When that color is very light (white, pale, pastel), `Color.White` text becomes invisible.

**Rule**: Any sheet that puts user-chosen category/account color as a background must compute content color via luminance:

```kotlin
val isLightBg  = catColor.luminance() > 0.5f
val onCatColor = if (isLightBg) Color(0xFF1C1B1F) else Color.White
val displayColor = if (isLightBg) Color(0xFF1C1B1F) else catColor  // for text/icons on surface bg
```

- `onCatColor` replaces every `Color.White` inside the colored section (title, amounts, progress bar, drag handle).
- `displayColor` replaces `catColor` in the white/surface lower section (amount display text, `confirmColor` of `SharedCalcKeypad`, FAB icon tint).
- Threshold `> 0.5f` catches white, near-white, and light pastels while leaving all dark/medium colors unchanged.

**Affected sheets** (all fixed 2026-05-31):
| Sheet | File | Background source |
|---|---|---|
| `BudgetInputSheet` | `BudgetSheets.kt` | `catRow.category.colorHex` |
| `CategoryActionSheet` header | `CategorySheets.kt` | `category.colorHex` |
| `QuickExpenseSheet` category panel | `CategorySheets.kt` | `category.colorHex` |

**Do not** hardcode `Color.White` as text/icon color inside any colored section. Always derive from `onCatColor`.

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

`categories` passed to `CategoriesGridContent` contains **only subcategories** (`parentId != null`) in subcategory mode — both in the main `CategoriesScreen` and in `EditCategoriesScreen`. Root categories are excluded at the caller level before `CategoriesGridContent` is reached. `parentColors` (the parent-color tint on subcategory chips) resolves the parent from `allCategoriesForTab`, which still contains all categories.

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

`CATEGORY_ICONS_LIST` in `CategoryIcons.kt` is the canonical set of valid icon keys. `suggestCategoryStyle` in `CategoryStyleUtil.kt` has 55 rules checked top-to-bottom — all rules and their colors:

| Key | Color | Top matching keywords |
|---|---|---|
| `ai` | `#6200EA` deep-purple | ai, chatgpt, openai, claude, gemini, gpt |
| `aliexpress` | `#FF6D00` orange | aliexpress, ali, temu, shein |
| `server` | `#37474F` dark-grey | хостинг, хостінг, hosting, vps, сервер |
| `cloud` | `#0288D1` sky-blue | cloud, хмар, icloud, dropbox |
| `refund` | `#00897B` teal | повернення, повернен, refund, cashback, кешбек, компенсація |
| `transfer` | `#00897B` teal | переказ, transfer, відправк |
| `delivery` | `#FF6F00` amber | кур'єр, доставка, нова пошта |
| `devices` | `#607D8B` blue-grey | електрон, техніка, ноутбук, гаджет |
| `wifi` | `#00BCD4` cyan | інтернет, wifi, провайдер |
| `phone` | `#3F51B5` indigo | зв'язок, мобільн, lifecell, kyivstar |
| `beauty` | `#AD1457` dark-pink | краса, салон, манікюр, спа |
| `shoes` | `#5D4037` dark-brown | взуття, shoes, boots |
| `clothes` | `#00838F` dark-cyan | одяг, fashion |
| `toys` | `#FF6D00` orange | іграшки, toys, ляльки, конструктор, lego |
| `family` | `#7A48F2` purple | сім'я, дітям, дитяч |
| `receipt` | `#546E7A` blue-grey | рахунки, bills, платіж, оплат |
| `coffee` | `#795548` brown | кафе, кав'ярня, кава, coffee |
| `restaurant` | `#4659BE` blue | ресторан, ресторація, їдальня, food, pizza |
| `grocery` | `#4AAFE8` light-blue | продукти, атб, сільпо, фора |
| `flower` | `#E91E63` pink | квіти, цвіти, flower, флорист, букет |
| `souvenir` | `#7B1FA2` purple | сувенір, souvenir |
| `celebration` | `#FF6D00` orange | розваг, свят, party, вечірк, банкет |
| `theater` | `#F73579` pink | дозвілл, театр, концерт, шоу, entertainment |
| `movie` | `#9C27B0` purple | кіно, cinema, фільм, netflix |
| `book` | `#5E35B1` deep-purple | книги, книга, book, бібліотек |
| `gaming` | `#607D8B` blue-grey | gaming, ігри, playstation, xbox, steam |
| `telegram` | `#2196F3` blue | telegram, телеграм, viber, messenger |
| `dating` | `#E91E63` pink | dating, tinder, bumble, знайомств |
| `ticket` | `#AD1457` dark-pink | квиток, квитки |
| `music` | `#AB47BC` purple | музик, spotify |
| `store` | `#1E88E5` blue | rozetka, ebay, маркетплейс, prom.ua, hotline |
| `fitness` | `#D32F2F` dark-red | спортивні товари, спорттовар, decathlon |
| `shopping` | `#7B5947` brown | покупки, магазин, shopping |
| `taxi` | `#FDD835` yellow | таксі, taxi, uklon, bolt, uber |
| `gas_station` | `#FF8F00` amber | азс, азц, заправк, wog, okko, socar |
| `train` | `#1565C0` dark-blue | залізниця, потяг, поїзд, train, укрзалізниц |
| `bus` | `#FFA834` orange | **транспорт**, громадськ, автобус, метро, маршрутк |
| `auto_parts` | `#E64A19` deep-orange | запчастин, автозапч, шиномонтаж, ремонт авто |
| `car` | `#FF7043` deep-orange | авто, машин, автомоб, паркінг, бензин, пальне |
| `tools` | `#546E7A` blue-grey | інструмент, дриль, пилк, шуруповерт |
| `hardware` | `#BF360C` deep-orange | будматеріал, будівельн, цегла, ламінат |
| `home` | `#546E7A` blue-grey | комунальн, квартир, оренда, ремонт |
| `work` | `#1565C0` dark-blue | зарплат, офіс, фриланс, дохід |
| `school` | `#FF9800` orange | освіт, навчан, школа, курс |
| `volunteer` | `#48B456` green | здоров, самопочутт |
| `pharmacy` | `#43A047` dark-green | аптека, ліки, medication, таблетк |
| `dental` | `#0097A7` teal | стоматолог, дантист, dental, зубн |
| `doctor` | `#D81B60` pink-red | медицин, лікар, клінік, hospital |
| `hotel` | `#4527A0` deep-purple | готель, hotel, hostel, airbnb |
| `flight` | `#03A9F4` light-blue | відпочин, туризм, перельот, travel, booking |
| `money` | `#F9A825` amber-dark | **фінанс**, інвестиц, банк, крипто, депозит |
| `pets` | `#8D6E63` brown-light | тварин, кіт, собак, ветеринар |
| `gift` | `#F34B4D` red | подарун, birthday |
| `sports` | `#F44336` red | спорт, фітнес, gym, тренув |
| `gavel` | `#BF360C` deep-orange | штраф, пеня, санкц, fine, penalty |
| `percent` | `#F9A825` amber | процент, відсоток, податок, пдв, interest, tax |

**Fallback**: unrecognised name → `category` key, color `#4CAF50` for INCOME or `#78909C` for EXPENSE.

**Notes on recent rule changes:**
- `health` key **removed** from rules. Replaced by `pharmacy` (аптека/ліки) and `doctor` (медицин/лікар). `health` kept as legacy entry in `iconColorMap` only.
- `celebration` **added** (2026-05-31): matches "розваг", "свят", "party", "вечірк".
- `gavel` and `percent` **added** (2026-05-31).
- `server` **added** (2026-05-31): "хостинг/hosting/vps/сервер" extracted from `cloud` — `cloud` now covers only generic cloud storage.
- `shoes`, `toys`, `flower`, `souvenir`, `book`, `store`, `fitness`, `train`, `auto_parts`, `tools`, `hardware`, `dental`, `hotel` **added** (2026-05-31): see ADR-035.
- `clothes` rule: "взуття" removed (→ `shoes`). `doctor` rule: "стоматолог" removed (→ `dental`). `flight` rule: "готель/hotel" removed (→ `hotel`). `bus` rule: "громадськ" added.
- `refund` **added** (2026-05-31): icon `AssignmentReturn` (AutoMirrored), color `#00897B` teal. Matches "повернення", "cashback", "кешбек", "компенсація", "refund". Rule placed before `transfer`.

**Critical rule orderings** (do not reorder these pairs):
- `server` before `cloud` — хостинг is more specific than generic cloud storage
- `wifi` before `phone` — both match "інтернет"-related terms
- `shoes` before `clothes` — взуття is shoes (specific), not general clothing
- `toys` before `family` — іграшки is more specific than general family spending
- `coffee` before `restaurant` — кафе more specific
- `grocery` before `shopping` — продукти more specific
- `flower` and `souvenir` before `celebration` and `gift` — distinct purchase types
- `receipt` before `home` — "рахунки/оплат" more specific than generic home
- `celebration` before `theater` — "розваг" and "свят" go to celebration, not broad leisure
- `book` before `movie` — both leisure, book more specific
- `volunteer` before `pharmacy` before `dental` before `doctor` — wellness → pills → dental → general medical
- `store` before `fitness` before `shopping` — marketplace names → sport goods store → generic shopping
- `train` before `bus` — залізниця more specific than general public transport
- `auto_parts` before `car` — repair/parts more specific than generic vehicle
- `tools` before `hardware` before `home` — power tools → building materials → generic home
- `hotel` before `flight` — accommodation more specific than generic travel
- `theater` → `movie` → `gaming`/`telegram`/`dating` → `ticket` — leisure specificity chain
- `taxi` → `gas_station` → `train` → `bus` → `auto_parts` → `car` — transport specificity chain
- `gavel` and `percent` after `sports` (bottom of list) — narrow keyword sets

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

### EditCategoriesScreen (rewritten 2026-05-31)

`EditCategoriesScreen` lives in `EditCategoriesScreen.kt`. Opened via the pencil icon in `SharedTopBar` on the Categories tab; full-screen `Box` overlay inside `MainScreen`.

**Layout (list-based, not orbital grid):**
- Top bar: ← back + context-sensitive title + "Субкатегорії" `TextButton`
- `TabRow`: ↓ Витрати / ⊕ Доходи
- Scrollable `Column` list — each row (72dp):
  - `DragHandle` icon — long-press activates drag-to-reorder (hidden when `showSubcategories = true`)
  - 44dp colored circle + category icon
  - Category name + "Субкатегорія" label (if `parentId != null`)
  - `[×]` delete button — opens `ConfirmationDialog` before deleting
- "Додати категорію" row at the bottom

**Tap on a row** → opens `CategoryFormSheet` for full editing (same as before).

**Drag-to-reorder:**
- Only enabled when `showSubcategories = false` (root categories only).
- Uses `detectDragGesturesAfterLongPress` on the `DragHandle` icon.
- Local `mutableStateListOf<CategoryEntity>` (`localCats`) is synced from props via `LaunchedEffect(displayed)` when not dragging.
- Every time accumulated drag offset reaches `rowHeightPx / 2`, the item swaps one position in `localCats` and the accumulator resets.
- On drag end: `onReorder(localCats.toList())` → `MainScreen` calls `categoriesViewModel.reorderCategories(it)` → sets `sortOrder = idx` and batch-updates via `CategoryRepository.updateAll` → `CategoryDao.updateCategories`.
- DAO sorts by `sortOrder ASC, name ASC`, so the new order is immediately reflected everywhere.

**`CategoryEntity.sortOrder: Int` (default 0)** — the sort field already existed in the DB schema. No migration required.

**Add/edit/detach subcategory flows:** unchanged from before — `editCategory`, `addSubcategoryFor`, `onDetachSubcategory` all still work via `CategoryFormSheet`.

**Data flow:**
`EditCategoriesScreen` is data-driven (no own ViewModel). Callbacks: `onSave`, `onDelete`, `onAddSubcategory`, `onReorder`, `onDismiss` — all wired in `MainScreen` to `categoriesViewModel`.

## SharedCalcKeypad (`ui/components/calculator/CalcKeypad.kt`)

`SharedCalcKeypad` is the shared 5-column calculator layout used across budget, transaction, transfer, and amount-picker screens.


### Signature

```kotlin
SharedCalcKeypad(
    calc:            CalcStateHolder,
    modifier:        Modifier = Modifier,
    currencySymbol:  String   = "₴",
    confirmColor:    Color    = Color(0xFF4CAF50),
    onConfirm:       () -> Unit,
    onCurrencyClick: (() -> Unit)? = null,
    row2ExtraKey:    (@Composable RowScope.() -> Unit)? = null
)
```
### Key Layout

```
Row 1: ÷  |  7  |  8  |  9  |  ⌫  (backspace — Outlined icon)
Row 2: ×  |  4  |  5  |  6  |  [row2ExtraKey or C]
Row 3: −  |  1  |  2  |  3  |  [= or ✓] (rows 3-4 share weight=2f)
Row 4: +  |  ₴* |  0  |  ,  |
```

*Currency key shows `currencySymbol`. When `onCurrencyClick != null`: bg = `primaryContainer` (highlighted, tappable). When `null`: bg = `surfaceVariant` (inert). Do not pass `onCurrencyClick` unless the caller manages a currency-picker state.

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

### CalcStateHolder display methods

| Method | Returns |
|---|---|
| `displayExprNoSymbol()` | Numeric expression only — `currentStr` or `"${pendingVal} $op $currentStr"` |
| `displayExpr(symbol)` | `"${displayExprNoSymbol()} $symbol"` — delegates to `displayExprNoSymbol()` |

Use `displayExprNoSymbol()` when the currency symbol needs to be a separate interactive element. Use `displayExpr(symbol)` for a plain non-interactive string (e.g., `AmountCalculatorSheet` preview).

### Backspace Icon

Backspace uses `Icons.AutoMirrored.Outlined.Backspace` (outlined, not filled). Do not revert to `Filled.Backspace`.

### Hardware Keyboard Support

`SharedCalcKeypad` automatically requests focus on composition (`FocusRequester` + `LaunchedEffect`) and handles `onKeyEvent` via `focusable()`. Two-tier dispatch:

1. **Character-based** (`utf16CodePoint`): catches `0-9`, `+`, `-`, `*`, `/`, `,`, `.`, `=` — including Shift-combos (Shift+8 = `*`, Shift+= = `+`).
2. **Key-code-based**: numpad keys (`Key.NumPad0`…`Key.NumPad9`, `NumPadDivide`, `NumPadMultiply`, `NumPadSubtract`, `NumPadAdd`, `NumPadComma`), `Backspace` → `⌫`, `Delete` → `C`, `Enter`/`NumPadEnter` → `=` or `onConfirm`.

Both tiers return `true` (event consumed) on match. Non-matching events return `false` to propagate normally.

**Rule:** Do not remove `focusable()` or `focusRequester` from `SharedCalcKeypad` — they are required for hardware keyboard events to reach the composable. The `try { requestFocus() } catch` guard prevents crashes when called before the composable is attached to the layout.

## Budget Screen

`BudgetScreen` (`Бюджет` tab) is a `Column { LazyColumn(weight=1f) + IncomeBudgetBar }` layout.

### Budget Input Sheets — Currency Symbol

`BudgetInputSheet` derives the currency symbol from `catRow.category.currencyCode` via `CURRENCIES_ALL`. The calculator display and `SharedCalcKeypad` both receive this resolved symbol. **Do not hardcode "₴"** — always use the entity's currency code.

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

**Rule**: The header shows budget-based savings (incomeBudget − expenses), not cash-flow savings. This matches "Доступно в бюджеті" in `IncomeBudgetBar`. The `incomeBudget` parameter passed to `SavingsSectionCard` is `effectiveIncomeBudget` (= `incomeSection.totalBudget`) — both the card and the bar use the same value to stay consistent.

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

**Signature (current):**
```kotlin
IncomeBudgetBar(
    effectiveIncomeBudget: Double,       // = incomeSection.totalBudget
    expenseTotal:          Double,
    hasIncomeCategories:   Boolean,      // always clickable when true
    onClick:               () -> Unit,
    modifier:              Modifier = Modifier
)
```

- `hasBudget = effectiveIncomeBudget > 0`
- Bar is **always clickable** when `hasIncomeCategories == true`
- When `hasBudget && overspend > 0`: red overspend row
- When `hasBudget`: "Доступно в бюджеті: X ₴" (available = effectiveIncomeBudget − expenseTotal)
- When `!hasBudget`: italic "Введіть суму очікуваного доходу..."

### IncomeCategoryPickerSheet

Opens when the user taps `IncomeBudgetBar` and there are **2+ income categories**. Shows income categories as a list — user picks one, then `BudgetInputSheet` opens for that category. Defined in `BudgetSheets.kt`.

**Flow:**
1. Tap bar → `state.incomeSection.rows.size == 1` → directly open `BudgetInputSheet` for the only category
2. Tap bar → multiple categories → open `IncomeCategoryPickerSheet`
3. Pick category → close picker + open `BudgetInputSheet` for chosen category

**Layout:**
- `ModalBottomSheet`, surface background
- "Бюджет доходів" title + `monthLabel` subtitle
- `LazyColumn` of `ListItem` rows: 40dp colored circle icon + category name + "отримано X ₴" subtitle
- Trailing: budget amount pill (`"X ₴" + "в бюджеті"`) if `budgetAmount > 0`; `Icons.Default.Add` otherwise

**Rule:** Income budget is **always per-category** (stored in `CategoryEntity.budgetAmount`). There is no global income budget field in `BudgetUiState` or `BudgetViewModel`. Do not store income budget targets in `SettingsRepository`.

### BudgetUiState — Simplified Structure

```kotlin
data class BudgetUiState(
    val selectedMonth:  BudgetSelMonth    = …,
    val appMonth:       AppMonth          = …,
    val daysInMonth:    Int               = 31,
    val daysPassed:     Int               = 0,
    val pillLabel:      String            = "",
    val pillBadge:      String            = "31",
    val totalBalance:   Double            = 0.0,
    val expenseSection: BudgetSectionData = …,
    val incomeSection:  BudgetSectionData = …
)
```

No `accounts`, `globalIncomeBudget`, or `incomeBudgetAccountId` fields. `BudgetViewModel` uses a single `combine(5 flows)` — no nested outer combine needed.

`effectiveIncomeBudget` in `BudgetScreen`:
```kotlin
val effectiveIncomeBudget = state.incomeSection.totalBudget
```

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

### AddTransactionScreen — Currency Selection

Two entry points open `CurrencyPickerSheet` in `AddTransactionScreen`:
1. **Currency symbol key** — bottom-left key (row 4) of `SharedCalcKeypad`
2. **Currency symbol in the amount display** — the `" ₴"` / `" $"` / `" €"` suffix rendered next to "0" above the keypad

Both set `showCurrencyPicker = true`, which opens `CurrencyPickerSheet(title = "Валюта транзакції")`.

**Amount display layout** (split `Row`, not a single `Text`):
```kotlin
Row(verticalAlignment = CenterVertically) {
    Text(calc.displayExprNoSymbol(), fontSize=34.sp, ...)        // numeric part — not clickable
    Text(" $currencySymbol", fontSize=34.sp,                     // symbol — clickable
        modifier = Modifier.clickable { showCurrencyPicker = true })
}
```

**`CalcStateHolder.displayExprNoSymbol()`:** returns the numeric expression without the currency symbol:
- No pending op: `currentStr` (e.g. `"0"`, `"123"`, `"45,6"`)
- Pending op: `"${nf.format(pendingVal)} $pendingOp $currentStr"` (e.g. `"100 + 50"`)

`displayExpr(symbol)` is now a one-liner that delegates: `"${displayExprNoSymbol()} $symbol"`.

**State:**
```kotlin
var selectedCurrency by remember { mutableStateOf(fromAccount?.currency ?: "UAH") }
LaunchedEffect(fromAccount?.currency) { selectedCurrency = fromAccount?.currency ?: "UAH" }
val currencySymbol = remember(selectedCurrency) {
    CURRENCIES_ALL.find { it.code == selectedCurrency }?.symbol ?: selectedCurrency
}
```
- Initialises from the selected account's currency; syncs when the user switches accounts.
- `SharedCalcKeypad` receives both `currencySymbol` and `onCurrencyClick = { showCurrencyPicker = true }`.

**Note:** The selected currency is UI-only — `TransactionEntity` has no `currency` field. It affects only the display symbol during entry.

**`CurrencyPickerSheet` — `title` parameter:** `CurrencyPickerSheet` (`AccountPickerSheets.kt`) now has `title: String = "Валюта рахунку"` so it can be reused with context-specific titles. Default "Валюта рахунку" used by `AccountFormSheet`; "Валюта транзакції" used by `AddTransactionScreen`.

### AddTransactionScreen — Right Panel (Category / Transfer Picker)

The **right panel** in `AddTransactionScreen` always opens `CategoryPickerSheet` (was a `DropdownMenu` — removed). The panel tap sets `showCatPicker = true` for all transaction types:

```kotlin
.clickable { showCatPicker = true }   // was: if (isTransfer) showToAccPicker else showCatPicker
```

The label shown on the right panel:
- Expense/Income: **"До категорії"** (was "Категорія")
- Transfer: **"На рахунок"**

`CategoryPickerSheet` is called with `currentType = state.type`. When user picks a category, `viewModel.setType(cat.type)` is also called so the transaction type stays in sync with the selected category. For transfers, `onTransfer(acc)` sets `toAccountId` and forces `TRANSFER` type.

### CategoryPickerSheet — Simplified Mode (`currentType`)

`CategoryPickerSheet` supports two rendering modes:

| `currentType` | Mode |
|---|---|
| `null` | **Tabbed** — 3 tabs (Income / Expense / Transfer) with full layout |
| non-null | **Simplified** — no tabs, single type, smaller height (55% screen) |

**Simplified mode layout:**
- Header row: type icon + type label (e.g. "Витрата")
- Content: `CategoryGrid` (4-column `LazyVerticalGrid`) for EXPENSE/INCOME, or account list for TRANSFER
- Account list (transfer): "Рахунки" title + total balance + `AccountPickerRow` per account

**Full tabbed mode (default):** 3 tabs — 0=Дохід, 1=Витрата, 2=Переказ. `initialTab: Int = 1` sets the initially selected tab (default Витрата).

**`CategoryGrid`** (private composable in `CategoryPickerSheet.kt`): `LazyVerticalGrid(GridCells.Fixed(4))` — reused by both modes for income/expense tabs.

**Signature:**
```kotlin
CategoryPickerSheet(
    expenseCategories: List<CategoryEntity>,
    incomeCategories:  List<CategoryEntity>,
    accounts:          List<AccountEntity>,
    categorySpending:  Map<Long?, Double>,
    initialTab:        Int = 1,
    currentType:       TransactionType? = null,
    onSelect:          (CategoryEntity) -> Unit,
    onTransfer:        (AccountEntity) -> Unit,
    onDismiss:         () -> Unit
)
```

**Rule:** Pass `currentType = state.type` from `AddTransactionScreen` to open the correct mode automatically. Pass `currentType = null` from `TransactionsListScreen`/search screens that need full tab navigation.

Sheet/dialog composables for the transactions tab were split from `TransactionSheets.kt` (deleted) into four dedicated files in `ui/transactions`:

| File | Contents |
|---|---|
| `TxSearchScreen.kt` | `TxSearchScreen`, `SearchSectionHeader`, `TypeFilterCard`, `ColoredFilterChip` |
| `CategoryPickerSheet.kt` | `CategoryPickerSheet`, `CategoryPickerCell`, `AccountPickerRow`, `CategoryGrid` |
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
