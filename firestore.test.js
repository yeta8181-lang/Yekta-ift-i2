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
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const nowTimestamp = () => new Date(Date.now() - 1000);

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

// --- DIRTY DOZEN & SECURITY BOUNDS TESTS ---

test("1. Unauthenticated user cannot read or write /users or /public_profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("public_profiles").get());
});

test("2. Authenticated user cannot read another user's private PII (/users/{userId})", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      name: "Bob",
      email: "bob@example.com",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("3. Authenticated owner can initialize and read their own record with bio, interests, and geolocation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      bio: "Kahve ve müzik tutkunu",
      interests: ["Kahve ☕", "Müzik 🎵", "Seyahat ✈️"],
      geolocation: { latitude: 41.0082, longitude: 28.9784 },
      latitude: 41.0082,
      longitude: 28.9784,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Invalid out-of-range geolocation latitude (> 90) is rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      bio: "Kahve",
      interests: ["Kahve ☕"],
      geolocation: { latitude: 145.0, longitude: 28.9784 },
      latitude: 145.0,
      longitude: 28.9784,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
});

test("4. Identity spoofing and ghost fields on /users are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Spoofing userId
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: BOB_UID,
      name: "Alice",
      email: "alice@example.com",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
  // Ghost field isAdmin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      isAdmin: true,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
});

test("5. Public profile creation with bio, interests, geolocation, age validation, and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const validProfile = {
    userId: ALICE_UID,
    name: "Alice",
    age: 25,
    city: "İstanbul",
    bio: "Merhaba",
    occupation: "Mimar",
    education: "İTÜ",
    heightCm: 170,
    zodiac: "Aslan",
    lookingFor: "Ciddi İlişki",
    photosJson: "https://example.com/1.jpg",
    interestsJson: "Kahve|||Müzik",
    interests: ["Kahve", "Müzik"],
    geolocation: { latitude: 41.0082, longitude: 28.9784 },
    lifestyleJson: "Spor",
    promptQuestion: "En sevdiğim yer",
    promptAnswer: "Moda Sahili",
    isVerified: true,
    isVip: false,
    latitude: 41.0082,
    longitude: 28.9784,
    isOnline: true,
    visibility: "public",
    createdAt: nowTimestamp(),
    updatedAt: nowTimestamp(),
  };

  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE_UID).set(validProfile));

  // Underage profile rejected
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      ...validProfile,
      age: 16,
    })
  );

  // Bob can list public profiles ONLY with visibility == 'public' filter
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("public_profiles").where("visibility", "==", "public").get()
  );
});

test("6. Likes isolation: only sender and recipient can read, no self-likes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("likes").doc(`${ALICE_UID}_likes_${BOB_UID}`).set({
      fromUid: ALICE_UID,
      toUid: BOB_UID,
      isSuperLike: true,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );

  // Self-like rejected
  await assertFails(
    aliceDb.collection("likes").doc(`${ALICE_UID}_likes_${ALICE_UID}`).set({
      fromUid: ALICE_UID,
      toUid: ALICE_UID,
      isSuperLike: false,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );

  // Charlie cannot read Alice->Bob like
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("likes").doc(`${ALICE_UID}_likes_${BOB_UID}`).get());

  // Bob can query incoming likes where toUid == BOB_UID ("Seni Beğenenler")
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("likes").where("toUid", "==", BOB_UID).get()
  );
});

test("7. Chat messages participant isolation and immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const channelId = `${ALICE_UID}_${BOB_UID}`;
  const msgRef = aliceDb.collection("conversations").doc(channelId).collection("messages").doc("msg_1");

  await assertSucceeds(
    msgRef.set({
      channelId,
      senderId: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      text: "Selam!",
      createdAt: nowTimestamp(),
    })
  );

  // Bob (participant) can query with array-contains participantIds
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc(channelId)
      .collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  // Charlie (non-participant) cannot read or query
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("conversations").doc(channelId).collection("messages").doc("msg_1").get()
  );

  // Message update/delete is forbidden
  await assertFails(msgRef.update({ text: "Tampered" }));
  await assertFails(msgRef.delete());
});

test("8. Moments text-only (no photo, no location) and Firebase post likes (likedUserIds) work and admin yeta8181@gmail.com has full privileges", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext("admin_uid_999", { email: "yeta8181@gmail.com" }).firestore();

  // Alice creates a text-only moment without photo or location
  const momentRef = aliceDb.collection("moments").doc("moment_text_1");
  await assertSucceeds(
    momentRef.set({
      id: "moment_text_1",
      authorId: ALICE_UID,
      authorName: "Alice",
      authorAvatar: "https://example.com/a.jpg",
      mediaUri: "",
      mediaType: "NONE",
      caption: "Bugün hava harika!",
      location: "",
      likesCount: 0,
      likedUserIds: [],
      commentsJson: "",
      visibility: "public",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );

  // Bob likes Alice's moment in Firebase
  await assertSucceeds(
     bobDb.collection("moments").doc("moment_text_1").update({
      likesCount: 1,
      likedUserIds: [BOB_UID],
      updatedAt: nowTimestamp(),
    })
  );

  // Bob cannot delete Alice's moment
  await assertFails(bobDb.collection("moments").doc("moment_text_1").delete());

  // Admin (yeta8181@gmail.com) CAN delete Alice's moment and can delete user profiles
  await assertSucceeds(adminDb.collection("moments").doc("moment_text_1").delete());

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("public_profiles").doc(BOB_UID).set({
      userId: BOB_UID,
      name: "Bob",
      age: 28,
      visibility: "public",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    });
  });

  await assertSucceeds(adminDb.collection("public_profiles").doc(BOB_UID).delete());
});

