# DB Schema

Room database: `AppDatabase`

Current version: `5`

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

Any schema change must add a migration and update this file.
