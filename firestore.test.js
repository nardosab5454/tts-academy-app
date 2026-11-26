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

test("Legacy IDs with spaces/dots, lowercase 'admin' role doc, and preserved clock-skewed createdAt succeed while path-traversal and student writes fail", async () => {
  const futureSkew = new Date(Date.now() + 60 * 1000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const rawDb = ctx.firestore();
    await rawDb.collection("users").doc("lowercase_admin_uid").set({
      userId: "lowercase_admin_uid",
      email: "faculty@ttsacademy.edu",
      name: "Faculty Admin",
      role: "admin",
      isPremium: true,
      createdAt: new Date(Date.now() - 10000),
    });
    await rawDb.collection("courses").doc("MATH 101.v1").set({
      id: "MATH 101.v1",
      code: "MATH 101",
      title: "Freshman Calculus",
      category: "STEM",
      isPublished: true,
      createdAt: futureSkew,
    });
  });

  const lowerAdminDb = testEnv.authenticatedContext("lowercase_admin_uid", {
    email: "faculty@ttsacademy.edu",
    email_verified: true,
  }).firestore();
  const abebeDb = verifiedAdminContext("abebe_uid", "abebewottee@gmail.com").firestore();
  const studentDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@ttsacademy.edu",
    email_verified: true,
  }).firestore();

  // Lowercase 'admin' role user can update existing course with space/dot ID & preserved future-skewed createdAt
  await assertSucceeds(
    lowerAdminDb.collection("courses").doc("MATH 101.v1").set({
      id: "MATH 101.v1",
      code: "MATH 101",
      title: "Freshman Calculus Updated",
      description: "Limits and Derivatives",
      category: "STEM",
      iconName: "school",
      totalQuestions: 1,
      totalNotes: 1,
      isPublished: true,
      orderIndex: 1,
      createdAt: futureSkew,
      updatedAt: new Date(),
    })
  );

  // Bootstrapped admin (abebewottee@gmail.com) adds topic, note, question, past_exam referencing legacy courseId
  const now = new Date();
  await assertSucceeds(
    abebeDb.collection("topics").doc("T.1 Limits").set({
      id: "T.1 Limits",
      courseId: "MATH 101.v1",
      title: "Limits and Continuity",
      description: "Core limit theorems",
      orderIndex: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    abebeDb.collection("notes").doc("NOTE.1.1").set({
      id: "NOTE.1.1",
      courseId: "MATH 101.v1",
      topicId: "T.1 Limits",
      title: "Chapter 1: Limits",
      chapterNumber: 1,
      summary: "Epsilon-delta definition",
      keyFormulas: "lim x->0 sin(x)/x = 1",
      contentMarkdown: "# Limits\nComplete lecture notes",
      content: "# Limits\nComplete lecture notes",
      isPublished: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    abebeDb.collection("questions").doc("Q.1.1").set({
      id: "Q.1.1",
      courseId: "MATH 101.v1",
      topicId: "T.1 Limits",
      questionText: "What is lim(x->0) sin(x)/x?",
      optionA: "1",
      optionB: "0",
      optionC: "Infinity",
      optionD: "-1",
      correctAnswer: "A",
      isPublished: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student can immediately read the newly published legacy-ID course, topic, note, and question
  await assertSucceeds(studentDb.collection("courses").doc("MATH 101.v1").get());
  await assertSucceeds(studentDb.collection("topics").doc("T.1 Limits").get());
  await assertSucceeds(studentDb.collection("notes").doc("NOTE.1.1").get());
  await assertSucceeds(studentDb.collection("questions").doc("Q.1.1").get());

  // Student still cannot write or delete
  await assertFails(studentDb.collection("notes").doc("NOTE.1.1").delete());

  // Leading/trailing whitespace IDs are still rejected
  await assertFails(
    abebeDb.collection("courses").doc(" BAD_ID ").set({
      id: " BAD_ID ",
      code: "BAD",
      title: "Bad",
      category: "STEM",
      isPublished: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Exact Kotlin client payloads succeed across all admin collections for spikenarrd, abebewottee, and role-promoted ADMIN", async () => {
  const spikeDb = verifiedAdminContext("spike_uid", "spikenarrd@gmail.com").firestore();
  const abebeDb = verifiedAdminContext("abebe_uid", "abebewottee@gmail.com").firestore();
  const createdAt = new Date(Date.now() - 2000);
  const updatedAt = new Date();

  // Promote a non-bootstrapped user via users/{uid}.role == 'ADMIN'
  await assertSucceeds(
    spikeDb.collection("users").doc("promoted_admin_uid").set({
      userId: "promoted_admin_uid",
      email: "dean@ttsacademy.edu",
      name: "Faculty Dean",
      role: "ADMIN",
      department: "Science College",
      studentId: "ADM-DEAN01",
      isPremium: true,
      activationCode: "",
      questionsAnsweredCount: 0,
      correctAnswersCount: 0,
      enrolledCourses: ["MATH_101"],
      createdAt,
      updatedAt,
    })
  );

  const promotedDb = verifiedAdminContext("promoted_admin_uid", "dean@ttsacademy.edu").firestore();

  // Announcement (exact Kotlin saveAnnouncement payload)
  await assertSucceeds(
    abebeDb.collection("announcements").doc("ann_12345678").set({
      id: "ann_12345678",
      title: "Final Exam Schedule",
      content: "All freshman final exams start next week.",
      message: "All freshman final exams start next week.",
      category: "EXAM",
      isUrgent: true,
      isPublished: true,
      createdAt,
      updatedAt,
    })
  );

  // Course (exact Kotlin saveCourse payload)
  await assertSucceeds(
    spikeDb.collection("courses").doc("C_AB12CD").set({
      id: "C_AB12CD",
      code: "MATH 103",
      title: "Linear Algebra",
      description: "Matrices, vectors, and eigenvalues",
      category: "STEM",
      iconName: "school",
      totalQuestions: 0,
      totalNotes: 0,
      isPublished: true,
      orderIndex: 15,
      createdAt,
      updatedAt,
    })
  );

  // Topic (exact Kotlin saveTopic payload)
  await assertSucceeds(
    promotedDb.collection("topics").doc("T_123456").set({
      id: "T_123456",
      courseId: "C_AB12CD",
      title: "Vector Spaces",
      description: "Basis and dimension",
      orderIndex: 1,
      createdAt,
      updatedAt,
    })
  );

  // Question (exact Kotlin saveQuestion payload with optional empty strings)
  await assertSucceeds(
    promotedDb.collection("questions").doc("Q_123456").set({
      id: "Q_123456",
      courseId: "C_AB12CD",
      topicId: "T_123456",
      questionText: "What is the determinant of the 2x2 identity matrix?",
      optionA: "1",
      optionB: "0",
      optionC: "",
      optionD: "",
      correctAnswer: "A",
      explanationGeneral: "",
      explanationA: "",
      explanationB: "",
      explanationC: "",
      explanationD: "",
      difficulty: "MEDIUM",
      isPastExam: false,
      examYear: "",
      examType: "",
      isVerified: true,
      isPublished: true,
      createdAt,
      updatedAt,
    })
  );

  // Note (exact Kotlin saveNote payload)
  await assertSucceeds(
    abebeDb.collection("notes").doc("NOTE_123456").set({
      id: "NOTE_123456",
      courseId: "C_AB12CD",
      topicId: "T_123456",
      title: "Chapter 1: Matrices",
      chapterNumber: 1,
      summary: "",
      keyFormulas: "",
      contentMarkdown: "# Matrices\nDefinition and operations.",
      content: "# Matrices\nDefinition and operations.",
      isPublished: true,
      createdAt,
      updatedAt,
    })
  );

  // Past Exam (exact Kotlin savePastExam payload)
  await assertSucceeds(
    spikeDb.collection("past_exams").doc("EXAM_C_AB12CD_2025").set({
      id: "EXAM_C_AB12CD_2025",
      courseId: "C_AB12CD",
      university: "Addis Ababa University (AAU)",
      academicYear: "2025",
      semester: "Semester 1",
      examType: "FINAL",
      title: "Linear Algebra Final",
      instructions: "",
      totalQuestions: 20,
      durationMinutes: 90,
      isDigitized: true,
      isVerified: true,
      documentSummary: "",
      isPublished: true,
      createdAt,
      updatedAt,
    })
  );

  // Access Code (exact Kotlin saveAccessCode payload)
  await assertSucceeds(
    promotedDb.collection("access_codes").doc("987654").set({
      code: "987654",
      createdByAdminId: "promoted_admin_uid",
      isRedeemed: false,
      redeemedByUserId: "",
      redeemedByEmail: "",
      note: "Permanent Academic Access Pass",
      expiresAt: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000),
      createdAt,
      updatedAt,
    })
  );

  // Admin Invite (exact Kotlin saveAdminInvite payload)
  await assertSucceeds(
    abebeDb.collection("admin_invites").doc("newadmin@ttsacademy.edu").set({
      email: "newadmin@ttsacademy.edu",
      department: "Academic Directorate",
      invitedBy: "abebe_uid",
      createdAt,
      updatedAt,
    })
  );

  // Role-promoted admin can update announcement & course preserving createdAt
  await assertSucceeds(
    promotedDb.collection("announcements").doc("ann_12345678").set({
      id: "ann_12345678",
      title: "Final Exam Schedule (Updated)",
      content: "Updated timetable posted.",
      message: "Updated timetable posted.",
      category: "EXAM",
      isUrgent: false,
      isPublished: true,
      createdAt,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    promotedDb.collection("courses").doc("C_AB12CD").update({
      totalQuestions: 1,
      totalNotes: 1,
      updatedAt: new Date(),
    })
  );
});

test("Explicit null optional fields on partial updates, SUPER_ADMIN role, trimmed/mixed-case email, and diag_* write self-test probe succeed", async () => {
  const createdAt = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const rawDb = ctx.firestore();
    // Legacy course document with explicit null optional fields
    await rawDb.collection("courses").doc("C_NULL_OPTS").set({
      id: "C_NULL_OPTS",
      code: "CHEM 101",
      title: "General Chemistry",
      description: null,
      category: "STEM",
      iconName: null,
      totalQuestions: 0,
      totalNotes: 0,
      isPublished: true,
      orderIndex: 5,
      createdAt,
    });
    // SUPER_ADMIN user document with explicit null optional fields
    await rawDb.collection("users").doc("super_admin_uid").set({
      userId: "super_admin_uid",
      email: "director@ttsacademy.edu",
      name: "Academic Director",
      role: "SUPER_ADMIN",
      department: null,
      studentId: null,
      isPremium: true,
      activationCode: null,
      createdAt,
    });
  });

  const superAdminDb = testEnv.authenticatedContext("super_admin_uid", {
    email: "director@ttsacademy.edu",
    email_verified: true,
  }).firestore();
  const mixedCaseBootstrappedDb = verifiedAdminContext("abebe_trim_uid", "  AbebeWottee@Gmail.com ").firestore();
  const studentDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@ttsacademy.edu",
    email_verified: true,
  }).firestore();

  // Partial update on course with null optional fields succeeds for SUPER_ADMIN
  await assertSucceeds(
    superAdminDb.collection("courses").doc("C_NULL_OPTS").update({
      totalQuestions: 12,
      totalNotes: 4,
      updatedAt: new Date(),
    })
  );

  // Partial update on user doc with null optional fields succeeds for owner
  await assertSucceeds(
    superAdminDb.collection("users").doc("super_admin_uid").update({
      questionsAnsweredCount: 5,
      correctAnswersCount: 4,
      updatedAt: new Date(),
    })
  );

  // Live write self-test probe (CREATE -> READ -> DELETE) on announcements/diag_* succeeds for trimmed/mixed-case bootstrapped admin
  const probeRef = mixedCaseBootstrappedDb.collection("announcements").doc("diag_abebe_trim_uid");
  await assertSucceeds(
    probeRef.set({
      id: "diag_abebe_trim_uid",
      title: "Admin Write Check",
      content: "Temporary write verification probe",
      message: "Temporary write verification probe",
      category: "GENERAL",
      isUrgent: false,
      isPublished: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(probeRef.get());
  await assertSucceeds(probeRef.delete());

  // Student is strictly denied from running the admin write self-test probe
  await assertFails(
    studentDb.collection("announcements").doc("diag_alice_student").set({
      id: "diag_alice_student",
      title: "Student Probe",
      content: "Should fail",
      message: "Should fail",
      category: "GENERAL",
      isUrgent: false,
      isPublished: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Custom claim 'admin' role (role: 'admin' and admin: true) explicitly grants admin write access; non-admin claims denied", async () => {
  const claimRoleAdminDb = testEnv.authenticatedContext("claim_admin_1", {
    email: "customclaim1@ttsacademy.edu",
    role: "admin",
  }).firestore();
  const claimBoolAdminDb = testEnv.authenticatedContext("claim_admin_2", {
    email: "customclaim2@ttsacademy.edu",
    admin: true,
  }).firestore();
  const studentClaimDb = testEnv.authenticatedContext("claim_student_1", {
    email: "studentclaim@ttsacademy.edu",
    role: "student",
    admin: false,
  }).firestore();
  const spoofedClaimDb = testEnv.authenticatedContext("claim_spoof_1", {
    email: "spoofclaim@ttsacademy.edu",
    role: "administrator",
    admin: "true",
  }).firestore();

  const createdAt = new Date(Date.now() - 2000);

  // User with custom claim role == 'admin' (no bootstrapped email, no invite, no users/{uid} doc) can CRUD courses & announcements
  await assertSucceeds(
    claimRoleAdminDb.collection("courses").doc("C_CLAIM_101").set({
      id: "C_CLAIM_101",
      code: "CS 105",
      title: "Discrete Mathematics",
      description: "Logic, sets, and graph theory",
      category: "STEM",
      iconName: "school",
      totalQuestions: 0,
      totalNotes: 0,
      isPublished: true,
      orderIndex: 20,
      createdAt,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    claimRoleAdminDb.collection("courses").doc("C_CLAIM_101").update({
      title: "Discrete Mathematics I",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    claimRoleAdminDb.collection("announcements").doc("ANN_CLAIM_1").set({
      id: "ANN_CLAIM_1",
      title: "Claim Admin Announcement",
      content: "Published using custom claim role='admin'.",
      message: "Published using custom claim role='admin'.",
      category: "GENERAL",
      isUrgent: false,
      isPublished: true,
      createdAt,
      updatedAt: new Date(),
    })
  );

  // User with custom claim admin == true can also update & delete
  await assertSucceeds(
    claimBoolAdminDb.collection("announcements").doc("ANN_CLAIM_1").update({
      isUrgent: true,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    claimBoolAdminDb.collection("courses").doc("C_CLAIM_101").delete()
  );

  // Non-admin and invalid-typed custom claims are strictly denied
  await assertFails(
    studentClaimDb.collection("announcements").doc("ANN_DENIED_1").set({
      id: "ANN_DENIED_1",
      title: "Unauthorized",
      content: "Student claim should fail",
      isUrgent: false,
      isPublished: true,
      createdAt,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    spoofedClaimDb.collection("announcements").doc("ANN_DENIED_2").set({
      id: "ANN_DENIED_2",
      title: "Unauthorized",
      content: "Spoofed claim should fail",
      isUrgent: false,
      isPublished: true,
      createdAt,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    spoofedClaimDb.collection("announcements").doc("ANN_CLAIM_1").delete()
  );
});




