package com.example.ui.salary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.SalaryBreakdown
import com.example.domain.SalarySlipPdfGenerator
import com.example.localization.LocalStrings
import com.example.model.CalculationMethod
import com.example.model.PayrollRecord
import com.example.model.UserProfile
import com.example.ui.components.formatRupeesExact
import com.example.ui.summary.MonthlySummaryPreviewCard
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.TealSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SalaryScreen(
    profile: UserProfile?,
    breakdown: SalaryBreakdown,
    payrollRecords: List<PayrollRecord>,
    onChangeCalculationMethod: (CalculationMethod) -> Unit,
    onSaveMonthlyPayrollAdjustments: (PayrollRecord) -> Unit,
    onNotify: (String) -> Unit,
    onOpenMonthlySummary: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val currentProfile = profile ?: UserProfile()
    var showAdjustmentsDialog by remember { mutableStateOf(false) }

    val pdfCreateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    SalarySlipPdfGenerator.writeSalarySlipPdf(out, currentProfile, breakdown, strings.isHindi)
                }
                onNotify(if (strings.isHindi) "PDF वेतन पर्ची सेव की गई" else "PDF Salary Slip saved")
            } catch (e: Exception) {
                onNotify("PDF export failed: ${e.localizedMessage}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Disclaimer Banner
        item {
            Surface(
                color = AmberContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.salaryEstimateDisclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnAmberContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Net Salary Summary + PDF/Print Buttons
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${strings.netPayable} (${breakdown.monthKey})",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF99F6E4)
                    )
                    Text(
                        text = formatRupeesExact(breakdown.netEstimatedSalary),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${strings.grossEarnings}: ${formatRupeesExact(breakdown.grossEarned)}  •  ${strings.totalDeductions}: ${formatRupeesExact(breakdown.totalDeductions)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    if (breakdown.actualSalaryReceived > 0.0) {
                        Text(
                            text = if (strings.isHindi) {
                                "वास्तविक प्राप्त: ${formatRupeesExact(breakdown.actualSalaryReceived)} (${breakdown.paymentDate})"
                            } else {
                                "Actual Received: ${formatRupeesExact(breakdown.actualSalaryReceived)} (${breakdown.paymentDate})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF86EFAC),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { pdfCreateLauncher.launch("SalarySlip_${breakdown.monthKey}.pdf") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("salary_download_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.downloadPdfSlip, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                SalarySlipPdfGenerator.printSalarySlip(context, currentProfile, breakdown, strings.isHindi)
                            },
                            modifier = Modifier.testTag("salary_print_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.printSalarySlip, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        item {
            MonthlySummaryPreviewCard(
                totalHoursWorked = breakdown.totalWorkingMinutes / 60.0,
                overtimeHours = breakdown.overtimeHours,
                estimatedNetSalary = breakdown.netEstimatedSalary,
                onOpenSummaryDashboard = onOpenMonthlySummary
            )
        }

        // Calculation Method Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.calculationMethodLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CalculationMethod.entries.forEach { method ->
                            FilterChip(
                                selected = breakdown.calculationMethod == method,
                                onClick = { onChangeCalculationMethod(method) },
                                label = {
                                    Text(
                                        text = if (strings.isHindi) method.labelHi else method.labelEn,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                    Text(
                        text = "Daily Rate: ${formatRupeesExact(breakdown.dailyRate)}  |  Hourly: ${formatRupeesExact(breakdown.hourlyRate)}  |  OT Rate: ${formatRupeesExact(breakdown.overtimeRatePerHour)}/hr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Transparent Rupee-by-Rupee Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isHindi) "आय का पूरा विवरण (Earnings)" else "Earnings Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PresentGreen
                        )
                        Text(
                            text = "${breakdown.payableDays} days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BreakdownRow(if (strings.isHindi) "मूल वेतन (Basic)" else "Basic Earned", breakdown.basicEarned)
                    BreakdownRow(if (strings.isHindi) "महंगाई भत्ता (DA)" else "Dearness Allowance (DA)", breakdown.daEarned)
                    BreakdownRow("HRA", breakdown.hraEarned)
                    BreakdownRow(if (strings.isHindi) "अन्य भत्ते" else "Other Allowances", breakdown.otherAllowancesEarned)
                    BreakdownRow(
                        if (strings.isHindi) "ओवरटाइम (${breakdown.overtimeHours} घंटे)" else "Overtime (${breakdown.overtimeHours} hrs)",
                        breakdown.overtimeEarnings
                    )
                    BreakdownRow(if (strings.isHindi) "नाइट अलाउंस" else "Night Allowance", breakdown.nightAllowance)
                    BreakdownRow(if (strings.isHindi) "अटेंडेंस / अन्य बोनस" else "Attendance & Other Bonuses", breakdown.attendanceBonus + breakdown.otherBonuses)
                    HorizontalDivider()
                    BreakdownRow(strings.grossEarnings, breakdown.grossEarned, bold = true, valueColor = PresentGreen)

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (strings.isHindi) "कटौती का पूरा विवरण (Deductions)" else "Deductions Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AbsentRed
                    )
                    BreakdownRow(if (strings.isHindi) "कर्मचारी PF (अनुमानित)" else "Employee PF (Est.)", breakdown.employeePf)
                    BreakdownRow(if (strings.isHindi) "कर्मचारी ESI (अनुमानित)" else "Employee ESI (Est.)", breakdown.employeeEsi)
                    BreakdownRow(if (strings.isHindi) "चाय / भोजन कटौती" else "Tea / Lunch Deduction", breakdown.teaLunchDeduction)
                    BreakdownRow(if (strings.isHindi) "कैंटीन कटौती" else "Canteen Deduction", breakdown.canteenDeduction)
                    BreakdownRow(if (strings.isHindi) "बस / परिवहन कटौती" else "Bus / Transport Deduction", breakdown.transportDeduction)
                    BreakdownRow(if (strings.isHindi) "एडवांस कटौती" else "Advance Deduction", breakdown.advanceDeduction)
                    BreakdownRow(if (strings.isHindi) "अन्य कटौती" else "Other Deductions", breakdown.otherDeductions)
                    HorizontalDivider()
                    BreakdownRow(strings.totalDeductions, breakdown.totalDeductions, bold = true, valueColor = AbsentRed)

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAdjustmentsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("salary_adjustments_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.recordActualSalary)
                    }
                }
            }
        }

        // Monthly Financial History
        item {
            Text(
                text = if (strings.isHindi) "मासिक वेतन इतिहास (Monthly History)" else "Monthly Financial History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (payrollRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TealSecondary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (strings.isHindi) "प्रत्येक माह का वास्तविक प्राप्त वेतन और एडवांस रिकॉर्ड करने के लिए ऊपर 'वास्तविक प्राप्त वेतन दर्ज करें' पर टैप करें।"
                            else "Tap 'Record Actual Salary Received' above to save monthly payment history and verified amounts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(payrollRecords.sortedByDescending { it.monthKey }, key = { it.monthKey }) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rec.monthKey, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Received: ${formatRupeesExact(rec.actualSalaryReceived)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = PresentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Est. Net: ${formatRupeesExact(rec.netEstimatedSalary)} | Payment Date: ${rec.paymentDate.ifBlank { "-" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "PF: ${formatRupeesExact(rec.employeePf)} (${rec.pfVerifiedStatus}) • ESI: ${formatRupeesExact(rec.employeeEsi)} (${rec.esiVerifiedStatus})",
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyPrimary
                        )
                    }
                }
            }
        }
    }

    if (showAdjustmentsDialog) {
        val existing = payrollRecords.firstOrNull { it.monthKey == breakdown.monthKey }
        PayrollAdjustmentDialog(
            monthKey = breakdown.monthKey,
            breakdown = breakdown,
            existing = existing,
            onDismiss = { showAdjustmentsDialog = false },
            onSave = { updated ->
                onSaveMonthlyPayrollAdjustments(updated)
                showAdjustmentsDialog = false
            }
        )
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: Double,
    bold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = formatRupeesExact(amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun PayrollAdjustmentDialog(
    monthKey: String,
    breakdown: SalaryBreakdown,
    existing: PayrollRecord?,
    onDismiss: () -> Unit,
    onSave: (PayrollRecord) -> Unit
) {
    var advance by remember { mutableStateOf((existing?.advanceDeduction ?: 0.0).toString()) }
    var nightAllow by remember { mutableStateOf((existing?.nightAllowance ?: 0.0).toString()) }
    var otherBonus by remember { mutableStateOf((existing?.otherBonuses ?: 0.0).toString()) }
    var otherDed by remember { mutableStateOf((existing?.otherDeductions ?: 0.0).toString()) }
    var actualReceived by remember { mutableStateOf((existing?.actualSalaryReceived ?: breakdown.netEstimatedSalary).toString()) }
    var paymentDate by remember { mutableStateOf(existing?.paymentDate ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payroll Record ($monthKey)", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = actualReceived,
                    onValueChange = { actualReceived = it },
                    label = { Text("Actual Salary Received (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it },
                    label = { Text("Payment Date (e.g. 2026-12-07)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = advance,
                    onValueChange = { advance = it },
                    label = { Text("Advance Deduction (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = nightAllow,
                    onValueChange = { nightAllow = it },
                    label = { Text("Night Allowance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = otherBonus,
                    onValueChange = { otherBonus = it },
                    label = { Text("Other Bonuses (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = otherDed,
                    onValueChange = { otherDed = it },
                    label = { Text("Other Deductions (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rec = (existing ?: PayrollRecord(monthKey = monthKey)).copy(
                        monthKey = monthKey,
                        basicEarned = breakdown.basicEarned,
                        daEarned = breakdown.daEarned,
                        hraEarned = breakdown.hraEarned,
                        otherAllowancesEarned = breakdown.otherAllowancesEarned,
                        overtimeEarnings = breakdown.overtimeEarnings,
                        nightAllowance = nightAllow.toDoubleOrNull() ?: 0.0,
                        attendanceBonus = breakdown.attendanceBonus,
                        otherBonuses = otherBonus.toDoubleOrNull() ?: 0.0,
                        grossEarned = breakdown.grossEarned,
                        employeePf = breakdown.employeePf,
                        employerPf = breakdown.employerPfEpf,
                        epsContribution = breakdown.employerEps,
                        edliContribution = breakdown.employerEdli,
                        employeeEsi = breakdown.employeeEsi,
                        employerEsi = breakdown.employerEsi,
                        canteenDeduction = breakdown.canteenDeduction,
                        transportDeduction = breakdown.transportDeduction,
                        teaLunchDeduction = breakdown.teaLunchDeduction,
                        advanceDeduction = advance.toDoubleOrNull() ?: 0.0,
                        otherDeductions = otherDed.toDoubleOrNull() ?: 0.0,
                        netEstimatedSalary = breakdown.netEstimatedSalary,
                        actualSalaryReceived = actualReceived.toDoubleOrNull() ?: 0.0,
                        paymentDate = paymentDate.trim()
                    )
                    onSave(rec)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
