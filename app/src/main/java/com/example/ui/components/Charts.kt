package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategory
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel.MonthCashFlow
import java.util.Locale

@Composable
fun CashFlowChart(
    data: List<MonthCashFlow>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            Text("No cash flow data yet", color = TextTertiary, fontSize = 13.sp)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = remember(data) {
        val maxAmount = data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1000.0
        if (maxAmount <= 0) 1000.0 else maxAmount * 1.15
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected Month Tooltip / Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldNeon))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Income", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RoseNeon))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Expense", color = TextSecondary, fontSize = 11.sp)
            }

            if (selectedIndex != null && selectedIndex in data.indices) {
                val item = data[selectedIndex!!]
                Text(
                    text = "${item.monthLabel}: In $currencySymbol${item.income.toInt()} | Out $currencySymbol${item.expense.toInt()}",
                    color = CyanNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "Tap month for details",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val totalWidth = size.width
            val totalHeight = size.height - 24.dp.toPx() // leave room for labels
            val barCount = data.size
            val groupWidth = totalWidth / barCount
            val barWidth = groupWidth * 0.28f
            val spacing = groupWidth * 0.08f

            // Draw subtle horizontal grid lines
            val gridLines = 3
            for (i in 1..gridLines) {
                val y = totalHeight * (i.toFloat() / gridLines)
                drawLine(
                    color = DarkCardBorder.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(totalWidth, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw bars
            data.forEachIndexed { index, item ->
                val centerX = groupWidth * index + groupWidth / 2f
                val incomeHeight = ((item.income / maxVal) * totalHeight).toFloat().coerceAtLeast(4f)
                val expenseHeight = ((item.expense / maxVal) * totalHeight).toFloat().coerceAtLeast(4f)

                val incomeLeft = centerX - barWidth - (spacing / 2f)
                val expenseLeft = centerX + (spacing / 2f)

                val isSelected = selectedIndex == index

                // Income Bar (Emerald/Cyan gradient)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            EmeraldNeon,
                            EmeraldNeon.copy(alpha = if (isSelected) 0.9f else 0.5f)
                        )
                    ),
                    topLeft = Offset(incomeLeft, totalHeight - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Expense Bar (Rose/Red gradient)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            RoseNeon,
                            RoseNeon.copy(alpha = if (isSelected) 0.9f else 0.5f)
                        )
                    ),
                    topLeft = Offset(expenseLeft, totalHeight - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        // Month Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEachIndexed { index, item ->
                Text(
                    text = item.monthLabel,
                    color = if (selectedIndex == index) CyanNeon else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable {
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                )
            }
        }
    }
}

@Composable
fun SpendingBreakdownBar(
    breakdown: Map<ExpenseCategory, Double>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val totalExpense = breakdown.values.sum()

    if (totalExpense <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No expenses recorded this month", color = TextTertiary, fontSize = 12.sp)
        }
        return
    }

    val sortedCategories = breakdown.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }

    Column(modifier = modifier.fillMaxWidth()) {
        // Multi-segment progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(DarkSurfaceElevated)
        ) {
            sortedCategories.forEach { entry ->
                val weight = (entry.value / totalExpense).toFloat().coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(entry.key.color)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category items list with percentage pills
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sortedCategories.take(5).forEach { entry ->
                val percentage = (entry.value / totalExpense * 100.0)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(entry.key.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entry.key.label,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", entry.value)}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", percentage)}%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CyanNeon,
    trackColor: Color = DarkSurfaceElevated,
    strokeWidth: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progress_ring"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )

            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(color, color.copy(alpha = 0.6f), color)
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

@Composable
fun NetWorthSplineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = CyanNeon,
    height: Dp = 120.dp
) {
    if (points.size < 2) return

    val minVal = points.minOrNull() ?: 0f
    val maxVal = points.maxOrNull() ?: 1f
    val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val width = size.width
        val chartHeight = size.height

        val stepX = width / (points.size - 1)
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, value ->
            val x = i * stepX
            val normalizedY = (value - minVal) / range
            val y = chartHeight - (normalizedY * (chartHeight - 20.dp.toPx()) + 10.dp.toPx())

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, chartHeight)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevNormalizedY = (points[i - 1] - minVal) / range
                val prevY = chartHeight - (prevNormalizedY * (chartHeight - 20.dp.toPx()) + 10.dp.toPx())

                val controlX1 = prevX + (x - prevX) / 2f
                val controlY1 = prevY
                val controlX2 = prevX + (x - prevX) / 2f
                val controlY2 = y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }

        fillPath.lineTo(width, chartHeight)
        fillPath.close()

        // Gradient fill under the spline
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = chartHeight
            )
        )

        // Spline stroke
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
