# UI Contracts — Typography & Component Sizing

## Font Family

The app uses **system Roboto** (`FontFamily.Default`). Defined in `ui/theme/Typography.kt` as `AppTypography`. Passed to `MaterialTheme(typography = AppTypography)` in `Theme.kt`.

Use `FontWeight.Normal` (400) for regular and `FontWeight.Medium` (500) for medium. Do NOT use `FontWeight.Bold` (700) on the categories screen or bottom nav.

## Material 3 Tokens (Roboto/System)

| Token | Size | Weight |
|---|---|---|
| `labelSmall` | 11sp | Medium 500 |
| `labelMedium` | 12sp | Medium 500 |
| `bodySmall` | 12sp | Regular 400 |
| `bodyMedium` | 14sp | Regular 400 |
| `titleSmall` | 14sp | Medium 500 |
| `titleMedium` | 16sp | Medium 500 |

## SharedTopBar

Sizes via `OneMoneyTheme.dimens`: avatar button `topBarAvatarSize=44dp`, avatar icon 28dp, action button `topBarAvatarSize`, action icon `topBarAvatarIconSize=22dp` (Search) or 28dp (others). Colors via `OneMoneyTheme.colors`.

| Element | Token / Style | Size | Weight | Color |
|---|---|---|---|---|
| "Всі рахунки" subtitle | `topBarLabel` (12sp/Medium) | 12sp | Medium 500 | `colors.primaryText` (#111111) |
| Total balance | `topBarBalance.copy(fontSize=18sp, Normal)` | 18sp | Normal 400 | `colors.primaryText` (no red for negative) |

Balance format: `formatMoney(totalBalance) + " ₴"`. Background: `colorScheme.background`. Padding: `Spacing.sm` (8dp) horizontal/vertical.

## SharedMonthNavPill

Navigation arrows: custom `DoubleChevronLeft/Right` from `ui/components/icons/NavIcons.kt` (36dp). Row padding = `start=24dp, end=10dp, top=6dp, bottom=6dp`.

**Colour:** Pill colour depends on period. Current calendar month (`PeriodMode.MONTH` + matching month+year) → dark neutral palette. All other periods → red palette.

| State | `pillColor` | `pillBg` |
|---|---|---|
| Current month | `#111111` | `#D5D6EC` |
| Other periods | `MonthRed = #D7261E` | `MonthRedLight = #F8D9D5` |

| Element | Style | Size | Weight | Color |
|---|---|---|---|---|
| Day-count badge | `labelSmall.copy(fontSize=11sp, letterSpacing=0)` | 11sp | Light 300 | `pillColor` text + border |
| Period label | `bodyMedium.copy(fontSize=11sp, letterSpacing=0)` | 11sp | Light 300 | `pillColor` |
| Dropdown arrow | `KeyboardArrowDown` | `dimens.pillDropArrowSize` | — | `pillColor` |
| Left nav arrow | `DoubleChevronLeft` | `dimens.pillArrowSize` | — | `pillColor` |
| Right nav arrow | `DoubleChevronRight` | `dimens.pillArrowSize` | — | `#111111` always |

**Rule:** Badge, label, dropdown arrow, and left nav arrow all use `pillColor`. Right nav arrow is always `Color(0xFF111111)`. Do not use blue — current-month colour is `#D5D6EC` (lavender), not blue.

**Month flip animation:** `AnimatedContent` — forward: slides left; backward: slides right. 220ms enter / 180ms fade. `Ref<Boolean>` tracks direction.

## CategoryChip — Normal (`isCompact = false`)

Chip: `CHIP_WIDTH×CHIP_HEIGHT` dp (adaptive — set by `CategoriesGridContent` via `BoxWithConstraints`; defaults 80×120dp). Icon circle: `tokens.categoryCircleSize` (54dp default). Icon: `tokens.categoryIconSize` (24dp). Name box `height=16dp`, `maxLines=1`, `softWrap=false`.

Typography via `OneMoneyTheme.typography` (`typo`) and `OneMoneyTheme.dimens` (`tokens`):

| Element | Token / Size | Weight | Color |
|---|---|---|---|
| Category name | `typo.categoryTitle` (11sp/Light/13sp lh) | Light 300 | `Color(0xFF111111)` |
| Budget/zero row | `typo.categoryTopAmount` (10sp/Normal/12sp lh), box `height=12dp` | Normal 400 | over-budget: white on `fallbackColor`; normal: `onSurface` 42% |
| Spending amount | `typo.categoryBottomAmount` (10sp/Normal/12sp lh) | Normal 400 | `fallbackColor` if spending>0 else `onSurface` 35% |
| +N child badge | `childBadgeSize` 18dp circle, 8sp | — | white on `primary` |

**Category visual overrides** — `CategoryScreenTokens.resolve()` in `CategoriesWidgets.kt` returns `CategoryVisualStyle(circleBg, iconTint)` per category name (9 overrides) or computes fallback from `colorHex` with `hasBudget` alpha.

## CategoryChip — Compact (`isCompact = true`)

Icon circle: `tokens.categoryCircleCompactSize` (40dp). Icon: `tokens.categoryIconCompactSize` (22dp). Name box `height=16dp` → override to 22dp via `overrideHeight` if needed.

| Element | Token / Size | Weight |
|---|---|---|
| Category name | `typo.categoryTitle.copy(fontSize=10sp, lineHeight=10sp)` | Medium 500 |
| Budget row | `typo.categoryTopAmount.copy(fontSize=11sp, lineHeight=12sp)` | Normal 400 |
| Spending amount | `typo.categoryBottomAmount.copy(fontSize=10sp)` | Normal 400 |
| +N child badge | `childBadgeCompactSize` 16dp, 7sp | — |

## DonutChart Center

Ring empty state: `colors.centerRing`. Expense segments use category colors.

| Element | Style | Size | Weight | Color |
|---|---|---|---|---|
| "Витрати" / "Доходи" label | `typo.centerTitle.copy(letterSpacing=0)` | 15sp | Light 300 | `onSurface` |
| Expense total | `typo.centerAmount.copy(letterSpacing=0)` | 15sp | Normal 400 | `colors.expensePink` (#FF5A8A) |
| Income total | `typo.centerAmount.copy(letterSpacing=0)` | 15sp | Normal 400 | `colors.incomeTeal` (#4DD4C8) |

Format: `formatMoney(total) + " ₴"` — integers without decimals ("0 ₴"). No toggle arrow icon — removed.

## Bottom Navigation

Container: `colors.bottomNavBg`. Indicator: `colors.bottomNavPill`. Icons `dimens.bottomNavIconSize=24dp` (Material3 default).

| State | Icon / Text Color | Size | Weight |
|---|---|---|---|
| Active | `colors.primaryText` (#111111) | 10sp | Medium 500 |
| Inactive | `colors.primaryText` (#111111) | 10sp | Light 300 |

Label: `Text(tab.label, maxLines=1, fontSize=10.sp, fontWeight=if(active) Medium else Light, letterSpacing=0.sp)`.

**Rule:** No blue active colour. No bold (700). All 5 labels ("Рахунки", "Категорії", "Операції", "Бюджет", "Огляд") must fit untruncated at 10sp Roboto. Colors access via `OneMoneyTheme.colors` — never hardcode `BottomNavActivePill` directly.

## Colour Tokens

Access via `OneMoneyTheme.colors` (CompositionLocal). Source values in `OneMoneyLightTokens.colors` / `Theme.kt`.

| `OneMoneyColors` field | Value | Usage |
|---|---|---|
| `primaryText` | `#111111` | TopBar labels, nav items |
| `secondaryText` | `#8E8E8E` | Subtitle text |
| `tertiaryText` | `#C9C9C9` | Zero-spend category row |
| `incomeGreen` | `#4CAF50` | Income transaction tint |
| `expenseRed` | `#F44336` | Expense transaction tint |
| `transferBlue` | `#2196F3` | Transfer transaction tint |
| `debtOrange` | `#FF9800` | Borrow/Lend tint |
| `budgetExpense` | `#D81B60` | Budget expense bars, negative balance |
| `budgetIncome` | `#26A69A` | Budget income bars, savings |
| `bottomNavBg` | `#FFFFFF` | Bottom nav container |
| `bottomNavPill` | `#E7E7F2` | Active nav indicator |
| `expensePink` | `#FF5A8A` | Donut expense total |
| `incomeTeal` | `#4DD4C8` | Donut income total |

Top-level `Theme.kt` vals still used by name where CompositionLocal is not available:

| Top-level val | Value | Usage |
|---|---|---|
| `MonthRed` | `#D7261E` | Month pill (all elements) |
| `MonthRedLight` | `#F8D9D5` | Pill background |
| `BudgetExpenseColor` | `#D81B60` | = `colors.budgetExpense` |
| `BudgetIncomeColor` | `#26A69A` | = `colors.budgetIncome` |

## AddCategoryChip

Dashed circle 40dp, stroke `Color(0xFFC7C2C8)`, dashWidth=8dp, dashGap=6dp, strokeWidth=1dp. Plus icon 18dp, tint `Color(0xFF9E9EA6)`. No "Додати" label.
