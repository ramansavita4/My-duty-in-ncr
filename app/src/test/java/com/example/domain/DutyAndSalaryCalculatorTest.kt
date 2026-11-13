package com.example.domain

import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.CalculationMethod
import com.example.model.FacilityItem
import com.example.model.UserProfile
import com.example.model.WorkplaceQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DutyAndSalaryCalculatorTest {

    private fun calMillis(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun expectedOutTime_addsStandardHoursAndUnpaidLunchOnlyWhenTeaIsPaid() {
        val profile = UserProfile(
            standardWorkingHours = 8.0,
            lunchBreakMinutes = 30L,
            teaBreakMinutes = 15L,
            deductTeaBreak = false
        )
        val inMs = calMillis(9, 0)
        val expectedOutMs = DutyCalculator.calculateExpectedOutTimeMillis(inMs, profile)
        assertEquals(calMillis(17, 30), expectedOutMs)

        val deductTeaProfile = profile.copy(deductTeaBreak = true)
        val expectedWithTeaDeducted = DutyCalculator.calculateExpectedOutTimeMillis(inMs, deductTeaProfile)
        assertEquals(calMillis(17, 45), expectedWithTeaDeducted)
    }

    @Test
    fun completedDuty_calculatesWorkingMinutesLunchDeductionAndOvertime() {
        val profile = UserProfile(
            standardWorkingHours = 8.0,
            shiftStartTime = "09:00",
            lunchBreakMinutes = 30L,
            teaBreakMinutes = 15L,
            deductTeaBreak = false
        )
        val inMs = calMillis(9, 0)
        val outMs = calMillis(19, 30) // 10h 30m inside factory -> 30m lunch deducted -> 10h worked -> 2h OT
        val res = DutyCalculator.calculateCompletedDuty(inMs, outMs, profile)

        assertEquals(630L, res.totalWorkplaceMinutes)
        assertEquals(30L, res.breakMinutesDeducted)
        assertEquals(600L, res.actualWorkingMinutes)
        assertEquals(120L, res.overtimeMinutes)
        assertEquals(AttendanceStatus.PRESENT, res.status)
    }

    @Test
    fun incompletePunch_isMarkedMissingAndExcludedFromSalary() {
        val profile = UserProfile(
            calculationMethod = CalculationMethod.DAYS_26.key,
            monthlyGrossSalary = 13000.0,
            basicSalary = 10000.0,
            da = 0.0,
            hra = 3000.0,
            pfApplicable = false,
            esiApplicable = false
        )
        val inOnly = AttendanceRecord(
            dateKey = "2026-11-10",
            monthKey = "2026-11",
            status = AttendanceStatus.PRESENT.code,
            punchInTimeMillis = calMillis(9, 0),
            punchOutTimeMillis = null
        )
        val calcIncomplete = DutyCalculator.calculateCompletedDuty(calMillis(9, 0), null, profile)
        assertEquals(AttendanceStatus.MISSING_PUNCH, calcIncomplete.status)
        assertEquals(0L, calcIncomplete.actualWorkingMinutes)

        val salary = SalaryCalculator.calculateMonthlySalary(
            monthKey = "2026-11",
            profile = profile,
            attendanceRecords = listOf(inOnly),
            facilities = emptyList(),
            daysInMonth = 30
        )
        assertEquals(0, salary.presentDays)
        assertEquals(1, salary.missingPunchDays)
        assertEquals(0.0, salary.grossEarned, 0.01)
    }

    @Test
    fun salaryCalculator_supportsAllMethodsAndStatutoryPfEsiAndFreeFacilities() {
        val baseProfile = UserProfile(
            calculationMethod = CalculationMethod.DAYS_26.key,
            monthlyGrossSalary = 18000.0,
            basicSalary = 12000.0,
            da = 3000.0, // Basic + DA = 15,000
            hra = 2000.0,
            otherAllowances = 1000.0,
            standardWorkingHours = 8.0,
            overtimeMultiplier = 2.0,
            pfApplicable = true,
            esiApplicable = true
        )
        val records = (1..22).map { day ->
            AttendanceRecord(
                dateKey = "2026-11-${day.toString().padStart(2, '0')}",
                monthKey = "2026-11",
                status = "P",
                punchInTimeMillis = 1000L,
                punchOutTimeMillis = 2000L,
                actualWorkingMinutes = 480L,
                overtimeMinutes = if (day <= 4) 60L else 0L // 4 hours OT
            )
        } + (23..26).map { day ->
            AttendanceRecord(
                dateKey = "2026-11-$day",
                monthKey = "2026-11",
                status = "WO"
            )
        }

        val facilities = listOf(
            FacilityItem(name = "Free Tea", category = "TEA", enabled = true, isFree = true, monthlyCharge = 500.0),
            FacilityItem(name = "Paid Canteen", category = "CANTEEN", enabled = true, isFree = false, monthlyCharge = 600.0)
        )

        // 1. 26-Day Formula (22 P + 4 WO = 26 payable days -> full month earned)
        val res26 = SalaryCalculator.calculateMonthlySalary("2026-11", baseProfile, records, facilities, daysInMonth = 30)
        assertEquals(26.0, res26.payableDays, 0.01)
        assertEquals(15000.0, res26.pfApplicableWage, 0.01)
        assertEquals(1800.0, res26.employeePf, 0.01) // 12% of 15,000
        assertEquals(1249.5, res26.employerEps, 0.01) // 8.33% of 15,000
        assertEquals(550.5, res26.employerPfEpf, 0.01) // 3.67% of 15,000
        assertEquals(75.0, res26.employerEdli, 0.01) // 0.5% of 15,000
        assertTrue(res26.esiEligible)
        assertEquals(0.0, res26.teaLunchDeduction, 0.01) // Free tea NEVER deducted
        assertEquals(600.0, res26.canteenDeduction, 0.01)

        // 2. 30-Day Formula
        val res30 = SalaryCalculator.calculateMonthlySalary(
            "2026-11",
            baseProfile.copy(calculationMethod = CalculationMethod.DAYS_30.key),
            records,
            facilities,
            daysInMonth = 30
        )
        assertTrue(res30.dailyRate < res26.dailyRate)

        // 3. Monthly Fixed Formula
        val resFixed = SalaryCalculator.calculateMonthlySalary(
            "2026-11",
            baseProfile.copy(calculationMethod = CalculationMethod.MONTHLY_FIXED.key),
            records,
            facilities,
            daysInMonth = 30
        )
        assertEquals(12000.0, resFixed.basicEarned, 0.01)

        // 4. Daily Wage Formula
        val resDaily = SalaryCalculator.calculateMonthlySalary(
            "2026-11",
            baseProfile.copy(
                calculationMethod = CalculationMethod.DAILY_WAGE.key,
                dailyWage = 650.0,
                basicSalary = 0.0,
                da = 0.0,
                hra = 0.0,
                otherAllowances = 0.0
            ),
            records,
            emptyList(),
            daysInMonth = 30
        )
        assertEquals(650.0, resDaily.dailyRate, 0.01)
        assertEquals(22.0 * 650.0, resDaily.basicEarned, 0.01)

        // 5. Hourly Wage Formula
        val resHourly = SalaryCalculator.calculateMonthlySalary(
            "2026-11",
            baseProfile.copy(
                calculationMethod = CalculationMethod.HOURLY_WAGE.key,
                hourlyWage = 100.0,
                basicSalary = 0.0,
                da = 0.0,
                hra = 0.0,
                otherAllowances = 0.0
            ),
            records,
            emptyList(),
            daysInMonth = 30
        )
        assertEquals(100.0, resHourly.hourlyRate, 0.01)

        // 6. Actual Payable Formula
        val resActual = SalaryCalculator.calculateMonthlySalary(
            "2026-11",
            baseProfile.copy(calculationMethod = CalculationMethod.ACTUAL_PAYABLE.key),
            records,
            facilities,
            daysInMonth = 31
        )
        assertEquals(18000.0 / 31.0, resActual.dailyRate, 0.05)

        // 7. ESI ineligible above ₹21,000
        val highPay = SalaryCalculator.calculateMonthlySalary(
            "2026-11",
            baseProfile.copy(monthlyGrossSalary = 25000.0),
            records,
            emptyList(),
            daysInMonth = 30
        )
        assertFalse(highPay.esiEligible)
        assertEquals(0.0, highPay.employeeEsi, 0.01)
    }

    @Test
    fun qrValidation_checksMatchingWorkplaceActiveAndExpiry() {
        val now = 1_760_000_000_000L
        val wp1 = WorkplaceQr(
            id = "wp_1",
            companyName = "AutoTech",
            factoryName = "Plant A",
            qrToken = "WP-VALID123",
            qrExpiresAtMillis = now + 86_400_000L,
            isActive = true
        )
        val payload = QrCodeHelper.buildQrPayload(wp1)

        val valid = QrCodeHelper.validateScannedQr(payload, listOf(wp1), "wp_1", now)
        assertTrue(valid is QrValidationResult.Valid)

        val wrongWorkplace = QrCodeHelper.validateScannedQr(payload, listOf(wp1), "wp_other", now)
        assertTrue(wrongWorkplace is QrValidationResult.UnrecognizedOrUnauthorized)

        val expired = QrCodeHelper.validateScannedQr(
            payload,
            listOf(wp1.copy(qrExpiresAtMillis = now - 1000L)),
            "wp_1",
            now
        )
        assertTrue(expired is QrValidationResult.Expired)

        val inactive = QrCodeHelper.validateScannedQr(
            payload,
            listOf(wp1.copy(isActive = false)),
            "wp_1",
            now
        )
        assertTrue(inactive is QrValidationResult.Inactive)
    }

    @Test
    fun monthlyChartDataBuilder_buildsDailyHoursAndCumulativeEarnings() {
        val profile = UserProfile(
            calculationMethod = CalculationMethod.DAYS_26.key,
            monthlyGrossSalary = 15600.0, // 600/day, 75/hr
            basicSalary = 15600.0,
            standardWorkingHours = 8.0,
            overtimeMultiplier = 2.0,
            pfApplicable = false,
            esiApplicable = false
        )
        val records = listOf(
            AttendanceRecord(
                dateKey = "2026-11-01",
                monthKey = "2026-11",
                status = "P",
                punchInTimeMillis = 1000L,
                punchOutTimeMillis = 2000L,
                actualWorkingMinutes = 600L, // 8h reg + 2h OT
                overtimeMinutes = 120L,
                dayEarnings = 900.0
            ),
            AttendanceRecord(
                dateKey = "2026-11-02",
                monthKey = "2026-11",
                status = "M", // Incomplete punch must be excluded
                punchInTimeMillis = 3000L,
                punchOutTimeMillis = null,
                actualWorkingMinutes = 0L
            ),
            AttendanceRecord(
                dateKey = "2026-11-03",
                monthKey = "2026-11",
                status = "P",
                punchInTimeMillis = 4000L,
                punchOutTimeMillis = 5000L,
                actualWorkingMinutes = 480L,
                overtimeMinutes = 0L,
                dayEarnings = 600.0
            )
        )
        val breakdown = SalaryCalculator.calculateMonthlySalary("2026-11", profile, records, emptyList(), daysInMonth = 30)
        val chartSummary = MonthlyChartDataBuilder.build("2026-11", profile, records, breakdown, daysInMonth = 30)

        assertEquals(30, chartSummary.days.size)
        assertTrue(chartSummary.hasRecordedActivity)
        assertEquals(16.0, chartSummary.totalRegularHours, 0.01)
        assertEquals(2.0, chartSummary.totalOvertimeHours, 0.01)
        assertEquals(18.0, chartSummary.totalHoursWorked, 0.01)

        val day1 = chartSummary.days[0]
        assertEquals(8.0, day1.regularHours, 0.01)
        assertEquals(2.0, day1.overtimeHours, 0.01)
        assertEquals(900.0, day1.estimatedDayEarnings, 0.01)
        assertEquals(900.0, day1.cumulativeEarnings, 0.01)

        // Day 2 incomplete punch contributes 0 hours & 0 earnings
        val day2 = chartSummary.days[1]
        assertEquals(0.0, day2.totalHours, 0.01)
        assertEquals(900.0, day2.cumulativeEarnings, 0.01)

        val day3 = chartSummary.days[2]
        assertEquals(8.0, day3.regularHours, 0.01)
        assertEquals(1500.0, day3.cumulativeEarnings, 0.01)
    }
}
