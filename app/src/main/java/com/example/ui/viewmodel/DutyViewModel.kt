package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminOverviewData
import com.example.data.DutyRepository
import com.example.domain.SalaryBreakdown
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
import com.example.model.PayrollRecord
import com.example.model.UserProfile
import com.example.model.WorkplaceQr
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiResult<out T> {
    data object Loading : UiResult<Nothing>
    data class Success<T>(val data: T) : UiResult<T>
    data class Error(val message: String) : UiResult<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class DutyViewModel(
    private val repository: DutyRepository,
    val currentUserId: String
) : ViewModel() {

    private val _selectedMonthKey = MutableStateFlow(currentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _adminOverview = MutableStateFlow<AdminOverviewData?>(null)
    val adminOverview: StateFlow<AdminOverviewData?> = _adminOverview.asStateFlow()

    val userProfileState: StateFlow<UiResult<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .combine(MutableStateFlow(Unit)) { p, _ -> UiResult.Success(p) as UiResult<UserProfile?> }
            .catch { emit(UiResult.Error(unwrapFirestoreMsg(it))) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiResult.Loading)

    val allAttendance: StateFlow<List<AttendanceRecord>> =
        repository.observeAllAttendance(currentUserId)
            .catch { Log.w(TAG, "Attendance stream error", it); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val monthlyAttendance: StateFlow<List<AttendanceRecord>> =
        _selectedMonthKey.flatMapLatest { month ->
            repository.observeMonthlyAttendance(currentUserId, month)
                .catch { Log.w(TAG, "Monthly attendance error", it); emit(emptyList()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val facilities: StateFlow<List<FacilityItem>> =
        repository.observeFacilities(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val payrollRecords: StateFlow<List<PayrollRecord>> =
        repository.observePayrollRecords(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val disputes: StateFlow<List<AttendanceDispute>> =
        repository.observeDisputes(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> =
        repository.observeNotifications(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeWorkplaces: StateFlow<List<WorkplaceQr>> =
        repository.observeActiveWorkplaces()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myWorkplaces: StateFlow<List<WorkplaceQr>> =
        repository.observeMyWorkplaces(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val approvedCompanies: StateFlow<List<CompanyProfile>> =
        repository.observeApprovedCompanies()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myCompanies: StateFlow<List<CompanyProfile>> =
        repository.observeMyCompanies(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val openJobs: StateFlow<List<JobVacancy>> =
        repository.observeOpenJobs()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myPostedJobs: StateFlow<List<JobVacancy>> =
        repository.observeMyPostedJobs(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myApplications: StateFlow<List<JobApplication>> =
        repository.observeMyApplications(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val incomingApplications: StateFlow<List<JobApplication>> =
        repository.observeIncomingApplications(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myComplaints: StateFlow<List<ComplaintReport>> =
        repository.observeMyComplaints(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val currentSalaryBreakdown: StateFlow<SalaryBreakdown> = combine(
        _selectedMonthKey,
        userProfileState,
        monthlyAttendance,
        facilities,
        payrollRecords
    ) { month, profileRes, att, facs, payrolls ->
        val profile = (profileRes as? UiResult.Success)?.data ?: UserProfile(userId = currentUserId)
        val saved = payrolls.firstOrNull { it.monthKey == month }
        SalaryCalculator.calculateMonthlySalary(
            monthKey = month,
            profile = profile,
            attendanceRecords = att,
            facilities = facs,
            savedPayroll = saved,
            daysInMonth = daysInMonthKey(month)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        SalaryCalculator.calculateMonthlySalary(
            currentMonthKey(),
            UserProfile(userId = currentUserId),
            emptyList(),
            emptyList()
        )
    )

    init {
        viewModelScope.launch {
            _isAdmin.value = repository.checkIsAdmin(currentUserId)
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun selectMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
    }

    fun stepMonth(delta: Int) {
        val parts = _selectedMonthKey.value.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        _selectedMonthKey.value = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }

    fun saveProfile(profile: UserProfile, isHindi: Boolean, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
                .onSuccess {
                    repository.ensureDefaultFacilitiesIfEmpty(currentUserId, isHindi)
                    _actionMessage.value = if (isHindi) "प्रोफाइल सुरक्षित किया गया" else "Profile saved successfully"
                    onSaved()
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to save profile" }
        }
    }

    fun punchIn(profile: UserProfile, workplace: WorkplaceQr, selfieUri: String, isHindi: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val dateKey = todayDateKey()
            val monthKey = currentMonthKey()
            repository.punchIn(profile, dateKey, monthKey, workplace, selfieUri)
                .onSuccess {
                    _actionMessage.value = if (isHindi) "पंच-इन सफल! ड्यूटी शुरू।" else "Punch-In successful! Duty started."
                    onSuccess()
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Punch-In failed" }
        }
    }

    fun punchOut(profile: UserProfile, workplace: WorkplaceQr, selfieUri: String, isHindi: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val dateKey = todayDateKey()
            repository.punchOut(profile, dateKey, workplace, selfieUri)
                .onSuccess { rec ->
                    val otH = String.format(Locale.US, "%.1f", rec.overtimeMinutes / 60.0)
                    _actionMessage.value = if (isHindi) "पंच-आउट सफल! ओवरटाइम: ${otH}h" else "Punch-Out complete! OT: ${otH}h"
                    onSuccess()
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Punch-Out failed" }
        }
    }

    fun saveDayStatus(dateKey: String, monthKey: String, status: AttendanceStatus, note: String) {
        viewModelScope.launch {
            repository.saveDayStatus(currentUserId, dateKey, monthKey, status, note)
                .onSuccess { _actionMessage.value = "Updated $dateKey -> ${status.code}" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun saveFacility(item: FacilityItem) {
        viewModelScope.launch {
            repository.saveFacility(item)
                .onSuccess { _actionMessage.value = "Facility updated: ${item.name}" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun savePayrollRecord(record: PayrollRecord) {
        viewModelScope.launch {
            repository.savePayrollRecord(record)
                .onSuccess { _actionMessage.value = "Payroll record saved for ${record.monthKey}" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun submitDispute(dispute: AttendanceDispute, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.submitDispute(dispute)
                .onSuccess {
                    _actionMessage.value = "Correction request submitted"
                    onDone()
                }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(currentUserId, id)
        }
    }

    fun createOrRotateWorkplace(workplace: WorkplaceQr, onDone: (WorkplaceQr) -> Unit = {}) {
        viewModelScope.launch {
            repository.createOrRotateWorkplaceQr(workplace)
                .onSuccess {
                    _actionMessage.value = "Workplace QR generated & activated"
                    onDone(it)
                }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun registerCompany(company: CompanyProfile, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.registerCompany(company)
                .onSuccess {
                    _actionMessage.value = "Company submitted for verification"
                    onDone()
                }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun postJob(job: JobVacancy, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.postJobVacancy(job)
                .onSuccess {
                    _actionMessage.value = "Job vacancy posted"
                    onDone()
                }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun toggleJobStatus(job: JobVacancy) {
        viewModelScope.launch {
            repository.closeJobVacancy(job)
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun applyForJob(job: JobVacancy, profile: UserProfile?) {
        viewModelScope.launch {
            repository.applyForJob(job, profile)
                .onSuccess { _actionMessage.value = "Application submitted to ${job.companyName}" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun updateApplicationStatus(app: JobApplication, status: String, note: String) {
        viewModelScope.launch {
            repository.updateApplicationStatus(app, status, note)
                .onSuccess { _actionMessage.value = "Application marked $status" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun reportJob(jobId: String) {
        viewModelScope.launch {
            repository.reportJob(jobId)
                .onSuccess { _actionMessage.value = "Job reported to Admin for review" }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun submitComplaint(report: ComplaintReport, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.submitComplaintReport(report)
                .onSuccess {
                    _actionMessage.value = "Complaint submitted"
                    onDone()
                }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun loadAdminOverview() {
        viewModelScope.launch {
            repository.adminFetchPlatformOverview()
                .onSuccess { _adminOverview.value = it }
                .onFailure { _actionMessage.value = unwrapFirestoreMsg(it) }
        }
    }

    fun adminSetCompanyStatus(companyId: String, status: String) {
        viewModelScope.launch {
            repository.adminUpdateCompanyStatus(companyId, status)
                .onSuccess { loadAdminOverview() }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun adminVerifyWorkplace(workplaceId: String, verified: Boolean) {
        viewModelScope.launch {
            repository.adminVerifyWorkplace(workplaceId, verified)
                .onSuccess { loadAdminOverview() }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun adminToggleUserSuspension(userId: String, suspended: Boolean) {
        viewModelScope.launch {
            repository.adminToggleUserSuspension(userId, suspended)
                .onSuccess { loadAdminOverview() }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun adminUpdateReportStatus(reportId: String, status: String) {
        viewModelScope.launch {
            repository.adminUpdateReportStatus(reportId, status)
                .onSuccess { loadAdminOverview() }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteWorkerAccount(currentUserId)
                .onSuccess { onDeleted() }
                .onFailure { _actionMessage.value = it.localizedMessage }
        }
    }

    private fun unwrapFirestoreMsg(error: Throwable): String {
        val ffe = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (ffe == null && error is CancellationException) throw error
        return (ffe ?: error).localizedMessage ?: "Data sync error"
    }

    companion object {
        private const val TAG = "DutyViewModel"

        fun todayDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        fun currentMonthKey(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

        fun daysInMonthKey(monthKey: String): Int {
            val parts = monthKey.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: return 30
            val month = (parts.getOrNull(1)?.toIntOrNull() ?: return 30) - 1
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }
}
