# Bug Fixes

## High-Risk Areas

- `TransactionRepository`: balance mutation must stay reversible.
- `CategoriesScreen`: layout is visually strict and regression-prone.
- `AppDatabase`: migrations must match entity changes.
- `MainScreen`: edge swipes, bottom tabs, and embedded screens share gesture/state boundaries.
- Widgets/workers: background entry points may run without the main UI.

## Fix Rules

- Reproduce the issue from the affected screen or repository path before changing broad shared code.
- Keep fixes scoped to the failing behavior.
- Do not silently change seeded default categories unless that is the requested behavior.
- Do not change transaction type semantics without updating `TRANSACTION_ACCOUNTING.md`.
- When a bug touches persistence, update `DB_SCHEMA.md` or explain why schema is unchanged.

## Known Bugs Fixed

### CategoryChip — Spending Text Clipped At Bottom (2026-05-28)

**Symptom:** Spending amounts for parent categories disappeared from chips. Only chip icon was visible; spending text was invisible.

**Root cause:** `CHIP_HEIGHT` was too small (116dp). Content = 28+11+3+48+3+12 = 105dp. Available height = 116-4(padding) = 112dp. At font scale > 1.0 the bottom `sp`-measured spending text overflowed and got clipped by the fixed `.size()` modifier.

**Fix:** Increased `CHIP_HEIGHT = 124.dp` (gives 120dp available, 15dp spare). Also made budget text conditional — when `budgetAmount == 0`, a `Spacer(11dp)` substitutes so layout alignment is preserved without showing "0 ₴".

**Regression rule:** Do not reduce `CHIP_HEIGHT` below 120dp. Do not add new text rows to `CategoryChip` without verifying total content height fits within `CHIP_HEIGHT - 4dp` at font scale 1.3.

## Verification Checklist

- Kotlin compile passes.
- Existing affected flow still works.
- No unrelated user-visible labels changed.
- No unrelated dirty files reverted.
- `.context` is updated if behavior, architecture, schema, or contracts changed.
