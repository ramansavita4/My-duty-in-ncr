package com.example.ui.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.domain.DutyCalculator
import com.example.domain.SalaryBreakdown
import com.example.localization.LocalStrings
import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.ui.components.CompactMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatRupees
import com.example.ui.components.statusColors
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.HalfDayAmber
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OvertimeIndigo
import com.example.ui.theme.OvertimeIndigoBg
import com.example.ui.theme.PaidLeaveBlue
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.WeeklyOffSlate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DutyCalendarScreen(
    selectedMonthKey: String,
    attendanceRecords: List<AttendanceRecord>,
    salaryBreakdown: SalaryBreakdown,
    weeklyOffDay: String,
    onStepMonth: (Int) -> Unit,
    onSelectMonthYear: (String) -> Unit,
    onUpdateDayStatus: (dateKey: String, monthKey: String, status: AttendanceStatus, note: String) -> Unit,
    onRaiseDisputeForDate: (dateKey: String) -> Unit
) {
    val strings = LocalStrings.current
    var selectedDateKey by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MonthlyDutyCalendarCard(
                selectedMonthKey = selectedMonthKey,
                attendanceRecords = attendanceRecords,
                weeklyOffDay = weeklyOffDay,
                onStepMonth = onStepMonth,
                onSelectMonthYear = onSelectMonthYear,
                onDateClick = { dateKey -> selectedDateKey = dateKey }
            )
        }

        item {
            Text(
                text = strings.monthlyAttendance,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactMetricCard(
                    label = strings.totalWorkingDays,
                    value = "${salaryBreakdown.presentDays} P / ${salaryBreakdown.halfDays} HD",
                    accentColor = PresentGreen,
                    modifier = Modifier.weight(1f)
                )
                CompactMetricCard(
                    label = strings.overtime,
                    value = "${String.format(Locale.US, "%.1f", salaryBreakdown.overtimeHours)} hrs",
                    accentColor = OvertimeIndigo,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactMetricCard(
                    label = strings.absentDays,
                    value = "${salaryBreakdown.absentDays} A",
                    accentColor = AbsentRed,
                    modifier = Modifier.weight(1f)
                )
                CompactMetricCard(
                    label = strings.weeklyOffs,
                    value = "${salaryBreakdown.weeklyOffDays} WO / ${salaryBreakdown.paidLeaveDays} PL",
                    accentColor = WeeklyOffSlate,
                    modifier = Modifier.weight(1f)
                )
                CompactMetricCard(
                    label = strings.monthlyEarnings,
                    value = formatRupees(salaryBreakdown.netEstimatedSalary),
                    accentColor = TealSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    selectedDateKey?.let { dateKey ->
        val record = attendanceRecords.firstOrNull { it.dateKey == dateKey }
        DayDetailBottomSheet(
            dateKey = dateKey,
            monthKey = selectedMonthKey,
            record = record,
            onDismiss = { selectedDateKey = null },
            onUpdateStatus = { newStatus ->
                onUpdateDayStatus(dateKey, selectedMonthKey, newStatus, "Marked ${newStatus.code} from Calendar")
                selectedDateKey = null
            },
            onRaiseDispute = {
                selectedDateKey = null
                onRaiseDisputeForDate(dateKey)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyDutyCalendarCard(
    selectedMonthKey: String,
    attendanceRecords: List<AttendanceRecord>,
    weeklyOffDay: String,
    onStepMonth: (Int) -> Unit,
    onSelectMonthYear: (String) -> Unit,
    onDateClick: (String) -> Unit
) {
    val strings = LocalStrings.current
    var showMonthYearPicker by remember { mutableStateOf(false) }

    val parts = selectedMonthKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
    val monthZero = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1

    val cal = remember(year, monthZero) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZero)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceIn(0, 6)
    val monthTitle = remember(year, monthZero, strings.isHindi) {
        SimpleDateFormat("MMMM yyyy", if (strings.isHindi) Locale("hi", "IN") else Locale.ENGLISH)
            .format(cal.time)
    }

    val recordMap = remember(attendanceRecords) {
        attendanceRecords.associateBy { it.dateKey }
    }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onStepMonth(-1) },
                    modifier = Modifier.testTag("calendar_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month"
                    )
                }

                Box {
                    TextButton(
                        onClick = { showMonthYearPicker = true },
                        modifier = Modifier.testTag("calendar_month_picker")
                    ) {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }
                    MonthYearPickerDialog(
                        visible = showMonthYearPicker,
                        currentYear = year,
                        currentMonthZero = monthZero,
                        onDismiss = { showMonthYearPicker = false },
                        onConfirm = { newY, newMZero ->
                            val mStr = (newMZero + 1).toString().padStart(2, '0')
                            onSelectMonthYear("$newY-$mStr")
                            showMonthYearPicker = false
                        }
                    )
                }

                IconButton(
                    onClick = { onStepMonth(1) },
                    modifier = Modifier.testTag("calendar_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month"
                    )
                }
            }

            // Day of week headers
            val weekDays = if (strings.isHindi) {
                listOf("रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि")
            } else {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val totalCells = firstDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - firstDayOffset + 1
                        if (dayNum < 1 || dayNum > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val dateKey = "$selectedMonthKey-${dayNum.toString().padStart(2, '0')}"
                            val rec = recordMap[dateKey]
                            val dayCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNum) }
                            val dayName = SimpleDateFormat("EEEE", Locale.US).format(dayCal.time).uppercase(Locale.US)
                            val inferredCode = rec?.status ?: if (dayName == weeklyOffDay.uppercase(Locale.US)) "WO" else ""
                            val hasOt = (rec?.overtimeMinutes ?: 0L) > 0L
                            val isToday = dateKey == todayKey

                            val (bg, fg) = if (inferredCode.isNotBlank()) {
                                statusColors(inferredCode)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) to MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.92f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .then(
                                        if (isToday) Modifier.border(2.dp, TealSecondary, RoundedCornerShape(8.dp))
                                        else Modifier
                                    )
                                    .clickable { onDateClick(dateKey) }
                                    .testTag("calendar_day_$dateKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                        color = fg
                                    )
                                    if (inferredCode.isNotBlank()) {
                                        Text(
                                            text = if (hasOt) "$inferredCode+" else inferredCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (hasOt) OvertimeIndigo else fg,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            // Legend
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LegendChip("P", if (strings.isHindi) "उपस्थित" else "Present")
                LegendChip("A", if (strings.isHindi) "अनुपस्थित" else "Absent")
                LegendChip("HD", if (strings.isHindi) "आधा दिन" else "Half Day")
                LegendChip("WO", if (strings.isHindi) "वीकली ऑफ" else "Weekly Off")
                LegendChip("PL", if (strings.isHindi) "पेड लीव" else "Paid Leave")
                LegendChip("H", if (strings.isHindi) "अवकाश" else "Holiday")
                LegendChip("M", if (strings.isHindi) "मिसिंग" else "Missing")
                Surface(color = OvertimeIndigoBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = "+ OT",
                        style = MaterialTheme.typography.labelSmall,
                        color = OvertimeIndigo,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendChip(code: String, label: String) {
    val (bg, fg) = statusColors(code)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$code=$label",
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MonthYearPickerDialog(
    visible: Boolean,
    currentYear: Int,
    currentMonthZero: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    if (!visible) return
    var pickedYear by remember(currentYear) { mutableStateOf(currentYear) }
    var pickedMonth by remember(currentMonthZero) { mutableStateOf(currentMonthZero) }
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month & Year", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pickedYear -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Year")
                    }
                    Text(
                        text = pickedYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { pickedYear += 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Year")
                    }
                }
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 3) {
                            val idx = row * 3 + col
                            FilterChip(
                                selected = pickedMonth == idx,
                                onClick = { pickedMonth = idx },
                                label = { Text(months[idx], style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pickedYear, pickedMonth) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DayDetailBottomSheet(
    dateKey: String,
    monthKey: String,
    record: AttendanceRecord?,
    onDismiss: () -> Unit,
    onUpdateStatus: (AttendanceStatus) -> Unit,
    onRaiseDispute: () -> Unit
) {
    val strings = LocalStrings.current
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateKey,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StatusPill(statusCode = record?.status ?: "A")
            }

            if (record != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailRow(
                            label = if (strings.isHindi) "पंच-इन समय" else "Punch-In Time",
                            value = if (record.punchInTimeMillis > 0L) timeFmt.format(Date(record.punchInTimeMillis)) else "--"
                        )
                        DetailRow(
                            label = if (strings.isHindi) "पंच-आउट समय" else "Punch-Out Time",
                            value = if (record.punchOutTimeMillis != null) timeFmt.format(Date(record.punchOutTimeMillis)) else "--"
                        )
                        DetailRow(
                            label = strings.dutyHours,
                            value = DutyCalculator.formatMinutesAsHoursMinutes(record.actualWorkingMinutes)
                        )
                        DetailRow(
                            label = strings.breakDuration,
                            value = "${record.breakMinutesDeducted} min"
                        )
                        DetailRow(
                            label = strings.overtime,
                            value = DutyCalculator.formatMinutesAsHoursMinutes(record.overtimeMinutes)
                        )
                        DetailRow(
                            label = if (strings.isHindi) "दिन की अनुमानित कमाई" else "Day Earnings (Est.)",
                            value = formatRupees(record.dayEarnings)
                        )
                        DetailRow(
                            label = if (strings.isHindi) "QR कार्यस्थल सत्यापन" else "QR Workplace Verification",
                            value = if (record.qrVerified) "Verified (${record.workplaceName})" else "Not QR Verified"
                        )
                        DetailRow(
                            label = if (strings.isHindi) "सुधार स्थिति" else "Correction Status",
                            value = record.correctionStatus
                        )
                    }
                }

                if (record.punchInSelfieUri.isNotBlank() || record.punchOutSelfieUri.isNotBlank()) {
                    Text(
                        text = if (strings.isHindi) "सेल्फी हाजिरी प्रमाण" else "Selfie Attendance Evidence",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (record.punchInSelfieUri.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = rememberAsyncImagePainter(record.punchInSelfieUri),
                                    contentDescription = "Punch-In Selfie",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text("In", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (record.punchOutSelfieUri.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = rememberAsyncImagePainter(record.punchOutSelfieUri),
                                    contentDescription = "Punch-Out Selfie",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text("Out", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = if (strings.isHindi) "इस तारीख के लिए कोई पंच रिकॉर्ड नहीं है।" else "No punch record recorded for this date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()
            Text(
                text = if (strings.isHindi) "अवकाश / स्थिति चिह्नित करें" else "Mark Leave / Day Status",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    AttendanceStatus.PAID_LEAVE,
                    AttendanceStatus.WEEKLY_OFF,
                    AttendanceStatus.HOLIDAY,
                    AttendanceStatus.ABSENT
                ).forEach { st ->
                    OutlinedButton(
                        onClick = { onUpdateStatus(st) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("${st.code} • ${if (strings.isHindi) st.labelHi else st.labelEn}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Button(
                onClick = onRaiseDispute,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("day_detail_dispute_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.raiseDispute)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
