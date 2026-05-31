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

// ── Schema migrations (structural) ───────────────────────────────────────────

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

// ── Data migrations (icon/color backfills) ────────────────────────────────────

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
        database.execSQL("UPDATE categories SET icon = 'movie'    WHERE name IN ('Дозвілля', 'Розваги', 'Кіно')")
        database.execSQL("UPDATE categories SET icon = 'gaming'   WHERE name = 'Gaming'")
        database.execSQL("UPDATE categories SET icon = 'telegram' WHERE name = 'Telegram'")
        database.execSQL("UPDATE categories SET icon = 'dating'   WHERE name = 'Dating'")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'theater', colorHex = '#7B1FA2' WHERE name = 'Дозвілля'")
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
        database.execSQL(
            """
            DELETE FROM categories
            WHERE type = 'EXPENSE'
              AND (LOWER(name) LIKE '%фінанс%' OR LOWER(name) LIKE '%финанс%')
            """.trimIndent()
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
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

// Single authoritative subcategory icon migration — parentId IS NOT NULL guard,
// correct colors, TRIM+LIKE matching. Replaces the four previous attempts.
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'delivery',   colorHex = '#FF6F00' WHERE parentId IS NOT NULL AND (LOWER(TRIM(name)) LIKE '%food delivery%' OR LOWER(TRIM(name)) = 'glovo' OR LOWER(TRIM(name)) LIKE '%bolt food%' OR LOWER(TRIM(name)) LIKE '%uber eats%')")
        database.execSQL("UPDATE categories SET icon = 'coffee',     colorHex = '#795548' WHERE parentId IS NOT NULL AND (LOWER(TRIM(name)) LIKE '%кафе%' OR LOWER(TRIM(name)) LIKE '%cafe%' OR LOWER(TRIM(name)) LIKE '%кав''ярн%')")
        database.execSQL("UPDATE categories SET icon = 'restaurant', colorHex = '#E53935' WHERE parentId IS NOT NULL AND LOWER(TRIM(name)) LIKE '%ресторан%' AND LOWER(TRIM(name)) != 'ресторація'")
        database.execSQL("UPDATE categories SET colorHex = '#9C27B0' WHERE icon = 'movie'  AND colorHex != '#9C27B0'")
        database.execSQL("UPDATE categories SET colorHex = '#795548' WHERE icon = 'coffee' AND colorHex != '#795548'")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'money', colorHex = '#F9A825' WHERE name IN ('Фінанси', 'Фінансові послуги', 'Finance', 'Finances')")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'delivery', colorHex = '#FF6F00' WHERE parentId IS NOT NULL AND name IN ('Food delivery', 'food delivery', 'Food Delivery', 'Glovo', 'glovo', 'Bolt Food', 'Uber Eats')")
        database.execSQL("UPDATE categories SET icon = 'coffee',   colorHex = '#795548' WHERE parentId IS NOT NULL AND (name = 'Кафе' OR name = 'кафе' OR name = 'Cafe' OR name = 'cafe')")
        database.execSQL("UPDATE categories SET icon = 'restaurant', colorHex = '#E53935' WHERE parentId IS NOT NULL AND (name = 'Ресторани' OR name = 'ресторани' OR name = 'Ресторан' OR name = 'Restaurants')")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'bus' WHERE parentId IS NULL AND name = 'Транспорт'")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Fix categories still stuck on placeholder after prior migrations
        database.execSQL("UPDATE categories SET icon = 'money',       colorHex = '#F9A825' WHERE (LOWER(name) LIKE '%фінанс%' OR LOWER(name) LIKE '%финанс%') AND icon = 'category'")
        database.execSQL("UPDATE categories SET icon = 'celebration', colorHex = '#FF6D00' WHERE LOWER(name) LIKE '%розваг%' AND icon = 'category'")
        database.execSQL("UPDATE categories SET icon = 'theater',     colorHex = '#F73579' WHERE LOWER(name) LIKE '%дозвілл%' AND icon = 'category'")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // "bus" is now in CategoryIcons — restore Транспорт
        database.execSQL("UPDATE categories SET icon = 'bus' WHERE parentId IS NULL AND name = 'Транспорт'")
        // Give every duplicated subcategory its own distinct icon
        database.execSQL("UPDATE categories SET icon = 'pharmacy'    WHERE name = 'Аптека'")
        database.execSQL("UPDATE categories SET icon = 'doctor'      WHERE name = 'Лікар'")
        database.execSQL("UPDATE categories SET icon = 'parking'     WHERE name = 'Паркінг'")
        database.execSQL("UPDATE categories SET icon = 'gas_station' WHERE name = 'Пальне'")
        database.execSQL("UPDATE categories SET icon = 'key'         WHERE name = 'Оренда'")
        database.execSQL("UPDATE categories SET icon = 'laptop'      WHERE name = 'Фриланс'")
        database.execSQL("UPDATE categories SET icon = 'sports'      WHERE name = 'Спорт'")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'UAH'")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Fix user-created categories that received wrong icons via old suggestCategoryStyle
        database.execSQL("UPDATE categories SET icon = 'delivery' WHERE LOWER(name) LIKE '%кур%єр%' OR LOWER(name) LIKE '%кур%ер%' OR LOWER(name) LIKE '%доставк%'")
        database.execSQL("UPDATE categories SET icon = 'clothes'  WHERE LOWER(name) = 'одяг' OR LOWER(name) = 'взуття'")
        database.execSQL("UPDATE categories SET icon = 'school'   WHERE LOWER(name) LIKE '%освіт%' OR LOWER(name) LIKE '%навчан%'")
        database.execSQL("UPDATE categories SET icon = 'devices'  WHERE LOWER(name) IN ('техніка', 'гаджети', 'електроніка') AND icon NOT IN ('devices', 'gaming')")
        database.execSQL("UPDATE categories SET icon = 'doctor'   WHERE LOWER(name) LIKE '%стоматол%' OR LOWER(name) LIKE '%стоматолог%'")
        database.execSQL("UPDATE categories SET icon = 'sports'   WHERE LOWER(name) LIKE '%спортивн%'")
        database.execSQL("UPDATE categories SET icon = 'parking'  WHERE LOWER(name) LIKE '%паркуван%'")
        database.execSQL("UPDATE categories SET icon = 'percent'  WHERE LOWER(name) LIKE '%процент%' OR LOWER(name) LIKE '%відсоток%'")
        database.execSQL("UPDATE categories SET icon = 'percent'  WHERE LOWER(name) LIKE '%податок%' OR LOWER(name) LIKE '%податки%' OR LOWER(name) LIKE '%пдв%'")
        database.execSQL("UPDATE categories SET icon = 'gavel'    WHERE LOWER(name) LIKE '%штраф%' OR LOWER(name) LIKE '%пеня%'")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE categories SET icon = 'home',  colorHex = '#546E7A' WHERE LOWER(name) LIKE '%комунал%'")
        database.execSQL("UPDATE categories SET icon = 'phone', colorHex = '#3F51B5' WHERE LOWER(REPLACE(name, '''', '''')) LIKE '%зв_язок%'")
        database.execSQL("UPDATE categories SET icon = 'wifi',  colorHex = '#00BCD4' WHERE LOWER(name) = 'інтернет'")
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Fix Здоров'я root stuck on 'health' (cross) icon from old import/seed
        database.execSQL("UPDATE categories SET icon = 'volunteer', colorHex = '#48B456' WHERE LOWER(name) LIKE '%здоров%' AND parentId IS NULL AND icon IN ('health', 'doctor')")
        // Fix Спорт stuck on 'health' icon — must come before the broad health fix
        database.execSQL("UPDATE categories SET icon = 'sports' WHERE LOWER(name) = 'спорт' AND icon IN ('health', 'doctor', 'volunteer')")
        // Any remaining root-level categories still carrying the old 'health' cross icon
        database.execSQL("UPDATE categories SET icon = 'volunteer', colorHex = '#48B456' WHERE icon = 'health' AND parentId IS NULL")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Unconditional fix — imported data can overwrite prior migrations via REPLACE strategy
        database.execSQL("UPDATE categories SET icon = 'theater',     colorHex = '#F73579' WHERE LOWER(TRIM(name)) = 'дозвілля'  AND parentId IS NULL")
        database.execSQL("UPDATE categories SET icon = 'celebration', colorHex = '#FF6D00' WHERE LOWER(TRIM(name)) LIKE '%розваг%'")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insert Зарплата income category if no income category with similar name exists
        database.execSQL("""
            INSERT INTO categories (name, type, colorHex, icon, budgetAmount, budgetPeriod, isDefault, sortOrder, archived, parentId, currencyCode)
            SELECT 'Зарплата', 'INCOME', '#4CAF50', 'work', 0.0, 'MONTHLY', 1, 1, 0, NULL, 'UAH'
            WHERE NOT EXISTS (SELECT 1 FROM categories WHERE type = 'INCOME' AND LOWER(name) LIKE '%зарплат%')
        """.trimIndent())
        // Insert Фриланс if missing
        database.execSQL("""
            INSERT INTO categories (name, type, colorHex, icon, budgetAmount, budgetPeriod, isDefault, sortOrder, archived, parentId, currencyCode)
            SELECT 'Фриланс', 'INCOME', '#26A69A', 'laptop', 0.0, 'MONTHLY', 1, 2, 0, NULL, 'UAH'
            WHERE NOT EXISTS (SELECT 1 FROM categories WHERE type = 'INCOME' AND LOWER(name) LIKE '%фриланс%')
        """.trimIndent())
        // Insert Інше income if no fallback income category exists
        database.execSQL("""
            INSERT INTO categories (name, type, colorHex, icon, budgetAmount, budgetPeriod, isDefault, sortOrder, archived, parentId, currencyCode)
            SELECT 'Інше', 'INCOME', '#78909C', 'category', 0.0, 'MONTHLY', 1, 3, 0, NULL, 'UAH'
            WHERE NOT EXISTS (SELECT 1 FROM categories WHERE type = 'INCOME' AND (LOWER(name) LIKE '%інше%' OR LOWER(name) LIKE '%інший%'))
        """.trimIndent())
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
    MIGRATION_17_18,
    MIGRATION_18_19,
    MIGRATION_19_20,
    MIGRATION_20_21,
    MIGRATION_21_22,
    MIGRATION_22_23,
    MIGRATION_23_24,
    MIGRATION_24_25,
    MIGRATION_25_26
)

@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 26,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
}
