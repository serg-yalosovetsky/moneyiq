# UI Contracts — Transactions & Overview Screens

## AddTransactionScreen — Currency Selection

Two entry points open `CurrencyPickerSheet`:
1. **Currency key** — bottom-left key (row 4) of `SharedCalcKeypad`
2. **Currency symbol in the amount display** — clickable `Text(" $currencySymbol")` next to "0"

Both set `showCurrencyPicker = true` → `CurrencyPickerSheet(title = "Валюта транзакції")`.

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

`onSelect` also calls `viewModel.setType(cat.type)` to keep type in sync.

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
