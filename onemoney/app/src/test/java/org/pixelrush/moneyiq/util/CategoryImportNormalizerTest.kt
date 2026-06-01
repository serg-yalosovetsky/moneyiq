package org.syalosovetskyi.onemoney.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.syalosovetskyi.onemoney.data.db.entities.CategoryEntity
import org.syalosovetskyi.onemoney.data.db.entities.TransactionType

class CategoryImportNormalizerTest {

    @Test
    fun `normalizes generic family icon for phone category`() {
        val normalized = normalizeImportedCategory(
            CategoryEntity(name = "Зв'язок", type = TransactionType.EXPENSE, icon = "family", colorHex = "#000000")
        )

        assertEquals("phone", normalized.icon)
        assertEquals("#3F51B5", normalized.colorHex)
    }

    @Test
    fun `keeps family icon for actual family category`() {
        val normalized = normalizeImportedCategory(
            CategoryEntity(name = "Сім'я", type = TransactionType.EXPENSE, icon = "family", colorHex = "#7A48F2")
        )

        assertEquals("family", normalized.icon)
        assertEquals("#7A48F2", normalized.colorHex)
    }

    @Test
    fun `normalizes imported food delivery subcategory`() {
        val normalized = normalizeImportedCategory(
            CategoryEntity(
                name = "Glovo",
                type = TransactionType.EXPENSE,
                icon = "category",
                colorHex = "#000000",
                parentId = 1L
            )
        )

        assertEquals("delivery", normalized.icon)
        assertEquals("#FF6F00", normalized.colorHex)
    }

    @Test
    fun `normalizes root health category stuck on legacy health icon`() {
        val normalized = normalizeImportedCategory(
            CategoryEntity(name = "Здоров'я", type = TransactionType.EXPENSE, icon = "health", colorHex = "#000000")
        )

        assertEquals("volunteer", normalized.icon)
        assertEquals("#48B456", normalized.colorHex)
    }

    @Test
    fun `repairs known stale icon colors`() {
        val normalized = normalizeImportedCategory(
            CategoryEntity(name = "Кіно", type = TransactionType.EXPENSE, icon = "movie", colorHex = "#000000")
        )

        assertEquals("movie", normalized.icon)
        assertEquals("#9C27B0", normalized.colorHex)
    }
}
