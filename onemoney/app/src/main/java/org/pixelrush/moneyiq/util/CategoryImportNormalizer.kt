package org.syalosovetskyi.onemoney.util

import org.syalosovetskyi.onemoney.data.db.entities.CategoryEntity

fun normalizeImportedCategory(cat: CategoryEntity): CategoryEntity {
    val n = cat.name.lowercase().trim()
    if (cat.parentId != null) {
        when {
            n.contains("food delivery") || n == "glovo" || n.contains("bolt food") || n.contains("uber eats") || n.contains("uklon food") ->
                return cat.copy(icon = "delivery", colorHex = "#FF6F00")
            n.contains("кафе") || n.contains("cafe") || n.contains("кав'ярн") ->
                return cat.copy(icon = "coffee", colorHex = "#795548")
            n.contains("ресторан") && n != "ресторація" ->
                return cat.copy(icon = "restaurant", colorHex = "#E53935")
        }
    }

    when {
        n == "спорт" && cat.icon in listOf("health", "doctor") ->
            return cat.copy(icon = "sports", colorHex = "#F44336")
        n.contains("здоров") && cat.parentId == null && cat.icon in listOf("health", "doctor") ->
            return cat.copy(icon = "volunteer", colorHex = "#48B456")
        cat.icon == "health" && cat.parentId == null ->
            return cat.copy(icon = "volunteer", colorHex = "#48B456")
    }

    if (cat.icon in setOf("category", "family")) {
        val (suggested, color) = suggestCategoryStyle(cat.name, cat.type)
        if (suggested != "category" && suggested != cat.icon) {
            return cat.copy(icon = suggested, colorHex = color)
        }
    }

    return when (cat.icon) {
        "movie" -> if (cat.colorHex != "#9C27B0") cat.copy(colorHex = "#9C27B0") else cat
        "coffee" -> if (cat.colorHex != "#795548") cat.copy(colorHex = "#795548") else cat
        "sports" -> if (cat.colorHex != "#F44336") cat.copy(colorHex = "#F44336") else cat
        else -> cat
    }
}
