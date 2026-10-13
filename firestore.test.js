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

test("Unauthenticated user: cannot create quiz attempt", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").doc("attempt_1").set({
      id: "attempt_1",
      userId: ALICE_UID,
      courseId: "math101",
      score: 10,
      totalQuestions: 10,
      completedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read another user's profile or subcollections", async () => {
  // Alice cannot read Bob's profile
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("notes").doc("note_1").get());
});

test("Authenticated user: can create their own profile with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@ttsacademy.edu",
      name: "Alice Smith",
      role: "STUDENT",
      department: "Computer Science",
      enrolledCourses: ["math101", "cs101"],
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: fails profile creation with ghost keys or wrong role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Ghost key injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@ttsacademy.edu",
      name: "Alice Smith",
      role: "STUDENT",
      ghostField: "hacker",
      createdAt: new Date(),
    })
  );

  // Invalid role
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@ttsacademy.edu",
      name: "Alice Smith",
      role: "SUPERUSER",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can record and query their own quiz attempts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const attemptRef = aliceDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").doc("attempt_1");

  await assertSucceeds(
    attemptRef.set({
      id: "attempt_1",
      userId: ALICE_UID,
      courseId: "calculus_1",
      topicId: "derivatives",
      score: 8,
      totalQuestions: 10,
      percentage: 80,
      completedAt: new Date(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").get());
});

test("Authenticated user: can create and update study notes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1");

  const now = new Date();
  await assertSucceeds(
    noteRef.set({
      id: "note_1",
      userId: ALICE_UID,
      courseId: "physics_1",
      title: "Newton's Laws",
      content: "First law: inertia. Second law: F=ma.",
      tags: ["physics", "mechanics"],
      createdAt: now,
    })
  );

  await assertSucceeds(
    noteRef.update({
      content: "Updated note with deeper derivations.",
      updatedAt: new Date(),
    })
  );
});
