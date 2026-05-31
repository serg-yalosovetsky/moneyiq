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

## BudgetSectionCard — Section Header Layout

The section header (title + totals row) also uses a `drawBehind` progress bar:

```kotlin
val headerProgress = if (data.totalBudget > 0.0)
    (data.totalAmount / data.totalBudget).coerceIn(0.0, 1.0).toFloat()
else 0f

Row(modifier = Modifier.height(IntrinsicSize.Min).drawBehind {
    drawRect(Color.White)
    drawRect(accentColor.copy(alpha = 0.20f), size = Size(size.width * headerProgress, size.height))
}) { ... }
```

- `headerProgress = totalAmount / totalBudget`, capped at 1.0 (overbudget = full fill).
- Same pattern as `BudgetCatFullRow`: `Color.White` base + filled portion in `accentColor`.
- When `totalBudget == 0`, `headerProgress = 0f` (white header, no fill).

**Rule:** Section header and individual category rows use identical fill logic — do not give the header a flat tinted background while rows have a progress bar.

## BudgetSectionCard — Row vs Chip Rules

`BudgetSectionCard` uses the same rendering logic for both expense and income sections — `incomeMode` was removed.

| `currentExpensesMode` | `budgetedRows` | `chipRows` |
|---|---|---|
| `false` (default) | categories with `budgetAmount > 0` → full rows | categories with `budgetAmount == 0` and `amount > 0` → chips |
| `true` | empty | all categories with `amount > 0`, sorted by amount desc |

Income categories with a `budgetAmount > 0` appear as **full rows** (`BudgetCatFullRow`), exactly like expense categories. The progress bar shows `received / budget`. Categories without a budget only appear as chips if they have spending.

### `BudgetCatFullRow` — Layout

```
[52dp circle]   CategoryName               139 ₴  ← remaining, titleMedium Bold accentColor
                spent ₴ (labelSmall,       в бюджеті  200 ₴  ← grey, number Bold
                         accentColor)
```

Row padding: `horizontal = 12.dp, vertical = 10.dp`. Icon circle: 52dp, inner icon: 26dp.

- **Spending** (`row.amount`) is a small `labelSmall` text **below the category name** in the middle `Column` (not below the icon). Color: `accentColor`.
- **Within budget:** remaining shown top-right as `titleMedium Bold` in `accentColor`.
- **Overbudget:** `Surface(RoundedCornerShape(50), color=accentColor)` pill with white Bold `labelMedium` text showing `|remaining|` (absolute overspend).
- **"в бюджеті X ₴"** uses two adjacent `Text` composables — prefix in normal weight, budget number in `FontWeight.Bold` — both same `labelSmall` style and alpha 0.45f.
- **Progress bar / background (via `drawBehind`):**
  - **Normal (within budget):** base `Color.White` + `color.copy(alpha=0.18f)` over `width * progress`. `progress = (spent / budget).coerceIn(0.0, 1.0)`.
  - **Overbudget:** solid `color.copy(alpha=0.12f)` fill — the category's own color tints the entire row to visually flag it. No progress bar drawn.
  - Do **not** use a plain `.background()` — it cannot show the progress split for in-budget rows.
  - Do **not** use a tinted base for in-budget rows — unfilled area must be white.

### Chip Row Background

The chip row (unbudgeted categories + `MoreLessChip`) uses `Color.White` background. Do **not** use `MaterialTheme.colorScheme.surface` or any alpha tint — the row must appear white to match the unfilled area of the full rows above it.

### `MoreLessChip` — Hidden Total

`MoreLessChip(expanded: Boolean, hiddenTotal: Double, onClick: () -> Unit)`:
- Shows `"${formatMoney(hiddenTotal)} ₴"` below the chevron when collapsed and `hiddenTotal > 0`.
- `hiddenTotal = chipRows.drop(3).sumOf { it.amount }` — computed in `BudgetSectionCard`.
- Shows `" "` (spacer) when expanded.

**Rule:** Income categories with `budgetAmount > 0` **must** appear as full rows, same as expense categories. The progress bar shows `received / budget` (received = `amount`, budget = `budgetAmount`). Do not revert to chips for income categories that have a budget set.

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
