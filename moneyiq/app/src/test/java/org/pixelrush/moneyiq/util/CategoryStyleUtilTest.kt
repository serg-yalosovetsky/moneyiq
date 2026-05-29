package org.pixelrush.moneyiq.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.pixelrush.moneyiq.data.db.entities.TransactionType

class CategoryStyleUtilTest {

    // ── expense categories ───────────────────────────────────────────────────

    @Test
    fun `продукти returns grocery icon`() {
        val (icon, _) = suggestCategoryStyle("продукти", TransactionType.EXPENSE)
        assertEquals("grocery", icon)
    }

    @Test
    fun `ресторація returns restaurant icon`() {
        val (icon, _) = suggestCategoryStyle("ресторація", TransactionType.EXPENSE)
        assertEquals("restaurant", icon)
    }

    @Test
    fun `транспорт returns car icon`() {
        val (icon, _) = suggestCategoryStyle("транспорт", TransactionType.EXPENSE)
        assertEquals("car", icon)
    }

    @Test
    fun `здоров'я returns volunteer icon`() {
        val (icon, _) = suggestCategoryStyle("здоров'я", TransactionType.EXPENSE)
        assertEquals("volunteer", icon)
    }

    @Test
    fun `дозвілля returns theater icon`() {
        val (icon, _) = suggestCategoryStyle("дозвілля", TransactionType.EXPENSE)
        assertEquals("theater", icon)
    }

    @Test
    fun `подарунок returns gift icon`() {
        val (icon, _) = suggestCategoryStyle("подарунок", TransactionType.EXPENSE)
        assertEquals("gift", icon)
    }

    @Test
    fun `спорт returns sports icon`() {
        val (icon, _) = suggestCategoryStyle("спорт", TransactionType.EXPENSE)
        assertEquals("sports", icon)
    }

    @Test
    fun `кафе returns coffee icon`() {
        val (icon, _) = suggestCategoryStyle("кафе", TransactionType.EXPENSE)
        assertEquals("coffee", icon)
    }

    @Test
    fun `інтернет returns wifi icon`() {
        val (icon, _) = suggestCategoryStyle("інтернет", TransactionType.EXPENSE)
        assertEquals("wifi", icon)
    }

    @Test
    fun `аптека returns health icon`() {
        val (icon, _) = suggestCategoryStyle("аптека", TransactionType.EXPENSE)
        assertEquals("health", icon)
    }

    @Test
    fun `одяг returns clothes icon`() {
        val (icon, _) = suggestCategoryStyle("одяг", TransactionType.EXPENSE)
        assertEquals("clothes", icon)
    }

    @Test
    fun `aliexpress keyword returns aliexpress icon`() {
        val (icon, _) = suggestCategoryStyle("AliExpress", TransactionType.EXPENSE)
        assertEquals("aliexpress", icon)
    }

    @Test
    fun `сім'я returns family icon`() {
        val (icon, _) = suggestCategoryStyle("сім'я", TransactionType.EXPENSE)
        assertEquals("family", icon)
    }

    @Test
    fun `комунальні returns receipt icon`() {
        val (icon, _) = suggestCategoryStyle("рахунки", TransactionType.EXPENSE)
        assertEquals("receipt", icon)
    }

    // ── income categories ────────────────────────────────────────────────────

    @Test
    fun `зарплата returns work icon`() {
        val (icon, _) = suggestCategoryStyle("зарплата", TransactionType.INCOME)
        assertEquals("work", icon)
    }

    @Test
    fun `фриланс returns work icon`() {
        val (icon, _) = suggestCategoryStyle("фриланс", TransactionType.INCOME)
        assertEquals("work", icon)
    }

    @Test
    fun `інвестиції returns money icon`() {
        val (icon, _) = suggestCategoryStyle("інвестиції", TransactionType.INCOME)
        assertEquals("money", icon)
    }

    // ── fallback colors ──────────────────────────────────────────────────────

    @Test
    fun `unknown name with INCOME returns green fallback color`() {
        val (icon, color) = suggestCategoryStyle("xyzunknown12345", TransactionType.INCOME)
        assertEquals("category", icon)
        assertEquals("#4CAF50", color)
    }

    @Test
    fun `unknown name with EXPENSE returns grey fallback color`() {
        val (icon, color) = suggestCategoryStyle("xyzunknown12345", TransactionType.EXPENSE)
        assertEquals("category", icon)
        assertEquals("#78909C", color)
    }

    // ── case insensitivity ───────────────────────────────────────────────────

    @Test
    fun `keyword matching is case insensitive`() {
        val (iconLower, _) = suggestCategoryStyle("продукти", TransactionType.EXPENSE)
        val (iconUpper, _) = suggestCategoryStyle("ПРОДУКТИ", TransactionType.EXPENSE)
        val (iconMixed, _) = suggestCategoryStyle("Продукти", TransactionType.EXPENSE)
        assertEquals(iconLower, iconUpper)
        assertEquals(iconLower, iconMixed)
    }

    // ── specific color checks ────────────────────────────────────────────────

    @Test
    fun `транспорт returns orange color`() {
        val (_, color) = suggestCategoryStyle("транспорт", TransactionType.EXPENSE)
        assertEquals("#FFA834", color)
    }

    @Test
    fun `здоров'я returns green color`() {
        val (_, color) = suggestCategoryStyle("здоров'я", TransactionType.EXPENSE)
        assertEquals("#48B456", color)
    }

    @Test
    fun `кафе returns brown color`() {
        val (_, color) = suggestCategoryStyle("кафе", TransactionType.EXPENSE)
        assertEquals("#7B5947", color)
    }

    @Test
    fun `дозвілля returns pink color`() {
        val (_, color) = suggestCategoryStyle("дозвілля", TransactionType.EXPENSE)
        assertEquals("#F73579", color)
    }

    // ── trim whitespace ──────────────────────────────────────────────────────

    @Test
    fun `name with leading and trailing spaces is matched correctly`() {
        val (icon, _) = suggestCategoryStyle("  транспорт  ", TransactionType.EXPENSE)
        assertEquals("car", icon)
    }
}
