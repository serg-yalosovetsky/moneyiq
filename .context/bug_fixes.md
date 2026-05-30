# Bug Fixes

## High-Risk Areas

- `TransactionRepository`: balance mutation must stay reversible.
- `CategoriesScreen`: layout is visually strict and regression-prone.
- `AppDatabase`: migrations must match entity changes.
- `MainScreen`: edge swipes, bottom tabs, and embedded screens share gesture/state boundaries.
- Widgets/workers: background entry points may run without the main UI.
- `ui/transactions` sheet files: four files now — any composable shared between them must stay `internal`, not `private`.

## Fix Rules

- Reproduce the issue from the affected screen or repository path before changing broad shared code.
- Keep fixes scoped to the failing behavior.
- Do not silently change seeded default categories unless that is the requested behavior.
- Do not change transaction type semantics without updating `TRANSACTION_ACCOUNTING.md`.
- When a bug touches persistence, update `DB_SCHEMA.md` or explain why schema is unchanged.

## Known Bugs Fixed

### CategoriesScreen — ExtCat Strip Showed Two Disconnected Highlighted Elements (2026-05-31)

**Symptom:** Double-tapping a chip in the bottom grid (extCats) produced two separate blue-tinted areas: one for the expansion strip (above the grid) and one for the expanded chip in the grid. They looked like independent elements despite belonging to the same parent→child relationship.

**Root cause:** The expansion strip card and the chip's `isExpanded` background both applied the parent color independently, with a LazyColumn item gap between them.

**Fix:** Two changes:
1. `showParentHeader = true` passed to `ExpandedCategoryStrip` for extCats — the strip now shows a unified card: parent icon + name + spending (header), divider, children. One visual element represents the full parent+children relationship.
2. `expandedId = null` for extCats `CategoryGridRow` — the grid chip no longer shows a separate expanded background; the card above is the sole visual indicator.

**Regression rule:** Do not restore `expandedId = expandedCategoryId` for extCats rows. The strip's `showParentHeader = true` is the single source of visual expansion feedback for bottom-grid chips.

---

### App Crashes On Launch — Sentry Auto-Init Missing DSN (2026-05-29)

**Symptom:** App crashes immediately on startup with `java.lang.IllegalArgumentException: DSN is required. Use empty string or set enabled to false in SentryOptions to disable SDK` from `SentryInitProvider.onCreate`.

**Root cause:** Sentry SDK ships a `SentryInitProvider` ContentProvider that fires **before** `Application.onCreate()` and reads DSN from `AndroidManifest.xml` meta-data. The DSN was only configured in `MoneyIQApp.onCreate()` via `SentryAndroid.init()` — not in the manifest — so the ContentProvider crashed before reaching Application init.

**Fix:** Added `<meta-data android:name="io.sentry.auto-init" android:value="false" />` to `AndroidManifest.xml`. This disables `SentryInitProvider`; Sentry is initialized entirely by `SentryAndroid.init()` in `MoneyIQApp.onCreate()`.

**Regression rule:** Do not remove `io.sentry.auto-init=false` from the manifest unless the DSN is also added as `io.sentry.dsn` meta-data and the manual `SentryAndroid.init()` call is removed. Double-init causes unpredictable SDK behaviour.

---

### CategoryChip — Spending Text Clipped At Bottom (2026-05-28)

**Symptom:** Spending amounts for parent categories disappeared from chips. Only chip icon was visible; spending text was invisible.

**Root cause:** `CHIP_HEIGHT` was too small (116dp). Content = 28+11+3+48+3+12 = 105dp. Available height = 116-4(padding) = 112dp. At font scale > 1.0 the bottom `sp`-measured spending text overflowed and got clipped by the fixed `.size()` modifier.

**Fix:** Increased `CHIP_HEIGHT = 124.dp` (gives 120dp available, 15dp spare). Also made budget text conditional — when `budgetAmount == 0`, a `Spacer(11dp)` substitutes so layout alignment is preserved without showing "0 ₴".

**Regression rule:** Do not reduce `CHIP_HEIGHT` below 120dp. Do not add new text rows to `CategoryChip` without verifying total content height fits within `CHIP_HEIGHT - 4dp` at font scale 1.3.

### CategoryActionSheet — Spending Shows 0 For Parent Categories (2026-05-29)

**Symptom:** Long-pressing a parent category (e.g. "Робота") opened the action sheet with "0 операцій / 0,00 ₴" even though the chip showed 4 882,70 ₴.

**Root cause:** `catSpending` was taken from `state.monthSpending[cat.id]` (direct transactions only). Parent categories hold no direct transactions — all spending is in children. Same issue for `txCount`.

**Fix:** Changed to `effectiveSpending[cat.id]` and aggregated `monthTxCounts` across category + direct children. See `CategoriesScreen.kt` lines ~147–156.

**Regression rule:** CategoryActionSheet must always display the same spending figure the chip shows. If `effectiveSpending` calculation changes, update both the chip and the action sheet.

---

### CategoryActionSheet — Action Buttons Cut Off By Navigation Bar (2026-05-29)

**Symptom:** The "Редагувати / Бюджет / Операції" buttons at the bottom of the action sheet had their text labels clipped by the Android navigation bar.

**Root cause:** `Dialog` with `decorFitsSystemWindows = true` (default) does not propagate `WindowInsets` into the Compose hierarchy. The `Spacer(Modifier.navigationBarsPadding())` at the bottom of the sheet measured as 0 height, so content extended behind the nav bar.

**Fix:** Added `decorFitsSystemWindows = false` to the `DialogProperties` of `CategoryActionSheet` in `CategorySheets.kt`. The dialog window now covers the full screen; Compose receives the correct nav-bar insets and the Spacer correctly reserves that space.

**Regression rule:** Any full-screen custom `Dialog` composable must use `decorFitsSystemWindows = false` if it relies on `navigationBarsPadding()` or `WindowInsets.navigationBars` inside the dialog content.

---

### CategoryChip Badge — Shows Child Count Instead Of Budget (2026-05-29)

**Symptom:** Chip badges showed "+2", "+4" (child category count) instead of the budget amount.

**Fix:** Badge condition changed from `showChildBadge && childCount > 0` to `showChildBadge && budgetAmount > 0 && spending > 0`. Badge text changed from `"+$childCount"` to `"${budgetAmount.toInt()} ₴"`. Badge shape changed from `CircleShape` with fixed 18 dp to `RoundedCornerShape(50)` pill that wraps its content width. Budget text above icon now only shows when `spending == 0` (when spending > 0, budget is in the badge instead).

---

### CategoryChip Badge — Shows '+1' For Same-Name Child Despite Empty Expansion (2026-05-29)

**Symptom:** "Продукти" chip showed "+1" badge implying a subcategory exists, but double-clicking expanded nothing.

**Root cause:** `childCounts` (badge count) counted ALL non-archived children with `spending > 0`, including children whose name matched the parent name. Meanwhile `expandedChildren` already excluded same-name children. This mismatch caused the badge to say "+1" while the expansion showed nothing.

**Fix:** Applied the same lowercase name-equality exclusion to `childCounts` (see `CategoriesScreen.kt` lines ~87–95):
```kotlin
val catNameById = allCategoriesForTab.associate { it.id to it.name.trim().lowercase() }
val childCounts = allCategoriesForTab
    .filter { c ->
        c.parentId != null && (spending[c.id] ?: 0.0) > 0.0 &&
        c.name.trim().lowercase() != (catNameById[c.parentId] ?: "")
    }
    .groupBy { it.parentId!! }.mapValues { it.value.size }
```

**Regression rule:** `childCounts` and `expandedChildren` must use identical child-exclusion criteria. If one filter changes, update the other.

---

### TransactionSheets.kt — Compile Errors: missing import + private visibility (2026-05-29)

**Symptom:** Build failed: "Unresolved reference 'horizontalScroll'" and "Cannot access 'fun TxTopBar...': it is private in file" / same for `ActiveFilterChipsRow`.

**Root cause 1:** `horizontalScroll` was used in `ActiveFilterChipsRow` in `TransactionSheets.kt` but the import was missing.

**Root cause 2:** `TxTopBar` and `ActiveFilterChipsRow` were marked `private fun` but called from `TransactionsListScreen.kt` in the same package.

**Fix:** Added `import androidx.compose.foundation.horizontalScroll`. Removed `private` modifier from both functions.

**Regression rule:** See ADR-012. These functions must remain non-private.

---

### CategoriesScreen.kt — `SideSubcategoryPanel` missing (2026-05-29)

**Symptom:** Build failed: "Unresolved reference 'SideSubcategoryPanel'" at two call sites in the mid-row expansion logic.

**Root cause:** The inline subcategory panel composable was referenced in the layout code but never implemented.

**Fix:** Added `private fun SideSubcategoryPanel(parent, children, spending, onClickChild, onLongClickChild, modifier)` before `ExpandedCategoryStrip`. Shows subcategories as a scrollable `Card` column sorted by spending descending.

---

### CategoriesScreen — `strip_mid` Duplicates Inline Panel (2026-05-29)

**Symptom:** When a mid-row (side-column) category was expanded, a second "Продукти" card appeared below the donut section — identical in content to the inline panel already shown to the left of the donut.

**Root cause:** The `strip_mid` LazyColumn item rendered unconditionally when `expandedCat` was in `midLeft`/`midRight` and `expandedChildren` was non-empty — exactly the same condition that makes `showInlinePanel = true`. Both the inline `SideSubcategoryPanel` and the strip rendered simultaneously.

**Fix:** Removed `strip_mid` item block entirely. Children are already visible via the inline panel; there is no scenario where both are needed.

**Regression rule:** Do not re-introduce `strip_mid`. If a secondary strip is desired for mid-row categories, it must explicitly check `!showInlinePanel`.

---

### CategoriesScreen — `SideSubcategoryPanel` Oversized With Few Children (2026-05-29)

**Symptom:** The grey inline panel occupied the full `DONUT_SECTION_HEIGHT` (360dp) even when it contained only 1–2 subcategory rows, creating large empty space.

**Root cause (first diagnosis — incorrect):** Initially believed the cause was `fillMaxHeight()` at the call site and `fillMaxSize()` inside the Column. Removing these didn't fix it.

**Root cause (actual):** Compose `Row` with `height(360.dp)` passes `minHeight = 360` to ALL children, including weighted ones. A Card with no explicit height modifier still gets forced to 360dp because its minimum height constraint equals the Row's fixed height. `fillMaxHeight()` was irrelevant — the forced `minHeight` is the real cause.

**Fix:** Added `Modifier.wrapContentHeight(align = Alignment.CenterVertically)` chained after `weight(0.6f)` at both call sites in `CategoriesScreen.kt`. `wrapContentHeight()` overrides the incoming `minHeight` constraint to 0 before measuring the Card, allowing it to shrink to content size, then centers it within the 360dp Row.

```kotlin
modifier = Modifier.weight(0.6f)
    .wrapContentHeight(align = Alignment.CenterVertically)
```

**Where:** `SideSubcategoryPanel` lives in `CategoriesWidgets.kt` (not `CategoriesScreen.kt`). Call sites are in `CategoriesScreen.kt` mid-row inline panel section.

**Regression rule:** Any composable inside a fixed-height `Row` that should wrap its height must use `.wrapContentHeight()`. Simply removing `fillMaxHeight()` is NOT sufficient — the Row's `minHeight` constraint still forces full height.

---

### CategoryActionSheet — Category Icon Overflows Sheet Boundary (2026-05-29)

**Symptom:** The category icon chip was visible above the top edge of the action sheet, floating into the scrim area (or appearing over the categories grid background). Visually identical to the "chip overlapping other content" problem elsewhere.

**Root cause:** The icon Box used `.offset(y = (-36).dp)` to create a "floating above the sheet" effect. The outer container `Box(Modifier.fillMaxWidth())` had no `.clip()`, so the icon rendered 36dp above the sheet's visible top edge — into the scrim or behind-dialog area.

**Fix:** Removed the floating icon box entirely. Moved the category icon (now 64dp circle) inside the colored header Column, placed in a `Row` alongside the category name (`Modifier.weight(1f)` on the Text). No negative offset; icon stays fully within clipped bounds.

**Regression rule:** Do not use `.offset(y = negative)` to float elements outside a composable's clip boundary. If a "floating chip" design is wanted, it must be placed in a full-screen overlay `Box` where it won't escape the dialog window bounds. See `decorFitsSystemWindows = false` pattern in the same file.

---

### CategoriesScreen — Same-Name Subcategory Appears Inside Parent (2026-05-29)

**Symptom:** Expanding category "Продукти" showed a child chip also named "Продукти", which is semantically redundant (a category cannot be its own subcategory).

**Root cause:** `expandedChildren` was computed as all non-archived children of the parent, with no name-collision filter. If the user created a child with the same name as the parent, it appeared in the strip.

**Fix:** Added lowercase name comparison in `expandedChildren` filter:
```kotlin
c.name.trim().lowercase() != cat.name.trim().lowercase()
```

**Regression rule:** Same-name children are silently excluded from expansion strips and inline panels. If the user renames a parent to match a child (or vice versa), the child disappears from expansion view. This is intentional — the two are merged conceptually.

---

### TransactionSheets.kt — Orphaned Function Body At File Top Level (2026-05-29)

**Symptom:** Build failed: "Function declaration must have a name" at line 53 of `TransactionSheets.kt`. Multiple "Expecting a top level declaration" errors in the same file; also "Expecting '}'" in `TransactionsListScreen.kt`.

**Root cause:** During a refactoring session, the sheets-rendering block (filter sheet, category picker, quick expense, transfer, detail sheet) and the closing `}` of `TransactionsListScreen` were moved to `TransactionSheets.kt` without a wrapping function declaration. The block sat at file top-level between the import section and the first `@Composable` function.

**Fix in progress:** Correct approach is to wrap the block in a proper `@Composable fun TransactionOverlaySheets(...)` in `TransactionSheets.kt` with hoisted state parameters, then call it from `TransactionsListScreen`. Alternative: move the block back into `TransactionsListScreen` and add the missing `}`.

**Regression rule:** Any code moved out of a composable function must be enclosed in a new named function at the destination. Top-level executable Kotlin statements are not valid outside `fun`.

---

### CategoryChip — Long Cyrillic Names Wrap To Two Lines At Font Scale > 1.0 (2026-05-29)

**Symptom:** "Ресторація" rendered as "Ресторацi / я" (split across 2 lines) in the top-row chip. "Транспорт" split similarly in the compact side-column chip.

**Root cause:** Available text width was too narrow (78dp normal, 66dp compact) for 9–10 Cyrillic characters at system font scale > 1.0. At scale 1.15, "Ресторація" needs ~80dp, exceeding the 78dp available.

**Fix:** Increased `CHIP_WIDTH` 82→86dp and `CHIP_WIDTH_COMPACT` 70→74dp in `CategoriesScreen.kt`. New available text widths: 82dp normal (86−4), 70dp compact (74−4). On 360dp screens, 4 × 86dp = 344dp exactly fills the row (8dp horizontal padding each side); gaps open on wider devices.

**Regression rule:** Do not exceed `CHIP_WIDTH = 86dp` without checking that 4 chips still fit in 344dp on 360dp screens. Do not reduce `CHIP_WIDTH_COMPACT` below 74dp.

---

### CategoriesScreen — Zero-Spending Categories Not Shown (2026-05-29)

**Symptom:** In months where a category (e.g. "Транспорт") had no transactions, the chip completely disappeared from the categories grid. Users could not see that the category existed.

**Root cause:** `display` was computed as `active = sorted.filter { spending > 0 }`. When at least one category had spending, `display = active` — excluding all zero-spending categories from every grid slot.

**Fix:** Replaced the filter with `val display = sorted` in `CategoriesGridContent` (`CategoriesScreen.kt`). All root categories now always occupy grid slots. The pale appearance for `spending == 0` (tinted circle, colored icon, grey "0 ₴" text) was already implemented in `CategoryChip` and now takes effect.

**Regression rule:** Do not re-add a `spending > 0` filter to `display`. Zero-spending categories must remain visible. If hiding them is ever desired, gate it behind a UI toggle, not a hard filter.

---

### CategoriesScreen — Chips Appear Above CategoryActionSheet Dialog (2026-05-29)

**Symptom:** Opening `CategoryActionSheet` (long-press on a chip) showed one or more category chips or their icons floating visually above the dialog scrim — outside the sheet boundary.

**Root cause:** Three `.graphicsLayer { clip = false }` modifiers in `CategoriesScreen.kt` (on the mid-row `Row` and both side `Column`s) forced Android hardware layers. Hardware layers created via `graphicsLayer` are composited at a higher z-level than `Dialog` windows due to Android view hierarchy ordering — they render above the Dialog regardless of Compose semantics.

**Fix:** Removed all three `.graphicsLayer { clip = false }` modifiers and their now-unused imports (`drawBehind`, `graphicsLayer`) from `CategoriesScreen.kt`. The expansion ring drawn by `drawBehind` in `CategoriesWidgets.kt` fits within chip bounds without the modifier:
- compact chip: ring radius 24dp < half chip-width 37dp
- normal chip: ring radius 28dp < half chip-width 43dp

**Regression rule:** Do not add `graphicsLayer { clip = false }` to any composable that is a sibling of a `Dialog` call site. If content truly needs to escape clip bounds and coexist with Dialogs, use a full-screen overlay `Box` in the root composition instead.

---

### SideSubcategoryPanel — Expanded Panel Oversized (Large Grey Rectangle) (2026-05-29)

**Symptom:** Double-clicking a side-column category (e.g. "Дозвілля") showed a wide, mostly-empty grey rectangle — an inline panel stretched to ~155dp wide with just one subcategory row inside.

**Root cause:** Panel modifier used `weight(0.6f)`. In a `Row` with a 90dp fixed chip column, this gave the panel 60% of the remaining ~258dp = ~155dp. The Card content (32dp icon + text) needed only ~110dp.

**Fix:** Changed panel modifier from `Modifier.weight(0.6f).wrapContentHeight(...)` to `Modifier.widthIn(min = 110.dp, max = 130.dp).wrapContentHeight(...)`. Changed donut modifier from `weight(0.4f)` to `weight(1f)` so it fills the remaining space after the panel takes its intrinsic width.

**Regression rule:** Do not use `weight(X)` for `SideSubcategoryPanel` width. Use `widthIn(min=110.dp, max=130.dp)`; the donut must have `weight(1f)`. A weight fraction scales with screen width and always overshoots the content.

---


### CategoriesScreen — Side Columns Removed, Full-Width Donut (2026-05-29)

**Change:** The [2-left | donut | 2-right] side-column layout was replaced with a full-width donut + grid-below layout.

**Before:** display[4..9] filled left (indices 4, 5, 8) and right (6, 7, 9) columns flanking the donut. Only display[10+] appeared below. SideSubcategoryPanel showed subcategories inline when a side-column chip was double-clicked.

**After:** Donut always occupies full width (Box with DONUT_SECTION_HEIGHT). extCats = display.drop(4) — all categories from position 5 onward render in rows of 4 below the donut. SideSubcategoryPanel call sites removed; composable kept in CategoriesWidgets.kt but unused.

**Regression rule:** Do not re-introduce side columns or midLeft/midRight slot assignment. If a side-panel UX is ever wanted again, re-introduce it as a separate opt-in mode, not the default layout.

---
## Verification Checklist

- Kotlin compile passes.
- Existing affected flow still works.
- No unrelated user-visible labels changed.
- No unrelated dirty files reverted.
- `.context` is updated if behavior, architecture, schema, or contracts changed.
