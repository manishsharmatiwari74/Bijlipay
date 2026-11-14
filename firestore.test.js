const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function validConnection(uid) {
  const now = pastDate();
  return {
    userId: uid,
    providerName: "Tata Power",
    consumerNumber: "TP-900112233",
    nickname: "Home Meter",
    meterType: "SMART_METER",
    tariffRate: 7.5,
    monthlyTargetKwh: 350,
    billingCycleDay: 5,
    createdAt: now,
    updatedAt: now,
  };
}

function validBill(uid, connectionId, status = "PENDING") {
  const now = pastDate();
  return {
    userId: uid,
    connectionId,
    providerName: "Tata Power",
    consumerNumber: "TP-900112233",
    nickname: "Home Meter",
    billingMonth: "Nov 2026",
    unitsConsumed: 280,
    amount: 2100.5,
    lateFee: 0,
    dueDateMillis: Date.now() + 5 * 86400000,
    status,
    createdAt: now,
    updatedAt: now,
  };
}

function validReminder(uid, billId = "") {
  const now = pastDate();
  return {
    userId: uid,
    billId,
    title: "Pay Home Electricity Bill",
    consumerNumber: "TP-900112233",
    amount: 2100.5,
    dueDateMillis: Date.now() + 5 * 86400000,
    remindAtMillis: Date.now() + 2 * 86400000,
    daysBeforeDue: 3,
    enabled: true,
    notified: false,
    note: "Before 6 PM",
    createdAt: now,
    updatedAt: now,
  };
}

function validUsageReading(uid, connectionId) {
  const now = pastDate();
  return {
    userId: uid,
    connectionId,
    providerName: "Tata Power",
    monthLabel: "Nov 26",
    yearMonth: "2026-11",
    previousReading: 14200,
    currentReading: 14480,
    unitsKwh: 280,
    estimatedCost: 2100,
    readingDateMillis: Date.now(),
    notes: "Monthly check",
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user cannot read or write connections or bills", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("connections").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bills")
      .doc("b1")
      .set(validBill(ALICE_UID, "c1"))
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("connections")
      .doc("conn_1")
      .set(validConnection(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("connections").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("connections")
      .doc("conn_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("connections")
      .doc("conn_2")
      .set(validConnection(BOB_UID))
  );
});

test("Orphan check: cannot create bill or usage reading for non-existent connection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bills")
      .doc("bill_orphan")
      .set(validBill(ALICE_UID, "missing_conn"))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("usageReadings")
      .doc("read_orphan")
      .set(validUsageReading(ALICE_UID, "missing_conn"))
  );
});

test("Shadow update & ID poisoning are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = validConnection(ALICE_UID);
  payload.ghostAdminFlag = true;
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("connections")
      .doc("conn_1")
      .set(payload)
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("connections")
      .doc("bad$id!")
      .set(validConnection(ALICE_UID))
  );
});

test("Full lifecycle: create connection, bill, pay bill (terminal lock), reminder, and usage reading", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  // 1. Connection
  await assertSucceeds(
    userDoc.collection("connections").doc("conn_1").set(validConnection(ALICE_UID))
  );

  // 2. Bill
  await assertSucceeds(
    userDoc.collection("bills").doc("bill_1").set(validBill(ALICE_UID, "conn_1"))
  );

  // 3. Pay Bill -> PAID
  await assertSucceeds(
    userDoc.collection("bills").doc("bill_1").update({
      status: "PAID",
      paidAtMillis: Date.now(),
      paymentMethod: "UPI (GPay)",
      transactionId: "TXN202611139988",
      updatedAt: pastDate(100),
    })
  );

  // 4. Terminal state lock: Paid bill cannot be modified again
  await assertFails(
    userDoc.collection("bills").doc("bill_1").update({
      status: "PENDING",
      updatedAt: pastDate(50),
    })
  );

  // 5. Reminder linked to bill_1 and standalone reminder
  await assertSucceeds(
    userDoc.collection("reminders").doc("rem_1").set(validReminder(ALICE_UID, "bill_1"))
  );
  await assertSucceeds(
    userDoc.collection("reminders").doc("rem_2").set(validReminder(ALICE_UID, ""))
  );

  // 6. Usage Reading
  await assertSucceeds(
    userDoc.collection("usageReadings").doc("read_1").set(validUsageReading(ALICE_UID, "conn_1"))
  );

  // 7. Invalid usage reading (currentReading < previousReading) fails
  const badReading = validUsageReading(ALICE_UID, "conn_1");
  badReading.currentReading = 100;
  await assertFails(
    userDoc.collection("usageReadings").doc("read_bad").set(badReading)
  );
});
