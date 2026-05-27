package org.pixelrush.moneyiq.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType { CASH, CARD, SAVING, INVESTMENT, DEBT, OTHER }

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CASH,
    val balance: Double = 0.0,
    val currency: String = "USD",
    val colorHex: String = "#4CAF50",
    val icon: String = "account_balance_wallet",
    val includeInTotal: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
