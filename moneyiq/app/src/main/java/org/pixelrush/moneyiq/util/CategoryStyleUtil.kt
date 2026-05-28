package org.pixelrush.moneyiq.util

import org.pixelrush.moneyiq.data.db.entities.TransactionType

/**
 * Returns (iconKey, colorHex) for a category by matching name keywords.
 * Icon keys correspond to CATEGORY_ICONS_LIST in CategorySheets.kt.
 * Colors are from CATEGORY_FORM_COLORS.
 */
fun suggestCategoryStyle(name: String, type: TransactionType): Pair<String, String> {
    val n = name.lowercase().trim()

    val rules: List<Pair<String, List<String>>> = listOf(
        "restaurant" to listOf("еда", "ресторан", "кафе", "обед", "ужин", "завтрак",
                               "food", "cafe", "питан", "харч", "кухн"),
        "shopping"   to listOf("покупки", "одежда", "одяг", "магазин", "продукт",
                               "clothes", "market", "shopping", "взуття", "ринок"),
        "car"        to listOf("транспорт", "авто", "машин", "автомоб",
                               "car", "taxi", "таксі", "автобус", "метро"),
        "home"       to listOf("дом", "жильё", "жилл", "квартир", "аренд",
                               "home", "rent", "house", "комунал", "ремонт", "будинок"),
        "work"       to listOf("работа", "зарплат", "офис", "бизнес", "фриланс",
                               "work", "salary", "бізнес", "заробіт", "доход від"),
        "school"     to listOf("образован", "школ", "учёб", "учеб", "навчан",
                               "курс", "study", "education", "університет"),
        "health"     to listOf("здоровь", "аптек", "врач", "медицин",
                               "health", "doctor", "ліки", "лікар", "клінік"),
        "flight"     to listOf("путешеств", "перелёт", "перельот", "відпочин",
                               "flight", "travel", "туризм", "відпустк", "отпуск"),
        "music"      to listOf("музык", "музик", "розваг", "кино", "кіно",
                               "развлечен", "music", "entertainment", "концерт", "театр"),
        "money"      to listOf("деньги", "гроші", "финанс", "інвестиц", "инвестиц",
                               "банк", "money", "invest", "вклад", "криптo", "crypto"),
        "coffee"     to listOf("кофе", "кава", "coffee", "tea", "чай",
                               "снэк", "снек", "snack"),
        "pets"       to listOf("животн", "питомец", "тварин", "домашн",
                               "pet", "кошк", "собак", "кіт"),
        "gift"       to listOf("подарок", "подарун", "gift", "present",
                               "свят", "birthday", "день народ"),
        "phone"      to listOf("телефон", "связь", "зв'язок", "інтернет", "интернет",
                               "мобільн", "phone", "mobile", "інтерн"),
        "sports"     to listOf("спорт", "фитнес", "фітнес", "спортзал",
                               "gym", "sport", "тренув", "йога", "yoga")
    )

    val iconColorMap = mapOf(
        "restaurant" to "#FF5722",
        "shopping"   to "#4CAF50",
        "car"        to "#2196F3",
        "home"       to "#9C27B0",
        "work"       to "#4CAF50",
        "school"     to "#FF9800",
        "health"     to "#E91E63",
        "flight"     to "#03A9F4",
        "music"      to "#673AB7",
        "money"      to "#FFEB3B",
        "coffee"     to "#FF9800",
        "pets"       to "#FF5722",
        "gift"       to "#E91E63",
        "phone"      to "#3F51B5",
        "sports"     to "#F44336"
    )

    for ((icon, keywords) in rules) {
        if (keywords.any { n.contains(it) }) {
            return icon to (iconColorMap[icon] ?: "#FF5722")
        }
    }

    val fallbackColor = if (type == TransactionType.INCOME) "#4CAF50" else "#FF5722"
    return "category" to fallbackColor
}
