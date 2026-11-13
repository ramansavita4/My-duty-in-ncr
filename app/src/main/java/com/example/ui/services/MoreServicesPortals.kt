package com.example.ui.services

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AdminOverviewData
import com.example.domain.QrCodeHelper
import com.example.localization.LocalStrings
import com.example.model.AttendanceDispute
import com.example.model.CompanyProfile
import com.example.model.JobApplication
import com.example.model.JobVacancy
import com.example.model.NcrConstants
import com.example.model.UserProfile
import com.example.model.WorkplaceQr
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenBg
import com.example.ui.theme.TealSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkplaceQrManagerContent(
    myWorkplaces: List<WorkplaceQr>,
    activeWorkplaces: List<WorkplaceQr>,
    onCreateOrRotate: (WorkplaceQr) -> Unit
) {
    val strings = LocalStrings.current
    var companyName by remember { mutableStateOf("") }
    var factoryName by remember { mutableStateOf("") }
    var industrialArea by remember { mutableStateOf("IMT Manesar") }
    var address by remember { mutableStateOf("") }
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (strings.isHindi) "नया कार्यस्थल QR कोड बनाएं (30 दिन वैध)" else "Create Company Workplace QR (30-Day Expiry)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = factoryName,
                onValueChange = { factoryName = it },
                label = { Text("Factory / Unit Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NcrConstants.INDUSTRIAL_AREAS.take(6).forEach { area ->
                    FilterChip(
                        selected = industrialArea == area,
                        onClick = { industrialArea = area },
                        label = { Text(area, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Factory Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (companyName.trim().isNotEmpty() && factoryName.trim().isNotEmpty()) {
                        onCreateOrRotate(
                            WorkplaceQr(
                                companyName = companyName.trim(),
                                factoryName = factoryName.trim(),
                                industrialArea = industrialArea,
                                address = address.trim()
                            )
                        )
                        companyName = ""
                        factoryName = ""
                        address = ""
                    }
                },
                enabled = companyName.isNotBlank() && factoryName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_workplace_qr_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (strings.isHindi) "QR कोड जनरेट करें" else "Generate Workplace QR")
            }
        }
    }

    val combined = (myWorkplaces + activeWorkplaces).distinctBy { it.id }
    Text(
        text = if (strings.isHindi) "पंजीकृत कार्यस्थल QR कोड (${combined.size})" else "Registered Workplace QR Codes (${combined.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )

    if (combined.isEmpty()) {
        EmptyStateCard(
            if (strings.isHindi) "अभी कोई कार्यस्थल QR पंजीकृत नहीं है।"
            else "No workplace QR codes registered yet."
        )
    } else {
        combined.forEach { wp ->
            val payload = remember(wp.id, wp.qrToken, wp.qrExpiresAtMillis) {
                QrCodeHelper.buildQrPayload(wp)
            }
            val qrBitmap = remember(payload) { QrCodeHelper.generateQrBitmap(payload, 360) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${wp.companyName} • ${wp.factoryName}", fontWeight = FontWeight.Bold)
                            Text("${wp.industrialArea} • ${wp.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = if (wp.verifiedByAdmin) PresentGreenBg else AmberContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (wp.verifiedByAdmin) "ADMIN VERIFIED" else "ACTIVE QR",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (wp.verifiedByAdmin) PresentGreen else OnAmberContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Workplace QR for ${wp.companyName}",
                            modifier = Modifier
                                .size(170.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                    Text(
                        text = "Token: ${wp.qrToken} • Expires: ${if (wp.qrExpiresAtMillis > 0) dateFmt.format(Date(wp.qrExpiresAtMillis)) else "N/A"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (myWorkplaces.any { it.id == wp.id }) {
                        OutlinedButton(
                            onClick = { onCreateOrRotate(wp) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isHindi) "नया QR रोटेट करें" else "Rotate QR Token")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisputesContent(
    initialDate: String,
    disputes: List<AttendanceDispute>,
    onSubmitDispute: (AttendanceDispute, () -> Unit) -> Unit
) {
    val strings = LocalStrings.current
    val todayDefault = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var targetDate by remember(initialDate) { mutableStateOf(initialDate.ifBlank { todayDefault }) }
    var category by remember { mutableStateOf("MISSING_PUNCH_OUT") }
    var description by remember { mutableStateOf("") }
    var gatePass by remember { mutableStateOf("") }
    var evidenceUri by remember { mutableStateOf("") }

    val categories = listOf(
        "MISSING_PUNCH_OUT" to "Missing Punch-Out",
        "MISSING_PUNCH_IN" to "Missing Punch-In",
        "QR_SCAN_FAILED" to "QR Scan Failed",
        "OVERTIME_CORRECTION" to "Overtime Mismatch",
        "SALARY_DISCREPANCY" to "Salary Issue"
    )

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) evidenceUri = uri.toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (strings.isHindi) "नया सुधार / विवाद अनुरोध भेजें" else "Submit Correction / Dispute Request",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = targetDate,
                onValueChange = { targetDate = it },
                label = { Text("Date (YYYY-MM-DD) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { (key, label) ->
                    FilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (strings.isHindi) "समस्या का विवरण *" else "Describe Issue *") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = gatePass,
                onValueChange = { gatePass = it },
                label = { Text("Gate Pass / Supervisor Note (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (evidenceUri.isBlank()) "Attach Photo Proof (Optional)" else "Photo Attached ✓")
            }
            Button(
                onClick = {
                    if (targetDate.isNotBlank() && description.isNotBlank()) {
                        onSubmitDispute(
                            AttendanceDispute(
                                targetDate = targetDate.trim(),
                                category = category,
                                description = description.trim(),
                                gatePassNumber = gatePass.trim(),
                                evidenceUri = evidenceUri
                            )
                        ) {
                            description = ""
                            gatePass = ""
                            evidenceUri = ""
                        }
                    }
                },
                enabled = targetDate.isNotBlank() && description.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_dispute_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text(if (strings.isHindi) "अनुरोध सबमिट करें" else "Submit Request")
            }
        }
    }

    Text(
        text = if (strings.isHindi) "सबमिट किए गए अनुरोध (${disputes.size})" else "Submitted Requests (${disputes.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )

    if (disputes.isEmpty()) {
        EmptyStateCard(if (strings.isHindi) "कोई विवाद या सुधार टिकट नहीं है।" else "No dispute tickets submitted.")
    } else {
        disputes.forEach { d ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${d.targetDate} • ${d.category}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(d.status, style = MaterialTheme.typography.labelSmall, color = TealSecondary, fontWeight = FontWeight.Bold)
                    }
                    Text(d.description, style = MaterialTheme.typography.bodySmall)
                    d.auditTrail.forEach { log ->
                        Text(log, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanyHrPortalContent(
    myCompanies: List<CompanyProfile>,
    myPostedJobs: List<JobVacancy>,
    incomingApplications: List<JobApplication>,
    onRegisterCompany: (CompanyProfile, () -> Unit) -> Unit,
    onPostJob: (JobVacancy, () -> Unit) -> Unit,
    onToggleJobStatus: (JobVacancy) -> Unit,
    onUpdateApplicationStatus: (JobApplication, String, String) -> Unit
) {
    val strings = LocalStrings.current
    var hrTab by remember { mutableIntStateOf(0) }

    TabRow(selectedTabIndex = hrTab) {
        Tab(selected = hrTab == 0, onClick = { hrTab = 0 }, text = { Text("Company") })
        Tab(selected = hrTab == 1, onClick = { hrTab = 1 }, text = { Text("Post Job") })
        Tab(selected = hrTab == 2, onClick = { hrTab = 2 }, text = { Text("Applicants (${incomingApplications.size})") })
    }

    when (hrTab) {
        0 -> {
            var companyName by remember { mutableStateOf("") }
            var factoryName by remember { mutableStateOf("") }
            var area by remember { mutableStateOf("IMT Manesar") }
            var hrName by remember { mutableStateOf("") }
            var hrPhone by remember { mutableStateOf("") }
            var gstReg by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (strings.isHindi) "कंपनी / फैक्ट्री रजिस्टर करें" else "Register Company / Factory",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = factoryName, onValueChange = { factoryName = it }, label = { Text("Factory / Plant Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NcrConstants.INDUSTRIAL_AREAS.take(6).forEach { a ->
                            FilterChip(selected = area == a, onClick = { area = a }, label = { Text(a, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    OutlinedTextField(value = hrName, onValueChange = { hrName = it }, label = { Text("HR Contact Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hrPhone, onValueChange = { hrPhone = it }, label = { Text("HR Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = gstReg, onValueChange = { gstReg = it }, label = { Text("GST / Factory Reg. No.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            onRegisterCompany(
                                CompanyProfile(
                                    companyName = companyName.trim(),
                                    factoryName = factoryName.trim(),
                                    industrialArea = area,
                                    hrName = hrName.trim(),
                                    hrPhone = hrPhone.trim(),
                                    gstOrRegNumber = gstReg.trim()
                                )
                            ) {
                                companyName = ""
                                factoryName = ""
                                hrName = ""
                                hrPhone = ""
                                gstReg = ""
                            }
                        },
                        enabled = companyName.isNotBlank() && factoryName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Submit Company for Verification")
                    }
                }
            }

            myCompanies.forEach { co ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(co.companyName, fontWeight = FontWeight.Bold)
                            Text("${co.factoryName} • ${co.industrialArea}", style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(
                            color = if (co.verificationStatus == "APPROVED") PresentGreenBg else AmberContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = co.verificationStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        1 -> {
            var title by remember { mutableStateOf("") }
            var companyName by remember { mutableStateOf(myCompanies.firstOrNull()?.companyName ?: "") }
            var trade by remember { mutableStateOf("Operator") }
            var area by remember { mutableStateOf("IMT Manesar") }
            var salMin by remember { mutableStateOf("14000") }
            var salMax by remember { mutableStateOf("18000") }
            var pfProv by remember { mutableStateOf(true) }
            var esiProv by remember { mutableStateOf(true) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Post NCR Job Vacancy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Job Title * (e.g. CNC Operator)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NcrConstants.WORK_CATEGORIES.take(6).forEach { cat ->
                            FilterChip(selected = trade == cat, onClick = { trade = cat }, label = { Text(cat, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NcrConstants.INDUSTRIAL_AREAS.take(6).forEach { a ->
                            FilterChip(selected = area == a, onClick = { area = a }, label = { Text(a, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = salMin, onValueChange = { salMin = it }, label = { Text("Min ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = salMax, onValueChange = { salMax = it }, label = { Text("Max ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("PF Provided", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = pfProv, onCheckedChange = { pfProv = it })
                        Text("ESI Provided", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = esiProv, onCheckedChange = { esiProv = it })
                    }
                    Button(
                        onClick = {
                            onPostJob(
                                JobVacancy(
                                    companyId = myCompanies.firstOrNull()?.id ?: "",
                                    companyName = companyName.trim(),
                                    title = title.trim(),
                                    tradeCategory = trade,
                                    industrialArea = area,
                                    salaryMin = salMin.toDoubleOrNull() ?: 0.0,
                                    salaryMax = salMax.toDoubleOrNull() ?: 0.0,
                                    pfProvided = pfProv,
                                    esiProvided = esiProv
                                )
                            ) { title = "" }
                        },
                        enabled = companyName.isNotBlank() && title.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                    ) {
                        Text("Publish Vacancy")
                    }
                }
            }

            myPostedJobs.forEach { job ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(job.title, fontWeight = FontWeight.Bold)
                            Text("${job.companyName} • ${job.industrialArea} • ${job.status}", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { onToggleJobStatus(job) }) {
                            Text(if (job.status == "OPEN") "Close" else "Reopen", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        2 -> {
            if (incomingApplications.isEmpty()) {
                EmptyStateCard("No job applications received yet.")
            } else {
                incomingApplications.forEach { app ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${app.applicantName} (${app.applicantEmployeeId})", fontWeight = FontWeight.Bold)
                            Text("Applied for: ${app.jobTitle} • Status: ${app.status}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { onUpdateApplicationStatus(app, "SHORTLISTED", "Shortlisted by HR") }) {
                                    Text("Shortlist", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = { onUpdateApplicationStatus(app, "HIRED", "Selected by HR") },
                                    colors = ButtonDefaults.buttonColors(containerColor = PresentGreen)
                                ) {
                                    Text("Hire", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(onClick = { onUpdateApplicationStatus(app, "REJECTED", "Not matched") }) {
                                    Text("Reject", style = MaterialTheme.typography.labelSmall, color = AbsentRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobVacanciesContent(
    profile: UserProfile?,
    openJobs: List<JobVacancy>,
    myApplications: List<JobApplication>,
    onApply: (JobVacancy) -> Unit,
    onReportJob: (String) -> Unit
) {
    val strings = LocalStrings.current
    var search by remember { mutableStateOf("") }
    var areaFilter by remember { mutableStateOf("ALL") }
    var tradeFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(openJobs, search, areaFilter, tradeFilter) {
        openJobs.filter { job ->
            (search.isBlank() || job.title.contains(search, true) || job.companyName.contains(search, true)) &&
                (areaFilter == "ALL" || job.industrialArea == areaFilter) &&
                (tradeFilter == "ALL" || job.tradeCategory == tradeFilter)
        }
    }

    OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        label = { Text(if (strings.isHindi) "नौकरी या कंपनी खोजें" else "Search jobs or companies") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = areaFilter == "ALL", onClick = { areaFilter = "ALL" }, label = { Text("All NCR") })
        NcrConstants.INDUSTRIAL_AREAS.take(5).forEach { area ->
            FilterChip(
                selected = areaFilter == area,
                onClick = { areaFilter = area },
                label = { Text(area, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = tradeFilter == "ALL", onClick = { tradeFilter = "ALL" }, label = { Text("All Trades") })
        NcrConstants.WORK_CATEGORIES.take(5).forEach { trade ->
            FilterChip(
                selected = tradeFilter == trade,
                onClick = { tradeFilter = trade },
                label = { Text(trade, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }

    if (filtered.isEmpty()) {
        EmptyStateCard(
            if (strings.isHindi) "अभी इस फ़िल्टर के लिए कोई सत्यापित नौकरी रिक्ति उपलब्ध नहीं है।"
            else "No open verified NCR job vacancies match your filters right now."
        )
    } else {
        filtered.forEach { job ->
            val alreadyApplied = myApplications.any { it.jobId == job.id }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(job.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${job.companyName} • ${job.industrialArea}", style = MaterialTheme.typography.bodySmall, color = TealSecondary)
                        }
                        Icon(Icons.Default.Work, contentDescription = null, tint = NavyPrimary)
                    }
                    Text(
                        text = "Salary: ₹${job.salaryMin.toInt()} - ₹${job.salaryMax.toInt()} • ${job.tradeCategory} • ${job.workingHours}h (${job.shiftType})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "PF: ${if (job.pfProvided) "Yes" else "No"} • ESI: ${if (job.esiProvided) "Yes" else "No"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onApply(job) },
                            enabled = !alreadyApplied,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                        ) {
                            Text(if (alreadyApplied) "Applied ✓" else if (strings.isHindi) "आवेदन करें" else "Apply Now")
                        }
                        OutlinedButton(onClick = { onReportJob(job.id) }) {
                            Icon(Icons.Default.Flag, contentDescription = "Report Job", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (myApplications.isNotEmpty()) {
        HorizontalDivider()
        Text("My Applications (${myApplications.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        myApplications.forEach { app ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(app.jobTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(app.companyName, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(app.status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NavyPrimary)
                }
            }
        }
    }
}

@Composable
fun AdminControlPanelContent(
    isAdmin: Boolean,
    overview: AdminOverviewData?,
    onLoadOverview: () -> Unit,
    onToggleUserSuspension: (String, Boolean) -> Unit,
    onSetCompanyStatus: (String, String) -> Unit,
    onVerifyWorkplace: (String, Boolean) -> Unit,
    onUpdateReportStatus: (String, String) -> Unit
) {
    LaunchedEffect(isAdmin) {
        if (isAdmin) onLoadOverview()
    }

    if (!isAdmin) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("Restricted Admin Access", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Text(
                    text = "Admin controls are gated by Firebase Auth & Firestore security rules. Sign in with an authorized administrator account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        return
    }

    if (overview == null) {
        EmptyStateCard("Loading Admin Platform Overview...")
        return
    }

    var adminTab by remember { mutableIntStateOf(0) }
    TabRow(selectedTabIndex = adminTab) {
        Tab(selected = adminTab == 0, onClick = { adminTab = 0 }, text = { Text("Users (${overview.users.size})") })
        Tab(selected = adminTab == 1, onClick = { adminTab = 1 }, text = { Text("Co. (${overview.companies.size})") })
        Tab(selected = adminTab == 2, onClick = { adminTab = 2 }, text = { Text("QR (${overview.workplaces.size})") })
        Tab(selected = adminTab == 3, onClick = { adminTab = 3 }, text = { Text("Reports (${overview.reports.size})") })
    }

    when (adminTab) {
        0 -> overview.users.forEach { u ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(u.fullName.ifBlank { u.userId }, fontWeight = FontWeight.Bold)
                        Text("${u.employeeId} • ${u.companyName.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { onToggleUserSuspension(u.userId, !u.accountSuspended) }) {
                        Text(if (u.accountSuspended) "Reactivate" else "Suspend", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        1 -> overview.companies.forEach { co ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${co.companyName} (${co.verificationStatus})", fontWeight = FontWeight.Bold)
                    Text("${co.factoryName} • ${co.industrialArea}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSetCompanyStatus(co.id, "APPROVED") },
                            colors = ButtonDefaults.buttonColors(containerColor = PresentGreen)
                        ) { Text("Approve", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(onClick = { onSetCompanyStatus(co.id, "REJECTED") }) {
                            Text("Reject", style = MaterialTheme.typography.labelSmall, color = AbsentRed)
                        }
                    }
                }
            }
        }

        2 -> overview.workplaces.forEach { wp ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${wp.companyName} (${wp.factoryName})", fontWeight = FontWeight.Bold)
                        Text(if (wp.verifiedByAdmin) "Verified" else "Unverified", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = { onVerifyWorkplace(wp.id, !wp.verifiedByAdmin) }) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (wp.verifiedByAdmin) "Unverify" else "Verify", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        3 -> overview.reports.forEach { rep ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${rep.subject} [${rep.status}]", fontWeight = FontWeight.Bold)
                    Text(rep.details, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onUpdateReportStatus(rep.id, "UNDER_REVIEW") }) {
                            Text("Review", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(onClick = { onUpdateReportStatus(rep.id, "RESOLVED") }) {
                            Text("Resolve", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
