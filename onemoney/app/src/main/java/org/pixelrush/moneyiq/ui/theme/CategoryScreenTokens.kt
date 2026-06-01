package org.syalosovetskyi.onemoney.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import org.syalosovetskyi.onemoney.data.db.entities.TransactionType

@Immutable
data class CategoryVisualStyle(val circleBg: Color, val iconTint: Color)

object CategoryScreenTokens {

    val byName: Map<String, CategoryVisualStyle> = mapOf(
        "Продукти"   to CategoryVisualStyle(Color(0xFFDCE3ED), Color(0xFF49B7F5)),
        "Ресторація" to CategoryVisualStyle(Color(0xFFE4DDF0), Color(0xFF4E63E0)),
        "Дозвілля"   to CategoryVisualStyle(Color(0xFFF1DDE5), Color(0xFFFF5A8A)),
        "Транспорт"  to CategoryVisualStyle(Color(0xFFEADECE), Color(0xFFFFB13B)),
        "Здоров'я"   to CategoryVisualStyle(Color(0xFFE0E5E0), Color(0xFF58B957)),
        "Сім'я"      to CategoryVisualStyle(Color(0xFFE4DDF0), Color(0xFF7E55FF)),
        "Подарунки"  to CategoryVisualStyle(Color(0xFFF3DDDD), Color(0xFFFF5959)),
        "Покупки"    to CategoryVisualStyle(Color(0xFFE7E1DD), Color(0xFF805C4F)),
        "Робота"     to CategoryVisualStyle(Color(0xFFDCE3ED), Color(0xFF1976D2)),
    )

    fun resolve(
        name:      String,
        type:      TransactionType,
        fallback:  Color,
        hasBudget: Boolean,
    ): CategoryVisualStyle {
        byName[name]?.let { return it }
        val alpha = if (hasBudget) 0.22f else 0.10f
        return CategoryVisualStyle(circleBg = fallback.copy(alpha = alpha), iconTint = fallback)
    }
}
