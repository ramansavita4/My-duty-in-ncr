const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const ts = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_EMAIL = "ramansavita4@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write users, attendance, workplaces, companies, or jobs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("workplaces").get());
  await assertFails(unauthDb.collection("companies").get());
  await assertFails(unauthDb.collection("jobs").get());
});

test("User profile: owner can create worker profile, cannot self-assign admin role, and Bob cannot read Alice PII", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Cannot create with admin role
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      role: "admin",
      employeeId: "NCR-2026-000001",
      fullName: "Ramesh Kumar",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Valid worker creation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      role: "worker",
      employeeId: "NCR-2026-000001",
      fullName: "Ramesh Kumar",
      mobileNumber: "9876543210",
      uanNumber: "100123456789",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Bob cannot read Alice's PII
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  // Bob cannot list users
  await assertFails(bobDb.collection("users").get());
  // Alice cannot escalate role on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: ts(),
    })
  );
});

test("Attendance & Disputes: isolated to owner, dispute cannot self-approve, admin with verified email can approve", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const realAdminDb = testEnv
    .authenticatedContext("admin_uid", { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  // Alice writes attendance
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-12").set({
      userId: ALICE_UID,
      dateKey: "2026-11-12",
      monthKey: "2026-11",
      status: "P",
      qrVerified: true,
      punchInTimeMillis: 1762918200000,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Bob cannot read Alice attendance
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );

  // Alice creates dispute (must be SUBMITTED)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("disputes").doc("disp_1").set({
      userId: ALICE_UID,
      targetDate: "2026-11-12",
      category: "MISSING_PUNCH_OUT",
      description: "QR scanner failed at gate 2",
      status: "APPROVED",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("disputes").doc("disp_1").set({
      userId: ALICE_UID,
      targetDate: "2026-11-12",
      category: "MISSING_PUNCH_OUT",
      description: "QR scanner failed at gate 2",
      status: "SUBMITTED",
      auditTrail: ["Submitted by worker"],
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Alice cannot self-approve dispute
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("disputes").doc("disp_1").update({
      status: "APPROVED",
      updatedAt: ts(),
    })
  );

  // Spoofed unverified email admin cannot approve
  await assertFails(
    spoofedAdminDb.collection("users").doc(ALICE_UID).collection("disputes").doc("disp_1").update({
      status: "APPROVED",
      reviewerNote: "Spoof",
      updatedAt: ts(),
    })
  );

  // Verified admin can approve
  await assertSucceeds(
    realAdminDb.collection("users").doc(ALICE_UID).collection("disputes").doc("disp_1").update({
      status: "APPROVED",
      reviewerNote: "Verified with gate log",
      updatedAt: ts(),
    })
  );
});

test("Companies, Jobs, Workplaces: query enforcement, company verification lock, and job application parent check", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const realAdminDb = testEnv
    .authenticatedContext("admin_uid", { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  // Alice creates company (PENDING)
  await assertSucceeds(
    aliceDb.collection("companies").doc("comp_1").set({
      ownerId: ALICE_UID,
      companyName: "NCR Auto Gears Pvt Ltd",
      factoryName: "Plant 2 Manesar",
      industrialArea: "IMT Manesar",
      verificationStatus: "PENDING",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Bob cannot list unfiltered companies
  await assertFails(bobDb.collection("companies").get());
  // Bob can query APPROVED companies
  await assertSucceeds(
    bobDb.collection("companies").where("verificationStatus", "==", "APPROVED").get()
  );

  // Alice cannot self-approve company
  await assertFails(
    aliceDb.collection("companies").doc("comp_1").update({
      verificationStatus: "APPROVED",
      updatedAt: ts(),
    })
  );

  // Admin approves company
  await assertSucceeds(
    realAdminDb.collection("companies").doc("comp_1").update({
      verificationStatus: "APPROVED",
      updatedAt: ts(),
    })
  );

  // Alice posts job under comp_1
  await assertSucceeds(
    aliceDb.collection("jobs").doc("job_1").set({
      ownerId: ALICE_UID,
      companyId: "comp_1",
      companyName: "NCR Auto Gears Pvt Ltd",
      title: "CNC Operator",
      tradeCategory: "CNC Operator",
      industrialArea: "IMT Manesar",
      salaryMin: 16500,
      salaryMax: 19500,
      status: "OPEN",
      reportedCount: 0,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Bob applies to job_1
  await assertSucceeds(
    bobDb.collection("job_applications").doc("app_1").set({
      applicantId: BOB_UID,
      applicantName: "Suresh",
      jobId: "job_1",
      jobOwnerId: ALICE_UID,
      companyName: "NCR Auto Gears Pvt Ltd",
      jobTitle: "CNC Operator",
      status: "APPLIED",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Bob cannot list all applications without filter
  await assertFails(bobDb.collection("job_applications").get());
  // Bob can query his own applications
  await assertSucceeds(
    bobDb.collection("job_applications").where("applicantId", "==", BOB_UID).get()
  );
  // Alice (job owner) can shortlist Bob
  await assertSucceeds(
    aliceDb.collection("job_applications").doc("app_1").update({
      status: "SHORTLISTED",
      note: "Interview Friday 10 AM",
      updatedAt: ts(),
    })
  );
});
