package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 0 AND isBlocked = 0 AND isReported = 0")
    fun getAllOtherProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM users WHERE isCurrentUser = 0")
    suspend fun getAllOtherProfilesDirect(): List<UserProfileEntity>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserDirect(): UserProfileEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmailDirect(email: String): UserProfileEntity?

    @Query("UPDATE users SET isCurrentUser = 0 WHERE isCurrentUser = 1")
    suspend fun clearCurrentUserFlags()

    @Query("UPDATE users SET isCurrentUser = 1 WHERE id = :id")
    suspend fun setActiveCurrentUser(id: String)

    @Query("SELECT * FROM users WHERE isMatch = 1 AND isBlocked = 0 AND isReported = 0")
    fun getMatches(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdDirect(id: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserProfileEntity>)

    @Update
    suspend fun updateUser(user: UserProfileEntity)

    @Query("UPDATE users SET isLikedByMe = 1, isMatch = :isMatch, isSuperLiked = :isSuperLike WHERE id = :id")
    suspend fun setLiked(id: String, isMatch: Boolean, isSuperLike: Boolean)

    @Query("UPDATE users SET isBlocked = 1 WHERE id = :id")
    suspend fun blockUser(id: String)

    @Query("UPDATE users SET isReported = 1, isBlocked = 1 WHERE id = :id")
    suspend fun reportUser(id: String)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    @Query("UPDATE users SET isVerified = 1 WHERE isCurrentUser = 1")
    suspend fun verifyCurrentUser()

    @Query("UPDATE users SET isVip = :isVip WHERE isCurrentUser = 1")
    suspend fun updateVipStatus(isVip: Boolean)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("DELETE FROM users WHERE isCurrentUser = 0 AND (id LIKE 'user_%' OR id LIKE 'seed_%' OR id LIKE 'demo_%')")
    suspend fun deleteFakeProfiles()

    @Query("DELETE FROM users WHERE isCurrentUser = 0")
    suspend fun deleteAllOtherUsers()
}
