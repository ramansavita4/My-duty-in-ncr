package com.example.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.domain.DutyCalculator
import com.example.domain.SalaryBreakdown
import com.example.localization.LocalStrings
import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.UserProfile
import com.example.ui.calendar.MonthlyDutyCalendarCard
import com.example.ui.components.CompactMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatRupees
import com.example.ui.summary.MonthlySummaryPreviewCard
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.HalfDayAmber
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OvertimeIndigo
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.WeeklyOffSlate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    profile: UserProfile?,
    todayRecord: AttendanceRecord?,
    selectedMonthKey: String,
    monthlyAttendance: List<AttendanceRecord>,
    salaryBreakdown: SalaryBreakdown,
    onOpenRegistration: () -> Unit,
    onPunchInClick: () -> Unit,
    onPunchOutClick: () -> Unit,
    onStepMonth: (Int) -> Unit,
    onSelectMonthYear: (String) -> Unit,
    onOpenCalendarTab: () -> Unit,
    onOpenMonthlySummary: () -> Unit
) {
    val strings = LocalStrings.current
    val todayDisplay = remember(strings.isHindi) {
        SimpleDateFormat("EEE, dd MMM yyyy", if (strings.isHindi) Locale("hi", "IN") else Locale.ENGLISH)
            .format(Date())
    }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (profile == null || profile.fullName.isBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.completeProfilePrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = onOpenRegistration,
                            modifier = Modifier.testTag("home_setup_profile_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.setupProfileNow)
                        }
                    }
                }
            }
        }

        // 1-6. Employee Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(NavyDark, NavyPrimary)))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!profile?.photoUri.isNullOrBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(profile?.photoUri),
                                contentDescription = "Employee Photo",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_worker_avatar),
                                contentDescription = "Employee Avatar",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.fullName?.ifBlank { "NCR Employee" } ?: "NCR Employee",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${profile?.designation?.ifBlank { profile.workCategory } ?: "Operator"} • ${profile?.companyName?.ifBlank { "Set Company" } ?: "Set Company"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color(0x330D9488),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ID: ${profile?.employeeId?.ifBlank { "Pending" } ?: "Pending"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF99F6E4),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = todayDisplay,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7-12. Today's Attendance Status, Punch-In/Out, Duty Hours, Break, Overtime
        item {
            val isPunchedIn = todayRecord != null && todayRecord.punchInTimeMillis > 0L
            val isPunchedOut = todayRecord?.punchOutTimeMillis != null
            val todayStatus = when {
                isPunchedOut -> todayRecord?.status ?: "P"
                isPunchedIn -> "P"
                else -> "A"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.todayDuty,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StatusPill(
                            statusCode = if (isPunchedIn && !isPunchedOut) "PRESENT" else AttendanceStatus.fromCode(todayStatus).labelEn
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPunchInClick,
                            enabled = !isPunchedIn,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_punch_in_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PresentGreen)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.punchIn, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }

                        Button(
                            onClick = onPunchOutClick,
                            enabled = isPunchedIn && !isPunchedOut,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_punch_out_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.punchOut, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }

                    // Timing & Duty Metrics Row
                    val dutyTimeText = when {
                        todayRecord?.punchOutTimeMillis != null ->
                            DutyCalculator.formatMinutesAsHoursMinutes(todayRecord.actualWorkingMinutes)
                        isPunchedIn -> if (strings.isHindi) "चालू है" else "Active"
                        else -> "0h 00m"
                    }
                    val breakText = "${todayRecord?.breakMinutesDeducted ?: (profile?.lunchBreakMinutes ?: 30L)}m"
                    val otText = DutyCalculator.formatMinutesAsHoursMinutes(todayRecord?.overtimeMinutes ?: 0L)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniTimeTile(strings.dutyHours, dutyTimeText, Modifier.weight(1f))
                        MiniTimeTile(strings.breakDuration, breakText, Modifier.weight(1f))
                        MiniTimeTile(strings.overtime, otText, Modifier.weight(1f))
                    }

                    if (todayRecord?.expectedOutTimeMillis != null && !isPunchedOut) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${strings.expectedOutTime}: ${timeFmt.format(Date(todayRecord.expectedOutTimeMillis))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TealSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 13-16. Monthly Attendance, Est. Salary, PF & ESI + Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactMetricCard(
                    label = strings.monthlyAttendance,
                    value = "${salaryBreakdown.presentDays} P / ${salaryBreakdown.halfDays} HD",
                    accentColor = PresentGreen,
                    modifier = Modifier.weight(1f)
                )
                CompactMetricCard(
                    label = strings.estMonthlySalary,
                    value = formatRupees(salaryBreakdown.netEstimatedSalary),
                    accentColor = TealSecondary,
                    subLabel = if (strings.isHindi) "अनुमानित" else "Est. Net",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactMetricCard(
                    label = strings.estPf,
                    value = formatRupees(salaryBreakdown.employeePf),
                    accentColor = NavyPrimary,
                    subLabel = "Emp + Er: ${formatRupees(salaryBreakdown.totalMonthlyPfContribution)}",
                    modifier = Modifier.weight(1f)
                )
                CompactMetricCard(
                    label = strings.estEsi,
                    value = if (salaryBreakdown.esiEligible) formatRupees(salaryBreakdown.employeeEsi) else "N/A",
                    accentColor = OvertimeIndigo,
                    subLabel = if (salaryBreakdown.esiEligible) "Emp 0.75%" else "Not Covered",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Compact Summary Cards: Present, Absent, Half Days, Weekly Offs, Overtime, Est. Net Salary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactMetricCard(strings.presentDays, "${salaryBreakdown.presentDays}", PresentGreen, Modifier.weight(1f))
                CompactMetricCard(strings.absentDays, "${salaryBreakdown.absentDays}", AbsentRed, Modifier.weight(1f))
                CompactMetricCard(strings.halfDays, "${salaryBreakdown.halfDays}", HalfDayAmber, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactMetricCard(strings.weeklyOffs, "${salaryBreakdown.weeklyOffDays}", WeeklyOffSlate, Modifier.weight(1f))
                CompactMetricCard(strings.overtime, "${salaryBreakdown.overtimeHours}h", OvertimeIndigo, Modifier.weight(1f))
                CompactMetricCard(strings.estNetSalary, formatRupees(salaryBreakdown.netEstimatedSalary), TealSecondary, Modifier.weight(1f))
            }
        }

        // Monthly Summary Charts Preview Card
        item {
            MonthlySummaryPreviewCard(
                totalHoursWorked = salaryBreakdown.totalWorkingMinutes / 60.0,
                overtimeHours = salaryBreakdown.overtimeHours,
                estimatedNetSalary = salaryBreakdown.netEstimatedSalary,
                onOpenSummaryDashboard = onOpenMonthlySummary
            )
        }

        // Embedded Monthly Duty Calendar directly on Home Dashboard
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.monthlyCalendar,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onOpenCalendarTab,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(if (strings.isHindi) "पूरा कैलेंडर" else "Full View", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            MonthlyDutyCalendarCard(
                selectedMonthKey = selectedMonthKey,
                attendanceRecords = monthlyAttendance,
                weeklyOffDay = profile?.weeklyOffDay ?: "SUNDAY",
                onStepMonth = onStepMonth,
                onSelectMonthYear = onSelectMonthYear,
                onDateClick = { onOpenCalendarTab() }
            )
        }
    }
}

@Composable
private fun MiniTimeTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
