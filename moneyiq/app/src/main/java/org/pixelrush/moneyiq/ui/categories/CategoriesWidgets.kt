package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.main.formatMoney
import org.pixelrush.moneyiq.util.suggestCategoryStyle
// ── Чип категорії ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategoryChip(
    category:       CategoryEntity,
    spending:       Double,
    onClick:        () -> Unit,
    childCount:     Int     = 0,
    onLongPress:    () -> Unit = {},
    onDoubleClick:  () -> Unit = {},
    showChildBadge: Boolean = false,
    groupColorHex:  String? = null,
    isCompact:      Boolean = false,
    isExpanded:     Boolean = false
) {
    val chipW      = if (isCompact) CHIP_WIDTH_COMPACT   else CHIP_WIDTH
    val chipH      = if (isCompact) CHIP_HEIGHT_COMPACT  else CHIP_HEIGHT
    val circleSize = if (isCompact) CHIP_CIRCLE_COMPACT  else CHIP_CIRCLE_SIZE
    val iconSize   = if (isCompact) 22.dp  else 26.dp
    val titleSize  = if (isCompact) 12.sp  else 13.sp
    val moneySize  = if (isCompact) 10.sp  else 11.sp
    val spendSize  = if (isCompact) 12.sp  else 13.sp
    val hasBudget  = category.budgetAmount > 0.0
    val remainingBudget = category.budgetAmount - spending
    val overBudget = hasBudget && remainingBudget < 0.0
    val fillFraction = when {
        hasBudget -> (spending / category.budgetAmount).toFloat().coerceIn(0f, 1f)
        spending > 0.0 -> 1f
        else -> 0f
    }

    val color = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val groupBg = remember(groupColorHex) {
        groupColorHex?.let {
            try { Color(android.graphics.Color.parseColor(it)).copy(alpha = 0.13f) }
            catch (_: Exception) { null }
        }
    }

    Column(
        modifier = Modifier
            .size(width = chipW, height = chipH)
            .let { m ->
                when {
                    isExpanded  -> m.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f))
                    groupBg != null -> m.clip(RoundedCornerShape(12.dp)).background(groupBg)
                    else -> m
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongPress, onDoubleClick = onDoubleClick)
            .padding(vertical = 2.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Назва
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = if (isCompact) 24.dp else 28.dp,
                    max = if (isCompact) 34.dp else 40.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                category.name,
                style      = MaterialTheme.typography.labelSmall.copy(
                    fontSize   = titleSize,
                    lineHeight = if (isCompact) 14.sp else 16.sp
                ),
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                softWrap   = false,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.fillMaxWidth()
            )
        }
        // 2. Залишок бюджету, перевитрата або 0 для категорій без бюджету
        if (hasBudget) {
            val budgetText = formatBudgetAmount(kotlin.math.abs(remainingBudget)) + " ₴"
            Box(
                modifier = Modifier
                    .height(if (isCompact) 15.dp else 17.dp)
                    .then(
                        if (overBudget) {
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(color)
                                .padding(horizontal = if (isCompact) 6.dp else 8.dp)
                        } else {
                            Modifier.padding(horizontal = if (isCompact) 6.dp else 8.dp)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    budgetText,
                    style      = MaterialTheme.typography.labelSmall.copy(
                        fontSize   = moneySize,
                        lineHeight = if (isCompact) 12.sp else 13.sp
                    ),
                    fontWeight = if (overBudget) FontWeight.Bold else FontWeight.SemiBold,
                    color      = if (overBudget) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    textAlign  = TextAlign.Center
                )
            }
        } else {
            Text(
                "0 ₴",
                style      = MaterialTheme.typography.labelSmall.copy(
                    fontSize   = moneySize,
                    lineHeight = if (isCompact) 12.sp else 13.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(if (isCompact) 1.dp else 2.dp))
        // 3. Іконка — outer Box рисує кільце expansion поза кліпом внутрішнього кола
        val iconKey = remember(category.icon, category.name) {
            if (category.icon == "category")
                suggestCategoryStyle(category.name, category.type).first
            else
                category.icon
        }
        val hasSpending = spending > 0.0
        Box(
            modifier = Modifier
                .size(circleSize)
                .then(
                    if (isExpanded) Modifier.drawBehind {
                        drawCircle(
                            color  = color.copy(alpha = 0.45f),
                            radius = size.minDimension / 2f + 4.dp.toPx(),
                            style  = Stroke(width = 2.5.dp.toPx())
                        )
                    } else Modifier
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (hasBudget) 0.28f else 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                if (fillFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(fillFraction)
                            .background(color)
                    )
                }
                Icon(
                    categoryIconFor(iconKey), null,
                    tint     = if (hasSpending || hasBudget) Color.White else color,
                    modifier = Modifier.size(iconSize)
                )
            }
            if (showChildBadge && childCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (isCompact) 16.dp else 18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+$childCount",
                        style    = MaterialTheme.typography.labelSmall.copy(fontSize = if (isCompact) 7.sp else 8.sp),
                        color    = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.height(if (isCompact) 1.dp else 2.dp))
        // 4. Витрачено
        Text(
            formatMoney(spending) + " ₴",
            style      = MaterialTheme.typography.labelSmall.copy(
                fontSize   = spendSize,
                lineHeight = if (isCompact) 14.sp else 15.sp
            ),
            fontWeight = FontWeight.Bold,
            color      = if (spending > 0.0) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
    }
}



// ── Інлайн-панель підкатегорій (в рядку доната) ──────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SideSubcategoryPanel(
    parent:           CategoryEntity,
    children:         List<CategoryEntity>,
    spending:         Map<Long, Double>,
    onClickChild:     (CategoryEntity) -> Unit,
    onLongClickChild: (CategoryEntity) -> Unit = {},
    modifier:         Modifier = Modifier
) {
    val parentColor = remember(parent.colorHex) {
        try { Color(android.graphics.Color.parseColor(parent.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val sortedKids = children
        .filter { (spending[it.id] ?: 0.0) > 0.0 }
        .sortedByDescending { spending[it.id] ?: 0.0 }

    Card(
        modifier  = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = parentColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            sortedKids.forEach { child ->
                val childColor = remember(child.colorHex) {
                    try { Color(android.graphics.Color.parseColor(child.colorHex)) }
                    catch (_: Exception) { Color(0xFFFF5722) }
                }
                val childIconKey = if (child.icon == "category")
                    suggestCategoryStyle(child.name, child.type).first else child.icon
                val childSpend = spending[child.id] ?: 0.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick     = { onClickChild(child) },
                            onLongClick = { onLongClickChild(child) }
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (childSpend > 0) childColor else childColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            categoryIconFor(childIconKey), null,
                            tint     = if (childSpend > 0) Color.White else childColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            child.name,
                            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatMoney(childSpend) + " ₴",
                            style      = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.SemiBold,
                            color      = if (childSpend > 0) childColor
                                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            maxLines   = 1
                        )
                    }
                }
            }
        }
    }
}

// ── Полоса підкатегорій ───────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExpandedCategoryStrip(
    parent:           CategoryEntity,
    children:         List<CategoryEntity>,
    spending:         Map<Long, Double>,
    onClickParent:    () -> Unit,
    onClickChild:     (CategoryEntity) -> Unit,
    onLongClickChild: (CategoryEntity) -> Unit = {}
) {
    val parentColor = remember(parent.colorHex) {
        try { Color(android.graphics.Color.parseColor(parent.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }
    val sortedKids = children
        .filter { (spending[it.id] ?: 0.0) > 0.0 }
        .sortedByDescending { spending[it.id] ?: 0.0 }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = parentColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            sortedKids.take(4).forEach { child ->
                val childColor = remember(child.colorHex) {
                    try { Color(android.graphics.Color.parseColor(child.colorHex)) }
                    catch (_: Exception) { Color(0xFFFF5722) }
                }
                val childIconKey = if (child.icon == "category")
                    suggestCategoryStyle(child.name, child.type).first else child.icon
                val childSpend = spending[child.id] ?: 0.0

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick     = { onClickChild(child) },
                            onLongClick = { onLongClickChild(child) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (childSpend > 0) childColor else childColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            categoryIconFor(childIconKey), null,
                            tint     = if (childSpend > 0) Color.White else childColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        child.name,
                        style     = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                    Text(
                        formatMoney(childSpend) + " ₴",
                        style      = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.SemiBold,
                        color      = if (childSpend > 0) childColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        maxLines   = 1,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── Чип «Додати» ─────────────────────────────────────────────────────────────

@Composable
internal fun AddCategoryChip(onClick: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .size(width = CHIP_WIDTH, height = CHIP_HEIGHT)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        Text("", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp))
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(CHIP_CIRCLE_SIZE)
                .clip(CircleShape)
                .dashedCircleBorder(color = dashColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add, null,
                tint     = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            "Додати",
            style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines  = 1,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

private fun formatBudgetAmount(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.getDefault())
    nf.maximumFractionDigits = 0
    nf.minimumFractionDigits = 0
    return nf.format(amount)
}

// ── Donut-чарт ───────────────────────────────────────────────────────────────

@Composable
internal fun DonutChart(
    categories:   List<CategoryEntity>,
    spending:     Map<Long, Double>,
    totalExpense: Double,
    totalIncome:  Double,
    selectedTab:  Int,
    onToggle:     () -> Unit,
    modifier:     Modifier = Modifier
) {
    val emptyColor   = MaterialTheme.colorScheme.surfaceVariant
    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor  = Color(0xFF26A69A)

    val tabType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME
    val activeSpending = categories
        .filter { it.type == tabType && !it.archived }
        .mapNotNull { cat -> (spending[cat.id] ?: 0.0).takeIf { it > 0.0 }?.let { cat to it } }
        .sortedByDescending { it.second }

    val tabTotal = activeSpending.sumOf { it.second }

    val categoryColors = activeSpending.map { (cat, _) ->
        try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            val sw     = minDim * 0.09f
            val inset  = sw / 2f
            val arcDim = minDim - sw
            val arcSz  = Size(arcDim, arcDim)
            val tl     = Offset(
                x = (size.width  - minDim) / 2f + inset,
                y = (size.height - minDim) / 2f + inset
            )

            if (tabTotal == 0.0) {
                drawArc(
                    color      = emptyColor,
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter  = false, topLeft = tl, size = arcSz,
                    style      = Stroke(width = sw)
                )
            } else {
                var startAngle = -90f
                activeSpending.forEachIndexed { idx, (_, amount) ->
                    val sweep = (amount / tabTotal * 360.0).toFloat()
                    drawArc(
                        color      = categoryColors[idx],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter  = false, topLeft = tl, size = arcSz,
                        style      = Stroke(width = sw, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(20.dp)
                .clickable(onClick = onToggle)
        ) {
            Text(
                if (selectedTab == 0) "Витрати" else "Доходи",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                formatMoney(totalExpense),
                style      = MaterialTheme.typography.titleSmall.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold,
                color      = expenseColor,
                maxLines   = 1
            )
            Text(
                formatMoney(totalIncome),
                style      = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Medium,
                color      = incomeColor,
                maxLines   = 1
            )
            Icon(
                if (selectedTab == 0) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = "Переключити",
                modifier = Modifier.size(14.dp),
                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

// ── Пунктирна кругла рамка ────────────────────────────────────────────────────

internal fun Modifier.dashedCircleBorder(
    color:       Color,
    dashWidth:   Dp = 8.dp,
    dashGap:     Dp = 5.dp,
    strokeWidth: Dp = 1.5.dp
): Modifier = this.drawBehind {
    val sw = strokeWidth.toPx()
    drawCircle(
        color  = color,
        radius = (size.minDimension - sw) / 2f,
        style  = Stroke(
            width      = sw,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashWidth.toPx(), dashGap.toPx()), 0f
            )
        )
    )
}
