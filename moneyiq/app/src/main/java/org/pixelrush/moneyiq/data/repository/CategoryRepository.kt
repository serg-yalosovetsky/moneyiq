package org.pixelrush.moneyiq.data.repository

import kotlinx.coroutines.flow.Flow
import org.pixelrush.moneyiq.data.db.dao.CategoryDao
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val dao: CategoryDao) {

    fun getAll(): Flow<List<CategoryEntity>> = dao.getAllCategories()

    fun getByType(type: TransactionType): Flow<List<CategoryEntity>> = dao.getCategoriesByType(type)

    suspend fun getById(id: Long): CategoryEntity? = dao.getCategoryById(id)

    suspend fun save(category: CategoryEntity): Long = dao.insertCategory(category)

    suspend fun update(category: CategoryEntity) = dao.updateCategory(category)

    suspend fun delete(category: CategoryEntity) = dao.deleteCategory(category)

    suspend fun seedDefaults() {
        // Посев только при первом запуске
        if (dao.count() > 0) return

        val defaults = listOf(
            // Расходы
            CategoryEntity(name = "Еда и рестораны",   type = TransactionType.EXPENSE, colorHex = "#FF5722", icon = "restaurant", isDefault = true, sortOrder = 1),
            CategoryEntity(name = "Транспорт",          type = TransactionType.EXPENSE, colorHex = "#2196F3", icon = "car",        isDefault = true, sortOrder = 2),
            CategoryEntity(name = "Продукты",           type = TransactionType.EXPENSE, colorHex = "#4CAF50", icon = "shopping",   isDefault = true, sortOrder = 3),
            CategoryEntity(name = "Жильё",              type = TransactionType.EXPENSE, colorHex = "#9C27B0", icon = "home",       isDefault = true, sortOrder = 4),
            CategoryEntity(name = "Развлечения",        type = TransactionType.EXPENSE, colorHex = "#673AB7", icon = "music",      isDefault = true, sortOrder = 5),
            CategoryEntity(name = "Здоровье",           type = TransactionType.EXPENSE, colorHex = "#E91E63", icon = "health",     isDefault = true, sortOrder = 6),
            CategoryEntity(name = "Одежда",             type = TransactionType.EXPENSE, colorHex = "#009688", icon = "shopping",   isDefault = true, sortOrder = 7),
            CategoryEntity(name = "Связь",              type = TransactionType.EXPENSE, colorHex = "#3F51B5", icon = "phone",      isDefault = true, sortOrder = 8),
            CategoryEntity(name = "Образование",        type = TransactionType.EXPENSE, colorHex = "#FF9800", icon = "school",     isDefault = true, sortOrder = 9),
            CategoryEntity(name = "Прочие расходы",     type = TransactionType.EXPENSE, colorHex = "#9E9E9E", icon = "category",   isDefault = true, sortOrder = 10),
            // Доходы
            CategoryEntity(name = "Зарплата",           type = TransactionType.INCOME,  colorHex = "#4CAF50", icon = "work",       isDefault = true, sortOrder = 1),
            CategoryEntity(name = "Фриланс",            type = TransactionType.INCOME,  colorHex = "#4CAF50", icon = "work",       isDefault = true, sortOrder = 2),
            CategoryEntity(name = "Инвестиции",         type = TransactionType.INCOME,  colorHex = "#FFEB3B", icon = "money",      isDefault = true, sortOrder = 3),
            CategoryEntity(name = "Подарок",            type = TransactionType.INCOME,  colorHex = "#E91E63", icon = "gift",       isDefault = true, sortOrder = 4),
            CategoryEntity(name = "Прочие доходы",      type = TransactionType.INCOME,  colorHex = "#4CAF50", icon = "category",   isDefault = true, sortOrder = 5),
        )
        dao.insertCategories(defaults)
    }
}
