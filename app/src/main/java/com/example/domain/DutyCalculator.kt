package com.example.domain

import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.UserProfile
import java.util.Calendar
import kotlin.math.max

data class DutyCalculationResult(
    val totalWorkplaceMinutes: Long,
    val breakMinutesDeducted: Long,
    val actualWorkingMinutes: Long,
    val standardWorkingMinutes: Long,
    val overtimeMinutes: Long,
    val lateArrivalMinutes: Long,
    val earlyDepartureMinutes: Long,
    val status: AttendanceStatus
)

object DutyCalculator {

    /**
     * Calculates expected out-time when Punch-In is completed:
     * punchIn + standardWorkingHours + unpaid lunch + (tea break only if deductTeaBreak == true)
     */
    fun calculateExpectedOutTimeMillis(
        punchInTimeMillis: Long,
        profile: UserProfile
    ): Long {
        val standardWorkMinutes = (profile.standardWorkingHours * 60.0).toLong().coerceAtLeast(60L)
        val unpaidBreakMinutes = profile.lunchBreakMinutes.coerceAtLeast(0L) +
            if (profile.deductTeaBreak) profile.teaBreakMinutes.coerceAtLeast(0L) else 0L
        return punchInTimeMillis + (standardWorkMinutes + unpaidBreakMinutes) * 60_000L
    }

    /**
     * Parses shiftStartTime (e.g. "09:00") onto the same calendar day as punchInTimeMillis.
     */
    fun shiftStartMillisForDay(punchInTimeMillis: Long, shiftStartTime: String): Long {
        val parts = shiftStartTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val cal = Calendar.getInstance().apply {
            timeInMillis = punchInTimeMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Computes actual working hours, deducted unpaid break, overtime, late arrival,
     * early departure, and attendance status when Punch-Out is completed.
     * Incomplete punches (punchOutTimeMillis == null) must never be counted as completed duty.
     */
    fun calculateCompletedDuty(
        punchInTimeMillis: Long,
        punchOutTimeMillis: Long?,
        profile: UserProfile
    ): DutyCalculationResult {
        val standardMinutes = (profile.standardWorkingHours * 60.0).toLong().coerceAtLeast(60L)
        if (punchOutTimeMillis == null || punchOutTimeMillis <= punchInTimeMillis) {
            return DutyCalculationResult(
                totalWorkplaceMinutes = 0L,
                breakMinutesDeducted = 0L,
                actualWorkingMinutes = 0L,
                standardWorkingMinutes = standardMinutes,
                overtimeMinutes = 0L,
                lateArrivalMinutes = calculateLateMinutes(punchInTimeMillis, profile.shiftStartTime),
                earlyDepartureMinutes = 0L,
                status = AttendanceStatus.MISSING_PUNCH
            )
        }

        val totalWorkplaceMinutes = ((punchOutTimeMillis - punchInTimeMillis) / 60_000L).coerceAtLeast(0L)
        // Only deduct lunch/unpaid break if the worker stayed beyond half-day threshold (e.g. > 240 mins)
        val configuredUnpaidBreak = profile.lunchBreakMinutes.coerceAtLeast(0L) +
            if (profile.deductTeaBreak) profile.teaBreakMinutes.coerceAtLeast(0L) else 0L
        val breakDeducted = if (totalWorkplaceMinutes > configuredUnpaidBreak + 120L) {
            configuredUnpaidBreak
        } else {
            0L
        }

        val actualWorkingMinutes = max(0L, totalWorkplaceMinutes - breakDeducted)
        val overtimeMinutes = max(0L, actualWorkingMinutes - standardMinutes)
        val lateArrivalMinutes = calculateLateMinutes(punchInTimeMillis, profile.shiftStartTime)
        val earlyDepartureMinutes = max(0L, standardMinutes - actualWorkingMinutes)

        val status = when {
            actualWorkingMinutes >= (standardMinutes * 0.75).toLong() -> AttendanceStatus.PRESENT
            actualWorkingMinutes >= (standardMinutes * 0.35).toLong() -> AttendanceStatus.HALF_DAY
            else -> AttendanceStatus.HALF_DAY
        }

        return DutyCalculationResult(
            totalWorkplaceMinutes = totalWorkplaceMinutes,
            breakMinutesDeducted = breakDeducted,
            actualWorkingMinutes = actualWorkingMinutes,
            standardWorkingMinutes = standardMinutes,
            overtimeMinutes = overtimeMinutes,
            lateArrivalMinutes = lateArrivalMinutes,
            earlyDepartureMinutes = earlyDepartureMinutes,
            status = status
        )
    }

    private fun calculateLateMinutes(punchInTimeMillis: Long, shiftStartTime: String): Long {
        val scheduledStart = shiftStartMillisForDay(punchInTimeMillis, shiftStartTime)
        return max(0L, (punchInTimeMillis - scheduledStart) / 60_000L)
    }

    fun formatMinutesAsHoursMinutes(minutes: Long): String {
        val safe = max(0L, minutes)
        val h = safe / 60L
        val m = safe % 60L
        return "${h}h ${m.toString().padStart(2, '0')}m"
    }

    fun isIncompletePunch(record: AttendanceRecord): Boolean {
        return record.punchInTimeMillis > 0L && record.punchOutTimeMillis == null
    }
}
