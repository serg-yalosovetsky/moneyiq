package org.syalosovetskyi.onemoney.ui.categories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.syalosovetskyi.onemoney.data.db.entities.CategoryEntity
import org.syalosovetskyi.onemoney.data.db.entities.TransactionType

/**
 * Порядок і пошук у пікері категорій. Перевіряється тут, а не на пристрої: `adb input text`
 * не вміє кирилицю, а саме на ній видно і сортування за правилами мови, і пошук.
 */
class CategoryPickerOrderTest {

    private fun cat(
        id: Long,
        name: String,
        parentId: Long? = null,
        sortOrder: Int = 0,
        type: TransactionType = TransactionType.EXPENSE,
        archived: Boolean = false,
    ) = CategoryEntity(
        id = id, name = name, type = type, parentId = parentId,
        sortOrder = sortOrder, archived = archived
    )

    // Транспорт (sortOrder 1) з підкатегоріями впереміш, Продукти (sortOrder 0) окремо
    private val categories = listOf(
        cat(10, "Продукти", sortOrder = 0),
        cat(20, "Транспорт", sortOrder = 1),
        cat(21, "Таксі", parentId = 20),
        cat(22, "АЗС", parentId = 20),
        cat(23, "Автозапчастини", parentId = 20),
        cat(11, "Пекарня", parentId = 10),
        cat(30, "Зарплата", type = TransactionType.INCOME, sortOrder = 0),
        cat(40, "Архівна", sortOrder = 2, archived = true),
    )

    @Test
    fun `subcategories stand right under their parent, alphabetically`() {
        val ordered = groupedByParent(categories, TransactionType.EXPENSE, includeSubcategories = true)

        assertEquals(
            listOf("Продукти", "Пекарня", "Транспорт", "Автозапчастини", "АЗС", "Таксі"),
            ordered.map { it.name }
        )
    }

    @Test
    fun `root order follows sortOrder, not the alphabet`() {
        // Продукти (0) перед Транспорт (1), хоч за абеткою було б навпаки
        val ordered = groupedByParent(categories, TransactionType.EXPENSE, includeSubcategories = true)
        val roots = ordered.filter { it.parentId == null }.map { it.name }

        assertEquals(listOf("Продукти", "Транспорт"), roots)
    }

    @Test
    fun `archived categories are not offered`() {
        val ordered = groupedByParent(categories, TransactionType.EXPENSE, includeSubcategories = true)

        assertTrue(ordered.none { it.name == "Архівна" })
    }

    @Test
    fun `without subcategories only roots are shown`() {
        val ordered = groupedByParent(categories, TransactionType.EXPENSE, includeSubcategories = false)

        assertEquals(listOf("Продукти", "Транспорт"), ordered.map { it.name })
    }

    @Test
    fun `orphaned subcategory does not disappear from the picker`() {
        // батько архівний → підкатегорія все одно має бути в списку, у кінці
        val orphan = listOf(cat(50, "Оренда", parentId = 999), cat(10, "Продукти"))
        val ordered = groupedByParent(orphan, TransactionType.EXPENSE, includeSubcategories = true)

        assertEquals(listOf("Продукти", "Оренда"), ordered.map { it.name })
    }

    @Test
    fun `search by parent name keeps the whole group`() {
        val ordered = groupedByParent(categories, TransactionType.EXPENSE, includeSubcategories = true)
        val parentNames = categories.associate { it.id to it.name }

        val found = filterByQuery(ordered, "транспорт", parentNames).map { it.name }

        assertEquals(listOf("Транспорт", "Автозапчастини", "АЗС", "Таксі"), found)
    }

    @Test
    fun `search matches a substring regardless of case`() {
        val ordered = groupedByParent(categories, TransactionType.EXPENSE, includeSubcategories = true)
        val parentNames = categories.associate { it.id to it.name }

        assertEquals(listOf("Таксі"), filterByQuery(ordered, "ТАКС", parentNames).map { it.name })
        assertEquals(listOf("Пекарня"), filterByQuery(ordered, "пекар", parentNames).map { it.name })
    }

    @Test
    fun `empty query returns the full list`() {
        val ordered = groupedByParent(categories, TransactionType.EXPENSE, includeSubcategories = true)
        val parentNames = categories.associate { it.id to it.name }

        assertEquals(ordered, filterByQuery(ordered, "   ", parentNames))
    }
}
