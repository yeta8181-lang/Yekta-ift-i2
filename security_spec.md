# Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **PII Isolation (`/users/{userId}`)**: A private user record containing `email` can only be read, created, updated, or deleted by `request.auth.uid == userId`. Cross-user reads or unauthenticated reads are strictly rejected.
2. **Public Profile Integrity (`/public_profiles/{userId}`)**: Only `request.auth.uid == userId` can create, update, or delete their public profile. No PII (`email`) is permitted in `public_profiles`. List queries require `visibility == 'public'`.
3. **Moment Ownership & Likes (`/moments/{momentId}`)**: Moments can only be created or deleted by `authorId == request.auth.uid`. Updates are partitioned into author updates (`caption`, `location`, `likesCount`, `updatedAt`) and member like updates (`likesCount`, `updatedAt`).
4. **Like Isolation (`/likes/{likeId}`)**: Likes can only be created by `fromUid == request.auth.uid` where `fromUid != toUid`, and can only be read by `fromUid` or `toUid`.
5. **Conversation Participant Isolation (`/conversations/{channelId}/messages/{messageId}`)**: Messages are append-only (`allow update, delete: if false`), must be authored by `senderId == request.auth.uid`, and can only be read or listed by users present in `participantIds` (bounded 2-element list).

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read (`/users/alice_123`)**: `auth = null` -> Rejected by `isSignedIn()`.
2. **Cross-User PII Read (`/users/bob_456` by `alice_123`)**: Rejected by `request.auth.uid == userId`.
3. **Identity Spoofing on Create (`/users/alice_123` with `userId: "bob_456"`)**: Rejected by `incoming().userId == request.auth.uid`.
4. **Ghost Field Injection on Create (`/users/alice_123` with `isAdmin: true`)**: Rejected by `incoming().keys().hasOnly(...)`.
5. **Shadow Update on Public Profile (`affectedKeys` includes `createdAt` or `userId`)**: Rejected by `incoming().createdAt == existing().createdAt` and `affectedKeys().hasOnly(...)`.
6. **Underage Profile Creation (`age: 15` in `/public_profiles/alice_123`)**: Rejected by `data.age >= 18 && data.age <= 120`.
7. **Unfiltered List Query on `/users` without `userId == request.auth.uid`**: Rejected by `existing().userId == request.auth.uid`.
8. **Unfiltered List Query on `/public_profiles` without `visibility == 'public'`**: Rejected by `existing().visibility == 'public'`.
9. **Cross-User Like Read (`/likes/alice_likes_charlie` read by `bob_456`)**: Rejected by `existing().fromUid == request.auth.uid || existing().toUid == request.auth.uid`.
10. **Self-Like Creation (`fromUid == toUid` in `/likes/alice_likes_alice`)**: Rejected by `data.fromUid != data.toUid`.
11. **Unauthorized Chat Message Read (`/conversations/ch1/messages/m1` by non-participant)**: Rejected by `request.auth.uid in existing().participantIds`.
12. **Chat Message Tampering (`update` or `delete` on `/conversations/ch1/messages/m1`)**: Rejected by `allow update, delete: if false`.
