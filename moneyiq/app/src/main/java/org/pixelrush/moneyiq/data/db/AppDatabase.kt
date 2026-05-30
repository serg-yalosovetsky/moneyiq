package org.pixelrush.moneyiq.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.pixelrush.moneyiq.data.db.dao.AccountDao
import org.pixelrush.moneyiq.data.db.dao.CategoryDao
import org.pixelrush.moneyiq.data.db.dao.TransactionDao
import org.pixelrush.moneyiq.data.db.entities.AccountEntity
import org.pixelrush.moneyiq.data.db.entities.AccountType
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType

class Converters {
    @TypeConverter fun accountTypeToString(v: AccountType): String = v.name
    @TypeConverter fun stringToAccountType(v: String): AccountType = AccountType.valueOf(v)
    @TypeConverter fun txTypeToString(v: TransactionType): String = v.name
    @TypeConverter fun stringToTxType(v: String): TransactionType = TransactionType.valueOf(v)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE accounts ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE accounts ADD COLUMN description TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN parentId INTEGER")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'grocery',   colorHex = '#03A9F4' WHERE name = 'Продукти'")
        database.execSQL("UPDATE categories SET icon = 'ticket',    colorHex = '#E91E63' WHERE name = 'Дозвілля'")
        database.execSQL("UPDATE categories SET icon = 'volunteer', colorHex = '#4CAF50' WHERE name = 'Здоров''я'")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'taxi',        colorHex = '#FDD835' WHERE name = 'Таксі'")
        database.execSQL("UPDATE categories SET icon = 'gas_station', colorHex = '#FF8F00' WHERE name = 'АЗС'")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'movie'   WHERE name IN ('Дозвілля', 'Розваги', 'Кіно')")
        database.execSQL("UPDATE categories SET icon = 'gaming'  WHERE name = 'Gaming'")
        database.execSQL("UPDATE categories SET icon = 'telegram' WHERE name = 'Telegram'")
        database.execSQL("UPDATE categories SET icon = 'dating'  WHERE name = 'Dating'")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Fix colors that were not updated in 5→6: Дозвілля stayed pink (similar to Подарунки)
        database.execSQL("UPDATE categories SET icon = 'theater', colorHex = '#7B1FA2' WHERE name = 'Дозвілля'")
        // Fix Транспорт: was blue/cyan in user DB — conflicts with Продукти #03A9F4
        database.execSQL("UPDATE categories SET colorHex = '#00897B' WHERE name = 'Транспорт'")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'phone', colorHex = '#3F51B5' WHERE name = 'Зв''язок'")
        database.execSQL("UPDATE categories SET icon = 'wifi',  colorHex = '#00BCD4' WHERE name = 'Інтернет'")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'home', colorHex = '#546E7A' WHERE name IN ('Комунальні', 'Комунальне', 'Комуналка', 'Комунальн')")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'money', colorHex = '#F9A825' WHERE LOWER(name) LIKE '%фінанс%'")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'delivery', colorHex = '#FF6F00' WHERE LOWER(name) IN ('food delivery', 'glovo', 'bolt food', 'uber eats', 'uklon food')")
        database.execSQL("UPDATE categories SET icon = 'coffee',   colorHex = '#795548' WHERE LOWER(name) LIKE '%кафе%' OR LOWER(name) IN ('cafe', 'coffee', 'кав''ярня')")
        database.execSQL("UPDATE categories SET icon = 'restaurant', colorHex = '#E53935' WHERE LOWER(name) IN ('ресторани', 'ресторан', 'restaurants')")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            DELETE FROM categories
            WHERE type = 'EXPENSE'
              AND (
                LOWER(name) LIKE '%фінанс%'
                OR LOWER(name) LIKE '%финанс%'
              )
            """.trimIndent()
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET colorHex = '#4AAFE8' WHERE name = 'Продукти'")
        database.execSQL("UPDATE categories SET colorHex = '#4659BE' WHERE name = 'Ресторація'")
        database.execSQL("UPDATE categories SET colorHex = '#F73579' WHERE name = 'Дозвілля'")
        database.execSQL("UPDATE categories SET colorHex = '#FFA834' WHERE name = 'Транспорт'")
        database.execSQL("UPDATE categories SET colorHex = '#48B456' WHERE name = 'Здоров''я'")
        database.execSQL("UPDATE categories SET colorHex = '#F34B4D' WHERE name = 'Подарунки'")
        database.execSQL("UPDATE categories SET colorHex = '#7A48F2' WHERE name = 'Сім''я'")
        database.execSQL("UPDATE categories SET colorHex = '#7B5947' WHERE name = 'Покупки'")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Re-apply Ресторація subcategory icons with broader matching (TRIM + LIKE)
        database.execSQL("UPDATE categories SET icon = 'delivery', colorHex = '#FF6F00' WHERE LOWER(TRIM(name)) LIKE '%food delivery%' OR LOWER(TRIM(name)) = 'glovo' OR LOWER(TRIM(name)) LIKE '%bolt food%' OR LOWER(TRIM(name)) LIKE '%uber eats%'")
        database.execSQL("UPDATE categories SET icon = 'coffee',   colorHex = '#795548' WHERE LOWER(TRIM(name)) LIKE '%кафе%' OR LOWER(TRIM(name)) LIKE '%cafe%' OR LOWER(TRIM(name)) LIKE '%кав''ярн%'")
        database.execSQL("UPDATE categories SET icon = 'restaurant', colorHex = '#E53935' WHERE LOWER(TRIM(name)) LIKE '%ресторан%' AND LOWER(TRIM(name)) != 'ресторація'")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Exact name matching for Ресторація subcategories (parentId IS NOT NULL = subcategory only)
        database.execSQL("UPDATE categories SET icon = 'delivery', colorHex = '#FF6F00' WHERE parentId IS NOT NULL AND name IN ('Food delivery', 'food delivery', 'Food Delivery', 'Glovo', 'glovo', 'Bolt Food', 'Uber Eats')")
        database.execSQL("UPDATE categories SET icon = 'coffee',   colorHex = '#795548' WHERE parentId IS NOT NULL AND (name = 'Кафе' OR name = 'кафе' OR name = 'Cafe' OR name = 'cafe')")
        database.execSQL("UPDATE categories SET icon = 'restaurant', colorHex = '#E53935' WHERE parentId IS NOT NULL AND (name = 'Ресторани' OR name = 'ресторани' OR name = 'Ресторан' OR name = 'Restaurants')")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Fix iconColorMap bugs: coffee had shopping's color; movie had theater's color
        database.execSQL("UPDATE categories SET colorHex = '#795548' WHERE icon = 'coffee' AND colorHex = '#7B5947'")
        database.execSQL("UPDATE categories SET colorHex = '#9C27B0' WHERE icon = 'movie'  AND colorHex = '#F73579'")
    }
}

val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12,
    MIGRATION_12_13,
    MIGRATION_13_14,
    MIGRATION_14_15,
    MIGRATION_15_16,
    MIGRATION_16_17,
    MIGRATION_17_18
)

@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 18,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
}
