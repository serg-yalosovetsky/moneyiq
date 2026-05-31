# DB Schema

Room database: `AppDatabase`

Current version: `26`

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
- `currencyCode: String`, default `"UAH"` (added migration 20→21)

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
- `15 -> 16`: data migration — safety net: sets `money`/`#F9A825` for any remaining "Фінанс*" categories by name
- `16 -> 17`: data migration — re-applies Ресторація subcategory icons using exact name matching (bypasses SQLite Cyrillic `LOWER()` bug): `delivery` for food delivery apps, `coffee` for Кафе, `restaurant` for Ресторани
- `17 -> 18`: data migration — sets `bus` icon for root "Транспорт" (`parentId IS NULL AND name = 'Транспорт'`)
- `18 -> 19`: data migration — fixes categories still stuck on `category` placeholder: `money`/`#F9A825` for фінанс*, `celebration`/`#FF6D00` for розваг*, `theater`/`#F73579` for дозвілл*
- `19 -> 20`: data migration — restores `bus` for Транспорт; assigns distinct icons to subcategories: `pharmacy`/Аптека, `doctor`/Лікар, `parking`/Паркінг, `gas_station`/Пальне, `key`/Оренда, `laptop`/Фриланс, `sports`/Спорт
- `20 -> 21`: **structural** — adds `categories.currencyCode TEXT NOT NULL DEFAULT 'UAH'`
- `21 -> 22`: data migration — fixes stale icons from old `suggestCategoryStyle` for existing rows: `delivery` (кур'єр/доставка), `clothes` (одяг/взуття), `school` (освіта/навчання), `devices` (техніка/гаджети), `doctor` (стоматолог), `sports` (спортивн*), `parking` (паркування), `percent` (проценти/відсоток/податки), `gavel` (штраф/пеня)
- `22 -> 23`: data migration — fixes root utilities icons: `home`/`#546E7A` (комунал*), `phone`/`#3F51B5` (зв'язок), `wifi`/`#00BCD4` (інтернет)
- `23 -> 24`: data migration — fixes Здоров'я root stuck on old `health`/`doctor` icon → `volunteer`/`#48B456`; fixes Спорт stuck on health icon → `sports`; fixes any remaining root-level `health` icon → `volunteer`
- `24 -> 25`: data migration — unconditional fix for Дозвілля (`theater`/`#F73579`) and Розваги (`celebration`/`#FF6D00`); no `icon = 'category'` guard because imported data can overwrite prior migrations via REPLACE strategy
- `25 -> 26`: data migration — inserts default income categories (`Зарплата`/`work`/`#4CAF50`, `Фриланс`/`laptop`/`#26A69A`, `Інше`/`category`/`#78909C`) using conditional INSERT … WHERE NOT EXISTS, so existing categories are never duplicated

Any schema change must add a migration and update this file.
