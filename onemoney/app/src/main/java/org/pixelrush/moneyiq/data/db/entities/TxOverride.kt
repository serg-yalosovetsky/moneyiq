package org.syalosovetskyi.onemoney.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Черга правок операцій, які треба віддати на mono-flow.
 *
 * Синхронізація тягне операції з сервера і перезаписує їх за id (REPLACE), тому
 * локальна правка операції, що прийшла з сервера, живе рівно до наступного синку —
 * якщо не доїде назад. Рядок тут лежить доти, доки сервер не підтвердить запис.
 *
 * `categoryName`, а не id: id категорії на сервері похідний від імені, а ім'я
 * переживає перезбирання видачі.
 */
@Entity(tableName = "tx_overrides")
data class TxOverrideEntity(
    @PrimaryKey val txId: Long,
    val note: String,
    val categoryName: String? = null,
    val txType: String,                              // EXPENSE / INCOME / TRANSFER
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
