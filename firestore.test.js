const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";
const ADMIN_UID = "admin_1";

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

// --- SECURITY BOUNDS & ZERO-TRUST TESTS ---

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated student: cannot read another student's profile directly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated student: cannot list all users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").get());
});

test("Authenticated admin: can list all users", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "spikenarrd@gmail.com" }).firestore();
  await assertSucceeds(adminDb.collection("users").get());
});

test("Authenticated student: can create their own profile with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@ttsacademy.edu",
      name: "Alice Smith",
      role: "STUDENT",
      department: "Computer Science",
      createdAt: new Date(),
    })
  );
});

test("Any user: can read announcements and courses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("announcements").get());
  await assertSucceeds(unauthDb.collection("courses").get());
});

test("Student: cannot create announcements", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("announcements").doc("ann_1").set({
      id: "ann_1",
      title: "Hacked notice",
      content: "Fake news",
      createdAt: new Date(),
    })
  );
});

test("Admin: can create announcements and courses", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "abebewottee@gmail.com" }).firestore();
  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_1").set({
      id: "ann_1",
      title: "Midterm Exam Schedule",
      content: "Freshman calculus exam will take place on Friday.",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    adminDb.collection("courses").doc("MATH_101").set({
      id: "MATH_101",
      code: "MATH 101",
      title: "Calculus I",
      category: "STEM",
      createdAt: new Date(),
    })
  );
});
