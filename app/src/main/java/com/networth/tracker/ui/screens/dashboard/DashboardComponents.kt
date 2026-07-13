package com.networth.tracker.ui.screens.dashboard


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Work
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToLong
import com.networth.tracker.data.AssetAddContext
import com.networth.tracker.data.AssetCategory
import com.networth.tracker.data.AssetEntity
import com.networth.tracker.data.BankAccountAddContext
import com.networth.tracker.data.BankAccountEntity
import com.networth.tracker.data.BankAccountType
import com.networth.tracker.data.BankAccountTypeSummary
import com.networth.tracker.data.CategorySummary
import com.networth.tracker.data.ExchangeRateState
import com.networth.tracker.data.NetWorthHistoryEntity
import com.networth.tracker.data.NetWorthSummary
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.EmiResult
import com.networth.tracker.data.EmiYearRow
import com.networth.tracker.data.InvestmentMode
import com.networth.tracker.data.InvestmentProjectionCalculator
import com.networth.tracker.data.InvestmentYearRow
import com.networth.tracker.data.LoanEmiCalculator
import com.networth.tracker.data.PrepaymentEffect
import com.networth.tracker.data.PrepaymentEntry
import com.networth.tracker.data.PrepaymentFrequency
import com.networth.tracker.data.TenureUnit
import com.networth.tracker.data.ReturnCalculator
import com.networth.tracker.data.ReturnMetrics
import com.networth.tracker.ui.theme.AssetPositiveColor
import com.networth.tracker.ui.theme.LiabilityColor
import com.networth.tracker.util.FormatUtils
import com.networth.tracker.viewmodel.DashboardViewModel
import com.networth.tracker.ui.screens.CategoryIcon

@Composable
internal fun GroupBackHeader(
    title: String,
    entryCount: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "$entryCount ${if (entryCount == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    onAdd: () -> Unit,
    addLabel: String,
    showAdd: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (showAdd) {
            TextButton(onClick = onAdd) {
                Text(addLabel)
            }
        }
    }
}

@Composable
internal fun NetWorthHeader(summary: NetWorthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Net Worth",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                FormatUtils.formatCompactInr(summary.netWorthInInr),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (summary.netWorthInInr >= 0) AssetPositiveColor else LiabilityColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                FormatUtils.formatInr(summary.netWorthInInr),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
internal fun TabTotalsRow(
    leftLabel: String,
    leftAmount: Double,
    leftColor: Color,
    rightLabel: String,
    rightAmount: Double,
    rightColor: Color,
    amountColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryChip(
            modifier = Modifier.weight(1f),
            label = leftLabel,
            amount = leftAmount,
            color = leftColor,
            amountColor = amountColor
        )
        SummaryChip(
            modifier = Modifier.weight(1f),
            label = rightLabel,
            amount = rightAmount,
            color = rightColor,
            amountColor = amountColor
        )
    }
}

@Composable
internal fun AssetsLiabilitiesRow(summary: NetWorthSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryChip(
            modifier = Modifier.weight(1f),
            label = "Assets",
            amount = summary.totalAssetsInInr,
            color = AssetPositiveColor
        )
        SummaryChip(
            modifier = Modifier.weight(1f),
            label = "Liabilities",
            amount = summary.totalLiabilitiesInInr,
            color = LiabilityColor
        )
    }
}

@Composable
internal fun AssetsLiabilitiesChart(history: List<NetWorthHistoryEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Assets vs Liabilities",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Pinch or slide to zoom · Double-tap to reset",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendDot(color = AssetPositiveColor, label = "Assets")
                ChartLegendDot(color = LiabilityColor, label = "Liabilities")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (history.isEmpty()) {
                Text(
                    "Add dated assets and liabilities to build this chart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                AssetsLiabilitiesLineChart(
                    history = history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AssetsLiabilitiesLineChart(
    history: List<NetWorthHistoryEntity>,
    modifier: Modifier = Modifier
) {
    val points = remember(history) {
        when {
            history.isEmpty() -> emptyList()
            history.size == 1 -> {
                val single = history.first()
                listOf(
                    single.copy(id = -1, recordedAt = single.recordedAt - 86_400_000L),
                    single
                )
            }
            else -> history
        }
    }
    val fullMinTimeMs = points.first().recordedAt
    val fullTimeSpanMs = (points.last().recordedAt - fullMinTimeMs).coerceAtLeast(1L)

    var scale by remember(points) { mutableFloatStateOf(1f) }
    var panFraction by remember(points) { mutableFloatStateOf(0f) }
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    val clampedScale = scale.coerceIn(1f, 24f)
    val visibleSpanMs = (fullTimeSpanMs / clampedScale).toLong().coerceAtLeast(1L)
    val maxWindowStartOffset = (fullTimeSpanMs - visibleSpanMs).coerceAtLeast(0L)
    val windowStartMs = fullMinTimeMs + (maxWindowStartOffset * panFraction.coerceIn(0f, 1f)).roundToLong()
    val windowEndMs = windowStartMs + visibleSpanMs

    val visibleMaxAmount = remember(points, windowStartMs, windowEndMs) {
        val visible = points.filter { it.recordedAt in windowStartMs..windowEndMs }
        val source = visible.ifEmpty { points }
        maxOf(
            source.maxOf { it.totalAssetsInInr },
            source.maxOf { it.totalLiabilitiesInInr },
            1.0
        )
    }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val markerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val selectedDateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val selectedPoint = selectedIndex?.let { points.getOrNull(it) }?.takeIf { it.id >= 0 }
    val density = LocalDensity.current

    fun resetZoom() {
        scale = 1f
        panFraction = 0f
    }

    Column(modifier = modifier) {
        if (selectedPoint != null) {
            Text(
                selectedDateFormatter.format(Date(selectedPoint.recordedAt)),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )
            Text(
                "Assets ${FormatUtils.formatCompactInr(selectedPoint.totalAssetsInInr)}" +
                    "  ·  Liabilities ${FormatUtils.formatCompactInr(selectedPoint.totalLiabilitiesInInr)}",
                style = MaterialTheme.typography.labelSmall,
                color = axisLabelColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Row(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(1f, 0.5f, 0f).forEach { fraction ->
                    Text(
                        FormatUtils.formatCompactInr(visibleMaxAmount * fraction),
                        style = MaterialTheme.typography.labelSmall,
                        color = axisLabelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(points, fullMinTimeMs, fullTimeSpanMs) {
                        detectTapGestures(
                            onDoubleTap = {
                                resetZoom()
                                selectedIndex = null
                            },
                            onTap = { offset ->
                                val hitSlop = with(density) { 48.dp.toPx() }
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()
                                val localScale = scale.coerceIn(1f, 24f)
                                val localSpan = (fullTimeSpanMs / localScale).toLong().coerceAtLeast(1L)
                                val localMaxStart = (fullTimeSpanMs - localSpan).coerceAtLeast(0L)
                                val localStart = fullMinTimeMs +
                                    (localMaxStart * panFraction.coerceIn(0f, 1f)).roundToLong()

                                fun xFor(time: Long): Float =
                                    ((time - localStart).toFloat() / localSpan.toFloat()) * width

                                val visible = points.filter {
                                    it.recordedAt in localStart..(localStart + localSpan)
                                }
                                val localMax = maxOf(
                                    visible.maxOfOrNull { it.totalAssetsInInr } ?: 0.0,
                                    visible.maxOfOrNull { it.totalLiabilitiesInInr } ?: 0.0,
                                    1.0
                                )

                                var bestIndex: Int? = null
                                var bestScore = Float.MAX_VALUE
                                points.forEachIndexed { index, point ->
                                    if (point.id < 0) return@forEachIndexed
                                    if (point.recordedAt < localStart ||
                                        point.recordedAt > localStart + localSpan
                                    ) {
                                        return@forEachIndexed
                                    }
                                    val x = xFor(point.recordedAt)
                                    val assetY = height -
                                        ((point.totalAssetsInInr / localMax).toFloat()
                                            .coerceIn(0f, 1f) * height)
                                    val liabilityY = height -
                                        ((point.totalLiabilitiesInInr / localMax).toFloat()
                                            .coerceIn(0f, 1f) * height)
                                    val score = minOf(
                                        hypot(offset.x - x, offset.y - assetY),
                                        hypot(offset.x - x, offset.y - liabilityY),
                                        abs(offset.x - x)
                                    )
                                    if (score < bestScore) {
                                        bestScore = score
                                        bestIndex = index
                                    }
                                }
                                selectedIndex = bestIndex?.takeIf { bestScore <= hitSlop }
                            }
                        )
                    }
                    .pointerInput(points, fullMinTimeMs, fullTimeSpanMs) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale.coerceIn(1f, 24f).toDouble()
                            val newScale = (oldScale * zoom).coerceIn(1.0, 24.0)
                            val width = size.width.toFloat().coerceAtLeast(1f).toDouble()
                            val totalSpan = fullTimeSpanMs.toDouble()

                            val oldSpan = totalSpan / oldScale
                            val oldMaxStart = (totalSpan - oldSpan).coerceAtLeast(0.0)
                            val oldStart = fullMinTimeMs + oldMaxStart * panFraction.coerceIn(0f, 1f)
                            val focusRatio = (centroid.x / width.toFloat()).toDouble().coerceIn(0.0, 1.0)
                            val focusTime = oldStart + focusRatio * oldSpan

                            val newSpan = totalSpan / newScale
                            val newMaxStart = (totalSpan - newSpan).coerceAtLeast(0.0)
                            var newStart = focusTime - focusRatio * newSpan
                            // Apply horizontal pan (slide) in visible-time units.
                            newStart -= (pan.x / width) * newSpan
                            newStart = newStart.coerceIn(
                                fullMinTimeMs.toDouble(),
                                fullMinTimeMs + newMaxStart
                            )

                            scale = newScale.toFloat()
                            panFraction = if (newMaxStart <= 0.0) {
                                0f
                            } else {
                                ((newStart - fullMinTimeMs) / newMaxStart).toFloat().coerceIn(0f, 1f)
                            }
                        }
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val span = visibleSpanMs.toFloat().coerceAtLeast(1f)

                fun xFor(time: Long): Float =
                    ((time - windowStartMs).toFloat() / span) * chartWidth

                fun yFor(amount: Double): Float =
                    chartHeight -
                        ((amount / visibleMaxAmount).toFloat().coerceIn(0f, 1f) * chartHeight)

                clipRect {
                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = chartHeight * fraction
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    fun drawSeries(values: List<Pair<Long, Double>>, color: Color) {
                        if (values.isEmpty()) return
                        val path = Path()
                        var started = false
                        values.forEach { (time, amount) ->
                            val x = xFor(time)
                            val y = yFor(amount)
                            if (!started) {
                                path.moveTo(x, y)
                                started = true
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        values.forEachIndexed { index, (time, amount) ->
                            if (time < windowStartMs || time > windowEndMs) return@forEachIndexed
                            val isSelected = index == selectedIndex && points[index].id >= 0
                            drawCircle(
                                color = color,
                                radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                                center = Offset(xFor(time), yFor(amount))
                            )
                        }
                    }

                    selectedPoint?.let { point ->
                        if (point.recordedAt in windowStartMs..windowEndMs) {
                            val x = xFor(point.recordedAt)
                            drawLine(
                                color = markerColor,
                                start = Offset(x, 0f),
                                end = Offset(x, chartHeight),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }

                    drawSeries(
                        points.map { it.recordedAt to it.totalAssetsInInr },
                        AssetPositiveColor
                    )
                    drawSeries(
                        points.map { it.recordedAt to it.totalLiabilitiesInInr },
                        LiabilityColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                dateFormatter.format(Date(windowStartMs)),
                style = MaterialTheme.typography.labelSmall,
                color = axisLabelColor
            )
            if (windowStartMs != windowEndMs) {
                Text(
                    dateFormatter.format(Date(windowEndMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = axisLabelColor
                )
            }
        }
    }
}

@Composable
internal fun SummaryChip(
    modifier: Modifier = Modifier,
    label: String,
    amount: Double,
    color: Color,
    amountColor: Color? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                FormatUtils.formatCompactInr(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun CategoryBreakdownCard(
    summary: CategorySummary,
    totalAssets: Double,
    onClick: () -> Unit
) {
    val fraction = if (summary.category.isLiability || totalAssets <= 0) {
        0f
    } else {
        (summary.totalInInr / totalAssets).toFloat().coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(summary.category)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    summary.category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${summary.entryCount} ${if (summary.entryCount == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (!summary.category.isLiability && fraction > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
            Text(
                FormatUtils.formatCompactInr(summary.totalInInr),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (summary.category.isLiability) LiabilityColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun OutstandingBreakdownCard(
    summary: BankAccountTypeSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BankAccountTypeIcon(summary.accountType)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    summary.accountType.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${summary.entryCount} ${if (summary.entryCount == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                FormatUtils.formatCompactInr(summary.totalInInr),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = LiabilityColor
            )
        }
    }
}

@Composable
internal fun PortfolioReturnCard(metrics: ReturnMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Total Assets Return",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            ReturnMetricsGrid(metrics)
        }
    }
}

@Composable
internal fun ReturnMetricsGrid(metrics: ReturnMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReturnMetricCell(
                modifier = Modifier.weight(1f),
                label = "Invested",
                value = FormatUtils.formatCompactInr(metrics.investedInInr)
            )
            ReturnMetricCell(
                modifier = Modifier.weight(1f),
                label = "Current",
                value = FormatUtils.formatCompactInr(metrics.currentInInr)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReturnMetricCell(
                modifier = Modifier.weight(1f),
                label = "Return",
                value = FormatUtils.formatSignedInr(metrics.returnInInr),
                valueColor = returnColor(metrics.returnInInr, MaterialTheme.colorScheme.onSurface)
            )
            ReturnMetricCell(
                modifier = Modifier.weight(1f),
                label = "Return %",
                value = FormatUtils.formatReturnPercent(metrics.returnPercent),
                valueColor = returnColor(metrics.returnInInr, MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
internal fun ReturnMetricCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

internal fun returnColor(returnInInr: Double, neutral: Color): Color = when {
    returnInInr > 0 -> AssetPositiveColor
    returnInInr < 0 -> LiabilityColor
    else -> neutral
}

@Composable
internal fun ExchangeRateCard(
    state: ExchangeRateState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "USD → INR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "1 USD = ${FormatUtils.formatExchangeRate(state.rate)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val statusText = when {
                    state.isLoading -> "Updating rate…"
                    state.error != null -> "Offline — using cached rate"
                    state.lastUpdated != null -> "Updated ${FormatUtils.formatRelativeTime(state.lastUpdated)}"
                    else -> "Tap refresh to fetch live rate"
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh exchange rate")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BankAccountListItem(
    account: BankAccountEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val amountColor = if (account.accountType.isLiability) LiabilityColor else MaterialTheme.colorScheme.onSurface
    val maskedNumber = FormatUtils.maskAccountNumber(account.accountNumber)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onClick() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                BankAccountTypeIcon(account.accountType, size = 36)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        account.accountName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        account.bankName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        buildString {
                            append(account.accountType.displayName)
                            if (maskedNumber.isNotBlank()) {
                                append(" · ")
                                append(maskedNumber)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        FormatUtils.formatCurrency(account.balance, account.currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColor
                    )
                }
                if (!selectionMode) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            if (account.accountType == BankAccountType.CREDIT_CARD) {
                Spacer(modifier = Modifier.height(8.dp))
                if (account.creditLimit > 0) {
                    LoanDetailRow(
                        label = "Credit Limit",
                        value = FormatUtils.formatCurrency(account.creditLimit, account.currency)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                account.creditUtilisationPercent()?.let { utilisation ->
                    LoanDetailRow(
                        label = "Utilisation",
                        value = FormatUtils.formatPercent(utilisation),
                        valueColor = if (utilisation >= 70) LiabilityColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            EntryNoteText(account.notes)
        }
    }
}

@Composable
internal fun BankAccountTypeIcon(type: BankAccountType, size: Int = 40) {
    val icon = when (type) {
        BankAccountType.SAVINGS -> Icons.Default.Savings
        BankAccountType.CURRENT -> Icons.Default.AccountBalance
        BankAccountType.SALARY -> Icons.Default.Work
        BankAccountType.CREDIT_CARD -> Icons.Default.CreditCard
        BankAccountType.OVERDRAFT -> Icons.Default.AccountBalance
    }
    val bgColor = if (type.isLiability) {
        LiabilityColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val tint = if (type.isLiability) LiabilityColor else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.displayName,
            tint = tint,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AssetListItem(
    asset: AssetEntity,
    usdToInrRate: Double,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val returnMetrics = ReturnCalculator.forAsset(asset, usdToInrRate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onClick() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                CategoryIcon(asset.category, size = 36)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(asset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        asset.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (!selectionMode) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            if (returnMetrics != null) {
                Spacer(modifier = Modifier.height(10.dp))
                ReturnMetricsGrid(returnMetrics)
            } else if (asset.category.isLoan) {
                Spacer(modifier = Modifier.height(8.dp))
                LoanDetailsGrid(asset)
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        FormatUtils.formatCurrency(asset.amount, asset.currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LiabilityColor
                    )
                }
            }
            EntryNoteText(asset.notes)
        }
    }
}

@Composable
internal fun LoanDetailsGrid(asset: AssetEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (asset.investedAmount > 0) {
            LoanDetailRow(
                label = "Original Amount",
                value = FormatUtils.formatCurrency(asset.investedAmount, asset.currency)
            )
        }
        LoanDetailRow(
            label = "Outstanding",
            value = FormatUtils.formatCurrency(asset.amount, asset.currency),
            valueColor = LiabilityColor
        )
        if (asset.interestRate > 0) {
            LoanDetailRow(
                label = "Rate of Interest",
                value = FormatUtils.formatPercent(asset.interestRate)
            )
        }
        if (asset.dateTakenMillis > 0) {
            LoanDetailRow(
                label = "Date Taken",
                value = FormatUtils.formatDate(asset.dateTakenMillis)
            )
        }
    }
}

@Composable
internal fun LoanDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
internal fun EntryNoteText(notes: String) {
    if (notes.isNotBlank()) {
        Text(
            notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
internal fun EmptyBankAccountsCard(
    onAdd: () -> Unit,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
internal fun EmptyStateCard(
    onAdd: (() -> Unit)?,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onAdd != null) Modifier.clickable(onClick = onAdd) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
