package org.syalosovetskyi.onemoney.ui.categories

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.syalosovetskyi.onemoney.R
import org.syalosovetskyi.onemoney.data.db.entities.CategoryEntity
import org.syalosovetskyi.onemoney.data.db.entities.TransactionType
import org.syalosovetskyi.onemoney.ui.theme.onemoneyTheme

/**
 * Пошук у пікері категорій — кирилицею, на пристрої.
 *
 * `adb shell input text` кирилицю не вміє, але `performTextInput` пише прямо у вузол
 * семантики, повз IME: перевіряти пошук українською тут МОЖНА, і саме тут це і треба
 * робити — unit-тест не бачить ані відступів, ані підпису батька.
 */
class CategoryPickerSearchUiTest {

    @get:Rule
    val compose = createComposeRule()

    // Рядки беремо з ресурсів: емулятор може стояти будь-якою мовою, і тест, який шукає
    // українські підписи, зламається не через дефект, а через локаль пристрою.
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(id: Int, vararg args: Any): String = ctx.getString(id, *args)

    private fun cat(id: Long, name: String, parentId: Long? = null, sortOrder: Int = 0) =
        CategoryEntity(
            id = id, name = name, type = TransactionType.EXPENSE,
            parentId = parentId, sortOrder = sortOrder
        )

    private val categories = listOf(
        cat(10, "Транспорт", sortOrder = 0),
        cat(11, "АЗС", parentId = 10),
        cat(12, "Таксі", parentId = 10),
        cat(20, "Дозвілля", sortOrder = 1),
        cat(21, "Кіно", parentId = 20),
    )

    private fun showPicker() {
        compose.setContent {
            onemoneyTheme {
                QuickCategoryPickerSheet(
                    categories = categories,
                    selectedCategoryId = -1L,
                    includeSubcategories = true,
                    onSelect = {},
                    onDismiss = {}
                )
            }
        }
    }

    @Test
    fun searchByParentNameKeepsTheWholeGroup() {
        showPicker()

        compose.onNode(hasSetTextAction()).performTextInput("транспорт")

        assert(compose.onAllNodesWithText("Транспорт").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText("АЗС").assertIsDisplayed()
        compose.onNodeWithText("Таксі").assertIsDisplayed()
        compose.onNodeWithText("Кіно").assertDoesNotExist()
    }

    @Test
    fun foundSubcategoryShowsItsParent() {
        showPicker()

        // «Кіно» знайдеться саме по собі, батька «Дозвілля» в результатах немає —
        // тому назва батька мусить бути підписом рядка, інакше рядок висить нізвідки
        compose.onNode(hasSetTextAction()).performTextInput("кіно")

        compose.onNodeWithText("Кіно").assertIsDisplayed()
        // підпис батька в тому ж рядку — сам «Дозвілля» у знайдене не потрапив
        assert(compose.onAllNodesWithText("Дозвілля").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun clearingSearchBringsTheFullListBack() {
        showPicker()

        compose.onNode(hasSetTextAction()).performTextInput("такс")
        compose.onNodeWithText("Кіно").assertDoesNotExist()

        compose.onNodeWithContentDescription(str(R.string.cat_search_clear)).performClick()

        // «АЗС» під фільтром «такс» не показувався; після очищення він знову у списку.
        // Беремо саме його, а не «Кіно»: ліниві списки не створюють вузли для рядків,
        // які не потрапили на екран, і перевірка нижнього елемента ловила б не дефект.
        compose.onNodeWithText("АЗС").assertIsDisplayed()
        compose.onNodeWithText("Транспорт").assertIsDisplayed()
    }

    @Test
    fun nothingFoundIsSaidOutLoud() {
        showPicker()

        compose.onNode(hasSetTextAction()).performTextInput("яхта")

        compose.onNodeWithText("Транспорт").assertDoesNotExist()
        compose.onNodeWithText("Кіно").assertDoesNotExist()
        compose.onNodeWithText("АЗС").assertDoesNotExist()
        compose.onNodeWithText(str(R.string.cat_search_empty, "яхта")).assertExists()
    }
}
