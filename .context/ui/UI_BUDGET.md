# UI Contracts — Budget Screen

`BudgetScreen` (`Бюджет` tab) — `Column { LazyColumn(weight=1f) + IncomeBudgetBar }` layout.

## BudgetInputSheet — Signature

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

- Floating category icon circle (top-right) is clickable when `onIconClick != null`.
- `pickedCurrency` initialised from `catRow.category.currencyCode`; synced via `LaunchedEffect(catRow.category.id)`.
- ₴ key → `showCurrencyPicker = true` → `Dialog(usePlatformDefaultWidth=false)` with `CurrencyPageContent` (130+ currencies, 3 tabs).
- Currency symbol resolved via `CURRENCIES_ALL`. **Do not hardcode "₴".**
- `BudgetViewModel.updateCategoryBudget(category, newBudget, currency)` persists both fields.

**Rule:** Only `BudgetInputSheet` owns the currency picker for category budgets. The initial currency comes from the category entity — user may override per-session.

**Rule:** Do not use a nested `ModalBottomSheet` for a picker on top of another sheet. Use `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))`.

## BudgetSectionCard — Row vs Chip Rules

`BudgetSectionCard` has an `incomeMode: Boolean = false` parameter that controls how category rows are rendered.

| Mode | `budgetedRows` | `chipRows` |
|---|---|---|
| `incomeMode = false`, `currentExpensesMode = false` | categories with `budgetAmount > 0` → full rows | categories with `budgetAmount == 0` and `amount > 0` → chips |
| `incomeMode = false`, `currentExpensesMode = true` | empty | all categories with `amount > 0`, sorted by amount desc |
| `incomeMode = true` | **always empty** | all income categories with `amount > 0` OR `budgetAmount > 0`, sorted by amount desc |

The income section always passes `incomeMode = true` — income categories never appear as full rows.

### `BudgetCatFullRow` — Layout

```
[52dp circle]   CategoryName          139 ₴   ← remaining (accentColor)
  spent ₴ (bold, cat color)          в бюджеті 200 ₴  ← grey labelSmall
```

- **Spending** is a small Bold text displayed **below** the icon circle (category color).
- **Within budget:** remaining = `budget − spent`, shown as plain text in `accentColor`.
- **Overbudget:** `Surface(RoundedCornerShape(50), color=accentColor)` pill with white Bold text showing `|remaining|` (absolute overspend). Row background (`color.copy(alpha=0.10f)`) already provides visual context — no extra color logic needed.

### `MoreLessChip` — Hidden Total

`MoreLessChip(expanded: Boolean, hiddenTotal: Double, onClick: () -> Unit)`:
- Shows `"${formatMoney(hiddenTotal)} ₴"` below the chevron when collapsed and `hiddenTotal > 0`.
- `hiddenTotal = chipRows.drop(3).sumOf { it.amount }` — computed in `BudgetSectionCard`.
- Shows `" "` (spacer) when expanded.

**Rule:** Do not show income categories as full rows. Even when `budgetAmount > 0`, income categories must be chips — the budget is a declared expectation, not a spending cap.

**Rule:** Do not revert overbudget display to a negative number in error color. The pill badge is the canonical overbudget indicator for budget rows.

## SavingsSectionCard — Formula

```
realSavings   = incomeTotal - expenseTotal
actualSavings = if (incomeBudget > 0) incomeBudget - expenseTotal else realSavings

hasForecast = daysPassed > 0 && daysInMonth > daysPassed && expenseTotal > 0
projectedExpenses = expenseTotal / daysPassed * daysInMonth
projectedSavings  = if (incomeBudget > 0) incomeBudget - projectedExpenses
                    else realSavings - (projectedExpenses - expenseTotal)
```

Display:
- **Header right**: `projectedSavings` when `hasForecast`; `actualSavings` otherwise.
- **"прогноз"** label when forecast active.
- **"збережено X ₴"**: only when `incomeTotal > 0`.
- Color: green `#26A69A` when ≥ 0, pink `#D81B60` when negative.

**Rule:** Header shows budget-based savings (incomeBudget − expenses). `incomeBudget` parameter = `effectiveIncomeBudget`. Both `SavingsSectionCard` and `IncomeBudgetBar` must receive the same value.

## IncomeBudgetBar Layout (CRITICAL)

`IncomeBudgetBar` is **outside** the `LazyColumn` — pinned to the bottom of the screen:

```
Column(fillMaxSize) {
    LazyColumn(Modifier.weight(1f)) { … }
    IncomeBudgetBar(modifier = Modifier.padding(bottom = bottomPadding))
}
```

**Do not** move `IncomeBudgetBar` inside the `LazyColumn`.

**Signature:**
```kotlin
IncomeBudgetBar(
    effectiveIncomeBudget: Double,
    expenseTotal:          Double,
    hasIncomeCategories:   Boolean,
    onClick:               () -> Unit,
    modifier:              Modifier = Modifier
)
```

- `hasBudget = effectiveIncomeBudget > 0`
- Always clickable when `hasIncomeCategories == true`
- When `hasBudget && overspend > 0`: red overspend row
- When `!hasBudget`: italic "Введіть суму очікуваного доходу..."

## IncomeCategoryPickerSheet

Opens when tapping `IncomeBudgetBar` with 2+ income categories.

**Flow:**
1. Tap bar → 1 income category → directly open `BudgetInputSheet`
2. Tap bar → multiple categories → open `IncomeCategoryPickerSheet`
3. Pick → close picker + open `BudgetInputSheet`

**Rule:** Income budget is always per-category (`CategoryEntity.budgetAmount`). No global income budget in `BudgetUiState`/`BudgetViewModel`.

## BudgetUiState — Structure

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

No `accounts`, `globalIncomeBudget`, or `incomeBudgetAccountId`. Single `combine(5 flows)`.

```kotlin
val effectiveIncomeBudget = state.incomeSection.totalBudget
```
