package org.syalosovetskyi.onemoney.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.syalosovetskyi.onemoney.data.db.entities.TxOverrideEntity

@Dao
interface TxOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: TxOverrideEntity)

    /**
     * Правки, які ще має сенс віддавати. Ті, що вичерпали спроби, лишаються в таблиці
     * (нічого не зникає), але більше не блокують синк — інакше одна відхилена сервером
     * правка зупинила б синхронізацію назавжди.
     */
    @Query("SELECT * FROM tx_overrides WHERE synced = 0 AND attempts < :maxAttempts ORDER BY updatedAt")
    suspend fun getPending(maxAttempts: Int): List<TxOverrideEntity>

    @Query("SELECT COUNT(*) FROM tx_overrides WHERE synced = 0 AND attempts >= :maxAttempts")
    suspend fun deadCount(maxAttempts: Int): Int

    @Query("UPDATE tx_overrides SET attempts = attempts + 1, lastError = :error WHERE txId = :txId AND updatedAt = :updatedAt")
    suspend fun markRejected(txId: Long, updatedAt: Long, error: String)

    /**
     * Позначає правку відданою. `updatedAt` у ключі навмисно: якщо поки летів запит
     * користувач правив операцію ще раз, рядок уже інший — і нова правка НЕ буде
     * помилково зарахована як віддана.
     */
    @Query("UPDATE tx_overrides SET synced = 1 WHERE txId = :txId AND updatedAt = :updatedAt")
    suspend fun markSynced(txId: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM tx_overrides WHERE synced = 0 AND attempts < :maxAttempts")
    suspend fun pendingCount(maxAttempts: Int): Int

    @Query("SELECT * FROM tx_overrides WHERE txId = :txId")
    suspend fun getById(txId: Long): TxOverrideEntity?
}
