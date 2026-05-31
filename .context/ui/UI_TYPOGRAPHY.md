# UI Contracts — Typography & Component Sizing

## Font Family

The app uses **Inter** via Google Fonts Compose (`ui-text-google-fonts`). Defined in `ui/theme/Typography.kt` as `InterFontFamily` and `InterTypography`. Passed to `MaterialTheme(typography = InterTypography)` in `Theme.kt`.

Weights registered: Light, Normal, Medium, SemiBold, Bold, Black.

Do NOT add a local font mapper or hardcode `FontFamily.Default` anywhere — all type styles inherit from `InterTypography`.

## Material 3 Tokens (Inter)

| Token | Size | Weight |
|---|---|---|
| `labelSmall` | 11sp | Medium 500 |
| `labelMedium` | 12sp | Medium 500 |
| `bodySmall` | 12sp | Regular 400 |
| `bodyMedium` | 14sp | Regular 400 |
| `titleSmall` | 14sp | Medium 500 |
| `titleMedium` | 16sp | Medium 500 |

## SharedTopBar

| Element | Style | Size | Weight |
|---|---|---|---|
| "Всі рахунки" subtitle | `labelSmall` | 11sp | Medium, alpha 55% |
| Total balance | `titleLarge` + Bold override | 22sp | Bold 700 |

## SharedMonthNavPill

Navigation arrows: `Icons.AutoMirrored.Filled.KeyboardArrowLeft/Right` (30dp), tint = `pillColor`. Row horizontal padding = 10dp.

| Element | Style | Size | Weight | Color |
|---|---|---|---|---|
| Day-count badge | `labelSmall` + Bold | 11sp | Bold 700 | `pillColor` (border + text) |
| Period label | `bodyMedium` + SemiBold | 14sp | SemiBold 600 | `pillColor` |
| Dropdown arrow | `KeyboardArrowDown` | 26dp | — | `pillColor.copy(alpha=0.7f)` |

**Month flip animation:** `AnimatedContent(targetState = pillLabel to pillBadge)` — forward: slides left; backward: slides right. 220ms enter / 180ms fade-in; 180ms exit / 120ms fade-out. Direction tracked via `Ref<Boolean>` (not `MutableState`).

## CategoryChip — Normal (`isCompact = false`)

Icon circle 48dp, icon 26dp. Name box `heightIn(min=28dp, max=40dp)`. Single-line.

| Element | Size | Weight |
|---|---|---|
| Category name | 13sp, lineHeight 16sp | SemiBold 600 |
| Budget row (position 2) | 11sp, lineHeight 13sp | SemiBold or Bold (overbudget) |
| Spending amount (bottom) | 13sp, lineHeight 15sp | **Bold 700** |
| +N child badge | 8sp | white on primary |

## CategoryChip — Compact (`isCompact = true`)

Icon circle 40dp, icon 22dp. Name box `heightIn(min=24dp, max=34dp)`. Single-line.

| Element | Size | Weight |
|---|---|---|
| Category name | 12sp, lineHeight 14sp | SemiBold 600 |
| Budget row (position 2) | 10sp, lineHeight 12sp | SemiBold or Bold (overbudget) |
| Spending amount | 12sp, lineHeight 14sp | **Bold 700** |
| +N child badge | 7sp | white on primary |

## DonutChart Center

| Element | Style | Size | Weight |
|---|---|---|---|
| "Витрати" / "Доходи" label | `labelSmall.copy(fontSize=14sp)` | 14sp | Medium, alpha 55% |
| Expense total | `titleSmall.copy(fontSize=20sp)` + Bold | 20sp | Bold 700, error color |
| Income total | `bodySmall.copy(fontSize=15sp)` + Medium | 15sp | Medium 500, teal #26A69A |
