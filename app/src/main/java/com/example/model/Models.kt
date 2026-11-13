package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class CalculationMethod(val key: String, val labelHi: String, val labelEn: String) {
    MONTHLY_FIXED("MONTHLY_FIXED", "मासिक निश्चित वेतन", "Monthly Fixed"),
    DAILY_WAGE("DAILY_WAGE", "दैनिक मजदूरी (Daily Wage)", "Daily Wage"),
    HOURLY_WAGE("HOURLY_WAGE", "प्रति घंटा वेतन (Hourly)", "Hourly Wage"),
    DAYS_26("DAYS_26", "26 दिन विभाजक (26-Day)", "26-Day Formula"),
    DAYS_30("DAYS_30", "30 दिन विभाजक (30-Day)", "30-Day Formula"),
    ACTUAL_PAYABLE("ACTUAL_PAYABLE", "वास्तविक कार्य दिवस", "Actual Payable Days");

    companion object {
        fun fromKey(key: String?): CalculationMethod =
            entries.firstOrNull { it.key == key } ?: DAYS_26
    }
}

enum class AttendanceStatus(val code: String, val labelHi: String, val labelEn: String) {
    PRESENT("P", "उपस्थित", "Present"),
    ABSENT("A", "अनुपस्थित", "Absent"),
    HALF_DAY("HD", "आधा दिन", "Half Day"),
    WEEKLY_OFF("WO", "साप्ताहिक अवकाश", "Weekly Off"),
    PAID_LEAVE("PL", "सवैतनिक अवकाश", "Paid Leave"),
    HOLIDAY("H", "अवकाश", "Holiday"),
    MISSING_PUNCH("M", "मिसिंग पंच", "Missing Punch");

    companion object {
        fun fromCode(code: String?): AttendanceStatus =
            entries.firstOrNull { it.code == code } ?: ABSENT
    }
}

object NcrConstants {
    val WORK_CATEGORIES = listOf(
        "Operator",
        "Helper",
        "Fitter",
        "CNC Operator",
        "Quality Inspector",
        "Welder",
        "Electrician",
        "Packing",
        "Production",
        "Maintenance",
        "Driver",
        "Data Entry",
        "Other"
    )

    val INDUSTRIAL_AREAS = listOf(
        "IMT Manesar",
        "Gurugram",
        "Bawal",
        "Dharuhera",
        "Rewari",
        "Neemrana",
        "Faridabad",
        "Noida",
        "Greater Noida",
        "IMT Sohna"
    )

    val FACILITY_TEMPLATES = listOf(
        "TEA" to ("चाय (Tea)" to "Tea"),
        "BREAKFAST" to ("नाश्ता (Breakfast)" to "Breakfast"),
        "LUNCH" to ("दोपहर का भोजन (Lunch)" to "Lunch"),
        "DINNER" to ("रात का भोजन (Dinner)" to "Dinner"),
        "CANTEEN" to ("कैंटीन (Canteen)" to "Canteen"),
        "BUS" to ("कंपनी बस (Bus)" to "Bus"),
        "CAB" to ("कैब (Cab)" to "Cab"),
        "MEDICAL" to ("मेडिकल सुविधा (Medical)" to "Medical Facility"),
        "UNIFORM" to ("यूनिफॉर्म (Uniform)" to "Uniform"),
        "SHOES" to ("सेफ्टी शूज (Shoes)" to "Safety Shoes"),
        "PF" to ("पीएफ (PF)" to "PF"),
        "ESI" to ("ईएसआई (ESI)" to "ESI"),
        "WEEKLY_OFF" to ("साप्ताहिक छुट्टी (Weekly Off)" to "Weekly Off"),
        "PAID_LEAVE" to ("पेड लीव (Paid Leave)" to "Paid Leave"),
        "BONUS" to ("बोनस (Bonus)" to "Bonus"),
        "OVERTIME" to ("ओवरटाइम (Overtime)" to "Overtime"),
        "NIGHT_ALLOWANCE" to ("नाइट अलाउंस (Night Allowance)" to "Night Allowance"),
        "ATTENDANCE_BONUS" to ("अटेंडेंस बोनस (Attendance Bonus)" to "Attendance Bonus"),
        "OTHER" to ("अन्य सुविधा (Other)" to "Other Facility")
    )
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "worker",
    val employeeId: String = "",
    // Personal
    val fullName: String = "",
    val mobileNumber: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val photoUri: String = "",
    val residentialAddress: String = "",
    val village: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val pinCode: String = "",
    // Employment
    val companyName: String = "",
    val factoryName: String = "",
    val factoryAddress: String = "",
    val industrialArea: String = "IMT Manesar",
    val department: String = "",
    val designation: String = "",
    val workCategory: String = "Operator",
    val dateOfJoining: String = "",
    val gatePassNumber: String = "",
    val workerId: String = "",
    val previousExperience: String = "",
    val hrContact: String = "",
    val workplaceId: String = "",
    // Salary & Duty Config
    val calculationMethod: String = CalculationMethod.DAYS_26.key,
    val monthlyGrossSalary: Double = 0.0,
    val basicSalary: Double = 0.0,
    val da: Double = 0.0,
    val hra: Double = 0.0,
    val otherAllowances: Double = 0.0,
    val dailyWage: Double = 0.0,
    val hourlyWage: Double = 0.0,
    val overtimeRatePerHour: Double = 0.0,
    val overtimeMultiplier: Double = 2.0,
    val standardWorkingHours: Double = 8.0,
    val shiftStartTime: String = "09:00",
    val lunchBreakMinutes: Long = 30L,
    val teaBreakMinutes: Long = 15L,
    val deductTeaBreak: Boolean = false,
    val weeklyOffDay: String = "SUNDAY",
    val pfApplicable: Boolean = true,
    val esiApplicable: Boolean = true,
    val nightAllowancePerShift: Double = 0.0,
    val attendanceBonusMonthly: Double = 0.0,
    // Government
    val uanNumber: String = "",
    val esicNumber: String = "",
    val pfAccountNumber: String = "",
    val consentSelfieGiven: Boolean = false,
    val accountSuspended: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val monthKey: String = "",
    val status: String = "P",
    val workplaceId: String = "",
    val workplaceName: String = "",
    val qrCodeId: String = "",
    val qrVerified: Boolean = false,
    val punchInTimeMillis: Long = 0L,
    val punchOutTimeMillis: Long? = null,
    val expectedOutTimeMillis: Long? = null,
    val totalWorkplaceMinutes: Long = 0L,
    val breakMinutesDeducted: Long = 0L,
    val actualWorkingMinutes: Long = 0L,
    val standardWorkingMinutes: Long = 480L,
    val overtimeMinutes: Long = 0L,
    val lateArrivalMinutes: Long = 0L,
    val earlyDepartureMinutes: Long = 0L,
    val dayEarnings: Double = 0.0,
    val punchInSelfieUri: String = "",
    val punchOutSelfieUri: String = "",
    val correctionStatus: String = "NONE",
    val auditNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PayrollRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val monthKey: String = "",
    val basicEarned: Double = 0.0,
    val daEarned: Double = 0.0,
    val hraEarned: Double = 0.0,
    val otherAllowancesEarned: Double = 0.0,
    val overtimeEarnings: Double = 0.0,
    val nightAllowance: Double = 0.0,
    val attendanceBonus: Double = 0.0,
    val otherBonuses: Double = 0.0,
    val grossEarned: Double = 0.0,
    val employeePf: Double = 0.0,
    val employerPf: Double = 0.0,
    val epsContribution: Double = 0.0,
    val edliContribution: Double = 0.0,
    val employeeEsi: Double = 0.0,
    val employerEsi: Double = 0.0,
    val canteenDeduction: Double = 0.0,
    val transportDeduction: Double = 0.0,
    val teaLunchDeduction: Double = 0.0,
    val advanceDeduction: Double = 0.0,
    val otherDeductions: Double = 0.0,
    val netEstimatedSalary: Double = 0.0,
    val actualSalaryReceived: Double = 0.0,
    val paymentDate: String = "",
    val pfVerifiedStatus: String = "ESTIMATED",
    val esiVerifiedStatus: String = "ESTIMATED",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FacilityItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val enabled: Boolean = false,
    @get:PropertyName("isFree") val isFree: Boolean = true,
    val dailyCharge: Double = 0.0,
    val monthlyCharge: Double = 0.0,
    val actualDeduction: Double = 0.0,
    val paymentMethod: String = "Salary Deduction",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttendanceDispute(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetDate: String = "",
    val category: String = "MISSING_PUNCH_OUT",
    val description: String = "",
    val gatePassNumber: String = "",
    val evidenceUri: String = "",
    val status: String = "SUBMITTED",
    val auditTrail: List<String> = emptyList(),
    val reviewerNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "PUNCH_IN",
    @get:PropertyName("isRead") val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkplaceQr(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val companyName: String = "",
    val factoryName: String = "",
    val industrialArea: String = "IMT Manesar",
    val address: String = "",
    val qrToken: String = "",
    val qrExpiresAtMillis: Long = 0L,
    @get:PropertyName("isActive") val isActive: Boolean = true,
    val verifiedByAdmin: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CompanyProfile(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val companyName: String = "",
    val factoryName: String = "",
    val industrialArea: String = "IMT Manesar",
    val address: String = "",
    val hrName: String = "",
    val hrPhone: String = "",
    val gstOrRegNumber: String = "",
    val verificationDocNote: String = "",
    val verificationStatus: String = "PENDING",
    val facilitiesOffered: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JobVacancy(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val title: String = "",
    val tradeCategory: String = "Operator",
    val industrialArea: String = "IMT Manesar",
    val salaryMin: Double = 0.0,
    val salaryMax: Double = 0.0,
    val workingHours: Double = 8.0,
    val shiftType: String = "Day Shift",
    val pfProvided: Boolean = true,
    val esiProvided: Boolean = true,
    val facilities: List<String> = emptyList(),
    val status: String = "OPEN",
    val reportedCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JobApplication(
    @DocumentId val id: String = "",
    val applicantId: String = "",
    val applicantName: String = "",
    val applicantEmployeeId: String = "",
    val jobId: String = "",
    val jobOwnerId: String = "",
    val companyName: String = "",
    val jobTitle: String = "",
    val tradeCategory: String = "",
    val status: String = "APPLIED",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ComplaintReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "LABOUR_ISSUE",
    val targetId: String = "",
    val subject: String = "",
    val details: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
