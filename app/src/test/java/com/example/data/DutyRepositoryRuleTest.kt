package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.AttendanceDispute
import com.example.model.UserProfile
import com.example.model.WorkplaceQr
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DutyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveProfile_and_punchInOut_succeedsForOwner() = runBlocking {
        val uid = signInTestUser("worker_${UUID.randomUUID().toString().take(8)}@test.com")
        val repo = DutyRepository(firestore, auth)

        val profile = UserProfile(
            userId = uid,
            fullName = "Vikram Yadav",
            mobileNumber = "9811002233",
            companyName = "NCR Precision Works",
            industrialArea = "IMT Manesar",
            monthlyGrossSalary = 18000.0,
            basicSalary = 10000.0,
            da = 2000.0,
            hra = 4000.0,
            otherAllowances = 2000.0,
            standardWorkingHours = 8.0,
            lunchBreakMinutes = 30L
        )
        val saved = withTimeout(15000L) { repo.saveUserProfile(profile).getOrThrow() }
        assertTrue(saved.employeeId.startsWith("NCR-"))

        val wp = withTimeout(15000L) {
            repo.createOrRotateWorkplaceQr(
                WorkplaceQr(
                    companyName = "NCR Precision Works",
                    factoryName = "Unit 1",
                    industrialArea = "IMT Manesar"
                )
            ).getOrThrow()
        }
        assertTrue(wp.qrToken.isNotBlank())

        val dateKey = "2026-11-${(10..28).random()}"
        val inTime = 1762918200000L
        val outTime = inTime + (10L * 60 + 30L) * 60_000L // 10h30m total - 30m lunch = 10h worked (2h OT)

        val inRec = withTimeout(15000L) {
            repo.punchIn(saved, dateKey, "2026-11", wp, "selfie_in_uri", inTime).getOrThrow()
        }
        assertEquals("P", inRec.status)

        val outRec = withTimeout(15000L) {
            repo.punchOut(saved, dateKey, wp, "selfie_out_uri", outTime).getOrThrow()
        }
        assertEquals(600L, outRec.actualWorkingMinutes)
        assertEquals(120L, outRec.overtimeMinutes)
    }

    @Test
    fun crossUserProfileRead_and_unauthenticatedQuery_failWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_worker@test.com")
        val aliceRepo = DutyRepository(firestore, auth)
        withTimeout(15000L) {
            aliceRepo.saveUserProfile(
                UserProfile(
                    userId = aliceUid,
                    fullName = "Alice Sharma",
                    employeeId = "NCR-2026-${UUID.randomUUID().toString().take(6)}"
                )
            ).getOrThrow()
        }

        // Bob cannot read Alice's profile
        signInTestUser("bob_worker@test.com")
        val bobRepo = DutyRepository(firestore, auth)
        var crossUserDenied = false
        try {
            withTimeout(10000L) { bobRepo.observeUserProfile(aliceUid).first() }
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertNotNull(ffe)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
            crossUserDenied = true
        }
        assertTrue(crossUserDenied)

        // Unauthenticated query fails
        auth.signOut()
        var unauthDenied = false
        try {
            withTimeout(10000L) { bobRepo.observeActiveWorkplaces().first() }
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertNotNull(ffe)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
            unauthDenied = true
        }
        assertTrue(unauthDenied)
    }

    @Test
    fun submitDispute_createsSubmittedTicketWithAuditTrail() = runBlocking {
        val uid = signInTestUser("dispute_worker@test.com")
        val repo = DutyRepository(firestore, auth)
        val id = withTimeout(15000L) {
            repo.submitDispute(
                AttendanceDispute(
                    userId = uid,
                    targetDate = "2026-11-11",
                    category = "MISSING_PUNCH_OUT",
                    description = "Gate QR scanner failed during rain"
                )
            ).getOrThrow()
        }
        assertTrue(id.isNotBlank())
        val disputes = withTimeout(10000L) { repo.observeDisputes(uid).first() }
        assertTrue(disputes.any { it.id == id && it.status == "SUBMITTED" && it.auditTrail.isNotEmpty() })
    }
}
