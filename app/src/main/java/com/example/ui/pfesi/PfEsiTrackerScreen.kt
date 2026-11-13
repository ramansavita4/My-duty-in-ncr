package com.example.ui.pfesi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.SalaryBreakdown
import com.example.localization.LocalStrings
import com.example.model.PayrollRecord
import com.example.model.UserProfile
import com.example.ui.components.CompactMetricCard
import com.example.ui.components.formatRupeesExact
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OvertimeIndigo
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.TealSecondary

@Composable
fun PfEsiTrackerScreen(
    profile: UserProfile?,
    breakdown: SalaryBreakdown,
    payrollRecords: List<PayrollRecord>,
    onUpdateUanEsic: (uan: String, esic: String) -> Unit,
    onMarkStatementVerified: (monthKey: String, verifyPf: Boolean, verifyEsi: Boolean) -> Unit
) {
    val strings = LocalStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var uanInput by remember(profile?.uanNumber) { mutableStateOf(profile?.uanNumber ?: "") }
    var esicInput by remember(profile?.esicNumber) { mutableStateOf(profile?.esicNumber ?: "") }

    val cumulativePf = remember(payrollRecords, breakdown) {
        val savedTotal = payrollRecords.sumOf { it.employeePf + it.employerPf + it.epsContribution + it.edliContribution }
        if (payrollRecords.any { it.monthKey == breakdown.monthKey }) savedTotal else savedTotal + breakdown.totalMonthlyPfContribution
    }
    val yearlyPfByYear = remember(payrollRecords) {
        payrollRecords.groupBy { it.monthKey.take(4) }
            .mapValues { (_, list) -> list.sumOf { it.employeePf + it.employerPf + it.epsContribution } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("PF (EPFO)") },
                modifier = Modifier.testTag("tab_pf")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("ESI (ESIC)") },
                modifier = Modifier.testTag("tab_esi")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = AmberContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.estimatedNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnAmberContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (selectedTab == 0) {
                // PF Dashboard
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.pfTrackerTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = if (breakdown.pfVerifiedStatus == "VERIFIED_BY_STATEMENT") Color(0xFFDCFCE7) else AmberContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = breakdown.pfVerifiedStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = uanInput,
                                    onValueChange = { uanInput = it },
                                    label = { Text("UAN Number (12 digits)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { onUpdateUanEsic(uanInput.trim(), esicInput.trim()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                                ) {
                                    Text("Save")
                                }
                            }
                            HorizontalDivider()
                            StatLine("Basic + DA Earned", formatRupeesExact(breakdown.basicEarned + breakdown.daEarned))
                            StatLine("Applicable PF Wage (Cap ₹15,000)", formatRupeesExact(breakdown.pfApplicableWage))
                            StatLine("Employee PF (12%)", formatRupeesExact(breakdown.employeePf))
                            StatLine("Employer EPF (3.67%)", formatRupeesExact(breakdown.employerPfEpf))
                            StatLine("Employer EPS Pension (8.33%)", formatRupeesExact(breakdown.employerEps))
                            StatLine("Employer EDLI (0.5%)", formatRupeesExact(breakdown.employerEdli))
                            HorizontalDivider()
                            StatLine("Total Monthly PF (Est.)", formatRupeesExact(breakdown.totalMonthlyPfContribution), bold = true)
                            StatLine("Cumulative Recorded + Current", formatRupeesExact(cumulativePf), bold = true)

                            if (breakdown.pfVerifiedStatus != "VERIFIED_BY_STATEMENT") {
                                OutlinedButton(
                                    onClick = { onMarkStatementVerified(breakdown.monthKey, true, false) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (strings.isHindi) "EPFO पासबुक विवरण से सत्यापित चिह्नित करें" else "Mark Verified by Employee Statement")
                                }
                            }
                        }
                    }
                }

                if (yearlyPfByYear.isNotEmpty()) {
                    item {
                        Text("Year-wise PF Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(yearlyPfByYear.entries.toList()) { (yr, total) ->
                        CompactMetricCard(
                            label = "Year $yr",
                            value = formatRupeesExact(total),
                            accentColor = NavyPrimary
                        )
                    }
                }

                item {
                    Text("Month-wise PF History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(payrollRecords.sortedByDescending { it.monthKey }) { rec ->
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
                                Text(rec.monthKey, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(rec.pfVerifiedStatus, style = MaterialTheme.typography.labelSmall, color = TealSecondary)
                            }
                            Text(
                                text = "Emp: ${formatRupeesExact(rec.employeePf)} | Er: ${formatRupeesExact(rec.employerPf + rec.epsContribution)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // ESI Dashboard
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.esiTrackerTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = if (breakdown.esiEligible) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (breakdown.esiEligible) "COVERED" else "NOT COVERED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = esicInput,
                                    onValueChange = { esicInput = it },
                                    label = { Text("ESIC Number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { onUpdateUanEsic(uanInput.trim(), esicInput.trim()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
                                ) {
                                    Text("Save")
                                }
                            }
                            HorizontalDivider()
                            if (!breakdown.esiEligible) {
                                Text(
                                    text = strings.esiNotApplicable,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                StatLine("ESI Wage", formatRupeesExact(breakdown.esiWage))
                                StatLine("Employee ESI (0.75%)", formatRupeesExact(breakdown.employeeEsi))
                                StatLine("Employer ESI (3.25%)", formatRupeesExact(breakdown.employerEsi))
                                HorizontalDivider()
                                StatLine("Total Monthly ESI (Est.)", formatRupeesExact(breakdown.totalMonthlyEsiContribution), bold = true)
                                StatLine("Status", breakdown.esiVerifiedStatus)
                                if (breakdown.esiVerifiedStatus != "VERIFIED_BY_STATEMENT") {
                                    OutlinedButton(
                                        onClick = { onMarkStatementVerified(breakdown.monthKey, false, true) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (strings.isHindi) "ESIC स्टेटमेंट से सत्यापित चिह्नित करें" else "Mark Verified by ESIC Statement")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Month-wise ESI History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(payrollRecords.sortedByDescending { it.monthKey }) { rec ->
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
                                Text(rec.monthKey, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(rec.esiVerifiedStatus, style = MaterialTheme.typography.labelSmall, color = OvertimeIndigo)
                            }
                            Text(
                                text = "Emp: ${formatRupeesExact(rec.employeeEsi)} | Er: ${formatRupeesExact(rec.employerEsi)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
