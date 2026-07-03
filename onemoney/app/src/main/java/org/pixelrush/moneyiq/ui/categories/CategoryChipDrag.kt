package org.syalosovetskyi.onemoney.ui.categories

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import org.syalosovetskyi.onemoney.data.db.entities.CategoryEntity

/**
 * Стан-машина drag-to-swap для сітки категорій. Тримає екранні центри чіпів,
 * поточний перетягуваний та цільовий id і зсув пальця; віддає модифікатор для
 * кожного чіпа (напівпрозорість + вимірювання позиції + жест перетягування).
 *
 * Раніше цей стан і фабрика модифікатора були вбудовані прямо в God-composable
 * CategoriesGridContent (~60 рядків pointerInput). Логіка перенесена сюди 1:1.
 * Ghost-накладка (чіп, що летить за пальцем) лишилася в CategoriesGridContent,
 * бо залежить від локальних розмірів layout, і лише читає стан звідси.
 */
class ChipDragState internal constructor(
    private val onSwap: (Long, Long) -> Unit,
) {
    /** Екранні центри чіпів (root-координати) за id категорії. */
    val chipCenters = mutableStateMapOf<Long, Offset>()

    /** id чіпа, який зараз тягнуть (null — нічого). */
    var draggingId  by mutableStateOf<Long?>(null); private set
    /** id чіпа-цілі під пальцем. */
    var hoverTarget by mutableStateOf<Long?>(null); private set
    /** накопичений зсув пальця від початку перетягування. */
    var dragOffset  by mutableStateOf(Offset.Zero); private set
    /** root-позиція контейнера сітки (для перерахунку в локальні координати ghost). */
    var containerRootPos by mutableStateOf(Offset.Zero)

    /** Модифікатор для чіпа [cat]: прозорість за станом + вимірювання центру +
     *  жест drag-after-long-press. Для null повертає порожній Modifier. */
    fun chipModifier(cat: CategoryEntity?): Modifier {
        if (cat == null) return Modifier
        val a = when {
            cat.id == draggingId  -> 0f     // невидимий — за пальцем летить ghost
            draggingId == null    -> 1f
            cat.id == hoverTarget -> 1f
            else                  -> 0.70f
        }
        return Modifier
            .alpha(a)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                chipCenters[cat.id] = Offset(
                    pos.x + coords.size.width / 2f,
                    pos.y + coords.size.height / 2f
                )
            }
            .pointerInput(cat.id) {
                var acc = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        draggingId = cat.id; hoverTarget = null
                        acc = Offset.Zero; dragOffset = Offset.Zero
                    },
                    onDrag = { change, d ->
                        change.consume()
                        acc += d
                        dragOffset = acc
                        val center = chipCenters[cat.id]
                            ?: return@detectDragGesturesAfterLongPress
                        val finger = center + acc
                        hoverTarget = chipCenters.entries
                            .filter { (id, _) -> id != cat.id }
                            .minByOrNull { (_, c) -> (c - finger).getDistance() }
                            ?.key
                    },
                    onDragEnd = {
                        hoverTarget?.let { onSwap(cat.id, it) }
                        draggingId = null; hoverTarget = null
                        acc = Offset.Zero; dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        draggingId = null; hoverTarget = null
                        acc = Offset.Zero; dragOffset = Offset.Zero
                    }
                )
            }
    }
}

/** Створює [ChipDragState], доки [onSwap] != null; інакше null (drag вимкнено). */
@Composable
fun rememberChipDragState(onSwap: ((Long, Long) -> Unit)?): ChipDragState? =
    if (onSwap == null) null else remember(onSwap) { ChipDragState(onSwap) }
