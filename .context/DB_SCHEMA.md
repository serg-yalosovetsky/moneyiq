# DB Schema

Room database: `AppDatabase`

Current version: `11`

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

Any schema change must add a migration and update this file.
