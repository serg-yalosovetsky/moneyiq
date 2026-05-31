# UI Contracts — Transactions & Overview Screens

## AddTransactionScreen — Currency Selection

Two entry points open `CurrencyBottomSheet`:
1. **Currency key** — bottom-left key (row 4) of `SharedCalcKeypad`
2. **Currency symbol in the amount display** — clickable `Text(" $currencySymbol")` next to "0"

Both set `showCurrencyPicker = true` → `CurrencyBottomSheet(title = "Валюта транзакції")`.

**Why `CurrencyBottomSheet` (not `CurrencyPickerSheet`):** `CurrencyPickerSheet` is a Dialog (`usePlatformDefaultWidth = false`). When opened from a navigation screen, Android dispatches `ACTION_UP` to the new Dialog window before its `Surface(fillMaxSize)` renders, hitting the transparent margin → `dismissOnClickOutside` fires immediately. `CurrencyBottomSheet` is a `ModalBottomSheet` and does not have this problem. See ADR-051.

**Amount display layout** (split `Row`):
```kotlin
Row(verticalAlignment = CenterVertically) {
    Text(calc.displayExprNoSymbol(), fontSize=34.sp, ...)   // not clickable
    Text(" $currencySymbol", fontSize=34.sp,                // clickable
        modifier = Modifier.clickable { showCurrencyPicker = true })
}
```

**State:**
```kotlin
var selectedCurrency by remember { mutableStateOf(fromAccount?.currency ?: "UAH") }
LaunchedEffect(fromAccount?.currency) { selectedCurrency = fromAccount?.currency ?: "UAH" }
val currencySymbol = remember(selectedCurrency) {
    CURRENCIES_ALL.find { it.code == selectedCurrency }?.symbol ?: selectedCurrency
}
```

The selected currency is UI-only — `TransactionEntity` has no `currency` field.

## AddTransactionScreen — Right Panel (Category / Transfer Picker)

The right panel tap always opens `CategoryPickerSheet(currentType = state.type)`. Label:
- EXPENSE/INCOME: "До категорії"
- TRANSFER: "На рахунок"

**`onSelect` order (CRITICAL):** Always call `setType` before `setCategory`:
```kotlin
viewModel.setType(cat.type)   // first — clears selectedCategoryId internally
viewModel.setCategory(cat.id) // then — sets the new id (would be wiped if reversed)
```
`setType` resets `selectedCategoryId = null` to prevent stale category from a different type. Reversing the order results in no category being set.

## AddTransactionScreen — BackHandler Pattern

`AddTransactionScreen` uses a **single top-level `BackHandler`** to guard all overlay states:

```kotlin
BackHandler(enabled = showCatPicker || showCurrencyPicker || showFromAccPicker || showDatePicker || showDeleteDialog) {
    when {
        showCatPicker      -> showCatPicker = false
        showCurrencyPicker -> showCurrencyPicker = false
        showFromAccPicker  -> showFromAccPicker = false
        showDatePicker     -> showDatePicker = false
        showDeleteDialog   -> showDeleteDialog = false
    }
}
```

Placed **before** `Scaffold`, not inside any `if (showX)` block.

**Rule:** Do not move BackHandler inside `if (showCatPicker)`. When `onDismissRequest` fires (`showCatPicker = false`), a nested BackHandler would immediately deactivate — allowing any in-flight back gesture (predictive back, Samsung right-edge swipe) to propagate to NavController and pop `AddTx`.

## CategoryPickerSheet — Simplified Mode (`currentType`)

| `currentType` | Mode |
|---|---|
| `null` | **Tabbed** — 3 tabs (Income / Expense / Transfer), `initialTab: Int = 1` |
| non-null | **Simplified** — no tabs, smaller height (55% screen) |

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

**Rule:** Pass `currentType = state.type` from `AddTransactionScreen`. Pass `currentType = null` from `TransactionsListScreen`/search for full tab navigation.

**Rule:** Do not revert to `DropdownMenu` for category/transfer selection in `AddTransactionScreen`.

## Transaction Sheet Files

Composables split from `TransactionSheets.kt` (deleted) into:

| File | Contents |
|---|---|
| `TxSearchScreen.kt` | `TxSearchScreen`, `SearchSectionHeader`, `TypeFilterCard`, `ColoredFilterChip` |
| `CategoryPickerSheet.kt` | `CategoryPickerSheet`, `CategoryPickerCell`, `AccountPickerRow`, `CategoryGrid` |
| `TransferQuickSheet.kt` | `TransferQuickSheet` |
| `TransactionDetailSheet.kt` | `TransactionDetailSheet` |

All `internal`. Do not mark `private`.

## Overview Screen

`OverviewScreen` (`Огляд` tab) — monthly totals, daily bar chart, stats row, list section.

**Mode toggle:** EXPENSE / INCOME — all amounts, chart bars, and list reflect the active mode.

**List section priority:**
1. Category rows (spending > 0 for mode + period)
2. Transaction rows via `TransactionListItem` (categories empty, transactions present)
3. "Немає категорій" empty state

Tapping a fallback transaction row is a no-op. `OverviewUiState.transactions` holds mode-filtered tx list.

**Category row icons:** call `categoryIconFor(row.icon)` from `CategoryIcons.kt` (48 keys). Do not add a local icon mapper in `OverviewScreen.kt`.

**Category Detail Sheet:** tapping a category row opens `CategoryDetailSheet` (`OverviewSheets.kt`) as `ModalBottomSheet` with category color as container.

### SpendingChart — Stacked Category Bars

`DayBar` carries `segments: List<CategorySegment>` — each segment has `colorHex` and `amount`, sorted largest-first by the ViewModel (`buildState`).

**Bar width:** `gap = slotW * 0.45f` → bar occupies ~55% of each day slot (thinner columns with visible gaps).

**Drawing order:** Loop starts at `currentBottom = h` and draws each segment upward. First segment (largest amount) anchors the bottom; smaller segments stack on top.

**Fallback:** If `segments.isEmpty()` (income with no category, or uncategorised transactions) → single `accentColor` bar.

**Rule:** `SpendingChart` draws segments in the order they arrive in `DayBar.segments`. Do not re-sort in the composable — sorting is the ViewModel's responsibility.
