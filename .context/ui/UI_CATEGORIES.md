# UI Contracts — Categories Screen

## Chip Dimensions (CRITICAL — do not change without audit)

```
CHIP_WIDTH              = 116.dp
CHIP_HEIGHT             = 136.dp   // must stay ≥136dp; reducing clips spending text
CHIP_CIRCLE_SIZE        = 60.dp
CHIP_WIDTH_COMPACT      = 82.dp
CHIP_HEIGHT_COMPACT     = 112.dp
CHIP_CIRCLE_COMPACT     = 40.dp
CATEGORY_VERTICAL_GAP   = 8.dp
DONUT_SECTION_HEIGHT    = CHIP_HEIGHT * 2 + CATEGORY_VERTICAL_GAP  // = 280dp
SUBCATEGORY_PANEL_WIDTH  = 150.dp
SUBCATEGORY_PANEL_HEIGHT = 76.dp
```

**CRITICAL spacing rule:** Never use `Modifier.height(N).padding(bottom = K)` on chip rows. Use `LazyColumn(verticalArrangement = Arrangement.spacedBy(CATEGORY_VERTICAL_GAP))` and `Modifier.height(chipHeight)` with no bottom padding.

The chip name `Box` uses `heightIn(min=28.dp, max=40.dp)` (compact: `min=22.dp, max=32.dp`), not a fixed height.

## Chip Visual Logic

- **All categories always shown**, including 0-spending (appear "pale").
- Icon circle solid (white icon): `spending > 0`; tinted (alpha 0.13): `spending == 0`
- **Budget row** (position 2, above icon): remaining budget `budgetAmount - spending`; over-budget shown as colored pill; `budgetAmount == 0` shows "0 ₴" dimmed.
- `budgetOverride: Double?` — overrides `category.budgetAmount` (used by Budget screen).
- **+N badge**: `showChildBadge && childCount > 0`; 18dp/16dp compact, `primary` bg, 8sp/7sp compact.
- Spending text: category color if `> 0`; grey (onSurface 35%) if `== 0`.
- `sorted = categories.sortedByDescending { spending[it.id] }` — simple sort, no grouping.

## Donut Chart Layout (1Money-style)

```
[0]     [1]      [2]      [3]     ← topRow (4 chips)
[4]  [ DONUT (280dp) ]  [6]       ← mid rows 1-2
[5]  [               ]  [7]
[8]  [      +        ]  [9]       ← mid row 3: midLeft3 | AddCategoryChip | midRight3
[10]   [11]   [12]   [13]         ← extCats = display.drop(10), rows of 4
```

- `topRow = display.take(4)`; `midLeft = [4,5]`; `midRight = [6,7]`; `midLeft3 = getOrNull(8)`; `midRight3 = getOrNull(9)`; `extCats = drop(10)`
- Donut center tap toggles `selectedTab` (EXPENSE/INCOME).
- **Subcategory mode** (`showSubcategories = true`): same orbital layout; `showChildBadge = false`; caller passes only `parentId != null` categories.
- `parentColors` resolves from `allCategoriesForTab` (contains all, not just roots).

### Donut Subcategory Focus

When a root category is double-tapped and `hasExpandedStrip == true`:

```kotlin
val donutCats = if (hasExpandedStrip) expandedChildren else categories
val donutExpense = if (hasExpandedStrip && selectedTab == 0) expandedChildren.sumOf { spending[it.id] ?: 0.0 } else totalExpense
```

**Rule:** Do not pass all `categories` to DonutChart when `hasExpandedStrip == true`.

## Expanded Subcategory Strip

Triggered by double-click on a chip. The chip row and its strip are **one LazyColumn item** (wrapped in `Column` — eliminates gap).

- `inline = true`: `HorizontalDivider(parent color 18% alpha)` + `Column(parent color 7% alpha)`. No Card.
- `inline = false` (legacy): wrapped in `Card(RoundedCornerShape(16dp))`.
- `flatBottom = true` on `CategoryChip`: bottom corners 0dp (fuses chip and strip visually).
- Children: `weight(1f)` per child (max 4), sorted by spending desc; only `spending > 0` OR `budgetAmount > 0`.
- Child icon circles: 44dp; icon 22dp; name 10sp; spending 9sp SemiBold.

**Rule:** All three insertion points (`topRow`, `mid_section`, `extCats`) use `inline = true`. Do not add a separate LazyColumn item for the strip.

## Same-Name Subcategory Deduplication

Any child whose `name.trim().lowercase()` matches the parent's is excluded from `expandedChildren` and `childCounts`. Applied at display layer only — DB retains the child record.

## graphicsLayer Prohibition In Category Chips

Do **not** use `.graphicsLayer { clip = false }` on chip containers — creates a hardware layer above `Dialog` z-level, causing chips to bleed through `CategoryActionSheet` scrim.

Expansion ring radius (28dp normal / 24dp compact) fits within chip bounds without the modifier.

## Category Aggregation (`effectiveSpending`)

When `showSubcategories = false`:
```kotlin
val result = spending.toMutableMap()
allCategoriesForTab.filter { it.parentId != null }.forEach { child ->
    child.parentId?.let { pid -> result[pid] = (result[pid] ?: 0.0) + (spending[child.id] ?: 0.0) }
}
```
Happens inside `CategoriesGridContent`, NOT in the ViewModel.

## QuickExpenseSheet — Panel Layout

Opens on single-tap of a category chip. Two-panel header (80dp):

| Type | Left panel | Right panel |
|---|---|---|
| **EXPENSE** | Account (indigo `#3949AB` bg) — tappable | Category (category color bg) — tappable → `onDismiss()` |
| **INCOME** | Category (category color bg) — tappable → `onDismiss()` | Account (indigo bg) — tappable |

Account panel tappable only when `accounts.size > 1`. Category panel tapping calls `onDismiss()` (back to grid).

**`onSave` signature (6 parameters):**
```kotlin
onSave: (accountId: Long, amount: Double, note: String, date: Long,
         repeatMode: String, reminderMode: String) -> Unit
```

**Zero-amount UX:** `if (amt > 0.0)` guard. When amount = 0 and user presses ✓, `amountError = true` triggers 600ms red flash (`animateColorAsState`). Do not remove the guard.

**Repeat/reminder:** `repeatMode`/`reminderMode` are local state from `CalcDateSheet → RepeatDialog / ReminderDialog`. Forwarded to `CategoriesViewModel.recordTransaction` / `TransactionsListViewModel.recordTransaction`. See `ADR_LOG.md#ADR-042` for full repeat automation.

**Currency:** `selectedCurrency` synced from `selectedAccount?.currency`. ₴ key → `CurrencyPickerSheet`.

**Rule:** Do not merge EXPENSE/INCOME panel layouts. Alignment anchors differ between variants — always use the `isIncome` branch.

## CategoryActionSheet Data

Uses **effective spending** (children aggregated into parent):
```kotlin
val catSpending = effectiveSpending[cat.id] ?: 0.0
val catTxCount  = allCategoriesForTab
    .filter { it.id == cat.id || it.parentId == cat.id }
    .sumOf { state.monthTxCounts[it.id] ?: 0 }
```

## Light-Background Contrast Rule

Any sheet using category/account color as background must compute content color:
```kotlin
val isLightBg    = catColor.luminance() > 0.5f
val onCatColor   = if (isLightBg) Color(0xFF1C1B1F) else Color.White
val displayColor = if (isLightBg) Color(0xFF1C1B1F) else catColor
```
**Affected:** `BudgetInputSheet`, `CategoryActionSheet` header, `QuickExpenseSheet` category panel. Do not hardcode `Color.White` inside colored sections.

## CategoryFormSheet

**Title logic:**
| Condition | Title |
|---|---|
| `existing == null && forParentId != null` | "Нова субкатегорія" |
| `existing == null` | "Нова категорія" |
| `existing.parentId != null` | "Субкатегорія" |
| `existing.parentId == null` | "Категорія" |

**Subcategories section:** only for root categories (`parentId == null`). Shows existing children + `LinkOff` detach button + "Додати підкатегорію" (when callback provided). Child categories never show this section.

**Name input:** inline `BasicTextField` in header. New category: `FocusRequester.requestFocus()` immediately. `ImeAction.Done` → `focusManager.clearFocus()`. `TextInputDialog` is NOT used for category names.

**Icon auto-suggest:** → see `UI_CATEGORY_ICONS.md`

## EditCategoriesScreen

Lives in `EditCategoriesScreen.kt`. Full-screen `Box` overlay opened via pencil icon in `SharedTopBar`.

**Layout:** Same grid as the main categories screen — reuses `CategoriesGridContent` with `childCounts = emptyMap()` (badges suppressed), `sortBySpending = false`.

**Chip gestures in edit mode:**
- **Short tap** → opens `CategoryFormSheet` for that category (`onChipClick`)
- **Long press + drag** → drag-and-drop reorder; the chip follows the finger (ghost overlay)
- `onChipLongClick = {}` (empty) — the edit form must NOT open on long press because it would interrupt drag

**Drag-to-reorder internals:**
- `onChipDragSwap` callback passed to `CategoriesGridContent` → enables drag mode
- In drag mode: original chip slot → alpha 0 (invisible); a ghost `Box` (shadow + full chip rendering) is drawn in an outer `Box` overlay and follows the finger via `dragOffset` state
- `dragOffset` accumulates `onDrag` deltas; ghost position = `chipCenters[draggingId] − containerRootPos + dragOffset − chipSize/2`
- Nearest chip to the current finger position becomes `hoverTarget` (alpha 1.0); others dim to 0.7
- On `onDragEnd`: calls `onChipDragSwap(fromId, toId)` → `localCats` swap → `onReorder(localCats.toList())` → `categoriesViewModel.reorderCategories` → `CategoryDao.updateCategories`

**`suppressLongPress` mechanism (gesture conflict fix):**
`CategoryGridSlot` has `suppressLongPress: Boolean = false`. When `true`, `CategoryChip` receives `onLongPress = null`, which makes `combinedClickable` skip the long-press detector entirely. Without this, `combinedClickable` (child, Main pass) consumes the long-press event before the parent `detectDragGesturesAfterLongPress` (parent, Main pass) can claim subsequent drag events — causing a brief flash and no actual drag.
`CategoriesGridContent` sets `suppressLongPress = (onChipDragSwap != null)` on every `CategoryGridRow` and direct `CategoryGridSlot` call.

**Local reorder state:**
```kotlin
val localCats = remember(selectedTab) { mutableStateListOf<CategoryEntity>() }
LaunchedEffect(allCategoriesForTab) { localCats.clear(); localCats.addAll(allCategoriesForTab) }
```
`localCats` drives the grid so swaps are visible immediately. DB write happens via `onReorder`.

**Data flow:** No own ViewModel. All callbacks wired in `MainScreen` to `categoriesViewModel`.
