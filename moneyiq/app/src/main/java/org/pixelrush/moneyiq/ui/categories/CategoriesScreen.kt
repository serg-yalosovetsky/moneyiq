package org.pixelrush.moneyiq.ui.categories

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import org.pixelrush.moneyiq.data.db.entities.CategoryEntity
import org.pixelrush.moneyiq.data.db.entities.TransactionType
import org.pixelrush.moneyiq.ui.main.formatMoney

// ── Helpers ───────────────────────────────────────────────────────────────────

private val MONTH_NAMES_RU = arrayOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

private val CHIP_WIDTH          = 76.dp
private val CHIP_CIRCLE_SIZE    = 52.dp
private val DONUT_SECTION_HEIGHT = 210.dp

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit = {},
    embeddedMode:   Boolean   = false,
    padding:        PaddingValues = PaddingValues(),
    viewModel:      CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab  by remember { mutableIntStateOf(0) }
    var showDialog   by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val typeDefault = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
    ) {
        // ── Шапка ──────────────────────────────────────────────────────────
        CategoriesTopBar(
            totalExpense = state.totalExpense,
            totalIncome  = state.totalIncome,
            onAddClick   = { showDialog = true }
        )

        // ── Навигация месяцев ───────────────────────────────────────────
        MonthNavRow(
            sel    = state.selectedMonth,
            onPrev = viewModel::prevMonth,
            onNext = viewModel::nextMonth
        )

        // ── Вкладки ─────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = MaterialTheme.colorScheme.surface,
            contentColor     = MaterialTheme.colorScheme.primary,
            divider = {
                HorizontalDivider(
                    thickness = 1.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text(
                    "Расходы",
                    modifier   = Modifier.padding(vertical = 12.dp),
                    fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text(
                    "Доходы",
                    modifier   = Modifier.padding(vertical = 12.dp),
                    fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        // ── Сетка с категориями ─────────────────────────────────────────
        val categories = if (selectedTab == 0) state.expenseCategories else state.incomeCategories
        val spending   = if (selectedTab == 0) state.monthSpending     else state.monthIncome

        CategoriesGridContent(
            categories    = categories,
            spending      = spending,
            totalExpense  = state.totalExpense,
            totalIncome   = state.totalIncome,
            bottomPadding = padding.calculateBottomPadding(),
            onAdd         = { showDialog = true },
            onEdit        = { editCategory = it }
        )
    }

    // ── Диалог создания / редактирования ─────────────────────────────────────
    if (showDialog || editCategory != null) {
        CategoryEditDialog(
            existing    = editCategory,
            defaultType = typeDefault,
            onSave = { name, type, color, budget ->
                if (editCategory != null) {
                    viewModel.update(
                        editCategory!!.copy(
                            name = name, type = type,
                            colorHex = color, budgetAmount = budget
                        )
                    )
                } else {
                    viewModel.add(name, type, color, budget)
                }
                showDialog   = false
                editCategory = null
            },
            onDelete = {
                editCategory?.let { viewModel.delete(it) }
                editCategory = null
            },
            onDismiss = { showDialog = false; editCategory = null }
        )
    }
}

// ── Шапка ────────────────────────────────────────────────────────────────────

@Composable
private fun CategoriesTopBar(
    totalExpense: Double,
    totalIncome:  Double,
    onAddClick:   () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Левый аватар-иконка
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Category, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Центр: «Категории» + суммы
        Column(
            modifier              = Modifier.weight(1f),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(
                "Категории",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            val subtitle = when {
                totalExpense > 0.0 || totalIncome > 0.0 ->
                    "${formatMoney(totalExpense)} / +${formatMoney(totalIncome)}"
                else -> "Нет операций за период"
            }
            Text(
                subtitle,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        // Кнопка «+»
        IconButton(
            onClick  = onAddClick,
            modifier = Modifier.size(44.dp).clip(CircleShape)
        ) {
            Icon(
                Icons.Default.Add, "Добавить категорию",
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── Навигатор месяцев ─────────────────────────────────────────────────────────

@Composable
private fun MonthNavRow(
    sel:    SelectedMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                Icons.Default.ChevronLeft, "Пред. месяц",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            "${MONTH_NAMES_RU[sel.month]} ${sel.year}",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNext) {
            Icon(
                Icons.Default.ChevronRight, "След. месяц",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Сетка категорий + донат-чарт ─────────────────────────────────────────────

@Composable
private fun CategoriesGridContent(
    categories:    List<CategoryEntity>,
    spending:      Map<Long, Double>,
    totalExpense:  Double,
    totalIncome:   Double,
    bottomPadding: Dp,
    onAdd:  () -> Unit,
    onEdit: (CategoryEntity) -> Unit
) {
    // Разбиваем по позициям:
    // Строка 1 (top):  кат 0..3
    // Строка 2 (mid):  кат 4..5 слева | донат | кат 6..7 справа
    // Строка 3 (bot):  кнопка «+» + кат 8..10
    // Строки  4+ (ext): кат 11+, по 4 в строке

    val topRow   = categories.take(4)
    val midLeft  = categories.drop(4).take(2)
    val midRight = categories.drop(6).take(2)
    val botFirst = categories.drop(8).take(3)
    val extCats  = categories.drop(11)

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 16.dp)
    ) {

        // ── Пустое состояние ────────────────────────────────────────────
        if (categories.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Category, null,
                            modifier = Modifier.size(56.dp),
                            tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Нет категорий",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Нажмите + чтобы добавить",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // ── Верхняя строка: 4 чипа ──────────────────────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0 until 4) {
                    val cat = topRow.getOrNull(i)
                    if (cat != null) {
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onEdit(cat) }
                        )
                    } else {
                        Spacer(Modifier.width(CHIP_WIDTH))
                    }
                }
            }
        }

        // ── Средняя строка: [левые кат] | [донат] | [правые кат] ───────
        item {
            Row(
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(DONUT_SECTION_HEIGHT)
                    .padding(vertical = 8.dp),
                verticalAlignment  = Alignment.CenterVertically
            ) {
                // Левая колонка
                Column(
                    modifier              = Modifier.width(CHIP_WIDTH + 4.dp).fillMaxHeight(),
                    verticalArrangement   = Arrangement.SpaceEvenly,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    midLeft.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onEdit(cat) }
                        )
                    }
                }

                // Донат-чарт
                DonutChart(
                    totalExpense = totalExpense,
                    totalIncome  = totalIncome,
                    modifier     = Modifier.weight(1f).fillMaxHeight().padding(8.dp)
                )

                // Правая колонка
                Column(
                    modifier              = Modifier.width(CHIP_WIDTH + 4.dp).fillMaxHeight(),
                    verticalArrangement   = Arrangement.SpaceEvenly,
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    midRight.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onEdit(cat) }
                        )
                    }
                }
            }
        }

        // ── Нижняя строка: «+» + след. 3 категории ─────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AddCategoryChip(onClick = onAdd)
                for (i in 0 until 3) {
                    val cat = botFirst.getOrNull(i)
                    if (cat != null) {
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onEdit(cat) }
                        )
                    } else {
                        Spacer(Modifier.width(CHIP_WIDTH))
                    }
                }
            }
        }

        // ── Дополнительные строки по 4 чипа ────────────────────────────
        if (extCats.isNotEmpty()) {
            items(extCats.chunked(4)) { rowCats ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowCats.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            spending = spending[cat.id] ?: 0.0,
                            onClick  = { onEdit(cat) }
                        )
                    }
                    repeat(4 - rowCats.size) { Spacer(Modifier.width(CHIP_WIDTH)) }
                }
            }
        }
    }
}

// ── Чип категории (круглая иконка + название + сумма) ────────────────────────

@Composable
private fun CategoryChip(
    category: CategoryEntity,
    spending: Double,
    onClick:  () -> Unit
) {
    val color = remember(category.colorHex) {
        try { Color(android.graphics.Color.parseColor(category.colorHex)) }
        catch (_: Exception) { Color(0xFFFF5722) }
    }

    Column(
        modifier = Modifier
            .width(CHIP_WIDTH)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(CHIP_CIRCLE_SIZE)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Category, null,
                tint     = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            category.name,
            style     = MaterialTheme.typography.labelSmall,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurface,
            modifier  = Modifier.fillMaxWidth()
        )
        if (spending > 0.0) {
            Text(
                formatMoney(spending),
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color      = color,
                maxLines   = 1,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Чип «Добавить» ───────────────────────────────────────────────────────────

@Composable
private fun AddCategoryChip(onClick: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .width(CHIP_WIDTH)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Добавить",
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            maxLines  = 1,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ── Донат-чарт ───────────────────────────────────────────────────────────────

@Composable
private fun DonutChart(
    totalExpense: Double,
    totalIncome:  Double,
    modifier:     Modifier = Modifier
) {
    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor  = Color(0xFF26A69A)          // teal
    val emptyColor   = MaterialTheme.colorScheme.surfaceVariant

    val total    = totalExpense + totalIncome
    val expAngle = if (total > 0.0) (totalExpense / total * 360.0).toFloat() else 0f
    val incAngle = if (total > 0.0) (totalIncome  / total * 360.0).toFloat() else 0f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw  = size.minDimension * 0.15f
            val inset = sw / 2f
            val arcSz = Size(size.width - sw, size.height - sw)
            val tl    = Offset(inset, inset)

            if (total == 0.0) {
                drawArc(
                    color      = emptyColor,
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter  = false, topLeft = tl, size = arcSz,
                    style      = Stroke(width = sw)
                )
            } else {
                if (expAngle > 0f) {
                    drawArc(
                        color      = expenseColor,
                        startAngle = -90f, sweepAngle = expAngle,
                        useCenter  = false, topLeft = tl, size = arcSz,
                        style      = Stroke(width = sw, cap = StrokeCap.Butt)
                    )
                }
                if (incAngle > 0f) {
                    drawArc(
                        color      = incomeColor,
                        startAngle = -90f + expAngle, sweepAngle = incAngle,
                        useCenter  = false, topLeft = tl, size = arcSz,
                        style      = Stroke(width = sw, cap = StrokeCap.Butt)
                    )
                }
            }
        }

        // Текст по центру
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(22.dp)
        ) {
            Text(
                "Расходы",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                formatMoney(totalExpense),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = expenseColor,
                maxLines   = 1
            )
            if (totalIncome > 0.0) {
                HorizontalDivider(
                    modifier  = Modifier.padding(vertical = 2.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Text(
                    "+${formatMoney(totalIncome)}",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = incomeColor,
                    maxLines = 1
                )
            }
        }
    }
}

// ── Диалог добавления / редактирования ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditDialog(
    existing:    CategoryEntity?,
    defaultType: TransactionType,
    onSave:   (String, TransactionType, String, Double) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name     by remember { mutableStateOf(existing?.name ?: "") }
    var type     by remember { mutableStateOf(existing?.type ?: defaultType) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: "#FF5722") }
    var budget   by remember {
        mutableStateOf(
            existing?.budgetAmount?.let { if (it > 0) it.toString() else "" } ?: ""
        )
    }

    val colorPalette = listOf(
        "#FF5722", "#F44336", "#E91E63", "#9C27B0",
        "#673AB7", "#3F51B5", "#2196F3", "#03A9F4",
        "#009688", "#4CAF50", "#FFEB3B", "#FF9800"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing != null) "Редактировать категорию" else "Новая категория")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Название") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Outlined.Category, null) }
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        TransactionType.EXPENSE to "Расход",
                        TransactionType.INCOME  to "Доход"
                    ).forEachIndexed { i, (t, label) ->
                        SegmentedButton(
                            selected = type == t,
                            onClick  = { type = t },
                            shape    = SegmentedButtonDefaults.itemShape(i, 2),
                            label    = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value         = budget,
                    onValueChange = { budget = it },
                    label         = { Text("Лимит в месяц (0 = без лимита)") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Outlined.AttachMoney, null) }
                )

                Text(
                    "Цвет",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorPalette) { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) }
                                catch (_: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex == hex) {
                                Icon(
                                    Icons.Default.Check, null,
                                    modifier = Modifier.size(18.dp),
                                    tint     = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val b = budget.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) onSave(name, type, colorHex, b)
            }) { Text("Сохранить") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (existing != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(); onDismiss() },
                        colors  = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Удалить") }
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}

// ── Пунктирный круговой бордер ────────────────────────────────────────────────

private fun Modifier.dashedCircleBorder(
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
