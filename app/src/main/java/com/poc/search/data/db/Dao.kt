package com.poc.search.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImages(items: List<LocalImageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImage(item: LocalImageEntity)

    @Query("SELECT * FROM local_images WHERE localUri = :localUri LIMIT 1")
    suspend fun getImage(localUri: String): LocalImageEntity?

    @Query(
        """
        SELECT * FROM local_images
        WHERE daycareId = :daycareId
        ORDER BY 
            CASE WHEN sortRank IS NULL THEN 1 ELSE 0 END,
            sortRank ASC,
            capturedAtEpochMs DESC
        """
    )
    fun observeImages(daycareId: String): Flow<List<LocalImageEntity>>

    @Query("UPDATE local_images SET sortRank = NULL WHERE daycareId = :daycareId")
    suspend fun clearSortRanks(daycareId: String)

    @Query("UPDATE local_images SET sortRank = :rank WHERE daycareId = :daycareId AND serverImageId = :serverImageId")
    suspend fun setSortRank(daycareId: String, serverImageId: String, rank: Int)

    @Query("UPDATE local_images SET uploadState = :state, serverImageId = :serverImageId, uploadedAtEpochMs = :uploadedAt, width = :width, height = :height WHERE localUri = :localUri")
    suspend fun markUploaded(
        localUri: String,
        state: UploadState,
        serverImageId: String?,
        uploadedAt: Long?,
        width: Int?,
        height: Int?
    )
}

@Dao
interface LocalInstanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstances(items: List<LocalInstanceEntity>)

    @Query("SELECT * FROM local_instances WHERE localUri = :localUri ORDER BY confidence DESC")
    fun observeInstancesForLocalUri(localUri: String): Flow<List<LocalInstanceEntity>>

    @Query("SELECT * FROM local_instances WHERE localUri = :localUri ORDER BY confidence DESC")
    suspend fun getInstancesForLocalUri(localUri: String): List<LocalInstanceEntity>

    @Query("UPDATE local_instances SET petId = :petId WHERE instanceId = :instanceId")
    suspend fun setPetId(instanceId: String, petId: String?)
}
