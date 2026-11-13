package com.example.ui.summary

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DailySummaryPoint
import com.example.domain.MonthlyChartDataBuilder
import com.example.domain.MonthlyChartSummary
import com.example.domain.SalaryBreakdown
import com.example.localization.LocalStrings
import com.example.model.AttendanceRecord
import com.example.model.UserProfile
import com.example.ui.components.CompactMetricCard
import com.example.ui.components.formatRupees
import com.example.ui.components.formatRupeesExact
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OvertimeIndigo
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.TealSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthlySummaryDashboardScreen(
    selectedMonthKey: String,
    profile: UserProfile?,
    attendanceRecords: List<AttendanceRecord>,
    salaryBreakdown: SalaryBreakdown,
    daysInMonth: Int,
    onStepMonth: (Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val strings = LocalStrings.current
    val currentProfile = profile ?: UserProfile()
    val summary = remember(selectedMonthKey, currentProfile, attendanceRecords, salaryBreakdown, daysInMonth) {
        MonthlyChartDataBuilder.build(
            monthKey = selectedMonthKey,
            profile = currentProfile,
            attendanceRecords = attendanceRecords,
            salaryBreakdown = salaryBreakdown,
            daysInMonth = daysInMonth
        )
    }

    val monthTitle = remember(selectedMonthKey, strings.isHindi) {
        val parts = selectedMonthKey.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", if (strings.isHindi) Locale("hi", "IN") else Locale.ENGLISH)
            .format(cal.time)
    }

    var selectedDayIndex by remember(selectedMonthKey, summary.days.size) {
        val lastActive = summary.days.indexOfLast { it.totalHours > 0.0 || it.estimatedDayEarnings > 0.0 }
        mutableIntStateOf(if (lastActive >= 0) lastActive else 0)
    }
    var showCumulativeSalary by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-screen Header with Month Switcher
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("summary_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = strings.monthlySummaryDashboard,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onStepMonth(-1) }, modifier = Modifier.testTag("summary_prev_month")) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TealSecondary
                    )
                    IconButton(onClick = { onStepMonth(1) }, modifier = Modifier.testTag("summary_next_month")) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero KPI Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactMetricCard(
                        label = if (strings.isHindi) "कुल कार्य घंटे" else "Total Hours Worked",
                        value = "${formatHours(summary.totalHoursWorked)} hrs",
                        accentColor = TealSecondary,
                        subLabel = "${formatHours(summary.totalRegularHours)}h Reg + ${formatHours(summary.totalOvertimeHours)}h OT",
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricCard(
                        label = strings.estNetSalary,
                        value = formatRupees(summary.totalEstimatedNet),
                        accentColor = PresentGreen,
                        subLabel = "${strings.grossEarnings}: ${formatRupees(summary.totalEstimatedGross)}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactMetricCard(
                        label = if (strings.isHindi) "देय कार्य दिवस" else "Payable Days",
                        value = "${salaryBreakdown.payableDays} days",
                        accentColor = NavyPrimary,
                        subLabel = "${salaryBreakdown.presentDays}P • ${salaryBreakdown.halfDays}HD • ${salaryBreakdown.weeklyOffDays}WO",
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricCard(
                        label = if (strings.isHindi) "ओवरटाइम कमाई" else "Overtime Earnings",
                        value = formatRupees(salaryBreakdown.overtimeEarnings),
                        accentColor = OvertimeIndigo,
                        subLabel = "${formatHours(summary.totalOvertimeHours)}h @ ${formatRupees(salaryBreakdown.overtimeRatePerHour)}/h",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!summary.hasRecordedActivity) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (strings.isHindi) {
                                    "इस महीने के लिए अभी कोई हाजिरी या पंच रिकॉर्ड नहीं है। होम स्क्रीन से पंच-इन/आउट करें या कैलेंडर में दिन चुनें।"
                                } else {
                                    "No completed punches or attendance recorded for this month yet. Punch-In/Out from Home or mark days in Calendar to populate your charts."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            val selectedPoint = summary.days.getOrNull(selectedDayIndex)

            // Selected Day Inspector Strip
            item {
                AnimatedVisibility(visible = selectedPoint != null) {
                    selectedPoint?.let { pt ->
                        SelectedDayInspectorCard(point = pt, isHindi = strings.isHindi)
                    }
                }
            }

            // Chart 1: Daily Hours Worked (Stacked Regular + OT Bar Chart)
            item {
                DailyHoursWorkedChartCard(
                    summary = summary,
                    standardShiftHours = currentProfile.standardWorkingHours.coerceAtLeast(1.0),
                    selectedDayIndex = selectedDayIndex,
                    onSelectDay = { selectedDayIndex = it }
                )
            }

            // Chart 2: Estimated Salary Earnings (Area/Line Chart)
            item {
                EstimatedSalaryChartCard(
                    summary = summary,
                    showCumulative = showCumulativeSalary,
                    selectedDayIndex = selectedDayIndex,
                    onToggleMode = { showCumulativeSalary = it },
                    onSelectDay = { selectedDayIndex = it }
                )
            }
        }
    }
}

@Composable
fun MonthlySummaryPreviewCard(
    totalHoursWorked: Double,
    overtimeHours: Double,
    estimatedNetSalary: Double,
    onOpenSummaryDashboard: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_monthly_summary_card"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(NavyDark, NavyPrimary)))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF5EEAD4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = strings.monthlySummaryDashboard,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (strings.isHindi) {
                            "${formatHours(totalHoursWorked)} घंटे (${formatHours(overtimeHours)}h OT) • अनुमानित ${formatRupees(estimatedNetSalary)}"
                        } else {
                            "${formatHours(totalHoursWorked)} hrs (${formatHours(overtimeHours)}h OT) • Est. ${formatRupees(estimatedNetSalary)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCCFBF1)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onOpenSummaryDashboard,
                    modifier = Modifier.testTag("open_monthly_summary_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (strings.isHindi) "चार्ट" else "Charts",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDayInspectorCard(
    point: DailySummaryPoint,
    isHindi: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${point.dateKey} ${if (point.statusCode.isNotBlank()) "• ${point.statusCode}" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = if (isHindi) {
                        "ड्यूटी: ${formatHours(point.regularHours)}h • OT: ${formatHours(point.overtimeHours)}h (कुल ${formatHours(point.totalHours)}h)"
                    } else {
                        "Regular: ${formatHours(point.regularHours)}h • OT: ${formatHours(point.overtimeHours)}h (Total ${formatHours(point.totalHours)}h)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatRupeesExact(point.estimatedDayEarnings),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PresentGreen
                )
                Text(
                    text = if (isHindi) "संचयी: ${formatRupees(point.cumulativeEarnings)}" else "Cumul: ${formatRupees(point.cumulativeEarnings)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealSecondary
                )
            }
        }
    }
}

@Composable
private fun DailyHoursWorkedChartCard(
    summary: MonthlyChartSummary,
    standardShiftHours: Double,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val strings = LocalStrings.current
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 260f),
        label = "hours_bar_anim"
    )
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val selectedHighlight = NavyPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hours_worked_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.hoursWorkedChartTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = "Max ${formatHours(summary.maxDailyHours)}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ChartLegendDot(
                    color = TealSecondary,
                    label = if (strings.isHindi) "सामान्य घंटे (${formatHours(summary.totalRegularHours)}h)" else "Regular (${formatHours(summary.totalRegularHours)}h)"
                )
                ChartLegendDot(
                    color = OvertimeIndigo,
                    label = if (strings.isHindi) "ओवरटाइम (${formatHours(summary.totalOvertimeHours)}h)" else "Overtime (${formatHours(summary.totalOvertimeHours)}h)"
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .height(170.dp)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("${formatHours(summary.maxDailyHours)}h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${formatHours(summary.maxDailyHours / 2.0)}h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("0h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(modifier = Modifier.weight(1f)) {
                    val count = summary.days.size.coerceAtLeast(1)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .pointerInput(count) {
                                detectTapGestures { offset ->
                                    val idx = ((offset.x / size.width.toFloat()) * count).toInt().coerceIn(0, count - 1)
                                    onSelectDay(idx)
                                }
                            }
                            .pointerInput(count) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    val idx = ((change.position.x / size.width.toFloat()) * count).toInt().coerceIn(0, count - 1)
                                    onSelectDay(idx)
                                }
                            }
                    ) {
                        val maxH = summary.maxDailyHours.coerceAtLeast(1.0).toFloat()
                        val slotW = size.width / count
                        val barW = (slotW * 0.62f).coerceAtLeast(4f)
                        val padV = 6.dp.toPx()
                        val usableH = (size.height - padV * 2).coerceAtLeast(10f)

                        // Gridlines
                        listOf(0f, 0.5f, 1f).forEach { frac ->
                            val y = padV + usableH * (1f - frac)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Standard shift reference dashed line
                        val stdFrac = (standardShiftHours.toFloat() / maxH).coerceIn(0f, 1f)
                        val stdY = padV + usableH * (1f - stdFrac)
                        drawLine(
                            color = NavyPrimary.copy(alpha = 0.55f),
                            start = Offset(0f, stdY),
                            end = Offset(size.width, stdY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )

                        summary.days.forEachIndexed { idx, pt ->
                            val left = idx * slotW + (slotW - barW) / 2f
                            val isSelected = idx == selectedDayIndex

                            if (isSelected) {
                                drawRoundRect(
                                    color = selectedHighlight.copy(alpha = 0.12f),
                                    topLeft = Offset(idx * slotW, 0f),
                                    size = Size(slotW, size.height),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }

                            val totalPx = ((pt.totalHours.toFloat() / maxH) * usableH * animProgress).coerceAtLeast(0f)
                            val regPx = ((pt.regularHours.toFloat() / maxH) * usableH * animProgress).coerceAtLeast(0f)
                            val otPx = (totalPx - regPx).coerceAtLeast(0f)
                            val baseBottom = padV + usableH

                            if (regPx > 0f) {
                                drawRoundRect(
                                    color = if (isSelected) NavyPrimary else TealSecondary,
                                    topLeft = Offset(left, baseBottom - regPx),
                                    size = Size(barW, regPx),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                            if (otPx > 0f) {
                                drawRoundRect(
                                    color = OvertimeIndigo,
                                    topLeft = Offset(left, baseBottom - regPx - otPx),
                                    size = Size(barW, otPx),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }
                    }
                }
            }

            // X-Axis Day Markers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(1, 5, 10, 15, 20, 25, summary.days.size).distinct().forEach { d ->
                    Text(
                        text = d.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EstimatedSalaryChartCard(
    summary: MonthlyChartSummary,
    showCumulative: Boolean,
    selectedDayIndex: Int,
    onToggleMode: (Boolean) -> Unit,
    onSelectDay: (Int) -> Unit
) {
    val strings = LocalStrings.current
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val maxVal = if (showCumulative) summary.maxCumulativeEarnings else summary.maxDailyEarnings

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("salary_earnings_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.salaryEarningsChartTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showCumulative,
                    onClick = { onToggleMode(true) },
                    label = { Text(if (strings.isHindi) "संचयी (Cumulative)" else "Cumulative") }
                )
                FilterChip(
                    selected = !showCumulative,
                    onClick = { onToggleMode(false) },
                    label = { Text(if (strings.isHindi) "दैनिक (Daily ₹)" else "Daily ₹") }
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .height(170.dp)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(formatRupees(maxVal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRupees(maxVal / 2.0), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(modifier = Modifier.weight(1f)) {
                    val count = summary.days.size.coerceAtLeast(2)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .pointerInput(count) {
                                detectTapGestures { offset ->
                                    val idx = ((offset.x / size.width.toFloat()) * count).toInt().coerceIn(0, count - 1)
                                    onSelectDay(idx)
                                }
                            }
                            .pointerInput(count) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    val idx = ((change.position.x / size.width.toFloat()) * count).toInt().coerceIn(0, count - 1)
                                    onSelectDay(idx)
                                }
                            }
                    ) {
                        val safeMax = maxVal.coerceAtLeast(1.0).toFloat()
                        val padV = 8.dp.toPx()
                        val usableH = (size.height - padV * 2).coerceAtLeast(10f)
                        val stepX = size.width / (count - 1).coerceAtLeast(1)

                        listOf(0f, 0.5f, 1f).forEach { frac ->
                            val y = padV + usableH * (1f - frac)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val linePath = Path()
                        val fillPath = Path()
                        val coords = summary.days.mapIndexed { idx, pt ->
                            val v = if (showCumulative) pt.cumulativeEarnings else pt.estimatedDayEarnings
                            val x = idx * stepX
                            val y = padV + usableH * (1f - (v.toFloat() / safeMax).coerceIn(0f, 1f))
                            Offset(x, y)
                        }

                        if (coords.isNotEmpty()) {
                            linePath.moveTo(coords.first().x, coords.first().y)
                            fillPath.moveTo(coords.first().x, padV + usableH)
                            fillPath.lineTo(coords.first().x, coords.first().y)

                            for (i in 1 until coords.size) {
                                linePath.lineTo(coords[i].x, coords[i].y)
                                fillPath.lineTo(coords[i].x, coords[i].y)
                            }
                            fillPath.lineTo(coords.last().x, padV + usableH)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(PresentGreen.copy(alpha = 0.28f), Color.Transparent)
                                )
                            )
                            drawPath(
                                path = linePath,
                                color = PresentGreen,
                                style = Stroke(
                                    width = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            coords.getOrNull(selectedDayIndex)?.let { sel ->
                                drawLine(
                                    color = NavyPrimary.copy(alpha = 0.5f),
                                    start = Offset(sel.x, padV),
                                    end = Offset(sel.x, padV + usableH),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 5.5.dp.toPx(),
                                    center = sel
                                )
                                drawCircle(
                                    color = NavyPrimary,
                                    radius = 3.5.dp.toPx(),
                                    center = sel
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(1, 5, 10, 15, 20, 25, summary.days.size).distinct().forEach { d ->
                    Text(
                        text = d.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()
            Text(
                text = if (strings.isHindi) {
                    "टिप: किसी भी दिन के घंटे और अनुमानित कमाई देखने के लिए ग्राफ़ पर टैप या स्लाइड करें।"
                } else {
                    "Tip: Tap or drag across either chart to inspect exact hours and estimated earnings for any day."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatHours(hours: Double): String {
    return String.format(Locale.US, "%.1f", hours)
}
