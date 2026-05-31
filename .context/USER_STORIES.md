# User Stories

## Accounts

- As a user, I can create an account with a name, type (Cash / Card / Saving / Investment / Debt / Other), opening balance, currency, color, icon, and description.
- As a user, I can set a credit limit on a card or debt account so I can track available credit separately from positive balance.
- As a user, I can mark an account as "default" (shown with a star badge) so it is pre-selected when adding transactions.
- As a user, I can toggle "Include in total" per account so savings or debt accounts don't distort my net balance.
- As a user, I can see the total balance of all included accounts in the shared top bar.
- As a user, I can see a currency symbol badge on every account icon so I know each account's currency at a glance.
- As a user, I can long-press an account to open an action sheet with six actions: Edit, Balance, Transactions, Income, Expense, Transfer.
- As a user, I can tap "Transactions" in the account action sheet to switch directly to the Transactions tab pre-filtered by that account.
- As a user, I can tap "Income / Expense / Transfer" in the account action sheet to open the Add Transaction form.
- As a user, I can edit an account's name, type, balance, currency, color, icon, description, credit limit, and include-in-total at any time.
- As a user, I can delete an account; all its transactions are cascade-deleted.
- As a user, I can pick a currency from 130+ currencies (Main / Other / Crypto tabs) when creating or editing an account.
- As a user, I can pick an icon and color for my account from the account form.

## Categories

### Browsing And Spending View
- As a user, I can see my spending broken down by category for the selected period as a donut chart + chip grid.
- As a user, I can toggle the chip grid between Expense and Income views by tapping the donut center.
- As a user, I can see all categories (even those with zero spending) in the grid — zero-spending chips appear pale.
- As a user, I can see each chip's name, remaining budget (or over-budget amount), and spending total.
- As a user, I can double-tap a category chip to expand its subcategories in an inline strip below the chip.
- As a user, I can see the donut chart switch to showing only the expanded subcategory breakdown when a category is expanded.
- As a user, I can single-tap a category chip to open a quick expense / income entry sheet pre-filled with that category.
- As a user, I can toggle between compact and normal chip size using the density toggle in the top bar.

### Category Management
- As a user, I can create a new root category with a name, type (Expense / Income), icon, and color.
- As a user, I can create subcategories under any root category (one level of hierarchy only).
- As a user, I can detach a subcategory from its parent, promoting it to a root category, without deleting it.
- As a user, I can edit a category's name, type, icon, color, budget amount, budget period, and currency.
- As a user, I can set a per-category budget currency independently from the app default currency.
- As a user, I can archive a category so it disappears from the grid but its transactions are preserved.
- As a user, I can delete a category; its transactions keep their amount but lose the category reference.
- As a user, I can open the Edit Categories screen (pencil icon in top bar) to manage all categories as a list with drag-to-reorder.
- As a user, I can drag-to-reorder root categories in the Edit Categories screen by long-pressing the drag handle.
- As a user, I can switch to "Subcategories" mode in the Edit Categories screen to see all subcategories as a flat list.
- As a user, I can get an auto-suggested icon and color when typing a new category name (e.g. "Аптека" → pharmacy icon, green).

### Quick Expense / Income
- As a user, I can tap a category chip to open a quick entry sheet with a calculator keypad.
- As a user, I can switch the source account in the quick entry sheet (when I have more than one account).
- As a user, I can change the currency for a quick entry independently from the account's default currency.
- As a user, I can add a note, date, repeat mode, and reminder to a quick entry.
- As a user, I am shown a red flash on the amount if I try to confirm a zero amount — the entry is blocked until I type a value.

## Transactions

### Adding Transactions
- As a user, I can add an expense, income, or transfer from the "+" floating button on any tab.
- As a user, I can enter an amount using a full calculator (with +, −, ×, ÷ operators and a decimal separator).
- As a user, I can change the transaction currency using the currency key in the calculator or by tapping the currency symbol in the amount display.
- As a user, I can pick a category (Expense/Income) or destination account (Transfer) from a compact picker sheet.
- As a user, I can pick a date using the date picker in the calculator row.
- As a user, I can set a repeat mode (daily / weekly / monthly / etc.) so the transaction recurs automatically.
- As a user, I can set a reminder so I receive a notification to confirm the recurring transaction.
- As a user, I can add a note to any transaction.

### Browsing And Search
- As a user, I can see all transactions in a list sorted by date.
- As a user, I can search transactions by keyword (note or category name).
- As a user, I can filter transactions by type (income / expense / transfer), account, and category using chip filters.
- As a user, I can open a transaction to see its full detail (type, amount, category, account, note, date).
- As a user, I can edit or delete a transaction from its detail sheet — account balances are updated automatically.
- As a user, I can view transactions pre-filtered by account when navigating from the Accounts tab.

## Budget

### Expense Budgets
- As a user, I can tap any expense category chip in the Budget tab to open a budget entry sheet for that category.
- As a user, I can enter a monthly budget amount using a full calculator with currency selection.
- As a user, I can see each budgeted category as a full row with a progress bar showing spending vs budget.
- As a user, I can see remaining budget in green (within budget) or in a colored pill (over budget).
- As a user, I can see unbudgeted categories that have spending as compact chips in a collapsible grid.
- As a user, I can see an "Expand" chip to show all unbudgeted categories when there are more than 3.

### Income Budgets
- As a user, I can set an income budget per income category using the "Expected income" bar at the bottom.
- As a user, I can pick which income category to set a budget for when I have multiple income categories.
- As a user, I can see "Available in budget: X ₴" showing how much of my income budget remains after expenses.
- As a user, I can see "Overspend: X ₴" in red when I've spent more than my income budget.

### Savings Forecast
- As a user, I can see a Savings section with the projected end-of-month savings based on my current spend rate.
- As a user, I can see "passed N of M days" and "projected expenses ~X ₴" for the linear forecast.
- As a user, I can see "saved X ₴" once I've received some income this month.

### Budget Settings
- As a user, I can open budget settings (speedometer icon in top bar) to toggle "Current Expenses" mode or delete all budgets.

## Overview

- As a user, I can see a monthly summary with total income and total expenses in a header toggle.
- As a user, I can switch between Expense and Income views to see the breakdown for each type.
- As a user, I can see a daily bar chart showing spending or income distribution across the month.
- As a user, I can see a stats row with averages and other period metrics.
- As a user, I can see a category list showing each category's spending share for the active mode.
- As a user, I can tap a category row to open a detail sheet with transactions in that category for the period.
- As a user, when no categories have spending, I can still see individual transactions in the list so the header total is never contradicted by an empty list.

## Reports

- As a user, I can inspect aggregate finance summaries and category trends across multiple periods.

## Settings

- As a user, I can choose between light, dark, and system theme.
- As a user, I can pick an accent color from a palette of predefined colors.
- As a user, I can set which tab opens by default when I launch the app.
- As a user, I can hide the Budget tab if I don't use budgeting.
- As a user, I can enable biometric login (Face / Fingerprint) that triggers after 30 seconds in the background.
- As a user, I can enable daily spending notifications delivered by a background worker.
- As a user, I can set the default display currency and number format (decimal separator style).
- As a user, I can browse all 130+ currencies (Main / Other / Crypto tabs) in the currency picker.

## Data And Backup

- As a user, I can export all my data as a JSON file for safekeeping.
- As a user, I can import (full restore) from a JSON backup — all existing data is replaced.
- As a user, I can merge-import from a JSON backup — existing data is kept and imported records are added without duplicating.
- As a user, I can save a local backup to device storage.
- As a user, I can back up to and restore from Google Drive.
- As a user, I can sync transactions from Monobank (MonoFlow) into the app.
- As a user, I can reset all app data from the Data screen after confirming the destructive action.

## Widgets

- As a user, I can place a Balance widget on my home screen to see total account balance without opening the app.
- As a user, I can place an Expense widget on my home screen to see recent spending at a glance.

## Period Navigation

- As a user, I can navigate between months using the month pill (prev/next arrows) on any period-aware screen.
- As a user, I can long-press the month pill to open a period selector and pick a specific month or a non-monthly range (today / week / year / all time / custom range).
- As a user, I can swipe left/right on a tab screen to go to the next/previous month — the swipe is sensitive enough to distinguish from vertical scrolling.
- As a user, I can see the month label animate (slide left for next, slide right for prev) so the direction is clear.
- As a user, pressing the system Back button from any tab navigates to my configured home tab; pressing Back from the home tab exits the app.
