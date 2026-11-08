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

test("Unauthenticated user: cannot read tasks or routines", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("routines").get());
});

test("Authenticated user: cannot read another user's tasks or routines", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task1").set({
      userId: ALICE_UID,
      title: "Alice Task",
      priority: "high",
      category: "Work",
      isCompleted: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
});

test("Authenticated user: can create, read, update, and delete their own task", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task1");

  const now = new Date();
  await assertSucceeds(taskRef.set({
    userId: ALICE_UID,
    title: "Alice First Task",
    priority: "high",
    category: "Personal",
    isCompleted: false,
    createdAt: now,
    updatedAt: now,
  }));

  await assertSucceeds(taskRef.get());

  await assertSucceeds(taskRef.update({
    isCompleted: true,
    completedAt: now,
    updatedAt: now,
  }));

  await assertSucceeds(taskRef.delete());
});

test("Task validation: rejects invalid priority or missing required fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task1");

  const now = new Date();
  // Invalid priority
  await assertFails(taskRef.set({
    userId: ALICE_UID,
    title: "Bad Task",
    priority: "super-extreme",
    category: "Work",
    isCompleted: false,
    createdAt: now,
    updatedAt: now,
  }));

  // Missing category
  await assertFails(taskRef.set({
    userId: ALICE_UID,
    title: "Missing Cat",
    priority: "medium",
    isCompleted: false,
    createdAt: now,
    updatedAt: now,
  }));
});

test("Authenticated user: can create and update routine", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const routineRef = aliceDb.collection("users").doc(ALICE_UID).collection("routines").doc("routine1");

  const now = new Date();
  await assertSucceeds(routineRef.set({
    userId: ALICE_UID,
    title: "Morning Habit",
    timeOfDay: "morning",
    targetTime: "07:00",
    daysOfWeek: ["MON", "TUE", "WED", "THU", "FRI"],
    steps: ["Drink water", "Stretch", "Plan tasks"],
    currentStreak: 0,
    bestStreak: 0,
    createdAt: now,
    updatedAt: now,
  }));

  await assertSucceeds(routineRef.get());

  await assertSucceeds(routineRef.update({
    currentStreak: 1,
    bestStreak: 1,
    lastCompletedDate: "2026-11-08",
    updatedAt: now,
  }));
});
