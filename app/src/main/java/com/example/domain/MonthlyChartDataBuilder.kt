package com.example.domain

import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.CalculationMethod
import com.example.model.UserProfile
import kotlin.math.round

data class DailySummaryPoint(
    val dayOfMonth: Int,
    val dateKey: String,
    val statusCode: String,
    val regularHours: Double,
    val overtimeHours: Double,
    val totalHours: Double,
    val estimatedDayEarnings: Double,
    val cumulativeEarnings: Double
)

data class MonthlyChartSummary(
    val monthKey: String,
    val days: List<DailySummaryPoint>,
    val totalRegularHours: Double,
    val totalOvertimeHours: Double,
    val totalHoursWorked: Double,
    val totalEstimatedGross: Double,
    val totalEstimatedNet: Double,
    val maxDailyHours: Double,
    val maxDailyEarnings: Double,
    val maxCumulativeEarnings: Double,
    val hasRecordedActivity: Boolean
)

object MonthlyChartDataBuilder {

    fun build(
        monthKey: String,
        profile: UserProfile,
        attendanceRecords: List<AttendanceRecord>,
        salaryBreakdown: SalaryBreakdown,
        daysInMonth: Int
    ): MonthlyChartSummary {
        val safeDays = daysInMonth.coerceIn(28, 31)
        val byDate = attendanceRecords.associateBy { it.dateKey }
        val method = CalculationMethod.fromKey(profile.calculationMethod)
        val standardHours = profile.standardWorkingHours.coerceAtLeast(1.0)

        var runningEarnings = 0.0
        var totalRegular = 0.0
        var totalOt = 0.0
        var hasActivity = false
        val points = ArrayList<DailySummaryPoint>(safeDays)

        for (day in 1..safeDays) {
            val dateKey = "$monthKey-${day.toString().padStart(2, '0')}"
            val rec = byDate[dateKey]

            var regHours = 0.0
            var otHours = 0.0
            var dayPay = 0.0
            val statusCode = rec?.status ?: ""

            if (rec != null) {
                val st = AttendanceStatus.fromCode(rec.status)
                val isIncomplete = DutyCalculator.isIncompletePunch(rec) || st == AttendanceStatus.MISSING_PUNCH

                if (!isIncomplete && (st == AttendanceStatus.PRESENT || st == AttendanceStatus.HALF_DAY)) {
                    val hasCompletedRecord = rec.punchOutTimeMillis != null || rec.actualWorkingMinutes > 0L || rec.dayEarnings > 0.0
                    if (hasCompletedRecord) {
                        hasActivity = true
                        val totalMins = if (rec.actualWorkingMinutes > 0L) {
                            rec.actualWorkingMinutes
                        } else if (st == AttendanceStatus.PRESENT) {
                            (standardHours * 60.0).toLong()
                        } else {
                            (standardHours * 30.0).toLong()
                        }
                        val otMins = rec.overtimeMinutes.coerceAtLeast(0L)
                        val regMins = (totalMins - otMins).coerceAtLeast(0L)
                        regHours = round2(regMins / 60.0)
                        otHours = round2(otMins / 60.0)

                        dayPay = if (rec.dayEarnings > 0.0) {
                            rec.dayEarnings
                        } else if (method == CalculationMethod.HOURLY_WAGE) {
                            round2(regHours * salaryBreakdown.hourlyRate + otHours * salaryBreakdown.overtimeRatePerHour)
                        } else {
                            val baseRatio = if (st == AttendanceStatus.HALF_DAY) 0.5 else 1.0
                            round2(salaryBreakdown.dailyRate * baseRatio + otHours * salaryBreakdown.overtimeRatePerHour)
                        }
                    }
                } else if (st == AttendanceStatus.WEEKLY_OFF || st == AttendanceStatus.PAID_LEAVE || st == AttendanceStatus.HOLIDAY) {
                    if (method != CalculationMethod.DAILY_WAGE && method != CalculationMethod.HOURLY_WAGE) {
                        dayPay = round2(salaryBreakdown.dailyRate)
                    } else if (st != AttendanceStatus.WEEKLY_OFF) {
                        dayPay = round2(salaryBreakdown.dailyRate)
                    }
                    if (dayPay > 0.0) hasActivity = true
                }
            }

            val totalDayHours = round2(regHours + otHours)
            totalRegular = round2(totalRegular + regHours)
            totalOt = round2(totalOt + otHours)
            runningEarnings = round2(runningEarnings + dayPay)

            points.add(
                DailySummaryPoint(
                    dayOfMonth = day,
                    dateKey = dateKey,
                    statusCode = statusCode,
                    regularHours = regHours,
                    overtimeHours = otHours,
                    totalHours = totalDayHours,
                    estimatedDayEarnings = round2(dayPay),
                    cumulativeEarnings = runningEarnings
                )
            )
        }

        val maxHours = (points.maxOfOrNull { it.totalHours } ?: 0.0).coerceAtLeast(standardHours)
        val maxDayEarn = (points.maxOfOrNull { it.estimatedDayEarnings } ?: 0.0).coerceAtLeast(100.0)
        val maxCumEarn = (points.maxOfOrNull { it.cumulativeEarnings } ?: 0.0).coerceAtLeast(100.0)

        return MonthlyChartSummary(
            monthKey = monthKey,
            days = points,
            totalRegularHours = totalRegular,
            totalOvertimeHours = totalOt,
            totalHoursWorked = round2(totalRegular + totalOt),
            totalEstimatedGross = salaryBreakdown.grossEarned,
            totalEstimatedNet = salaryBreakdown.netEstimatedSalary,
            maxDailyHours = maxHours,
            maxDailyEarnings = maxDayEarn,
            maxCumulativeEarnings = maxCumEarn,
            hasRecordedActivity = hasActivity
        )
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
