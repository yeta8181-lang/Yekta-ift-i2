package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MomentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDao {
    @Query("SELECT * FROM moments ORDER BY timestamp DESC")
    fun getAllMoments(): Flow<List<MomentEntity>>

    @Query("SELECT * FROM moments ORDER BY timestamp DESC")
    suspend fun getAllMomentsDirect(): List<MomentEntity>

    @Query("SELECT * FROM moments WHERE id = :id LIMIT 1")
    suspend fun getMomentByIdDirect(id: String): MomentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: MomentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(moments: List<MomentEntity>)

    @Query("UPDATE moments SET isLiked = :isLiked, likesCount = :likesCount WHERE id = :id")
    suspend fun updateLike(id: String, isLiked: Boolean, likesCount: Int)

    @Query("UPDATE moments SET commentsJson = :commentsJson WHERE id = :id")
    suspend fun updateComments(id: String, commentsJson: String)

    @Query("DELETE FROM moments WHERE id = :id")
    suspend fun deleteMomentById(id: String)

    @Query("DELETE FROM moments WHERE authorId = :authorId")
    suspend fun deleteMomentsByAuthor(authorId: String)

    @Query("SELECT COUNT(*) FROM moments")
    suspend fun getMomentsCount(): Int

    @Query("DELETE FROM moments WHERE id LIKE 'moment_1' OR id LIKE 'moment_2' OR id LIKE 'moment_3' OR id LIKE 'moment_4' OR id LIKE 'moment_seed_%'")
    suspend fun deleteFakeMoments()

    @Query("DELETE FROM moments")
    suspend fun deleteAllMoments()
}
