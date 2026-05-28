package org.pixelrush.moneyiq.data.repository

import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.pixelrush.moneyiq.data.db.dao.CategoryDao
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType

class CategoryRepositoryTest {

    private val dao: CategoryDao = mockk(relaxed = true)
    private lateinit var repo: CategoryRepository

    @Before
    fun setup() {
        repo = CategoryRepository(dao)
    }

    @Test
    fun `seedDefaults inserts categories when db is empty`() = runTest {
        coEvery { dao.count() } returns 0
        coEvery { dao.insertCategory(any()) } returns 1L

        repo.seedDefaults()

        coVerify(atLeast = 9) { dao.insertCategory(any()) }
    }

    @Test
    fun `seedDefaults inserts expense subcategories via insertCategories`() = runTest {
        coEvery { dao.count() } returns 0
        coEvery { dao.insertCategory(any()) } returns 1L

        repo.seedDefaults()

        coVerify(atLeast = 1) { dao.insertCategories(any()) }
    }

    @Test
    fun `seedDefaults inserts income categories`() = runTest {
        coEvery { dao.count() } returns 0
        coEvery { dao.insertCategory(any()) } returns 1L

        repo.seedDefaults()

        coVerify(atLeast = 1) {
            dao.insertCategories(match { list ->
                list.any { it.type == TransactionType.INCOME }
            })
        }
    }

    @Test
    fun `seedDefaults skips insertion when categories already exist`() = runTest {
        coEvery { dao.count() } returns 5

        repo.seedDefaults()

        coVerify(exactly = 0) { dao.insertCategory(any()) }
        coVerify(exactly = 0) { dao.insertCategories(any()) }
    }

    @Test
    fun `getByType delegates to dao getCategoriesByType`() {
        val flow = flowOf(emptyList<CategoryEntity>())
        every { dao.getCategoriesByType(TransactionType.EXPENSE) } returns flow

        val result = repo.getByType(TransactionType.EXPENSE)

        assertEquals(flow, result)
    }

    @Test
    fun `getAll returns flow from dao`() {
        val flow = flowOf(emptyList<CategoryEntity>())
        every { dao.getAllCategories() } returns flow

        val result = repo.getAll()

        assertEquals(flow, result)
    }

    @Test
    fun `save delegates to dao insertCategory`() = runTest {
        val category = CategoryEntity(name = "Test", type = TransactionType.EXPENSE)
        coEvery { dao.insertCategory(any()) } returns 1L
        repo.save(category)
        coVerify { dao.insertCategory(category) }
    }

    @Test
    fun `update delegates to dao updateCategory`() = runTest {
        val category = CategoryEntity(id = 1L, name = "Updated", type = TransactionType.EXPENSE)
        repo.update(category)
        coVerify { dao.updateCategory(category) }
    }

    @Test
    fun `delete delegates to dao deleteCategory`() = runTest {
        val category = CategoryEntity(id = 1L, name = "Test", type = TransactionType.EXPENSE)
        repo.delete(category)
        coVerify { dao.deleteCategory(category) }
    }

    @Test
    fun `getById delegates to dao getCategoryById`() = runTest {
        val category = CategoryEntity(id = 3L, name = "Found", type = TransactionType.INCOME)
        coEvery { dao.getCategoryById(3L) } returns category

        val result = repo.getById(3L)

        assertEquals(category, result)
    }
}
