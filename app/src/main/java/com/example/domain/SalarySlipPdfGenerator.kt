package com.example.domain

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.model.UserProfile
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Locale

object SalarySlipPdfGenerator {

    fun writeSalarySlipPdf(
        outputStream: OutputStream,
        profile: UserProfile,
        breakdown: SalaryBreakdown,
        isHindi: Boolean
    ) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 points
        val page = pdf.startPage(pageInfo)
        drawSalarySlipPage(page.canvas, profile, breakdown, isHindi)
        pdf.finishPage(page)
        pdf.writeTo(outputStream)
        pdf.close()
    }

    fun printSalarySlip(
        context: Context,
        profile: UserProfile,
        breakdown: SalaryBreakdown,
        isHindi: Boolean
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "MY_DUTY_IN_NCR_Slip_${breakdown.monthKey}"
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        writeSalarySlipPdf(out, profile, breakdown, isHindi)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }

    private fun drawSalarySlipPage(
        canvas: Canvas,
        profile: UserProfile,
        b: SalaryBreakdown,
        isHindi: Boolean
    ) {
        val navyPaint = Paint().apply { color = Color.parseColor("#0F2744") }
        val tealPaint = Paint().apply { color = Color.parseColor("#0D9488") }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#CCFBF1")
            textSize = 11f
            isAntiAlias = true
        }
        val headerLabelPaint = Paint().apply {
            color = Color.parseColor("#0F2744")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10.5f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
            isAntiAlias = true
        }

        // Top Navy Banner
        canvas.drawRect(24f, 24f, 571f, 92f, navyPaint)
        canvas.drawRect(24f, 88f, 571f, 92f, tealPaint)
        canvas.drawText("MY DUTY IN NCR  |  मेरी ड्यूटी इन NCR", 40f, 52f, titlePaint)
        canvas.drawText(
            if (isHindi) "वेतन पर्ची (कर्मचारी अनुमान) - माह: ${b.monthKey}"
            else "SALARY SLIP (EMPLOYEE ESTIMATE) - MONTH: ${b.monthKey}",
            40f,
            74f,
            subtitlePaint
        )

        var y = 114f
        // Estimate disclaimer badge
        val badgePaint = Paint().apply { color = Color.parseColor("#FEF3C7") }
        canvas.drawRect(24f, y, 571f, y + 24f, badgePaint)
        canvas.drawRect(24f, y, 571f, y + 24f, borderPaint)
        canvas.drawText(
            if (isHindi) "नोट: यह कर्मचारी द्वारा स्व-निर्मित अनुमानित वेतन पर्ची है जब तक कंपनी पेरोल सत्यापित न हो।"
            else "NOTE: Employee-generated salary estimate unless verified against official employer payroll.",
            34f,
            y + 16f,
            mutedPaint
        )

        y += 38f
        canvas.drawText(if (isHindi) "कर्मचारी विवरण / Employee Details" else "Employee Details", 28f, y, headerLabelPaint)
        y += 8f
        canvas.drawRect(24f, y, 571f, y + 102f, borderPaint)

        fun rowPair(label1: String, val1: String, label2: String, val2: String, rowY: Float) {
            canvas.drawText("$label1: $val1", 34f, rowY, textPaint)
            canvas.drawText("$label2: $val2", 310f, rowY, textPaint)
        }

        rowPair("Name", profile.fullName.ifBlank { "-" }, "Employee ID", profile.employeeId.ifBlank { "-" }, y + 20f)
        rowPair("Company", profile.companyName.ifBlank { "-" }, "Designation", profile.designation.ifBlank { profile.workCategory }, y + 38f)
        rowPair("Joining Date", profile.dateOfJoining.ifBlank { "-" }, "UAN", profile.uanNumber.ifBlank { "Not Added" }, y + 56f)
        rowPair("ESIC No.", profile.esicNumber.ifBlank { "Not Added" }, "Method", b.calculationMethod.labelEn, y + 74f)
        rowPair("Mobile", profile.mobileNumber.ifBlank { "-" }, "Area", profile.industrialArea, y + 92f)

        y += 120f
        canvas.drawText(if (isHindi) "उपस्थिति सारांश / Attendance Summary" else "Attendance Summary", 28f, y, headerLabelPaint)
        y += 8f
        canvas.drawRect(24f, y, 571f, y + 48f, borderPaint)
        val totalHours = String.format(Locale.US, "%.1f", b.totalWorkingMinutes / 60.0)
        rowPair(
            "Present / HD / PL",
            "${b.presentDays} P | ${b.halfDays} HD | ${b.paidLeaveDays} PL",
            "Absent / WO / Hol",
            "${b.absentDays} A | ${b.weeklyOffDays} WO | ${b.holidays} H",
            y + 20f
        )
        rowPair(
            "Working Hours",
            "$totalHours hrs",
            "Overtime",
            "${String.format(Locale.US, "%.1f", b.overtimeHours)} hrs",
            y + 38f
        )

        y += 68f
        // Earnings & Deductions side-by-side table
        canvas.drawRect(24f, y, 294f, y + 24f, navyPaint)
        canvas.drawRect(300f, y, 571f, y + 24f, navyPaint)
        val colHeaderPaint = Paint(boldPaint).apply { color = Color.WHITE }
        canvas.drawText("EARNINGS (आय)", 34f, y + 16f, colHeaderPaint)
        canvas.drawText("DEDUCTIONS (कटौती)", 310f, y + 16f, colHeaderPaint)

        y += 24f
        canvas.drawRect(24f, y, 294f, y + 210f, borderPaint)
        canvas.drawRect(300f, y, 571f, y + 210f, borderPaint)

        fun fmt(v: Double) = "₹" + String.format(Locale.US, "%.2f", v)

        val earningsRows = listOf(
            "Basic Earned" to fmt(b.basicEarned),
            "Dearness Allowance (DA)" to fmt(b.daEarned),
            "HRA" to fmt(b.hraEarned),
            "Other Allowances" to fmt(b.otherAllowancesEarned),
            "Overtime (${b.overtimeHours}h)" to fmt(b.overtimeEarnings),
            "Night Allowance" to fmt(b.nightAllowance),
            "Attendance / Other Bonus" to fmt(b.attendanceBonus + b.otherBonuses)
        )
        val deductionRows = listOf(
            "Employee PF (Est.)" to fmt(b.employeePf),
            "Employee ESI (Est.)" to fmt(b.employeeEsi),
            "Canteen Deduction" to fmt(b.canteenDeduction),
            "Tea / Lunch Deduction" to fmt(b.teaLunchDeduction),
            "Bus / Transport" to fmt(b.transportDeduction),
            "Advance Deduction" to fmt(b.advanceDeduction),
            "Other Deductions" to fmt(b.otherDeductions)
        )

        earningsRows.forEachIndexed { idx, (label, amount) ->
            val ry = y + 22f + idx * 24f
            canvas.drawText(label, 34f, ry, textPaint)
            canvas.drawText(amount, 220f, ry, boldPaint)
        }
        deductionRows.forEachIndexed { idx, (label, amount) ->
            val ry = y + 22f + idx * 24f
            canvas.drawText(label, 310f, ry, textPaint)
            canvas.drawText(amount, 496f, ry, boldPaint)
        }

        // Totals Row
        val totY = y + 196f
        canvas.drawText("Gross Earnings: ${fmt(b.grossEarned)}", 34f, totY, headerLabelPaint)
        canvas.drawText("Total Deductions: ${fmt(b.totalDeductions)}", 310f, totY, headerLabelPaint)

        y += 226f
        // Net Payable Box
        val netBgPaint = Paint().apply { color = Color.parseColor("#CCFBF1") }
        canvas.drawRect(24f, y, 571f, y + 48f, netBgPaint)
        canvas.drawRect(24f, y, 571f, y + 48f, borderPaint)
        val netTitlePaint = Paint(headerLabelPaint).apply { textSize = 14f }
        canvas.drawText(
            "NET PAYABLE SALARY (EST.): ${fmt(b.netEstimatedSalary)}",
            36f,
            y + 22f,
            netTitlePaint
        )
        val actualText = if (b.actualSalaryReceived > 0.0) {
            "Actual Received: ${fmt(b.actualSalaryReceived)} | Payment Date: ${b.paymentDate.ifBlank { "-" }}"
        } else {
            "Employer PF: ${fmt(b.employerPfEpf + b.employerEps)} | Employer ESI: ${fmt(b.employerEsi)} (Est.)"
        }
        canvas.drawText(actualText, 36f, y + 40f, mutedPaint)

        // Footer
        canvas.drawText(
            "MY DUTY IN NCR  |  मेरी ड्यूटी, मेरी हाजिरी, मेरा हिसाब।",
            28f,
            805f,
            mutedPaint
        )
    }
}
