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
- ₴ key → `showCurrencyPicker = true` → `ModalBottomSheet` listing `CURRENCIES_MAIN`.
- Currency symbol resolved via `CURRENCIES_ALL`. **Do not hardcode "₴".**
- `BudgetViewModel.updateCategoryBudget(category, newBudget, currency)` persists both fields.

**Rule:** Only `BudgetInputSheet` owns the currency picker for category budgets. The initial currency comes from the category entity — user may override per-session.

**Rule:** Do not use a nested `ModalBottomSheet` for a picker on top of another sheet. Use `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))`.

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
