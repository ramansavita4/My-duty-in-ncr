package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.DutyCalculator
import com.example.domain.SalaryCalculator
import com.example.model.AppNotification
import com.example.model.AttendanceDispute
import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.CompanyProfile
import com.example.model.ComplaintReport
import com.example.model.FacilityItem
import com.example.model.JobApplication
import com.example.model.JobVacancy
import com.example.model.NcrConstants
import com.example.model.PayrollRecord
import com.example.model.UserProfile
import com.example.model.WorkplaceQr
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DutyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun generateUniqueEmployeeId(uid: String): String {
        val year = SimpleDateFormat("yyyy", Locale.US).format(Date())
        val suffix = (uid.hashCode().toLong() and 0xFFFFFFL)
            .toString(36)
            .uppercase(Locale.US)
            .padStart(5, '0')
            .take(5)
        val rand = UUID.randomUUID().toString().substring(0, 2).uppercase(Locale.US)
        return "NCR-$year-$suffix$rand"
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.GET, "users/$userId")
                }
                throw error
            }

    suspend fun saveUserProfile(profile: UserProfile): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existingSnap = docRef.get().await()
        val finalEmpId = profile.employeeId.ifBlank {
            existingSnap.getString("employeeId")?.takeIf { it.isNotBlank() }
                ?: generateUniqueEmployeeId(uid)
        }
        val toSave = if (existingSnap.exists()) {
            val existingProfile = existingSnap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            profile.copy(
                id = "",
                userId = uid,
                role = existingProfile?.role ?: "worker",
                employeeId = finalEmpId,
                createdAt = existingProfile?.createdAt,
                updatedAt = null
            )
        } else {
            profile.copy(
                id = "",
                userId = uid,
                role = "worker",
                employeeId = finalEmpId,
                createdAt = null,
                updatedAt = null
            )
        }
        try {
            docRef.set(toSave).await()
            val refreshed = docRef.get().await()
                .toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            refreshed ?: toSave
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun checkIsAdmin(userId: String): Boolean {
        val currentEmail = auth.currentUser?.email
        val isVerified = auth.currentUser?.isEmailVerified == true
        if (currentEmail == "ramansavita4@gmail.com" && isVerified) return true
        return try {
            val doc = db.collection("admins").document(userId).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteWorkerAccount(userId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(uid == userId) { "Cannot delete another user's account" }
        val userRef = db.collection("users").document(uid)
        val subcollections = listOf("attendance", "payroll_records", "facilities", "disputes", "notifications")
        for (sub in subcollections) {
            val snaps = userRef.collection(sub).get().await()
            for (doc in snaps.documents) {
                doc.reference.delete().await()
            }
        }
        userRef.delete().await()
        auth.currentUser?.delete()?.await()
        Unit
    }

    // --- Attendance ---

    fun observeMonthlyAttendance(userId: String, monthKey: String): Flow<List<AttendanceRecord>> =
        db.collection("users").document(userId).collection("attendance")
            .whereEqualTo("monthKey", monthKey)
            .snapshots()
            .map { snap -> snap.toObjects(AttendanceRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/attendance")
                }
                throw error
            }

    fun observeAllAttendance(userId: String): Flow<List<AttendanceRecord>> =
        db.collection("users").document(userId).collection("attendance")
            .snapshots()
            .map { snap -> snap.toObjects(AttendanceRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/attendance")
                }
                throw error
            }

    suspend fun punchIn(
        profile: UserProfile,
        dateKey: String,
        monthKey: String,
        workplace: WorkplaceQr,
        selfieUri: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<AttendanceRecord> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("attendance").document(dateKey)
        val existingSnap = docRef.get().await()
        if (existingSnap.exists()) {
            val existing = existingSnap.toObject(AttendanceRecord::class.java, ServerTimestampBehavior.ESTIMATE)
            if (existing != null && existing.punchInTimeMillis > 0L) {
                throw IllegalStateException("Duplicate Punch-In prevented: Already punched in for $dateKey.")
            }
        }
        val expectedOut = DutyCalculator.calculateExpectedOutTimeMillis(nowMillis, profile)
        val calcIn = DutyCalculator.calculateCompletedDuty(nowMillis, null, profile)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "dateKey" to dateKey,
            "monthKey" to monthKey,
            "status" to AttendanceStatus.PRESENT.code,
            "workplaceId" to workplace.id,
            "workplaceName" to "${workplace.companyName} - ${workplace.factoryName}",
            "qrCodeId" to workplace.qrToken,
            "qrVerified" to true,
            "punchInTimeMillis" to nowMillis,
            "expectedOutTimeMillis" to expectedOut,
            "standardWorkingMinutes" to calcIn.standardWorkingMinutes,
            "lateArrivalMinutes" to calcIn.lateArrivalMinutes,
            "punchInSelfieUri" to selfieUri,
            "correctionStatus" to "NONE",
            "auditNote" to "QR & Selfie Punch-In recorded",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            addNotificationInternal(
                uid,
                "Punch-In Recorded",
                "Duty started at ${workplace.companyName}. Expected out-time calculated.",
                "PUNCH_IN"
            )
            docRef.get().await().toObject(AttendanceRecord::class.java, ServerTimestampBehavior.ESTIMATE)
                ?: AttendanceRecord(userId = uid, dateKey = dateKey, monthKey = monthKey, status = "P")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun punchOut(
        profile: UserProfile,
        dateKey: String,
        workplace: WorkplaceQr,
        selfieUri: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<AttendanceRecord> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("attendance").document(dateKey)
        val snap = docRef.get().await()
        val existing = snap.toObject(AttendanceRecord::class.java, ServerTimestampBehavior.ESTIMATE)
            ?: throw IllegalStateException("No Punch-In found for $dateKey. Please Punch-In first.")
        if (existing.punchOutTimeMillis != null) {
            throw IllegalStateException("Duplicate Punch-Out prevented: Already punched out for $dateKey.")
        }
        if (existing.workplaceId.isNotBlank() && existing.workplaceId != workplace.id) {
            throw IllegalStateException("Workplace mismatch: Punch-Out must be at the same registered workplace.")
        }
        val calc = DutyCalculator.calculateCompletedDuty(existing.punchInTimeMillis, nowMillis, profile)
        val dailyEstimate = estimateSingleDayEarnings(profile, calc.actualWorkingMinutes, calc.overtimeMinutes)
        val updated = existing.copy(
            id = "",
            status = calc.status.code,
            punchOutTimeMillis = nowMillis,
            totalWorkplaceMinutes = calc.totalWorkplaceMinutes,
            breakMinutesDeducted = calc.breakMinutesDeducted,
            actualWorkingMinutes = calc.actualWorkingMinutes,
            standardWorkingMinutes = calc.standardWorkingMinutes,
            overtimeMinutes = calc.overtimeMinutes,
            lateArrivalMinutes = calc.lateArrivalMinutes,
            earlyDepartureMinutes = calc.earlyDepartureMinutes,
            dayEarnings = dailyEstimate,
            punchOutSelfieUri = selfieUri,
            auditNote = "${existing.auditNote}; Punch-Out recorded (${DutyCalculator.formatMinutesAsHoursMinutes(calc.actualWorkingMinutes)})",
            updatedAt = null
        )
        try {
            docRef.set(updated).await()
            addNotificationInternal(
                uid,
                "Punch-Out Completed",
                "Worked ${DutyCalculator.formatMinutesAsHoursMinutes(calc.actualWorkingMinutes)}, OT: ${DutyCalculator.formatMinutesAsHoursMinutes(calc.overtimeMinutes)}",
                if (calc.overtimeMinutes > 0L) "OVERTIME" else "PUNCH_OUT"
            )
            docRef.get().await().toObject(AttendanceRecord::class.java, ServerTimestampBehavior.ESTIMATE) ?: updated
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun saveDayStatus(
        userId: String,
        dateKey: String,
        monthKey: String,
        status: AttendanceStatus,
        note: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(uid == userId)
        val docRef = db.collection("users").document(uid).collection("attendance").document(dateKey)
        val existingSnap = docRef.get().await()
        if (existingSnap.exists()) {
            val existing = existingSnap.toObject(AttendanceRecord::class.java, ServerTimestampBehavior.ESTIMATE)!!
            val updated = existing.copy(
                id = "",
                status = status.code,
                auditNote = "${existing.auditNote} | $note".take(450),
                updatedAt = null
            )
            docRef.set(updated).await()
        } else {
            val fresh = AttendanceRecord(
                userId = uid,
                dateKey = dateKey,
                monthKey = monthKey,
                status = status.code,
                auditNote = note.take(450)
            )
            docRef.set(fresh).await()
        }
    }

    private fun estimateSingleDayEarnings(
        profile: UserProfile,
        actualMinutes: Long,
        overtimeMinutes: Long
    ): Double {
        val stdHours = profile.standardWorkingHours.coerceAtLeast(1.0)
        val dailyBase = if (profile.dailyWage > 0.0) {
            profile.dailyWage
        } else if (profile.monthlyGrossSalary > 0.0) {
            profile.monthlyGrossSalary / 26.0
        } else 0.0
        val hourly = if (profile.hourlyWage > 0.0) profile.hourlyWage else dailyBase / stdHours
        val otRate = if (profile.overtimeRatePerHour > 0.0) profile.overtimeRatePerHour else hourly * profile.overtimeMultiplier
        val regularMins = (actualMinutes - overtimeMinutes).coerceAtLeast(0L)
        val regularPay = (regularMins / 60.0) * hourly
        val otPay = (overtimeMinutes / 60.0) * otRate
        return kotlin.math.round((regularPay + otPay) * 100.0) / 100.0
    }

    // --- Facilities ---

    fun observeFacilities(userId: String): Flow<List<FacilityItem>> =
        db.collection("users").document(userId).collection("facilities")
            .snapshots()
            .map { it.toObjects(FacilityItem::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/facilities")
                }
                throw error
            }

    suspend fun ensureDefaultFacilitiesIfEmpty(userId: String, isHindi: Boolean) {
        val col = db.collection("users").document(userId).collection("facilities")
        val existing = col.limit(1).get().await()
        if (!existing.isEmpty) return
        val batch = db.batch()
        NcrConstants.FACILITY_TEMPLATES.forEach { (cat, labels) ->
            val docRef = col.document(cat.lowercase(Locale.US))
            val item = FacilityItem(
                userId = userId,
                name = if (isHindi) labels.first else labels.second,
                category = cat,
                enabled = cat in listOf("PF", "ESI", "WEEKLY_OFF", "OVERTIME"),
                isFree = true,
                dailyCharge = 0.0,
                monthlyCharge = 0.0,
                actualDeduction = 0.0,
                paymentMethod = "Free"
            )
            batch.set(docRef, item)
        }
        batch.commit().await()
    }

    suspend fun saveFacility(item: FacilityItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        val id = item.id.ifBlank { item.category.lowercase(Locale.US) }
        val docRef = db.collection("users").document(uid).collection("facilities").document(id)
        val existing = docRef.get().await()
        val toSave = if (existing.exists()) {
            val prev = existing.toObject(FacilityItem::class.java, ServerTimestampBehavior.ESTIMATE)
            item.copy(id = "", userId = uid, createdAt = prev?.createdAt, updatedAt = null)
        } else {
            item.copy(id = "", userId = uid, createdAt = null, updatedAt = null)
        }
        docRef.set(toSave).await()
    }

    // --- Payroll History ---

    fun observePayrollRecords(userId: String): Flow<List<PayrollRecord>> =
        db.collection("users").document(userId).collection("payroll_records")
            .snapshots()
            .map { it.toObjects(PayrollRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/payroll_records")
                }
                throw error
            }

    suspend fun savePayrollRecord(record: PayrollRecord): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("payroll_records").document(record.monthKey)
        val existing = docRef.get().await()
        val toSave = if (existing.exists()) {
            val prev = existing.toObject(PayrollRecord::class.java, ServerTimestampBehavior.ESTIMATE)
            record.copy(id = "", userId = uid, createdAt = prev?.createdAt, updatedAt = null)
        } else {
            record.copy(id = "", userId = uid, createdAt = null, updatedAt = null)
        }
        docRef.set(toSave).await()
    }

    // --- Disputes & Correction Requests ---

    fun observeDisputes(userId: String): Flow<List<AttendanceDispute>> =
        db.collection("users").document(userId).collection("disputes")
            .snapshots()
            .map { it.toObjects(AttendanceDispute::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/disputes")
                }
                throw error
            }

    suspend fun submitDispute(dispute: AttendanceDispute): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("disputes").document()
        val nowStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date())
        val toSave = dispute.copy(
            id = "",
            userId = uid,
            status = "SUBMITTED",
            auditTrail = listOf("[$nowStr] Submitted by worker (${dispute.category})"),
            createdAt = null,
            updatedAt = null
        )
        docRef.set(toSave).await()
        addNotificationInternal(
            uid,
            "Dispute Submitted",
            "Correction request for ${dispute.targetDate} submitted for review.",
            "CORRECTION"
        )
        docRef.id
    }

    // --- Notifications ---

    fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        db.collection("users").document(userId).collection("notifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(AppNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, "users/$userId/notifications")
                }
                throw error
            }

    suspend fun markNotificationRead(userId: String, notificationId: String) {
        try {
            db.collection("users").document(userId)
                .collection("notifications").document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/notifications/$notificationId")
        }
    }

    private suspend fun addNotificationInternal(
        uid: String,
        title: String,
        message: String,
        type: String
    ) {
        try {
            val docRef = db.collection("users").document(uid).collection("notifications").document()
            val notif = AppNotification(
                userId = uid,
                title = title.take(140),
                message = message.take(480),
                type = type,
                isRead = false
            )
            docRef.set(notif).await()
        } catch (_: Exception) {
            // Non-blocking
        }
    }

    // --- Workplaces (QR Verification) ---

    fun observeActiveWorkplaces(): Flow<List<WorkplaceQr>> =
        db.collection("workplaces")
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { it.toObjects(WorkplaceQr::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "workplaces")
                throw error
            }

    fun observeMyWorkplaces(ownerId: String): Flow<List<WorkplaceQr>> =
        db.collection("workplaces")
            .whereEqualTo("ownerId", ownerId)
            .snapshots()
            .map { it.toObjects(WorkplaceQr::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "workplaces")
                throw error
            }

    suspend fun createOrRotateWorkplaceQr(workplace: WorkplaceQr): Result<WorkplaceQr> = runCatching {
        val uid = requireUserId()
        val isUpdate = workplace.id.isNotBlank()
        val docRef = if (isUpdate) db.collection("workplaces").document(workplace.id)
        else db.collection("workplaces").document()
        val token = "WP-${UUID.randomUUID().toString().replace("-", "").take(16).uppercase(Locale.US)}"
        val expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // 30 days validity
        val toSave = if (isUpdate) {
            val prev = docRef.get().await().toObject(WorkplaceQr::class.java, ServerTimestampBehavior.ESTIMATE)
            workplace.copy(
                id = "",
                ownerId = prev?.ownerId ?: uid,
                qrToken = token,
                qrExpiresAtMillis = expiresAt,
                verifiedByAdmin = prev?.verifiedByAdmin ?: false,
                createdAt = prev?.createdAt,
                updatedAt = null
            )
        } else {
            workplace.copy(
                id = "",
                ownerId = uid,
                qrToken = token,
                qrExpiresAtMillis = expiresAt,
                isActive = true,
                verifiedByAdmin = false,
                createdAt = null,
                updatedAt = null
            )
        }
        docRef.set(toSave).await()
        docRef.get().await().toObject(WorkplaceQr::class.java, ServerTimestampBehavior.ESTIMATE)
            ?: toSave.copy(id = docRef.id)
    }

    // --- Companies & Jobs (Secondary Services) ---

    fun observeApprovedCompanies(): Flow<List<CompanyProfile>> =
        db.collection("companies")
            .whereEqualTo("verificationStatus", "APPROVED")
            .snapshots()
            .map { it.toObjects(CompanyProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "companies")
                throw error
            }

    fun observeMyCompanies(ownerId: String): Flow<List<CompanyProfile>> =
        db.collection("companies")
            .whereEqualTo("ownerId", ownerId)
            .snapshots()
            .map { it.toObjects(CompanyProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "companies")
                throw error
            }

    suspend fun registerCompany(company: CompanyProfile): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("companies").document()
        val toSave = company.copy(
            id = "",
            ownerId = uid,
            verificationStatus = "PENDING",
            createdAt = null,
            updatedAt = null
        )
        docRef.set(toSave).await()
        docRef.id
    }

    fun observeOpenJobs(): Flow<List<JobVacancy>> =
        db.collection("jobs")
            .whereEqualTo("status", "OPEN")
            .snapshots()
            .map { it.toObjects(JobVacancy::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "jobs")
                throw error
            }

    fun observeMyPostedJobs(ownerId: String): Flow<List<JobVacancy>> =
        db.collection("jobs")
            .whereEqualTo("ownerId", ownerId)
            .snapshots()
            .map { it.toObjects(JobVacancy::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "jobs")
                throw error
            }

    suspend fun postJobVacancy(job: JobVacancy): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("jobs").document()
        val toSave = job.copy(
            id = "",
            ownerId = uid,
            status = "OPEN",
            reportedCount = 0L,
            createdAt = null,
            updatedAt = null
        )
        docRef.set(toSave).await()
        docRef.id
    }

    suspend fun closeJobVacancy(job: JobVacancy): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(job.ownerId == uid)
        db.collection("jobs").document(job.id).update(
            mapOf(
                "status" to if (job.status == "OPEN") "CLOSED" else "OPEN",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun observeMyApplications(applicantId: String): Flow<List<JobApplication>> =
        db.collection("job_applications")
            .whereEqualTo("applicantId", applicantId)
            .snapshots()
            .map { it.toObjects(JobApplication::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "job_applications")
                throw error
            }

    fun observeIncomingApplications(jobOwnerId: String): Flow<List<JobApplication>> =
        db.collection("job_applications")
            .whereEqualTo("jobOwnerId", jobOwnerId)
            .snapshots()
            .map { it.toObjects(JobApplication::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "job_applications")
                throw error
            }

    suspend fun applyForJob(job: JobVacancy, profile: UserProfile?): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("job_applications").document()
        val app = JobApplication(
            applicantId = uid,
            applicantName = profile?.fullName?.ifBlank { "Worker" } ?: "Worker",
            applicantEmployeeId = profile?.employeeId ?: "",
            jobId = job.id,
            jobOwnerId = job.ownerId,
            companyName = job.companyName,
            jobTitle = job.title,
            tradeCategory = job.tradeCategory,
            status = "APPLIED",
            note = "Applied via MY DUTY IN NCR"
        )
        docRef.set(app).await()
        addNotificationInternal(
            uid,
            "Job Application Sent",
            "Applied for ${job.title} at ${job.companyName}.",
            "JOB"
        )
        docRef.id
    }

    suspend fun updateApplicationStatus(application: JobApplication, newStatus: String, note: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("job_applications").document(application.id).update(
            mapOf(
                "status" to newStatus,
                "note" to note.take(450),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun reportJob(jobId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("jobs").document(jobId).update(
            mapOf(
                "reportedCount" to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        val reportRef = db.collection("complaints_reports").document()
        reportRef.set(
            ComplaintReport(
                reporterId = uid,
                targetType = "JOB",
                targetId = jobId,
                subject = "Reported Job Vacancy",
                details = "Worker flagged job listing $jobId for review.",
                status = "OPEN"
            )
        ).await()
    }

    // --- Labour Complaints / Support Reports ---

    fun observeMyComplaints(userId: String): Flow<List<ComplaintReport>> =
        db.collection("complaints_reports")
            .whereEqualTo("reporterId", userId)
            .snapshots()
            .map { it.toObjects(ComplaintReport::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "complaints_reports")
                throw error
            }

    suspend fun submitComplaintReport(report: ComplaintReport): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("complaints_reports").document()
        docRef.set(report.copy(id = "", reporterId = uid, status = "OPEN", createdAt = null, updatedAt = null)).await()
        docRef.id
    }

    // --- Admin Panel Queries (Server-verified via isAdmin() in Firestore Rules) ---

    suspend fun adminFetchPlatformOverview(): Result<AdminOverviewData> = runCatching {
        requireUserId()
        val users = db.collection("users").get().await()
            .toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
        val companies = db.collection("companies").get().await()
            .toObjects(CompanyProfile::class.java, ServerTimestampBehavior.ESTIMATE)
        val workplaces = db.collection("workplaces").get().await()
            .toObjects(WorkplaceQr::class.java, ServerTimestampBehavior.ESTIMATE)
        val jobs = db.collection("jobs").get().await()
            .toObjects(JobVacancy::class.java, ServerTimestampBehavior.ESTIMATE)
        val reports = db.collection("complaints_reports").get().await()
            .toObjects(ComplaintReport::class.java, ServerTimestampBehavior.ESTIMATE)
        AdminOverviewData(users, companies, workplaces, jobs, reports)
    }

    suspend fun adminUpdateCompanyStatus(companyId: String, status: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("companies").document(companyId).update(
            mapOf("verificationStatus" to status, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun adminVerifyWorkplace(workplaceId: String, verified: Boolean): Result<Unit> = runCatching {
        requireUserId()
        db.collection("workplaces").document(workplaceId).update(
            mapOf("verifiedByAdmin" to verified, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun adminToggleUserSuspension(userId: String, suspended: Boolean): Result<Unit> = runCatching {
        requireUserId()
        db.collection("users").document(userId).update(
            mapOf("accountSuspended" to suspended, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun adminUpdateReportStatus(reportId: String, status: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("complaints_reports").document(reportId).update(
            mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }
}

data class AdminOverviewData(
    val users: List<UserProfile> = emptyList(),
    val companies: List<CompanyProfile> = emptyList(),
    val workplaces: List<WorkplaceQr> = emptyList(),
    val jobs: List<JobVacancy> = emptyList(),
    val reports: List<ComplaintReport> = emptyList()
)
