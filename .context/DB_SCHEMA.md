# DB Schema

Room database: `AppDatabase`

Current version: `15`

Entities:

- `accounts`
- `categories`
- `transactions`

## Accounts

`AccountEntity`

- `id: Long` primary key, auto-generated
- `name: String`
- `type: AccountType` (`CASH`, `CARD`, `SAVING`, `INVESTMENT`, `DEBT`, `OTHER`)
- `balance: Double`
- `currency: String`, default `UAH`
- `colorHex: String`
- `icon: String`
- `includeInTotal: Boolean`
- `isDefault: Boolean`
- `sortOrder: Int`
- `description: String`
- `createdAt: Long`

## Categories

`CategoryEntity`

- `id: Long` primary key, auto-generated
- `name: String`
- `type: TransactionType`
- `colorHex: String`
- `icon: String`
- `budgetAmount: Double`
- `budgetPeriod: String`
- `isDefault: Boolean`
- `sortOrder: Int`
- `archived: Boolean`
- `parentId: Long?`

`parentId == null` means a root/broad category.

## Transactions

`TransactionEntity`

- `id: Long` primary key, auto-generated
- `type: TransactionType`
- `amount: Double`
- `accountId: Long`
- `toAccountId: Long?`
- `categoryId: Long?`
- `note: String`
- `date: Long`
- `createdAt: Long`

Foreign keys:

- `accountId` -> `accounts.id`, cascade delete
- `toAccountId` -> `accounts.id`, set null on delete
- `categoryId` -> `categories.id`, set null on delete

Indices:

- `accountId`
- `toAccountId`
- `categoryId`
- `date`

## Migrations

- `1 -> 2`: adds `accounts.isDefault`
- `2 -> 3`: adds `accounts.description`
- `3 -> 4`: adds `categories.archived`
- `4 -> 5`: adds `categories.parentId`
- `5 -> 6`: data migration — updates `icon`/`colorHex` for Продукти (`grocery`/`#03A9F4`), Дозвілля (`ticket`/`#E91E63`), Здоров'я (`volunteer`/`#4CAF50`)
- `6 -> 7`: data migration — updates `icon`/`colorHex` for Таксі (`taxi`/`#FDD835`), АЗС (`gas_station`/`#FF8F00`)
- `7 -> 8`: data migration — updates `icon` for Дозвілля/Розваги/Кіно → `movie`; Gaming → `gaming`; Telegram → `telegram`; Dating → `dating`
- `8 -> 9`: data migration — Дозвілля: icon → `theater`, colorHex → `#7B1FA2`; Транспорт: colorHex → `#00897B`
- `9 -> 10`: data migration — Зв'язок: icon → `phone`, colorHex → `#3F51B5`; Інтернет: icon → `wifi`, colorHex → `#00BCD4`
- `10 -> 11`: data migration — Комуналка/Комунальні/Комунальне: icon → `home`, colorHex → `#546E7A`
- `11 -> 12`: data migration — Фінанси: icon → `money`, colorHex → `#F9A825` (LIKE `%фінанс%`)
- `12 -> 13`: data migration — DELETE EXPENSE categories matching `%фінанс%` or `%финанс%` (finance ≠ expense)
- `13 -> 14`: data migration — root category color palette refresh: Продукти `#4AAFE8`, Ресторація `#4659BE`, Дозвілля `#F73579`, Транспорт `#FFA834`, Здоров'я `#48B456`, Подарунки `#F34B4D`, Сім'я `#7A48F2`, Покупки `#7B5947`
- `14 -> 15`: data migration — single authoritative subcategory icon fix: `delivery`/`#FF6F00` for food delivery; `coffee`/`#795548` for кафе; `restaurant`/`#E53935` for ресторани (parentId IS NOT NULL guard on all; TRIM+LIKE matching); fixes `movie`→`#9C27B0` and `coffee`→`#795548` iconColorMap bugs

Any schema change must add a migration and update this file.
