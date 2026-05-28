# Transaction Accounting

`TransactionRepository` owns balance mutations. Keep this invariant intact.

## Add Transaction

- `INCOME`: increase `accountId` balance.
- `EXPENSE`: decrease `accountId` balance.
- `BORROW`: increase `accountId` balance.
- `LEND`: decrease `accountId` balance.
- `REPAY`: decrease `accountId`; if `toAccountId` exists, increase it.
- `TRANSFER`: decrease `accountId`; if `toAccountId` exists, increase it.

## Delete Transaction

Deletion applies the exact inverse of add.

## Update Transaction

Update is a two-phase operation:

1. Roll back old transaction balance effects.
2. Persist the new transaction.
3. Apply new transaction balance effects.

## Regression Rules

- Do not update account balances directly in UI screens.
- Do not insert/update/delete transactions outside `TransactionRepository` unless the balance invariant is explicitly handled.
- Be careful with `toAccountId == null`; transfer-like operations become one-sided balance updates.
- If transaction semantics change, update tests and this document.
