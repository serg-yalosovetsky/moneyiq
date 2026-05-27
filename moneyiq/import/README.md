# MoneyIQ — Формат файла импорта

Файл импорта — это обычный **JSON в кодировке UTF-8**, содержащий счета, категории и транзакции.

## Быстрый старт

```bash
# Установить Pydantic (единственная зависимость)
pip install "pydantic>=2.0"

# Проверить свой файл
python schema.py my_data.json
```

---

## Структура файла

```json
{
  "version": "1.0",
  "exported_at": "2026-05-27T10:00:00",
  "accounts":     [ ... ],
  "categories":   [ ... ],
  "transactions": [ ... ]
}
```

| Поле | Обязательное | Описание |
|------|:---:|---------|
| `version` | | Версия формата (по умолчанию `"1.0"`) |
| `exported_at` | | ISO 8601 datetime, информационное поле |
| `accounts` | | Список счетов |
| `categories` | | Список категорий |
| `transactions` | | Список транзакций |

---

## Счёт (`accounts[]`)

```json
{
  "ref": "wallet",
  "name": "Кошелёк",
  "type": "CASH",
  "balance": 5000.0,
  "currency": "RUB",
  "color_hex": "#4CAF50",
  "icon": "account_balance_wallet",
  "include_in_total": true,
  "sort_order": 0
}
```

| Поле | Обязательное | По умолчанию | Допустимые значения |
|------|:---:|---|---|
| `ref` | ✅ | — | Уникальная строка в файле |
| `name` | ✅ | — | Строка 1–100 символов |
| `type` | | `CASH` | `CASH` `CARD` `SAVING` `INVESTMENT` `DEBT` `OTHER` |
| `balance` | | `0.0` | Число |
| `currency` | | `RUB` | ISO 4217 (3 буквы) |
| `color_hex` | | `#4CAF50` | `#RRGGBB` |
| `icon` | | `account_balance_wallet` | Material Design icon name |
| `include_in_total` | | `true` | `true` / `false` |
| `sort_order` | | `0` | Целое ≥ 0 |

---

## Категория (`categories[]`)

```json
{
  "ref": "food",
  "name": "Еда",
  "type": "EXPENSE",
  "color_hex": "#FF5722",
  "icon": "restaurant",
  "budget_amount": 15000.0,
  "budget_period": "MONTHLY"
}
```

| Поле | Обязательное | По умолчанию | Допустимые значения |
|------|:---:|---|---|
| `ref` | ✅ | — | Уникальная строка в файле |
| `name` | ✅ | — | Строка 1–100 символов |
| `type` | ✅ | — | `INCOME` или `EXPENSE` (не `TRANSFER`) |
| `color_hex` | | `#FF5722` | `#RRGGBB` |
| `icon` | | `category` | Material Design icon name |
| `budget_amount` | | `0.0` | Число ≥ 0 (0 = без лимита) |
| `budget_period` | | `MONTHLY` | `MONTHLY` `WEEKLY` |
| `is_default` | | `false` | Системная категория |
| `sort_order` | | `0` | Целое ≥ 0 |

---

## Транзакция (`transactions[]`)

```json
{
  "type": "EXPENSE",
  "amount": 450.0,
  "account_ref": "wallet",
  "category_ref": "food",
  "note": "Обед",
  "date": "2026-05-15T13:15:00"
}
```

```json
{
  "type": "TRANSFER",
  "amount": 5000.0,
  "account_ref": "sberbank",
  "to_account_ref": "wallet",
  "date": "2026-05-12T18:30:00"
}
```

| Поле | Обязательное | По умолчанию | Описание |
|------|:---:|---|---|
| `type` | ✅ | — | `INCOME` / `EXPENSE` / `TRANSFER` |
| `amount` | ✅ | — | Число > 0 |
| `account_ref` | ✅ | — | Ссылка на `accounts[].ref` |
| `to_account_ref` | ✅ при `TRANSFER` | `null` | Счёт назначения, ≠ `account_ref` |
| `category_ref` | | `null` | Ссылка на `categories[].ref` |
| `note` | | `""` | До 500 символов |
| `date` | ✅ | — | ISO 8601 (`2026-05-15T13:15:00`) |

### Правила для TRANSFER
- `to_account_ref` **обязателен**
- `to_account_ref` не может совпадать с `account_ref`
- `category_ref` можно не указывать

---

## Использование из Python

```python
from schema import MoneyIQImport, validate_file, summary, to_epoch_ms

# Загрузить и провалидировать файл
data = validate_file("sample_import.json")

# Краткая сводка
print(summary(data))

# Работа с данными
for account in data.accounts:
    print(f"{account.ref}: {account.name} = {account.balance} {account.currency}")

for tx in data.transactions:
    print(f"{tx.type.value}: {tx.amount} [{tx.date_epoch_ms()} ms]")

# Словарь ref → объект
account_map = data.account_ref_map()
category_map = data.category_ref_map()
```

---

## Валидация через CLI

```bash
python schema.py sample_import.json
# ✅ Файл валиден.
# MoneyIQ Import v1.0
#   Счетов:       3
#   Категорий:    7
#   Транзакций:   9 (доходы: 2, расходы: 5, переводы: 2)
```

---

## Полный пример

Смотри [`sample_import.json`](sample_import.json) — 3 счёта, 7 категорий, 9 транзакций.
