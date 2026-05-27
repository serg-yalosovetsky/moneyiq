"""
MoneyIQ Import Schema
=====================
Pydantic v2 модели для файла импорта данных в приложение MoneyIQ.

Формат файла: JSON
Кодировка: UTF-8

Использование:
    from schema import MoneyIQImport, validate_file

    data = validate_file("my_data.json")
    # или напрямую:
    data = MoneyIQImport.model_validate_json(json_string)
"""

from __future__ import annotations

import json
from datetime import datetime, timezone
from enum import Enum
from pathlib import Path
from typing import List, Optional

from pydantic import BaseModel, Field, field_validator, model_validator


# ---------------------------------------------------------------------------
# Энумы — зеркалят Kotlin-энумы в БД
# ---------------------------------------------------------------------------

class AccountType(str, Enum):
    """Тип счёта (AccountEntity.type)."""
    CASH       = "CASH"        # Наличные
    CARD       = "CARD"        # Банковская карта
    SAVING     = "SAVING"      # Сберегательный
    INVESTMENT = "INVESTMENT"  # Инвестиционный
    DEBT       = "DEBT"        # Долг / кредит
    OTHER      = "OTHER"       # Прочее


class TransactionType(str, Enum):
    """Тип транзакции (TransactionEntity.type и CategoryEntity.type)."""
    INCOME   = "INCOME"    # Доход
    EXPENSE  = "EXPENSE"   # Расход
    TRANSFER = "TRANSFER"  # Перевод между счетами


class BudgetPeriod(str, Enum):
    """Период бюджетного лимита (CategoryEntity.budgetPeriod)."""
    MONTHLY = "MONTHLY"  # Ежемесячный
    WEEKLY  = "WEEKLY"   # Еженедельный


# Regex для валидации цветового кода
_HEX_COLOR_PATTERN = r"^#[0-9A-Fa-f]{6}$"


# ---------------------------------------------------------------------------
# AccountImport
# ---------------------------------------------------------------------------

class AccountImport(BaseModel):
    """
    Описание счёта для импорта.

    `ref` — уникальный строковый ключ внутри файла импорта.
    Используется в транзакциях для ссылки на счёт (вместо числового id БД).
    """

    ref: str = Field(..., min_length=1, description="Уникальный ключ счёта в файле импорта")
    name: str = Field(..., min_length=1, max_length=100)
    type: AccountType = AccountType.CASH
    balance: float = Field(default=0.0, description="Начальный баланс счёта")
    currency: str = Field(default="RUB", min_length=3, max_length=3, description="Код валюты ISO 4217")
    color_hex: str = Field(default="#4CAF50", pattern=_HEX_COLOR_PATTERN)
    icon: str = Field(default="account_balance_wallet")
    include_in_total: bool = Field(default=True, description="Включать в общий баланс")
    sort_order: int = Field(default=0, ge=0)

    @field_validator("currency")
    @classmethod
    def currency_uppercase(cls, v: str) -> str:
        return v.upper()


# ---------------------------------------------------------------------------
# CategoryImport
# ---------------------------------------------------------------------------

class CategoryImport(BaseModel):
    """
    Описание категории для импорта.

    `ref` — уникальный строковый ключ внутри файла импорта.
    Тип может быть только INCOME или EXPENSE (не TRANSFER).
    """

    ref: str = Field(..., min_length=1, description="Уникальный ключ категории в файле импорта")
    name: str = Field(..., min_length=1, max_length=100)
    type: TransactionType = Field(..., description="INCOME или EXPENSE (не TRANSFER)")
    color_hex: str = Field(default="#FF5722", pattern=_HEX_COLOR_PATTERN)
    icon: str = Field(default="category")
    budget_amount: float = Field(default=0.0, ge=0.0, description="Лимит бюджета (0 = без лимита)")
    budget_period: BudgetPeriod = BudgetPeriod.MONTHLY
    is_default: bool = Field(default=False)
    sort_order: int = Field(default=0, ge=0)

    @field_validator("type")
    @classmethod
    def no_transfer_category(cls, v: TransactionType) -> TransactionType:
        if v == TransactionType.TRANSFER:
            raise ValueError(
                "Категория не может иметь тип TRANSFER. "
                "Используйте INCOME или EXPENSE."
            )
        return v


# ---------------------------------------------------------------------------
# TransactionImport
# ---------------------------------------------------------------------------

class TransactionImport(BaseModel):
    """
    Описание транзакции для импорта.

    Для TRANSFER обязателен `to_account_ref` и он не может совпадать с `account_ref`.
    `date` принимается как ISO 8601 datetime и сохраняется в Unix-миллисекундах (Room).
    """

    type: TransactionType
    amount: float = Field(..., gt=0, description="Сумма транзакции (строго > 0)")
    account_ref: str = Field(..., min_length=1, description="Ссылка на AccountImport.ref")
    to_account_ref: Optional[str] = Field(
        default=None,
        description="Ссылка на счёт назначения (только для TRANSFER)"
    )
    category_ref: Optional[str] = Field(
        default=None,
        description="Ссылка на CategoryImport.ref (не обязательна для TRANSFER)"
    )
    note: str = Field(default="", max_length=500)
    date: datetime = Field(..., description="Дата и время в ISO 8601 (напр. 2026-05-15T12:30:00)")

    @model_validator(mode="after")
    def validate_transfer(self) -> "TransactionImport":
        if self.type == TransactionType.TRANSFER:
            if not self.to_account_ref:
                raise ValueError(
                    "Для TRANSFER транзакции обязателен `to_account_ref`."
                )
            if self.to_account_ref == self.account_ref:
                raise ValueError(
                    "Счёт источника и счёт назначения не могут совпадать "
                    f"(оба: '{self.account_ref}')."
                )
        return self

    def date_epoch_ms(self) -> int:
        """Дата транзакции в миллисекундах (формат Room / Android)."""
        return to_epoch_ms(self.date)


# ---------------------------------------------------------------------------
# MoneyIQImport — корневая модель
# ---------------------------------------------------------------------------

class MoneyIQImport(BaseModel):
    """
    Корневая модель файла импорта MoneyIQ.

    Содержит списки счетов, категорий и транзакций.
    При валидации проверяется:
    - уникальность ref внутри accounts и categories
    - что все ссылки (account_ref, to_account_ref, category_ref) существуют
    """

    version: str = Field(default="1.0")
    exported_at: Optional[datetime] = Field(default=None)
    accounts: List[AccountImport] = Field(default_factory=list)
    categories: List[CategoryImport] = Field(default_factory=list)
    transactions: List[TransactionImport] = Field(default_factory=list)

    @model_validator(mode="after")
    def validate_refs(self) -> "MoneyIQImport":
        # Проверка уникальности ref счетов
        account_refs = [a.ref for a in self.accounts]
        duplicates = {r for r in account_refs if account_refs.count(r) > 1}
        if duplicates:
            raise ValueError(
                f"Дублирующиеся ref счетов: {sorted(duplicates)}"
            )

        # Проверка уникальности ref категорий
        category_refs = [c.ref for c in self.categories]
        duplicates = {r for r in category_refs if category_refs.count(r) > 1}
        if duplicates:
            raise ValueError(
                f"Дублирующиеся ref категорий: {sorted(duplicates)}"
            )

        account_ref_set = set(account_refs)
        category_ref_set = set(category_refs)

        # Проверка ссылок в транзакциях
        for i, tx in enumerate(self.transactions):
            if tx.account_ref not in account_ref_set:
                raise ValueError(
                    f"Транзакция [{i}]: account_ref '{tx.account_ref}' не найден в accounts."
                )
            if tx.to_account_ref and tx.to_account_ref not in account_ref_set:
                raise ValueError(
                    f"Транзакция [{i}]: to_account_ref '{tx.to_account_ref}' не найден в accounts."
                )
            if tx.category_ref and tx.category_ref not in category_ref_set:
                raise ValueError(
                    f"Транзакция [{i}]: category_ref '{tx.category_ref}' не найден в categories."
                )

        return self

    def account_ref_map(self) -> dict[str, AccountImport]:
        """Словарь ref → AccountImport для быстрого поиска."""
        return {a.ref: a for a in self.accounts}

    def category_ref_map(self) -> dict[str, CategoryImport]:
        """Словарь ref → CategoryImport для быстрого поиска."""
        return {c.ref: c for c in self.categories}


# ---------------------------------------------------------------------------
# Утилиты
# ---------------------------------------------------------------------------

def to_epoch_ms(dt: datetime) -> int:
    """
    Конвертирует datetime → Unix timestamp в миллисекундах.

    Room/Android хранит даты как Long (мс). Если datetime без timezone —
    трактуется как локальное время.

    Args:
        dt: datetime объект (timezone-aware или naive)

    Returns:
        Unix timestamp в миллисекундах (int)
    """
    if dt.tzinfo is None:
        # Naive datetime: считаем локальным, переводим в UTC через astimezone
        dt = dt.astimezone(timezone.utc)
    return int(dt.timestamp() * 1000)


def validate_file(path: str | Path) -> MoneyIQImport:
    """
    Читает и валидирует JSON-файл импорта.

    Args:
        path: путь к JSON-файлу

    Returns:
        Провалидированная модель MoneyIQImport

    Raises:
        FileNotFoundError: файл не найден
        pydantic.ValidationError: ошибки валидации данных
        json.JSONDecodeError: некорректный JSON
    """
    content = Path(path).read_text(encoding="utf-8")
    return MoneyIQImport.model_validate_json(content)


def summary(data: MoneyIQImport) -> str:
    """Возвращает краткую сводку по импортируемым данным."""
    income_tx  = sum(1 for t in data.transactions if t.type == TransactionType.INCOME)
    expense_tx = sum(1 for t in data.transactions if t.type == TransactionType.EXPENSE)
    transfer_tx = sum(1 for t in data.transactions if t.type == TransactionType.TRANSFER)

    lines = [
        f"MoneyIQ Import v{data.version}",
        f"  Счетов:       {len(data.accounts)}",
        f"  Категорий:    {len(data.categories)}",
        f"  Транзакций:   {len(data.transactions)} "
        f"(доходы: {income_tx}, расходы: {expense_tx}, переводы: {transfer_tx})",
    ]
    if data.exported_at:
        lines.append(f"  Экспорт от:   {data.exported_at.isoformat()}")
    return "\n".join(lines)


# ---------------------------------------------------------------------------
# CLI: python schema.py <file.json>
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Использование: python schema.py <import_file.json>")
        sys.exit(1)

    try:
        data = validate_file(sys.argv[1])
        print("✅ Файл валиден.")
        print(summary(data))
    except FileNotFoundError:
        print(f"❌ Файл не найден: {sys.argv[1]}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Ошибка валидации:\n{e}")
        sys.exit(1)
