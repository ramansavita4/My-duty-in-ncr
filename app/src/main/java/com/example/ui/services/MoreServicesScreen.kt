package com.example.ui.services

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.AdminOverviewData
import com.example.localization.LocalStrings
import com.example.localization.NotificationPreferences
import com.example.model.AppNotification
import com.example.model.AttendanceDispute
import com.example.model.CompanyProfile
import com.example.model.ComplaintReport
import com.example.model.JobApplication
import com.example.model.JobVacancy
import com.example.model.UserProfile
import com.example.model.WorkplaceQr
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.TealSecondary

enum class MoreSection {
    HUB,
    WORKPLACE_QR,
    DISPUTES,
    COMPANY_HR,
    JOB_VACANCIES,
    ADMIN_PANEL,
    NOTIFICATIONS,
    LABOUR_ASSISTANCE,
    PF_ESI_GUIDE,
    HELP_SUPPORT,
    PRIVACY_TERMS
}

@Composable
fun MoreServicesScreen(
    profile: UserProfile?,
    isAdmin: Boolean,
    initialSection: MoreSection,
    initialDisputeDate: String,
    myWorkplaces: List<WorkplaceQr>,
    activeWorkplaces: List<WorkplaceQr>,
    disputes: List<AttendanceDispute>,
    myCompanies: List<CompanyProfile>,
    myPostedJobs: List<JobVacancy>,
    openJobs: List<JobVacancy>,
    myApplications: List<JobApplication>,
    incomingApplications: List<JobApplication>,
    notifications: List<AppNotification>,
    notifPrefs: NotificationPreferences,
    myComplaints: List<ComplaintReport>,
    adminOverview: AdminOverviewData?,
    onSectionConsumed: () -> Unit,
    onOpenDigitalIdCard: () -> Unit,
    onOpenRegistration: () -> Unit,
    onCreateOrRotateWorkplace: (WorkplaceQr) -> Unit,
    onSubmitDispute: (AttendanceDispute, () -> Unit) -> Unit,
    onRegisterCompany: (CompanyProfile, () -> Unit) -> Unit,
    onPostJob: (JobVacancy, () -> Unit) -> Unit,
    onToggleJobStatus: (JobVacancy) -> Unit,
    onApplyJob: (JobVacancy) -> Unit,
    onUpdateApplicationStatus: (JobApplication, String, String) -> Unit,
    onReportJob: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onUpdateNotifPrefs: (NotificationPreferences) -> Unit,
    onSubmitComplaint: (ComplaintReport, () -> Unit) -> Unit,
    onLoadAdminOverview: () -> Unit,
    onToggleUserSuspension: (String, Boolean) -> Unit,
    onSetCompanyStatus: (String, String) -> Unit,
    onVerifyWorkplace: (String, Boolean) -> Unit,
    onUpdateReportStatus: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val strings = LocalStrings.current
    var activeSection by remember { mutableStateOf(initialSection) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(initialSection) {
        if (initialSection != MoreSection.HUB) {
            activeSection = initialSection
            onSectionConsumed()
        }
    }

    if (activeSection != MoreSection.HUB) {
        BackHandler {
            activeSection = MoreSection.HUB
        }
    }

    if (activeSection == MoreSection.HUB) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = strings.moreServicesTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            }

            item {
                ServiceMenuItem(
                    icon = Icons.Default.Badge,
                    title = strings.digitalIdCard,
                    subtitle = profile?.employeeId?.ifBlank { "MY DUTY IN NCR ID" } ?: "MY DUTY IN NCR ID",
                    onClick = onOpenDigitalIdCard,
                    testTag = "service_digital_id"
                )
                ServiceMenuItem(
                    icon = Icons.Default.Edit,
                    title = strings.editProfileFacilities,
                    subtitle = if (strings.isHindi) "प्रोफाइल, वेतन सेटिंग्स व कैंटीन/बस सुविधाएं" else "Personal, Employment, Salary & Facility Rules",
                    onClick = onOpenRegistration,
                    testTag = "service_edit_profile"
                )
                ServiceMenuItem(
                    icon = Icons.Default.QrCode2,
                    title = strings.workplaceQrManager,
                    subtitle = if (strings.isHindi) "फैक्ट्री QR जनरेट और सत्यापित करें" else "Create & rotate company attendance QR codes",
                    onClick = { activeSection = MoreSection.WORKPLACE_QR },
                    testTag = "service_workplace_qr"
                )
                ServiceMenuItem(
                    icon = Icons.Default.ReportProblem,
                    title = strings.attendanceDisputes,
                    subtitle = if (strings.isHindi) "मिसिंग पंच, ओवरटाइम व वेतन सुधार" else "Missing punch, OT & salary correction tickets",
                    onClick = { activeSection = MoreSection.DISPUTES },
                    testTag = "service_disputes"
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = if (strings.isHindi) "नौकरी और कंपनी पोर्टल (द्वितीयक सेवाएं)" else "Company HR & Job Portal (Secondary)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                ServiceMenuItem(
                    icon = Icons.Default.BusinessCenter,
                    title = strings.companyHrPortal,
                    subtitle = if (strings.isHindi) "कंपनी रजिस्ट्रेशन, भर्ती पोस्ट व आवेदक" else "Register factory, post vacancies & review applicants",
                    onClick = { activeSection = MoreSection.COMPANY_HR },
                    testTag = "service_company_hr"
                )
                ServiceMenuItem(
                    icon = Icons.Default.WorkOutline,
                    title = strings.jobVacancies,
                    subtitle = if (strings.isHindi) "मानेसर, गुरुग्राम, बावल, नोएडा, फरीदाबाद" else "Browse & apply for verified NCR industrial jobs",
                    onClick = { activeSection = MoreSection.JOB_VACANCIES },
                    testTag = "service_job_vacancies"
                )
                if (isAdmin) {
                    ServiceMenuItem(
                        icon = Icons.Default.AdminPanelSettings,
                        title = strings.adminControlPanel,
                        subtitle = "Verify companies, workplaces, users & reports",
                        onClick = { activeSection = MoreSection.ADMIN_PANEL },
                        testTag = "service_admin_panel"
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ServiceMenuItem(
                    icon = Icons.Default.NotificationsActive,
                    title = strings.notificationPrefs,
                    subtitle = if (strings.isHindi) "पंच, ओवरटाइम, वेतन और PF/ESI अलर्ट" else "Manage alerts & notification history",
                    onClick = { activeSection = MoreSection.NOTIFICATIONS },
                    testTag = "service_notifications"
                )
                ServiceMenuItem(
                    icon = Icons.Default.Gavel,
                    title = strings.labourAssistance,
                    subtitle = if (strings.isHindi) "श्रम सहायता व शिकायत दर्ज करें" else "Labour helpline info & submit complaint",
                    onClick = { activeSection = MoreSection.LABOUR_ASSISTANCE },
                    testTag = "service_labour_assistance"
                )
                ServiceMenuItem(
                    icon = Icons.Default.AccountBalance,
                    title = strings.pfEsiGuide,
                    subtitle = "EPFO 12% + ESIC 0.75%/3.25% rules & limits",
                    onClick = { activeSection = MoreSection.PF_ESI_GUIDE },
                    testTag = "service_pf_esi_guide"
                )
                ServiceMenuItem(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = strings.helpSupport,
                    subtitle = "FAQ & Worker Guide",
                    onClick = { activeSection = MoreSection.HELP_SUPPORT },
                    testTag = "service_help"
                )
                ServiceMenuItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "${strings.privacyPolicy} • ${strings.termsConditions}",
                    subtitle = if (strings.isHindi) "सेल्फी सहमति, डेटा सुरक्षा और नियम" else "Selfie consent, data safety & terms",
                    onClick = { activeSection = MoreSection.PRIVACY_TERMS },
                    testTag = "service_privacy_terms"
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("more_logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.signOut)
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("more_delete_account_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AbsentRed)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isHindi) "खाता हटाएं" else "Delete")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { activeSection = MoreSection.HUB },
                        modifier = Modifier.testTag("more_sub_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Services")
                    }
                    Text(
                        text = sectionTitle(activeSection, strings.isHindi),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (activeSection) {
                            MoreSection.WORKPLACE_QR -> WorkplaceQrManagerContent(
                                myWorkplaces = myWorkplaces,
                                activeWorkplaces = activeWorkplaces,
                                onCreateOrRotate = onCreateOrRotateWorkplace
                            )

                            MoreSection.DISPUTES -> DisputesContent(
                                initialDate = initialDisputeDate,
                                disputes = disputes,
                                onSubmitDispute = onSubmitDispute
                            )

                            MoreSection.COMPANY_HR -> CompanyHrPortalContent(
                                myCompanies = myCompanies,
                                myPostedJobs = myPostedJobs,
                                incomingApplications = incomingApplications,
                                onRegisterCompany = onRegisterCompany,
                                onPostJob = onPostJob,
                                onToggleJobStatus = onToggleJobStatus,
                                onUpdateApplicationStatus = onUpdateApplicationStatus
                            )

                            MoreSection.JOB_VACANCIES -> JobVacanciesContent(
                                profile = profile,
                                openJobs = openJobs,
                                myApplications = myApplications,
                                onApply = onApplyJob,
                                onReportJob = onReportJob
                            )

                            MoreSection.ADMIN_PANEL -> AdminControlPanelContent(
                                isAdmin = isAdmin,
                                overview = adminOverview,
                                onLoadOverview = onLoadAdminOverview,
                                onToggleUserSuspension = onToggleUserSuspension,
                                onSetCompanyStatus = onSetCompanyStatus,
                                onVerifyWorkplace = onVerifyWorkplace,
                                onUpdateReportStatus = onUpdateReportStatus
                            )

                            MoreSection.NOTIFICATIONS -> NotificationsSectionContent(
                                notifications = notifications,
                                prefs = notifPrefs,
                                onMarkRead = onMarkNotificationRead,
                                onUpdatePrefs = onUpdateNotifPrefs
                            )

                            MoreSection.LABOUR_ASSISTANCE -> LabourAssistanceContent(
                                complaints = myComplaints,
                                onSubmitComplaint = onSubmitComplaint
                            )

                            MoreSection.PF_ESI_GUIDE -> PfEsiInfoGuideContent()
                            MoreSection.HELP_SUPPORT -> HelpSupportContent()
                            MoreSection.PRIVACY_TERMS -> PrivacyTermsContent()
                            MoreSection.HUB -> Unit
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteAccount) },
            text = {
                Text(
                    if (strings.isHindi) "क्या आप वाकई अपना कर्मचारी खाता और सभी हाजिरी, पेरोल व प्रोफाइल डेटा स्थायी रूप से हटाना चाहते हैं?"
                    else "Are you sure you want to permanently delete your worker account and all attendance, payroll, and profile records?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed)
                ) {
                    Text(if (strings.isHindi) "स्थायी रूप से हटाएं" else "Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (strings.isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ServiceMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TealSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotificationsSectionContent(
    notifications: List<AppNotification>,
    prefs: NotificationPreferences,
    onMarkRead: (String) -> Unit,
    onUpdatePrefs: (NotificationPreferences) -> Unit
) {
    val context = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun ensureNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Notification Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            PrefSwitch("Punch-In / Punch-Out Alerts", prefs.punchAlerts) {
                if (it) ensureNotifPermission()
                onUpdatePrefs(prefs.copy(punchAlerts = it))
            }
            PrefSwitch("Overtime Alerts", prefs.overtimeAlerts) {
                if (it) ensureNotifPermission()
                onUpdatePrefs(prefs.copy(overtimeAlerts = it))
            }
            PrefSwitch("Salary Estimation Alerts", prefs.salaryAlerts) {
                if (it) ensureNotifPermission()
                onUpdatePrefs(prefs.copy(salaryAlerts = it))
            }
            PrefSwitch("PF / ESI Reminder Alerts", prefs.pfEsiAlerts) {
                if (it) ensureNotifPermission()
                onUpdatePrefs(prefs.copy(pfEsiAlerts = it))
            }
            PrefSwitch("NCR Job Alerts", prefs.jobAlerts) {
                if (it) ensureNotifPermission()
                onUpdatePrefs(prefs.copy(jobAlerts = it))
            }
        }
    }

    Text("Recent Notifications (${notifications.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    if (notifications.isEmpty()) {
        EmptyStateCard("No notifications yet.")
    } else {
        notifications.forEach { n ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!n.isRead) onMarkRead(n.id) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (n.isRead) MaterialTheme.colorScheme.surface else AmberContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(n.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(if (n.isRead) "Read" else "New", style = MaterialTheme.typography.labelSmall, color = TealSecondary)
                    }
                    Text(n.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PrefSwitch(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
private fun LabourAssistanceContent(
    complaints: List<ComplaintReport>,
    onSubmitComplaint: (ComplaintReport, () -> Unit) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("NCR Worker Labour Assistance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "Shram Suvidha / Labour Helpline: 1800-11-1144 • EPFO UAN Helpdesk: 1800-118-005 • ESIC Medical Helpline: 1800-11-2526",
                style = MaterialTheme.typography.bodySmall,
                color = TealSecondary
            )
            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Issue Subject *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details (Company / Salary / Safety issue) *") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    onSubmitComplaint(ComplaintReport(targetType = "LABOUR_ISSUE", subject = subject.trim(), details = details.trim())) {
                        subject = ""
                        details = ""
                    }
                },
                enabled = subject.isNotBlank() && details.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Submit Labour Complaint")
            }
        }
    }

    complaints.forEach { c ->
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(c.subject, fontWeight = FontWeight.Bold)
                    Text(c.status, style = MaterialTheme.typography.labelSmall, color = TealSecondary)
                }
                Text(c.details, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PfEsiInfoGuideContent() {
    InfoSectionCard(
        title = "EPFO Provident Fund (PF) Rules",
        body = "• Employee PF Contribution: 12% of Basic + DA (statutory ceiling ₹15,000/month unless voluntary higher contribution).\n• Employer Share (12%): 3.67% to EPF + 8.33% to EPS (Pension Scheme).\n• Employer EDLI Insurance: 0.50% of PF wages.\n• Note: Figures in MY DUTY IN NCR are calculated estimates until verified against your official EPFO Member Passbook."
    )
    InfoSectionCard(
        title = "ESIC Employee State Insurance Rules",
        body = "• Eligibility: Gross monthly wages up to ₹21,000 (₹25,000 for differently-abled employees).\n• Employee ESI Contribution: 0.75% of gross earned wages.\n• Employer ESI Contribution: 3.25% of gross earned wages.\n• Benefits: Medical care for employee & family, sickness benefit, maternity & disability cover."
    )
}

@Composable
private fun HelpSupportContent() {
    InfoSectionCard(
        title = "MY DUTY IN NCR — Worker Quick Guide",
        body = "1. Complete your Profile & Salary Calculation Formula (26-Day, 30-Day, Monthly Fixed, Daily Wage, Hourly, or Actual Payable).\n2. Configure Company Facilities (Tea, Lunch, Canteen, Bus) — Free facilities are never deducted.\n3. Use Home Punch-In & Punch-Out with Company QR & Live Selfie.\n4. Check Monthly Calendar for P, A, HD, WO, PL, H, M status and OT hours.\n5. Export or Print your monthly Salary Slip as PDF from the Salary tab."
    )
}

@Composable
private fun PrivacyTermsContent() {
    Surface(color = AmberContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "MY DUTY IN NCR • मेरी ड्यूटी इन NCR — Privacy & Terms",
            style = MaterialTheme.typography.titleSmall,
            color = OnAmberContainer,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp)
        )
    }
    InfoSectionCard(
        title = "Selfie & Attendance Privacy Policy",
        body = "• Selfie photographs captured during Punch-In / Punch-Out are stored under your own authenticated worker account as timestamped attendance evidence.\n• No automated facial recognition or biometric profiling is performed without explicit consent.\n• You can permanently delete your worker account and all stored attendance, payroll, and profile records anytime from More Services -> Delete Account."
    )
    InfoSectionCard(
        title = "Terms & Statutory Estimate Disclaimer",
        body = "• Salary slips, PF, and ESI calculations generated by MY DUTY IN NCR are employee record estimates based on your configured rules.\n• Always verify final deposits against official EPFO passbooks, ESIC portals, and employer-issued pay slips."
    )
}

@Composable
private fun InfoSectionCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = TealSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun sectionTitle(section: MoreSection, isHindi: Boolean): String {
    return when (section) {
        MoreSection.WORKPLACE_QR -> if (isHindi) "कार्यस्थल QR प्रबंधक" else "Workplace QR Manager"
        MoreSection.DISPUTES -> if (isHindi) "हाजिरी सुधार व विवाद" else "Attendance Disputes"
        MoreSection.COMPANY_HR -> if (isHindi) "कंपनी / फैक्ट्री HR पोर्टल" else "Company HR Portal"
        MoreSection.JOB_VACANCIES -> if (isHindi) "NCR नौकरी रिक्तियां" else "NCR Job Vacancies"
        MoreSection.ADMIN_PANEL -> if (isHindi) "एडमिन कंट्रोल पैनल" else "Admin Control Panel"
        MoreSection.NOTIFICATIONS -> if (isHindi) "सूचनाएं व प्राथमिकताएं" else "Notifications & Prefs"
        MoreSection.LABOUR_ASSISTANCE -> if (isHindi) "श्रम सहायता" else "Labour Assistance"
        MoreSection.PF_ESI_GUIDE -> if (isHindi) "PF और ESI जानकारी" else "PF & ESI Information"
        MoreSection.HELP_SUPPORT -> if (isHindi) "सहायता व समर्थन" else "Help & Support"
        MoreSection.PRIVACY_TERMS -> if (isHindi) "गोपनीयता नीति व शर्तें" else "Privacy Policy & Terms"
        MoreSection.HUB -> "MY DUTY IN NCR"
    }
}
