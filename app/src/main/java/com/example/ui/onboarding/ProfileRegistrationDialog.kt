package com.example.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.localization.LocalStrings
import com.example.model.CalculationMethod
import com.example.model.FacilityItem
import com.example.model.NcrConstants
import com.example.model.UserProfile
import com.example.model.WorkplaceQr
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TealSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileRegistrationDialog(
    initialProfile: UserProfile?,
    facilities: List<FacilityItem>,
    workplaces: List<WorkplaceQr>,
    onDismiss: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    onSaveFacility: (FacilityItem) -> Unit
) {
    val strings = LocalStrings.current
    val base = initialProfile ?: UserProfile()
    var tabIndex by remember { mutableIntStateOf(0) }

    // A. Personal
    var fullName by remember { mutableStateOf(base.fullName) }
    var mobile by remember { mutableStateOf(base.mobileNumber) }
    var dob by remember { mutableStateOf(base.dateOfBirth) }
    var gender by remember { mutableStateOf(base.gender.ifBlank { "Male" }) }
    var photoUri by remember { mutableStateOf(base.photoUri) }
    var address by remember { mutableStateOf(base.residentialAddress) }
    var village by remember { mutableStateOf(base.village) }
    var city by remember { mutableStateOf(base.city) }
    var district by remember { mutableStateOf(base.district) }
    var state by remember { mutableStateOf(base.state.ifBlank { "Haryana" }) }
    var pinCode by remember { mutableStateOf(base.pinCode) }

    // B. Employment
    var companyName by remember { mutableStateOf(base.companyName) }
    var factoryName by remember { mutableStateOf(base.factoryName) }
    var factoryAddress by remember { mutableStateOf(base.factoryAddress) }
    var industrialArea by remember { mutableStateOf(base.industrialArea.ifBlank { "IMT Manesar" }) }
    var department by remember { mutableStateOf(base.department) }
    var designation by remember { mutableStateOf(base.designation) }
    var workCategory by remember { mutableStateOf(base.workCategory.ifBlank { "Operator" }) }
    var dateOfJoining by remember { mutableStateOf(base.dateOfJoining) }
    var gatePass by remember { mutableStateOf(base.gatePassNumber) }
    var workerId by remember { mutableStateOf(base.workerId) }
    var prevExp by remember { mutableStateOf(base.previousExperience) }
    var hrContact by remember { mutableStateOf(base.hrContact) }
    var selectedWorkplaceId by remember { mutableStateOf(base.workplaceId) }

    // C. Salary & Duty
    var calcMethod by remember { mutableStateOf(base.calculationMethod) }
    var grossSalary by remember { mutableStateOf(if (base.monthlyGrossSalary > 0) base.monthlyGrossSalary.toString() else "") }
    var basicSalary by remember { mutableStateOf(if (base.basicSalary > 0) base.basicSalary.toString() else "") }
    var da by remember { mutableStateOf(if (base.da > 0) base.da.toString() else "") }
    var hra by remember { mutableStateOf(if (base.hra > 0) base.hra.toString() else "") }
    var otherAllow by remember { mutableStateOf(if (base.otherAllowances > 0) base.otherAllowances.toString() else "") }
    var dailyWage by remember { mutableStateOf(if (base.dailyWage > 0) base.dailyWage.toString() else "") }
    var hourlyWage by remember { mutableStateOf(if (base.hourlyWage > 0) base.hourlyWage.toString() else "") }
    var otRate by remember { mutableStateOf(if (base.overtimeRatePerHour > 0) base.overtimeRatePerHour.toString() else "") }
    var stdHours by remember { mutableStateOf(base.standardWorkingHours.toString()) }
    var shiftStart by remember { mutableStateOf(base.shiftStartTime) }
    var lunchMins by remember { mutableStateOf(base.lunchBreakMinutes.toString()) }
    var teaMins by remember { mutableStateOf(base.teaBreakMinutes.toString()) }
    var deductTea by remember { mutableStateOf(base.deductTeaBreak) }
    var weeklyOff by remember { mutableStateOf(base.weeklyOffDay) }
    var pfApp by remember { mutableStateOf(base.pfApplicable) }
    var esiApp by remember { mutableStateOf(base.esiApplicable) }

    // D. Government
    var uan by remember { mutableStateOf(base.uanNumber) }
    var esic by remember { mutableStateOf(base.esicNumber) }
    var pfAccount by remember { mutableStateOf(base.pfAccountNumber) }

    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPrimary)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (strings.isHindi) "कर्मचारी पंजीकरण व सुविधाएं" else "Employee Registration & Facilities",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Personal") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Job & Co.") })
                    Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Salary") })
                    Tab(selected = tabIndex == 3, onClick = { tabIndex = 3 }, text = { Text("Facilities") })
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (tabIndex) {
                        0 -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (photoUri.isNotBlank()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(photoUri),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_worker_avatar),
                                        contentDescription = "Default Avatar",
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (strings.isHindi) "फोटो चुनें" else "Select Photo")
                                }
                            }
                            Field("Full Name *", fullName) { fullName = it }
                            Field("Mobile Number (10 digits)", mobile, KeyboardType.Phone) { mobile = it }
                            Field("Date of Birth (DD/MM/YYYY)", dob) { dob = it }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Male", "Female", "Other").forEach { g ->
                                    FilterChip(selected = gender == g, onClick = { gender = g }, label = { Text(g) })
                                }
                            }
                            Field("Residential Address", address) { address = it }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("Village", village, modifier = Modifier.weight(1f)) { village = it }
                                Field("City", city, modifier = Modifier.weight(1f)) { city = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("District", district, modifier = Modifier.weight(1f)) { district = it }
                                Field("State", state, modifier = Modifier.weight(1f)) { state = it }
                            }
                            Field("PIN Code", pinCode, KeyboardType.Number) { pinCode = it }
                        }

                        1 -> {
                            Field("Company Name", companyName) { companyName = it }
                            Field("Factory Name", factoryName) { factoryName = it }
                            Field("Factory Address", factoryAddress) { factoryAddress = it }
                            Text("Industrial Area", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NcrConstants.INDUSTRIAL_AREAS.forEach { area ->
                                    FilterChip(
                                        selected = industrialArea == area,
                                        onClick = { industrialArea = area },
                                        label = { Text(area, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            Field("Department", department) { department = it }
                            Field("Job Designation", designation) { designation = it }
                            Text("Work Category", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NcrConstants.WORK_CATEGORIES.forEach { cat ->
                                    FilterChip(
                                        selected = workCategory == cat,
                                        onClick = { workCategory = cat },
                                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("Date of Joining", dateOfJoining, modifier = Modifier.weight(1f)) { dateOfJoining = it }
                                Field("Gate Pass No.", gatePass, modifier = Modifier.weight(1f)) { gatePass = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("Worker ID (if any)", workerId, modifier = Modifier.weight(1f)) { workerId = it }
                                Field("HR Contact", hrContact, KeyboardType.Phone, modifier = Modifier.weight(1f)) { hrContact = it }
                            }
                            Field("Previous Work Experience", prevExp) { prevExp = it }
                            if (workplaces.isNotEmpty()) {
                                Text("Assigned Workplace QR", style = MaterialTheme.typography.labelMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    workplaces.forEach { wp ->
                                        FilterChip(
                                            selected = selectedWorkplaceId == wp.id,
                                            onClick = { selectedWorkplaceId = wp.id },
                                            label = { Text("${wp.companyName} (${wp.factoryName})", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            Text("Calculation Method", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CalculationMethod.entries.forEach { m ->
                                    FilterChip(
                                        selected = calcMethod == m.key,
                                        onClick = { calcMethod = m.key },
                                        label = { Text(m.labelEn, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("Monthly Gross (₹)", grossSalary, KeyboardType.Decimal, Modifier.weight(1f)) { grossSalary = it }
                                Field("Basic Salary (₹)", basicSalary, KeyboardType.Decimal, Modifier.weight(1f)) { basicSalary = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("DA (₹)", da, KeyboardType.Decimal, Modifier.weight(1f)) { da = it }
                                Field("HRA (₹)", hra, KeyboardType.Decimal, Modifier.weight(1f)) { hra = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("Other Allowances (₹)", otherAllow, KeyboardType.Decimal, Modifier.weight(1f)) { otherAllow = it }
                                Field("Daily Wage (₹)", dailyWage, KeyboardType.Decimal, Modifier.weight(1f)) { dailyWage = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("Hourly Wage (₹)", hourlyWage, KeyboardType.Decimal, Modifier.weight(1f)) { hourlyWage = it }
                                Field("OT Rate/Hr (₹)", otRate, KeyboardType.Decimal, Modifier.weight(1f)) { otRate = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("Standard Duty Hrs", stdHours, KeyboardType.Decimal, Modifier.weight(1f)) { stdHours = it }
                                Field("Shift Start (HH:MM)", shiftStart, modifier = Modifier.weight(1f)) { shiftStart = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Field("Lunch Break (min)", lunchMins, KeyboardType.Number, Modifier.weight(1f)) { lunchMins = it }
                                Field("Tea Break (min)", teaMins, KeyboardType.Number, Modifier.weight(1f)) { teaMins = it }
                            }
                            ToggleRow("Deduct Tea Break?", deductTea) { deductTea = it }
                            ToggleRow("PF Applicable?", pfApp) { pfApp = it }
                            ToggleRow("ESI Applicable?", esiApp) { esiApp = it }
                            Field("Weekly Off Day (e.g. SUNDAY)", weeklyOff) { weeklyOff = it }
                            HorizontalDivider()
                            Text("Government IDs (UAN / ESIC)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Field("UAN Number", uan, KeyboardType.Number) { uan = it }
                            Field("ESIC Number", esic, KeyboardType.Number) { esic = it }
                            Field("PF Account Number", pfAccount) { pfAccount = it }
                        }

                        3 -> {
                            Text(
                                text = if (strings.isHindi) "कंपनी द्वारा दी जाने वाली सुविधाएं और कटौती चुनें (मुफ़्त सुविधाओं की कटौती नहीं होगी)"
                                else "Configure company facilities (Free facilities are never deducted)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val itemsToShow = if (facilities.isNotEmpty()) facilities else {
                                NcrConstants.FACILITY_TEMPLATES.map { (cat, labels) ->
                                    FacilityItem(
                                        id = cat.lowercase(),
                                        name = if (strings.isHindi) labels.first else labels.second,
                                        category = cat,
                                        enabled = false,
                                        isFree = true
                                    )
                                }
                            }
                            itemsToShow.forEach { item ->
                                FacilityEditorCard(item = item, onSave = onSaveFacility)
                            }
                        }
                    }

                    if (validationError != null) {
                        Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(if (strings.isHindi) "बंद करें" else "Cancel")
                    }
                    Button(
                        onClick = {
                            if (fullName.trim().length < 2) {
                                validationError = "Please enter your full name."
                                tabIndex = 0
                                return@Button
                            }
                            val updated = base.copy(
                                fullName = fullName.trim(),
                                mobileNumber = mobile.trim(),
                                dateOfBirth = dob.trim(),
                                gender = gender,
                                photoUri = photoUri,
                                residentialAddress = address.trim(),
                                village = village.trim(),
                                city = city.trim(),
                                district = district.trim(),
                                state = state.trim(),
                                pinCode = pinCode.trim(),
                                companyName = companyName.trim(),
                                factoryName = factoryName.trim(),
                                factoryAddress = factoryAddress.trim(),
                                industrialArea = industrialArea,
                                department = department.trim(),
                                designation = designation.trim(),
                                workCategory = workCategory,
                                dateOfJoining = dateOfJoining.trim(),
                                gatePassNumber = gatePass.trim(),
                                workerId = workerId.trim(),
                                previousExperience = prevExp.trim(),
                                hrContact = hrContact.trim(),
                                workplaceId = selectedWorkplaceId,
                                calculationMethod = calcMethod,
                                monthlyGrossSalary = grossSalary.toDoubleOrNull() ?: 0.0,
                                basicSalary = basicSalary.toDoubleOrNull() ?: 0.0,
                                da = da.toDoubleOrNull() ?: 0.0,
                                hra = hra.toDoubleOrNull() ?: 0.0,
                                otherAllowances = otherAllow.toDoubleOrNull() ?: 0.0,
                                dailyWage = dailyWage.toDoubleOrNull() ?: 0.0,
                                hourlyWage = hourlyWage.toDoubleOrNull() ?: 0.0,
                                overtimeRatePerHour = otRate.toDoubleOrNull() ?: 0.0,
                                standardWorkingHours = (stdHours.toDoubleOrNull() ?: 8.0).coerceIn(1.0, 24.0),
                                shiftStartTime = shiftStart.trim().ifBlank { "09:00" },
                                lunchBreakMinutes = (lunchMins.toLongOrNull() ?: 30L).coerceIn(0L, 240L),
                                teaBreakMinutes = (teaMins.toLongOrNull() ?: 15L).coerceIn(0L, 120L),
                                deductTeaBreak = deductTea,
                                weeklyOffDay = weeklyOff.trim().uppercase().ifBlank { "SUNDAY" },
                                pfApplicable = pfApp,
                                esiApplicable = esiApp,
                                uanNumber = uan.trim(),
                                esicNumber = esic.trim(),
                                pfAccountNumber = pfAccount.trim()
                            )
                            onSaveProfile(updated)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                    ) {
                        Text(if (strings.isHindi) "प्रोफाइल सेव करें" else "Save Profile")
                    }
                }
            }
        }
    }
}

@Composable
private fun FacilityEditorCard(
    item: FacilityItem,
    onSave: (FacilityItem) -> Unit
) {
    var enabled by remember(item.enabled) { mutableStateOf(item.enabled) }
    var isFree by remember(item.isFree) { mutableStateOf(item.isFree) }
    var monthlyCharge by remember(item.monthlyCharge) { mutableStateOf(if (item.monthlyCharge > 0) item.monthlyCharge.toString() else "") }
    var dailyCharge by remember(item.dailyCharge) { mutableStateOf(if (item.dailyCharge > 0) item.dailyCharge.toString() else "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        onSave(item.copy(enabled = it, isFree = isFree))
                    }
                )
            }
            if (enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isFree) "Free (No Deduction)" else "Paid (Deducted)", style = MaterialTheme.typography.labelSmall)
                    Switch(
                        checked = !isFree,
                        onCheckedChange = { paid ->
                            isFree = !paid
                            onSave(item.copy(enabled = enabled, isFree = isFree))
                        }
                    )
                }
                if (!isFree) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dailyCharge,
                            onValueChange = { dailyCharge = it },
                            label = { Text("Daily ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = monthlyCharge,
                            onValueChange = { monthlyCharge = it },
                            label = { Text("Monthly ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onSave(
                                item.copy(
                                    enabled = enabled,
                                    isFree = isFree,
                                    dailyCharge = dailyCharge.toDoubleOrNull() ?: 0.0,
                                    monthlyCharge = monthlyCharge.toDoubleOrNull() ?: 0.0,
                                    actualDeduction = monthlyCharge.toDoubleOrNull() ?: 0.0
                                )
                            )
                        }
                    ) {
                        Text("Apply Deduction Rule", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DigitalIdentityCardDialog(
    profile: UserProfile?,
    onDismiss: () -> Unit
) {
    val p = profile ?: UserProfile()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(NavyDark, NavyPrimary)))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("MY DUTY IN NCR", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("DIGITAL EMPLOYEE ID CARD", color = Color(0xFF99F6E4), style = MaterialTheme.typography.labelSmall)
                        }
                        Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF99F6E4))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (p.photoUri.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(p.photoUri),
                                contentDescription = "Employee ID Photo",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_worker_avatar),
                                contentDescription = "Default Avatar",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(p.fullName.ifBlank { "Unregistered" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(p.employeeId.ifBlank { "ID Pending" }, style = MaterialTheme.typography.labelLarge, color = TealSecondary, fontWeight = FontWeight.Bold)
                            Text("${p.designation.ifBlank { p.workCategory }} • ${p.companyName.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                    IdRow("Mobile", p.mobileNumber.ifBlank { "-" })
                    IdRow("DOB", p.dateOfBirth.ifBlank { "-" })
                    IdRow("Joining Date", p.dateOfJoining.ifBlank { "-" })
                    IdRow("Gate Pass No.", p.gatePassNumber.ifBlank { "-" })
                    IdRow("Address", listOf(p.residentialAddress, p.city, p.district, p.state, p.pinCode).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "-" })

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun IdRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
