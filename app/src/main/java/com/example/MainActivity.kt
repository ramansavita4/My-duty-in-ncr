package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.DutyRepository
import com.example.localization.LanguageStore
import com.example.localization.LocalStrings
import com.example.localization.NotificationPreferences
import com.example.localization.stringsFor
import com.example.model.PayrollRecord
import com.example.model.UserProfile
import com.example.ui.attendance.QrSelfieAttendanceDialog
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOut
import com.example.ui.calendar.DutyCalendarScreen
import com.example.ui.components.MyDutyTopBar
import com.example.ui.home.HomeDashboardScreen
import com.example.ui.onboarding.DigitalIdentityCardDialog
import com.example.ui.onboarding.ProfileRegistrationDialog
import com.example.ui.pfesi.PfEsiTrackerScreen
import com.example.ui.salary.SalaryScreen
import com.example.ui.services.MoreSection
import com.example.ui.services.MoreServicesScreen
import com.example.ui.summary.MonthlySummaryDashboardScreen
import com.example.ui.theme.MyDutyInNcrTheme
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TealSecondary
import com.example.ui.viewmodel.DutyViewModel
import com.example.ui.viewmodel.UiResult
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_CALENDAR = "calendar"
const val ROUTE_SALARY = "salary"
const val ROUTE_PF_ESI = "pf_esi"
const val ROUTE_MORE = "more"
const val ROUTE_SUMMARY = "summary"

private fun FirebaseAuth.authStateFlow(): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser?.uid)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContext = applicationContext
            val languageStore = remember { LanguageStore(appContext) }
            val language by languageStore.languageFlow.collectAsStateWithLifecycle(initialValue = "hi")
            val notifPrefs by languageStore.notificationPrefsFlow.collectAsStateWithLifecycle(
                initialValue = NotificationPreferences()
            )
            val strings = remember(language) { stringsFor(language) }
            val scope = rememberCoroutineScope()
            val credentialManager = remember(this) { CredentialManager.create(this) }

            val authFlow = remember { Firebase.auth.authStateFlow() }
            val currentUserId by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser?.uid)

            LaunchedEffect(Unit) {
                attemptAutoSignIn(
                    context = this@MainActivity,
                    credentialManager = credentialManager,
                    onAuthSuccess = {},
                    onUnauthenticated = {},
                    scope = scope
                )
            }

            CompositionLocalProvider(LocalStrings provides strings) {
                MyDutyInNcrTheme {
                    val uid = currentUserId
                    if (uid == null) {
                        AuthScreen(
                            onToggleLanguage = {
                                scope.launch { languageStore.toggleLanguage(language) }
                            },
                            onAuthSuccess = {}
                        )
                    } else {
                        key(uid) {
                            MainDutyApp(
                                currentUserId = uid,
                                notifPrefs = notifPrefs,
                                onToggleLanguage = {
                                    scope.launch { languageStore.toggleLanguage(language) }
                                },
                                onUpdateNotifPrefs = { prefs ->
                                    scope.launch { languageStore.updateNotificationPrefs(prefs) }
                                },
                                onSignOut = {
                                    signOut(credentialManager = credentialManager, scope = scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDutyApp(
    currentUserId: String,
    notifPrefs: NotificationPreferences,
    onToggleLanguage: () -> Unit,
    onUpdateNotifPrefs: (NotificationPreferences) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    val viewModel: DutyViewModel = viewModel(
        key = currentUserId,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = DutyRepository(context.applicationContext)
                return DutyViewModel(repo, currentUserId) as T
            }
        }
    )

    val profileResult by viewModel.userProfileState.collectAsStateWithLifecycle()
    val profile = (profileResult as? UiResult.Success)?.data
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val monthlyAttendance by viewModel.monthlyAttendance.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
    val facilities by viewModel.facilities.collectAsStateWithLifecycle()
    val payrollRecords by viewModel.payrollRecords.collectAsStateWithLifecycle()
    val salaryBreakdown by viewModel.currentSalaryBreakdown.collectAsStateWithLifecycle()
    val disputes by viewModel.disputes.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val activeWorkplaces by viewModel.activeWorkplaces.collectAsStateWithLifecycle()
    val myWorkplaces by viewModel.myWorkplaces.collectAsStateWithLifecycle()
    val myCompanies by viewModel.myCompanies.collectAsStateWithLifecycle()
    val openJobs by viewModel.openJobs.collectAsStateWithLifecycle()
    val myPostedJobs by viewModel.myPostedJobs.collectAsStateWithLifecycle()
    val myApplications by viewModel.myApplications.collectAsStateWithLifecycle()
    val incomingApplications by viewModel.incomingApplications.collectAsStateWithLifecycle()
    val myComplaints by viewModel.myComplaints.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val adminOverview by viewModel.adminOverview.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    var showProfileRegDialog by remember { mutableStateOf(false) }
    var showDigitalIdDialog by remember { mutableStateOf(false) }
    var qrAttendanceModeIsOut by remember { mutableStateOf<Boolean?>(null) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var pendingMoreSection by remember { mutableStateOf(MoreSection.HUB) }
    var pendingDisputeDate by remember { mutableStateOf("") }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val tabs = listOf(
        NavTab(ROUTE_HOME, strings.navHome, Icons.Default.Home, "nav_home"),
        NavTab(ROUTE_CALENDAR, strings.navCalendar, Icons.Default.CalendarMonth, "nav_calendar"),
        NavTab(ROUTE_SALARY, strings.navSalary, Icons.Default.AccountBalanceWallet, "nav_salary"),
        NavTab(ROUTE_PF_ESI, strings.navPfEsi, Icons.Default.VerifiedUser, "nav_pf_esi"),
        NavTab(ROUTE_MORE, strings.navMore, Icons.Default.GridView, "nav_more")
    )

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val todayKey = remember { DutyViewModel.todayDateKey() }
    val todayRecord = remember(allAttendance, todayKey) {
        allAttendance.firstOrNull { it.dateKey == todayKey }
    }
    val allWorkplaces = remember(activeWorkplaces, myWorkplaces) {
        (activeWorkplaces + myWorkplaces).distinctBy { it.id }
    }

    Scaffold(
        topBar = {
            MyDutyTopBar(
                unreadNotifications = unreadCount,
                onToggleLanguage = onToggleLanguage,
                onNotificationsClick = { showNotificationsSheet = true },
                onProfileClick = { showAccountSheet = true }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealSecondary,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (profileResult is UiResult.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TealSecondary)
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(ROUTE_HOME) {
                    HomeDashboardScreen(
                        profile = profile,
                        todayRecord = todayRecord,
                        selectedMonthKey = selectedMonthKey,
                        monthlyAttendance = monthlyAttendance,
                        salaryBreakdown = salaryBreakdown,
                        onOpenRegistration = { showProfileRegDialog = true },
                        onPunchInClick = { qrAttendanceModeIsOut = false },
                        onPunchOutClick = { qrAttendanceModeIsOut = true },
                        onStepMonth = { delta -> viewModel.stepMonth(delta) },
                        onSelectMonthYear = { monthKey -> viewModel.selectMonth(monthKey) },
                        onOpenCalendarTab = { navigateToTab(ROUTE_CALENDAR) },
                        onOpenMonthlySummary = {
                            navController.navigate(ROUTE_SUMMARY) { launchSingleTop = true }
                        }
                    )
                }

                composable(ROUTE_CALENDAR) {
                    DutyCalendarScreen(
                        selectedMonthKey = selectedMonthKey,
                        attendanceRecords = monthlyAttendance,
                        salaryBreakdown = salaryBreakdown,
                        weeklyOffDay = profile?.weeklyOffDay ?: "SUNDAY",
                        onStepMonth = { delta -> viewModel.stepMonth(delta) },
                        onSelectMonthYear = { monthKey -> viewModel.selectMonth(monthKey) },
                        onUpdateDayStatus = { dateKey, monthKey, status, note ->
                            viewModel.saveDayStatus(dateKey, monthKey, status, note)
                        },
                        onRaiseDisputeForDate = { dateKey ->
                            pendingDisputeDate = dateKey
                            pendingMoreSection = MoreSection.DISPUTES
                            navigateToTab(ROUTE_MORE)
                        }
                    )
                }

                composable(ROUTE_SALARY) {
                    SalaryScreen(
                        profile = profile,
                        breakdown = salaryBreakdown,
                        payrollRecords = payrollRecords,
                        onChangeCalculationMethod = { method ->
                            val base = profile ?: UserProfile(userId = currentUserId)
                            viewModel.saveProfile(base.copy(calculationMethod = method.key), strings.isHindi)
                        },
                        onSaveMonthlyPayrollAdjustments = { rec ->
                            viewModel.savePayrollRecord(rec)
                        },
                        onNotify = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                        onOpenMonthlySummary = {
                            navController.navigate(ROUTE_SUMMARY) { launchSingleTop = true }
                        }
                    )
                }

                composable(ROUTE_PF_ESI) {
                    PfEsiTrackerScreen(
                        profile = profile,
                        breakdown = salaryBreakdown,
                        payrollRecords = payrollRecords,
                        onUpdateUanEsic = { uan, esic ->
                            val base = profile ?: UserProfile(userId = currentUserId)
                            viewModel.saveProfile(
                                base.copy(uanNumber = uan, esicNumber = esic),
                                strings.isHindi
                            )
                        },
                        onMarkStatementVerified = { monthKey, verifyPf, verifyEsi ->
                            val existing = payrollRecords.firstOrNull { it.monthKey == monthKey }
                            val updated = (existing ?: PayrollRecord(monthKey = monthKey)).copy(
                                monthKey = monthKey,
                                basicEarned = salaryBreakdown.basicEarned,
                                daEarned = salaryBreakdown.daEarned,
                                hraEarned = salaryBreakdown.hraEarned,
                                otherAllowancesEarned = salaryBreakdown.otherAllowancesEarned,
                                overtimeEarnings = salaryBreakdown.overtimeEarnings,
                                grossEarned = salaryBreakdown.grossEarned,
                                employeePf = salaryBreakdown.employeePf,
                                employerPf = salaryBreakdown.employerPfEpf,
                                epsContribution = salaryBreakdown.employerEps,
                                edliContribution = salaryBreakdown.employerEdli,
                                employeeEsi = salaryBreakdown.employeeEsi,
                                employerEsi = salaryBreakdown.employerEsi,
                                netEstimatedSalary = salaryBreakdown.netEstimatedSalary,
                                pfVerifiedStatus = if (verifyPf) "VERIFIED_BY_STATEMENT" else (existing?.pfVerifiedStatus ?: "ESTIMATED"),
                                esiVerifiedStatus = if (verifyEsi) "VERIFIED_BY_STATEMENT" else (existing?.esiVerifiedStatus ?: "ESTIMATED")
                            )
                            viewModel.savePayrollRecord(updated)
                        }
                    )
                }

                composable(ROUTE_MORE) {
                    MoreServicesScreen(
                        profile = profile,
                        isAdmin = isAdmin,
                        initialSection = pendingMoreSection,
                        initialDisputeDate = pendingDisputeDate,
                        myWorkplaces = myWorkplaces,
                        activeWorkplaces = activeWorkplaces,
                        disputes = disputes,
                        myCompanies = myCompanies,
                        myPostedJobs = myPostedJobs,
                        openJobs = openJobs,
                        myApplications = myApplications,
                        incomingApplications = incomingApplications,
                        notifications = notifications,
                        notifPrefs = notifPrefs,
                        myComplaints = myComplaints,
                        adminOverview = adminOverview,
                        onSectionConsumed = { pendingMoreSection = MoreSection.HUB },
                        onOpenDigitalIdCard = { showDigitalIdDialog = true },
                        onOpenRegistration = { showProfileRegDialog = true },
                        onCreateOrRotateWorkplace = { wp -> viewModel.createOrRotateWorkplace(wp) },
                        onSubmitDispute = { dispute, done -> viewModel.submitDispute(dispute, done) },
                        onRegisterCompany = { co, done -> viewModel.registerCompany(co, done) },
                        onPostJob = { job, done -> viewModel.postJob(job, done) },
                        onToggleJobStatus = { job -> viewModel.toggleJobStatus(job) },
                        onApplyJob = { job -> viewModel.applyForJob(job, profile) },
                        onUpdateApplicationStatus = { app, st, note -> viewModel.updateApplicationStatus(app, st, note) },
                        onReportJob = { jobId -> viewModel.reportJob(jobId) },
                        onMarkNotificationRead = { id -> viewModel.markNotificationRead(id) },
                        onUpdateNotifPrefs = onUpdateNotifPrefs,
                        onSubmitComplaint = { rep, done -> viewModel.submitComplaint(rep, done) },
                        onLoadAdminOverview = { viewModel.loadAdminOverview() },
                        onToggleUserSuspension = { uid, susp -> viewModel.adminToggleUserSuspension(uid, susp) },
                        onSetCompanyStatus = { cid, st -> viewModel.adminSetCompanyStatus(cid, st) },
                        onVerifyWorkplace = { wid, ver -> viewModel.adminVerifyWorkplace(wid, ver) },
                        onUpdateReportStatus = { rid, st -> viewModel.adminUpdateReportStatus(rid, st) },
                        onSignOut = onSignOut,
                        onDeleteAccount = {
                            viewModel.deleteAccount { onSignOut() }
                        }
                    )
                }

                composable(ROUTE_SUMMARY) {
                    MonthlySummaryDashboardScreen(
                        selectedMonthKey = selectedMonthKey,
                        profile = profile,
                        attendanceRecords = monthlyAttendance,
                        salaryBreakdown = salaryBreakdown,
                        daysInMonth = DutyViewModel.daysInMonthKey(selectedMonthKey),
                        onStepMonth = { delta -> viewModel.stepMonth(delta) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showProfileRegDialog) {
        ProfileRegistrationDialog(
            initialProfile = profile,
            facilities = facilities,
            workplaces = allWorkplaces,
            onDismiss = { showProfileRegDialog = false },
            onSaveProfile = { updated ->
                viewModel.saveProfile(updated, strings.isHindi) {
                    showProfileRegDialog = false
                }
            },
            onSaveFacility = { item -> viewModel.saveFacility(item) }
        )
    }

    if (showDigitalIdDialog) {
        DigitalIdentityCardDialog(
            profile = profile,
            onDismiss = { showDigitalIdDialog = false }
        )
    }

    qrAttendanceModeIsOut?.let { isPunchOut ->
        QrSelfieAttendanceDialog(
            isPunchOut = isPunchOut,
            assignedWorkplaceId = profile?.workplaceId ?: "",
            registeredWorkplaces = allWorkplaces,
            onDismiss = { qrAttendanceModeIsOut = null },
            onCompleted = { workplace, selfieUri ->
                val currentProfile = profile ?: UserProfile(userId = currentUserId)
                if (isPunchOut) {
                    viewModel.punchOut(currentProfile, workplace, selfieUri, strings.isHindi) {
                        qrAttendanceModeIsOut = null
                    }
                } else {
                    viewModel.punchIn(currentProfile, workplace, selfieUri, strings.isHindi) {
                        qrAttendanceModeIsOut = null
                    }
                }
            },
            onManualCorrectionFallback = {
                qrAttendanceModeIsOut = null
                pendingDisputeDate = todayKey
                pendingMoreSection = MoreSection.DISPUTES
                navigateToTab(ROUTE_MORE)
            }
        )
    }

    if (showAccountSheet) {
        val firebaseUser = Firebase.auth.currentUser
        ModalBottomSheet(onDismissRequest = { showAccountSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = profile?.fullName?.ifBlank { firebaseUser?.displayName ?: "NCR Worker" } ?: (firebaseUser?.displayName ?: "NCR Worker"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${firebaseUser?.email ?: ""} • ID: ${profile?.employeeId?.ifBlank { "Pending" } ?: "Pending"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showAccountSheet = false
                            showProfileRegDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isHindi) "प्रोफाइल" else "Edit Profile")
                    }
                    OutlinedButton(
                        onClick = {
                            showAccountSheet = false
                            showDigitalIdDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ID Card")
                    }
                }
                OutlinedButton(
                    onClick = {
                        showAccountSheet = false
                        onSignOut()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.signOut)
                }
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(onDismissRequest = { showNotificationsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.notificationPrefs,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (notifications.isEmpty()) {
                    Text(
                        text = if (strings.isHindi) "अभी कोई नई सूचना नहीं है।" else "No notifications yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    notifications.take(5).forEach { n ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(n.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(n.message, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        showNotificationsSheet = false
                        pendingMoreSection = MoreSection.NOTIFICATIONS
                        navigateToTab(ROUTE_MORE)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                ) {
                    Text(if (strings.isHindi) "सभी सूचनाएं व सेटिंग्स देखें" else "Open Notification Settings")
                }
            }
        }
    }
}
