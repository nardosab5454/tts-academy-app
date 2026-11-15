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
const INVITED_ADMIN_UID = "invited_admin_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function verifiedAdminContext(uid = ADMIN_UID, email = "abebewottee@gmail.com") {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: true,
  });
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

// --- 1. UNAUTHENTICATED DENIAL (FAIL CLOSED) ---

test("Unauthenticated user: denied read/write across protected and curriculum collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("admin_invites").get());
  await assertFails(unauthDb.collection("courses").get());
  await assertFails(unauthDb.collection("topics").get());
  await assertFails(unauthDb.collection("questions").get());
  await assertFails(unauthDb.collection("notes").get());
  await assertFails(unauthDb.collection("past_exams").get());
  await assertFails(unauthDb.collection("announcements").get());
  await assertFails(unauthDb.collection("access_codes").get());
});

// --- 2. ADMIN AUTHENTICATION & ANTI-SPOOFING ---

test("Spoofed email substring ('hacker.admin@gmail.com') and unverified bootstrapped email are denied admin privileges", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("attacker_1", {
    email: "hacker.admin@gmail.com",
    email_verified: true,
  }).firestore();
  await assertFails(spoofedAdminDb.collection("users").get());
  await assertFails(
    spoofedAdminDb.collection("courses").doc("C_FAKE").set({
      id: "C_FAKE",
      code: "FAKE 101",
      title: "Unauthorized Course",
      category: "STEM",
      isPublished: true,
      createdAt: new Date(),
    })
  );

  const unverifiedAdminDb = testEnv.authenticatedContext("attacker_2", {
    email: "abebewottee@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(unverifiedAdminDb.collection("users").get());
  await assertFails(
    unverifiedAdminDb.collection("announcements").doc("ANN_FAKE").set({
      id: "ANN_FAKE",
      title: "Unverified",
      content: "Should fail",
      isUrgent: false,
      isPublished: true,
      createdAt: new Date(),
    })
  );
});

// --- 3. STUDENT PROFILE & CROSS-USER ISOLATION ---

test("Authenticated student: can read/create own valid STUDENT profile, cannot escalate role/premium or access other profiles", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@ttsacademy.edu" }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@ttsacademy.edu",
      name: "Alice Freshman",
      role: "STUDENT",
      department: "Addis Ababa University (AAU)",
      isPremium: false,
      activationCode: "",
      createdAt: new Date(),
    })
  );

  // Ghost field rejection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      hackedField: "oops",
    })
  );

  // Self-promotion to ADMIN denied
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
    })
  );

  // Self-grant isPremium without valid atomic code redemption denied
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isPremium: true,
      activationCode: "FAKE99",
    })
  );

  // Cross-user isolation & listing denied
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@ttsacademy.edu" }).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Cross-user isolation enforced on quiz_attempts, bookmarks, and personal notes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@ttsacademy.edu" }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@ttsacademy.edu" }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").doc("att_1").set({
      id: "att_1",
      userId: ALICE_UID,
      courseId: "MATH_101",
      score: 8,
      totalQuestions: 10,
      percentage: 80,
      completedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").set({
      id: "bm_1",
      userId: ALICE_UID,
      questionId: "Q_1",
      courseId: "MATH_101",
      savedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      id: "note_1",
      userId: ALICE_UID,
      courseId: "MATH_101",
      title: "Calculus Note",
      content: "Chain rule details",
      createdAt: new Date(),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").doc("att_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").get());
});

// --- 4. ATOMIC ACCESS CODE REDEMPTION ---

test("Atomic access code redemption: allows valid unused code transaction, blocks expired or reused code", async () => {
  const adminDb = verifiedAdminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@ttsacademy.edu" }).firestore();

  const future = new Date(Date.now() + 60 * 60 * 1000);
  const past = new Date(Date.now() - 60 * 1000);

  await assertSucceeds(
    adminDb.collection("access_codes").doc("PASS12").set({
      code: "PASS12",
      createdByAdminId: ADMIN_UID,
      isRedeemed: false,
      redeemedByUserId: "",
      redeemedByEmail: "",
      note: "Valid Pass",
      createdAt: new Date(),
      expiresAt: future,
    })
  );

  await assertSucceeds(
    adminDb.collection("access_codes").doc("OLD123").set({
      code: "OLD123",
      createdByAdminId: ADMIN_UID,
      isRedeemed: false,
      redeemedByUserId: "",
      redeemedByEmail: "",
      note: "Expired Pass",
      createdAt: new Date(Date.now() - 120 * 1000),
      expiresAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@ttsacademy.edu",
      name: "Alice Freshman",
      role: "STUDENT",
      department: "AAU",
      isPremium: false,
      activationCode: "",
      createdAt: new Date(),
    })
  );

  // Atomic transaction succeeds for PASS12
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const codeRef = aliceDb.collection("access_codes").doc("PASS12");
      const userRef = aliceDb.collection("users").doc(ALICE_UID);
      await tx.get(codeRef);
      await tx.get(userRef);
      tx.update(codeRef, {
        isRedeemed: true,
        redeemedByUserId: ALICE_UID,
        redeemedByEmail: "alice@ttsacademy.edu",
        redeemedAt: new Date(),
      });
      tx.update(userRef, {
        isPremium: true,
        activationCode: "PASS12",
        updatedAt: new Date(),
      });
    })
  );

  // Re-redeeming PASS12 by Bob fails
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@ttsacademy.edu" }).firestore();
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@ttsacademy.edu",
      name: "Bob",
      role: "STUDENT",
      department: "AAU",
      isPremium: false,
      activationCode: "",
      createdAt: new Date(),
    })
  );
  await assertFails(
    bobDb.runTransaction(async (tx) => {
      const codeRef = bobDb.collection("access_codes").doc("PASS12");
      const userRef = bobDb.collection("users").doc(BOB_UID);
      tx.update(codeRef, {
        isRedeemed: true,
        redeemedByUserId: BOB_UID,
        redeemedByEmail: "bob@ttsacademy.edu",
        redeemedAt: new Date(),
      });
      tx.update(userRef, {
        isPremium: true,
        activationCode: "PASS12",
        updatedAt: new Date(),
      });
    })
  );

  // Redeeming expired code fails
  await assertFails(
    bobDb.runTransaction(async (tx) => {
      const codeRef = bobDb.collection("access_codes").doc("OLD123");
      const userRef = bobDb.collection("users").doc(BOB_UID);
      tx.update(codeRef, {
        isRedeemed: true,
        redeemedByUserId: BOB_UID,
        redeemedByEmail: "bob@ttsacademy.edu",
        redeemedAt: new Date(),
      });
      tx.update(userRef, {
        isPremium: true,
        activationCode: "OLD123",
        updatedAt: new Date(),
      });
    })
  );
});

// --- 5. ADMIN PANEL FULL CURRICULUM CRUD & INVITED ADMINS ---

test("Verified admin & invited admin: can CRUD courses, topics, questions, notes, past_exams, announcements, and invites; student denied", async () => {
  const adminDb = verifiedAdminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@ttsacademy.edu" }).firestore();
  const createdTime = new Date(Date.now() - 5000);

  // Invite a secondary admin by verified email
  await assertSucceeds(
    adminDb.collection("admin_invites").doc("invited@ttsacademy.edu").set({
      email: "invited@ttsacademy.edu",
      department: "Mathematics Directorate",
      invitedBy: ADMIN_UID,
      createdAt: createdTime,
      updatedAt: createdTime,
    })
  );

  const invitedAdminDb = verifiedAdminContext(INVITED_ADMIN_UID, "invited@ttsacademy.edu").firestore();

  // Create course
  await assertSucceeds(
    adminDb.collection("courses").doc("C_CALC2").set({
      id: "C_CALC2",
      code: "MATH 102",
      title: "Calculus II",
      description: "Integration & Series",
      category: "STEM",
      iconName: "school",
      totalQuestions: 0,
      totalNotes: 0,
      isPublished: true,
      orderIndex: 13,
      createdAt: createdTime,
      updatedAt: createdTime,
    })
  );

  // Update course preserving createdAt succeeds; mutating createdAt fails
  await assertSucceeds(
    invitedAdminDb.collection("courses").doc("C_CALC2").set({
      id: "C_CALC2",
      code: "MATH 102",
      title: "Calculus II Updated",
      description: "Integration, Series & Differential Eq",
      category: "STEM",
      iconName: "school",
      totalQuestions: 1,
      totalNotes: 1,
      isPublished: true,
      orderIndex: 13,
      createdAt: createdTime,
      updatedAt: new Date(),
    }, { merge: true })
  );
  await assertFails(
    adminDb.collection("courses").doc("C_CALC2").update({
      createdAt: new Date(),
    })
  );

  // Topic, Question, Note, Past Exam, Announcement CRUD
  await assertSucceeds(
    invitedAdminDb.collection("topics").doc("T_SERIES").set({
      id: "T_SERIES",
      courseId: "C_CALC2",
      title: "Infinite Series",
      description: "Convergence tests",
      orderIndex: 1,
      createdAt: createdTime,
      updatedAt: createdTime,
    })
  );

  await assertSucceeds(
    adminDb.collection("questions").doc("Q_TEST1").set({
      id: "Q_TEST1",
      courseId: "C_CALC2",
      topicId: "T_SERIES",
      questionText: "Integral of 1/x dx?",
      optionA: "ln|x| + C",
      optionB: "x + C",
      optionC: "1/x^2",
      optionD: "e^x",
      correctAnswer: "A",
      explanationGeneral: "Standard logarithmic integral.",
      isPastExam: false,
      isVerified: true,
      isPublished: true,
      createdAt: createdTime,
      updatedAt: createdTime,
    })
  );

  await assertSucceeds(
    adminDb.collection("notes").doc("NOTE_CH1").set({
      id: "NOTE_CH1",
      courseId: "C_CALC2",
      topicId: "T_SERIES",
      title: "Chapter 1: Techniques of Integration",
      chapterNumber: 1,
      summary: "Substitution & Integration by Parts",
      keyFormulas: "udv = uv - vdu",
      contentMarkdown: "# Integration\nFull summary",
      content: "# Integration\nFull summary",
      isPublished: true,
      createdAt: createdTime,
      updatedAt: createdTime,
    })
  );

  await assertSucceeds(
    adminDb.collection("past_exams").doc("EXAM_C_CALC2_2016_FINAL").set({
      id: "EXAM_C_CALC2_2016_FINAL",
      courseId: "C_CALC2",
      university: "Addis Ababa University (AAU)",
      academicYear: "2016 E.C.",
      semester: "Semester 2",
      examType: "FINAL",
      title: "Calculus II Final Examination",
      totalQuestions: 25,
      durationMinutes: 90,
      isDigitized: true,
      isVerified: true,
      isPublished: true,
      createdAt: createdTime,
      updatedAt: createdTime,
    })
  );

  await assertSucceeds(
    adminDb.collection("announcements").doc("ANN_2025").set({
      id: "ANN_2025",
      title: "Midterm Dates",
      content: "Midterm exams begin Monday.",
      message: "Midterm exams begin Monday.",
      category: "EXAM",
      isUrgent: true,
      isPublished: true,
      createdAt: createdTime,
      updatedAt: createdTime,
    })
  );

  // Authenticated students can read curriculum collections, cannot write or delete
  await assertSucceeds(aliceDb.collection("courses").get());
  await assertSucceeds(aliceDb.collection("topics").get());
  await assertSucceeds(aliceDb.collection("questions").get());
  await assertSucceeds(aliceDb.collection("notes").get());
  await assertSucceeds(aliceDb.collection("past_exams").get());
  await assertSucceeds(aliceDb.collection("announcements").get());

  await assertFails(aliceDb.collection("courses").doc("C_CALC2").delete());
  await assertFails(aliceDb.collection("questions").doc("Q_TEST1").delete());
  await assertFails(aliceDb.collection("notes").doc("NOTE_CH1").delete());
  await assertFails(aliceDb.collection("past_exams").doc("EXAM_C_CALC2_2016_FINAL").delete());
  await assertFails(aliceDb.collection("announcements").doc("ANN_2025").delete());

  // Admin can clean up / delete
  await assertSucceeds(adminDb.collection("courses").doc("C_CALC2").delete());
});

// --- 6. LEGACY USER DOCS & CURRICULUM UPDATES REGRESSION ---

test("Legacy users doc missing role/createdAt can be promoted/backfilled; bootstrapped admin without user doc still passes isAdmin()", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const rawDb = ctx.firestore();
    await rawDb.collection("users").doc("legacy_admin_uid").set({
      userId: "legacy_admin_uid",
      email: "spikenarrd@gmail.com",
      name: "Legacy Admin",
      department: "Administration",
      isPremium: true,
    });
    await rawDb.collection("users").doc("legacy_student_uid").set({
      userId: "legacy_student_uid",
      email: "legacy_student@ttsacademy.edu",
      name: "Legacy Student",
    });
  });

  // Bootstrapped admin with no 'role' field in existing user doc still passes isAdmin() via email claim,
  // and can backfill their own 'role' + 'createdAt'
  const spikeDb = verifiedAdminContext("legacy_admin_uid", "spikenarrd@gmail.com").firestore();
  await assertSucceeds(
    spikeDb.collection("users").doc("legacy_admin_uid").set({
      userId: "legacy_admin_uid",
      email: "spikenarrd@gmail.com",
      name: "Legacy Admin",
      role: "ADMIN",
      isPremium: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    }, { merge: true })
  );

  // Admin can promote legacy student missing role/createdAt
  await assertSucceeds(
    spikeDb.collection("users").doc("legacy_student_uid").set({
      userId: "legacy_student_uid",
      email: "legacy_student@ttsacademy.edu",
      name: "Legacy Student",
      role: "ADMIN",
      isPremium: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    }, { merge: true })
  );

  // Student without role cannot self-escalate to ADMIN
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@ttsacademy.edu",
    email_verified: true,
  }).firestore();
  await assertFails(
    bobDb.collection("users").doc("legacy_student_uid").update({ role: "ADMIN" })
  );
});

test("Legacy curriculum docs with extra unknown fields & numeric createdAt can be overwritten cleanly by admin", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const rawDb = ctx.firestore();
    await rawDb.collection("courses").doc("C_LEGACY_101").set({
      id: "C_LEGACY_101",
      code: "PHYS 101",
      title: "Legacy Physics",
      category: "STEM",
      isPublished: true,
      legacyExtraField: "should_be_removed",
      createdAt: 1710000000000,
    });
    await rawDb.collection("notes").doc("NOTE_LEGACY_1").set({
      id: "NOTE_LEGACY_1",
      courseId: "C_LEGACY_101",
      topicId: "T_GEN",
      title: "Kinematics",
      chapterNumber: 1,
      contentMarkdown: "# Motion",
      isPublished: true,
      readTimeMinutes: 12,
      createdAt: 1710000000000,
    });
    await rawDb.collection("announcements").doc("ANN_LEGACY_1").set({
      id: "ANN_LEGACY_1",
      title: "Old Notice",
      content: "Legacy body",
      isUrgent: false,
      isPublished: true,
      targetCourseId: "C_LEGACY_101",
      targetCourseTitle: "Physics",
      createdAt: 1710000000000,
    });
  });

  const adminDb = verifiedAdminContext().firestore();
  const now = new Date();

  // Clean non-merge overwrite strips legacy extra keys and converts numeric createdAt to Timestamp
  await assertSucceeds(
    adminDb.collection("courses").doc("C_LEGACY_101").set({
      id: "C_LEGACY_101",
      code: "PHYS 101",
      title: "General Physics I",
      description: "Mechanics and Thermodynamics",
      category: "STEM",
      iconName: "science",
      totalQuestions: 10,
      totalNotes: 2,
      isPublished: true,
      orderIndex: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("notes").doc("NOTE_LEGACY_1").set({
      id: "NOTE_LEGACY_1",
      courseId: "C_LEGACY_101",
      topicId: "T_GEN",
      title: "Kinematics Updated",
      chapterNumber: 1,
      summary: "Velocity and acceleration",
      keyFormulas: "v = u + at",
      contentMarkdown: "# Motion\nv = u + at",
      content: "# Motion\nv = u + at",
      isPublished: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("announcements").doc("ANN_LEGACY_1").set({
      id: "ANN_LEGACY_1",
      title: "Updated Notice",
      content: "Clean cloud notice",
      message: "Clean cloud notice",
      category: "GENERAL",
      isUrgent: true,
      isPublished: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Once converted to a real Timestamp, mutating createdAt is still blocked
  await assertFails(
    adminDb.collection("courses").doc("C_LEGACY_101").update({
      createdAt: new Date(Date.now() - 60000),
    })
  );
});

test("Legacy student user doc with extra unknown fields, null fields, and numeric createdAt can be cleaned by owner and promoted by admin", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const rawDb = ctx.firestore();
    await rawDb.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      userId: ALICE_UID,
      email: "alice@ttsacademy.edu",
      name: "Alice Legacy",
      role: null,
      isPremium: null,
      activationCode: null,
      passwordHash: "legacy_hash",
      avatarUrl: "avatar_1",
      streakDays: 5,
      createdAt: 1710000000000,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@ttsacademy.edu",
    email_verified: true,
  }).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@ttsacademy.edu",
      name: "Alice Legacy",
      role: "STUDENT",
      department: "Computer Science",
      studentId: "STU-ALICE",
      isPremium: false,
      activationCode: "",
      questionsAnsweredCount: 0,
      correctAnswersCount: 0,
      enrolledCourses: ["MATH_101"],
      createdAt: now,
      updatedAt: now,
    })
  );

  const adminDb = verifiedAdminContext().firestore();
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@ttsacademy.edu",
      name: "Alice Legacy",
      role: "ADMIN",
      department: "Computer Science",
      studentId: "STU-ALICE",
      isPremium: true,
      activationCode: "",
      questionsAnsweredCount: 0,
      correctAnswersCount: 0,
      enrolledCourses: ["MATH_101"],
      createdAt: now,
      updatedAt: new Date(),
    })
  );
});


