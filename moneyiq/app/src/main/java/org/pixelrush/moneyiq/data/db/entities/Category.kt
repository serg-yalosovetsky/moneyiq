package org.pixelrush.moneyiq.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,    // Доход
    EXPENSE,   // Расход
    TRANSFER,  // Перевод между счетами
    BORROW,    // Взять в долг (деньги приходят, но обязательство создаётся)
    LEND,      // Дать в долг (деньги уходят, появляется дебиторка)
    REPAY      // Вернуть долг (погашение любого долга)
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,          // INCOME или EXPENSE
    val colorHex: String = "#FF5722",
    val icon: String = "category",
    val budgetAmount: Double = 0.0,     // 0 = без лимита
    val budgetPeriod: String = "MONTHLY", // MONTHLY / WEEKLY
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
