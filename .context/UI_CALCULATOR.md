# UI Contracts — SharedCalcKeypad

`SharedCalcKeypad` (`ui/components/calculator/CalcKeypad.kt`) — shared 5-column calculator layout.

## Signature

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

## Key Layout

```
Row 1: ÷  |  7  |  8  |  9  |  ⌫
Row 2: ×  |  4  |  5  |  6  |  [row2ExtraKey or C]
Row 3: −  |  1  |  2  |  3  |  [= or ✓]   (rows 3-4 share weight=2f)
Row 4: +  |  ₴* |  0  |  ,  |
```

*Currency key shows `currencySymbol`. `onCurrencyClick != null` → `primaryContainer` bg (interactive). `null` → `surfaceVariant` (inert). Do not pass `onCurrencyClick` unless the caller manages a currency-picker state.

## `row2ExtraKey` Callers

| Caller | row2ExtraKey |
|---|---|
| `AddTransactionScreen` | Calendar/date chip |
| `TransactionDetailSheet` | Date chip |
| `TransferQuickSheet` | "To account" button |
| `CategorySheets` (QuickExpenseSheet) | Account picker |
| `BudgetSheets` | not provided → **C button** |
| `AmountCalculatorSheet` | not provided → **C button** |

**Rule:** Do not pass `row2ExtraKey` unless a genuine context-specific action is needed. "C" (clear) is the correct default.

## CalcStateHolder Key Handling

| Key | Effect |
|---|---|
| `0-9` | Append digit (max 12 chars; max 2 decimal places) |
| `,` | Append decimal separator if not already present |
| `+` `−` `×` `÷` | Store pending op + current value; start new operand |
| `=` | Evaluate pending op; clear `pendingOp` |
| `⌫` | Drop last char; falls back to "0" when single digit |
| `C` | Full reset: `currentStr="0"`, `pendingOp=null`, `pendingVal=0.0` |

## CalcStateHolder Display Methods

| Method | Returns |
|---|---|
| `displayExprNoSymbol()` | Numeric expression only — `currentStr` or `"${pendingVal} $op $currentStr"` |
| `displayExpr(symbol)` | `"${displayExprNoSymbol()} $symbol"` |

Use `displayExprNoSymbol()` when the currency symbol needs to be a separate interactive element (e.g., `AddTransactionScreen` split-Row layout). Use `displayExpr(symbol)` for a plain non-interactive string.

## Hardware Keyboard Support

`SharedCalcKeypad` auto-requests focus (`FocusRequester` + `LaunchedEffect`) and handles `onKeyEvent` via `focusable()`:

1. **Character-based** (`utf16CodePoint`): `0-9`, `+`, `-`, `*`, `/`, `,`, `.`, `=` (incl. Shift-combos).
2. **Key-code-based**: numpad keys, `Backspace` → `⌫`, `Delete` → `C`, `Enter`/`NumPadEnter` → `=` or `onConfirm`.

**Rule:** Do not remove `focusable()` or `focusRequester` — required for hardware keyboard. The `try { requestFocus() } catch` guard prevents crashes before composable attaches.

## Backspace Icon

Uses `Icons.AutoMirrored.Outlined.Backspace` (outlined). Do not revert to `Filled.Backspace`.
