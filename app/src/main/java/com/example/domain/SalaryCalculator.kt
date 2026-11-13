package com.example.domain

import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.CalculationMethod
import com.example.model.FacilityItem
import com.example.model.PayrollRecord
import com.example.model.UserProfile
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.round

data class SalaryBreakdown(
    val monthKey: String,
    val calculationMethod: CalculationMethod,
    val presentDays: Int,
    val absentDays: Int,
    val halfDays: Int,
    val weeklyOffDays: Int,
    val paidLeaveDays: Int,
    val holidays: Int,
    val missingPunchDays: Int,
    val payableDays: Double,
    val totalWorkingMinutes: Long,
    val overtimeHours: Double,
    val dailyRate: Double,
    val hourlyRate: Double,
    val overtimeRatePerHour: Double,
    // Earnings
    val basicEarned: Double,
    val daEarned: Double,
    val hraEarned: Double,
    val otherAllowancesEarned: Double,
    val overtimeEarnings: Double,
    val nightAllowance: Double,
    val attendanceBonus: Double,
    val otherBonuses: Double,
    val grossEarned: Double,
    // Statutory PF & ESI (Estimated unless verified)
    val pfApplicableWage: Double,
    val employeePf: Double,
    val employerPfEpf: Double,
    val employerEps: Double,
    val employerEdli: Double,
    val totalMonthlyPfContribution: Double,
    val esiEligible: Boolean,
    val esiWage: Double,
    val employeeEsi: Double,
    val employerEsi: Double,
    val totalMonthlyEsiContribution: Double,
    // Facility & Other Deductions
    val teaLunchDeduction: Double,
    val canteenDeduction: Double,
    val transportDeduction: Double,
    val otherFacilityDeduction: Double,
    val advanceDeduction: Double,
    val otherDeductions: Double,
    val totalDeductions: Double,
    val netEstimatedSalary: Double,
    val actualSalaryReceived: Double,
    val paymentDate: String,
    val pfVerifiedStatus: String,
    val esiVerifiedStatus: String
)

object SalaryCalculator {

    private const val PF_WAGE_CEILING = 15000.0
    private const val ESI_WAGE_CEILING = 21000.0

    fun calculateMonthlySalary(
        monthKey: String,
        profile: UserProfile,
        attendanceRecords: List<AttendanceRecord>,
        facilities: List<FacilityItem>,
        savedPayroll: PayrollRecord? = null,
        daysInMonth: Int = 30
    ): SalaryBreakdown {
        val method = CalculationMethod.fromKey(profile.calculationMethod)

        // Never count incomplete punches (status == M or punchOut == null when punchIn > 0) as completed duty
        val completedRecords = attendanceRecords.filter { rec ->
            val st = AttendanceStatus.fromCode(rec.status)
            when (st) {
                AttendanceStatus.PRESENT, AttendanceStatus.HALF_DAY ->
                    rec.punchOutTimeMillis != null || rec.actualWorkingMinutes > 0L || rec.dayEarnings > 0.0
                else -> true
            }
        }

        val presentDays = completedRecords.count { it.status == AttendanceStatus.PRESENT.code }
        val absentDays = attendanceRecords.count { it.status == AttendanceStatus.ABSENT.code }
        val halfDays = completedRecords.count { it.status == AttendanceStatus.HALF_DAY.code }
        val weeklyOffDays = attendanceRecords.count { it.status == AttendanceStatus.WEEKLY_OFF.code }
        val paidLeaveDays = attendanceRecords.count { it.status == AttendanceStatus.PAID_LEAVE.code }
        val holidays = attendanceRecords.count { it.status == AttendanceStatus.HOLIDAY.code }
        val missingPunchDays = attendanceRecords.count {
            it.status == AttendanceStatus.MISSING_PUNCH.code || DutyCalculator.isIncompletePunch(it)
        }

        val totalWorkingMinutes = completedRecords.sumOf { it.actualWorkingMinutes }
        val totalOvertimeMinutes = completedRecords.sumOf { it.overtimeMinutes }
        val overtimeHours = round2(totalOvertimeMinutes / 60.0)

        val configuredMonthlyGross = if (profile.monthlyGrossSalary > 0.0) {
            profile.monthlyGrossSalary
        } else {
            profile.basicSalary + profile.da + profile.hra + profile.otherAllowances
        }

        val standardHours = profile.standardWorkingHours.coerceAtLeast(1.0)

        val divisorDays = when (method) {
            CalculationMethod.DAYS_26 -> 26.0
            CalculationMethod.DAYS_30 -> 30.0
            CalculationMethod.ACTUAL_PAYABLE -> daysInMonth.toDouble().coerceAtLeast(1.0)
            CalculationMethod.MONTHLY_FIXED -> daysInMonth.toDouble().coerceAtLeast(1.0)
            CalculationMethod.DAILY_WAGE -> 26.0
            CalculationMethod.HOURLY_WAGE -> 26.0
        }

        val dailyRate = when (method) {
            CalculationMethod.DAILY_WAGE -> {
                if (profile.dailyWage > 0.0) profile.dailyWage else configuredMonthlyGross / 26.0
            }
            CalculationMethod.HOURLY_WAGE -> {
                val hRate = if (profile.hourlyWage > 0.0) profile.hourlyWage else (configuredMonthlyGross / 26.0) / standardHours
                hRate * standardHours
            }
            else -> configuredMonthlyGross / divisorDays
        }

        val hourlyRate = when (method) {
            CalculationMethod.HOURLY_WAGE -> {
                if (profile.hourlyWage > 0.0) profile.hourlyWage else dailyRate / standardHours
            }
            else -> dailyRate / standardHours
        }

        val effectiveOvertimeRate = if (profile.overtimeRatePerHour > 0.0) {
            profile.overtimeRatePerHour
        } else {
            hourlyRate * profile.overtimeMultiplier.coerceAtLeast(1.0)
        }

        // Payable days: P + PL + H + (WO if monthly/26/30/actual) + 0.5 * HD
        val payableDays = when (method) {
            CalculationMethod.DAILY_WAGE -> presentDays + (halfDays * 0.5) + paidLeaveDays + holidays
            CalculationMethod.HOURLY_WAGE -> (totalWorkingMinutes / 60.0) / standardHours
            CalculationMethod.MONTHLY_FIXED -> {
                val totalAccounted = presentDays + halfDays * 0.5 + weeklyOffDays + paidLeaveDays + holidays
                if (totalAccounted == 0.0 && absentDays == 0) 0.0 else totalAccounted
            }
            else -> presentDays + (halfDays * 0.5) + weeklyOffDays + paidLeaveDays + holidays
        }

        val payRatio = if (configuredMonthlyGross > 0.0) {
            when (method) {
                CalculationMethod.MONTHLY_FIXED -> {
                    val unpaidRatio = (absentDays.toDouble() / divisorDays).coerceIn(0.0, 1.0)
                    if (payableDays == 0.0 && absentDays == 0) 0.0 else (1.0 - unpaidRatio).coerceAtLeast(0.0)
                }
                else -> (payableDays / divisorDays).coerceIn(0.0, 1.5)
            }
        } else 0.0

        val basicEarned: Double
        val daEarned: Double
        val hraEarned: Double
        val otherAllowancesEarned: Double

        if (configuredMonthlyGross > 0.0 && (profile.basicSalary + profile.da + profile.hra + profile.otherAllowances) > 0.0) {
            basicEarned = round2(profile.basicSalary * payRatio)
            daEarned = round2(profile.da * payRatio)
            hraEarned = round2(profile.hra * payRatio)
            otherAllowancesEarned = round2(profile.otherAllowances * payRatio)
        } else if (method == CalculationMethod.HOURLY_WAGE) {
            val regularHours = (totalWorkingMinutes - totalOvertimeMinutes).coerceAtLeast(0L) / 60.0
            basicEarned = round2(regularHours * hourlyRate)
            daEarned = 0.0
            hraEarned = 0.0
            otherAllowancesEarned = 0.0
        } else {
            basicEarned = round2(payableDays * dailyRate)
            daEarned = 0.0
            hraEarned = 0.0
            otherAllowancesEarned = 0.0
        }

        val overtimeEarnings = round2(overtimeHours * effectiveOvertimeRate)
        val nightAllowance = savedPayroll?.nightAllowance ?: 0.0
        val attendanceBonus = savedPayroll?.attendanceBonus
            ?: if (absentDays == 0 && presentDays >= 20 && profile.attendanceBonusMonthly > 0.0) profile.attendanceBonusMonthly else 0.0
        val otherBonuses = savedPayroll?.otherBonuses ?: 0.0

        val grossEarned = round2(
            basicEarned + daEarned + hraEarned + otherAllowancesEarned +
                overtimeEarnings + nightAllowance + attendanceBonus + otherBonuses
        )

        // PF Calculation (EPFO Statutory Rules: 12% Employee on Basic+DA capped at ₹15,000, Employer 3.67% EPF + 8.33% EPS + 0.5% EDLI)
        val basicPlusDa = basicEarned + daEarned
        val pfApplicableWage = if (profile.pfApplicable && basicPlusDa > 0.0) {
            min(basicPlusDa, PF_WAGE_CEILING)
        } else 0.0
        val employeePf = if (profile.pfApplicable) round2(pfApplicableWage * 0.12) else 0.0
        val employerEps = if (profile.pfApplicable) round2(pfApplicableWage * 0.0833) else 0.0
        val employerPfEpf = if (profile.pfApplicable) round2(pfApplicableWage * 0.0367) else 0.0
        val employerEdli = if (profile.pfApplicable) round2(pfApplicableWage * 0.005) else 0.0
        val totalMonthlyPf = round2(employeePf + employerPfEpf + employerEps + employerEdli)

        // ESI Calculation (ESIC Statutory Rules: Eligible if esiApplicable == true AND gross <= ₹21,000; Employee 0.75%, Employer 3.25%)
        val grossForEligibility = if (configuredMonthlyGross > 0.0) configuredMonthlyGross else grossEarned
        val esiEligible = profile.esiApplicable && grossForEligibility > 0.0 && grossForEligibility <= ESI_WAGE_CEILING
        val esiWage = if (esiEligible) grossEarned else 0.0
        val employeeEsi = if (esiEligible && esiWage > 0.0) ceil(esiWage * 0.0075) else 0.0
        val employerEsi = if (esiEligible && esiWage > 0.0) ceil(esiWage * 0.0325) else 0.0
        val totalMonthlyEsi = round2(employeeEsi + employerEsi)

        // Facility Deductions (NEVER deduct free facilities!)
        val activePaidFacilities = facilities.filter { it.enabled && !it.isFree }
        val activeDayCount = (presentDays + halfDays).coerceAtLeast(0)

        fun facilityCost(item: FacilityItem): Double {
            return when {
                item.actualDeduction > 0.0 -> item.actualDeduction
                item.monthlyCharge > 0.0 -> item.monthlyCharge
                item.dailyCharge > 0.0 -> item.dailyCharge * activeDayCount
                else -> 0.0
            }
        }

        val teaLunchDed = savedPayroll?.teaLunchDeduction ?: round2(
            activePaidFacilities.filter { it.category in listOf("TEA", "BREAKFAST", "LUNCH", "DINNER") }
                .sumOf { facilityCost(it) }
        )
        val canteenDed = savedPayroll?.canteenDeduction ?: round2(
            activePaidFacilities.filter { it.category == "CANTEEN" }.sumOf { facilityCost(it) }
        )
        val transportDed = savedPayroll?.transportDeduction ?: round2(
            activePaidFacilities.filter { it.category in listOf("BUS", "CAB") }.sumOf { facilityCost(it) }
        )
        val otherFacilityDed = round2(
            activePaidFacilities.filter {
                it.category !in listOf("TEA", "BREAKFAST", "LUNCH", "DINNER", "CANTEEN", "BUS", "CAB", "PF", "ESI", "WEEKLY_OFF", "PAID_LEAVE", "BONUS", "OVERTIME", "NIGHT_ALLOWANCE", "ATTENDANCE_BONUS")
            }.sumOf { facilityCost(it) }
        )

        val advanceDed = savedPayroll?.advanceDeduction ?: 0.0
        val otherDed = (savedPayroll?.otherDeductions ?: 0.0) + otherFacilityDed

        val totalDeductions = round2(
            employeePf + employeeEsi + teaLunchDed + canteenDed + transportDed + advanceDed + otherDed
        )
        val netEstimatedSalary = round2(grossEarned - totalDeductions)

        return SalaryBreakdown(
            monthKey = monthKey,
            calculationMethod = method,
            presentDays = presentDays,
            absentDays = absentDays,
            halfDays = halfDays,
            weeklyOffDays = weeklyOffDays,
            paidLeaveDays = paidLeaveDays,
            holidays = holidays,
            missingPunchDays = missingPunchDays,
            payableDays = round2(payableDays),
            totalWorkingMinutes = totalWorkingMinutes,
            overtimeHours = overtimeHours,
            dailyRate = round2(dailyRate),
            hourlyRate = round2(hourlyRate),
            overtimeRatePerHour = round2(effectiveOvertimeRate),
            basicEarned = basicEarned,
            daEarned = daEarned,
            hraEarned = hraEarned,
            otherAllowancesEarned = otherAllowancesEarned,
            overtimeEarnings = overtimeEarnings,
            nightAllowance = nightAllowance,
            attendanceBonus = attendanceBonus,
            otherBonuses = otherBonuses,
            grossEarned = grossEarned,
            pfApplicableWage = round2(pfApplicableWage),
            employeePf = employeePf,
            employerPfEpf = employerPfEpf,
            employerEps = employerEps,
            employerEdli = employerEdli,
            totalMonthlyPfContribution = totalMonthlyPf,
            esiEligible = esiEligible,
            esiWage = round2(esiWage),
            employeeEsi = employeeEsi,
            employerEsi = employerEsi,
            totalMonthlyEsiContribution = totalMonthlyEsi,
            teaLunchDeduction = teaLunchDed,
            canteenDeduction = canteenDed,
            transportDeduction = transportDed,
            otherFacilityDeduction = otherFacilityDed,
            advanceDeduction = advanceDed,
            otherDeductions = otherDed,
            totalDeductions = totalDeductions,
            netEstimatedSalary = netEstimatedSalary,
            actualSalaryReceived = savedPayroll?.actualSalaryReceived ?: 0.0,
            paymentDate = savedPayroll?.paymentDate ?: "",
            pfVerifiedStatus = savedPayroll?.pfVerifiedStatus ?: "ESTIMATED",
            esiVerifiedStatus = savedPayroll?.esiVerifiedStatus ?: "ESTIMATED"
        )
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
