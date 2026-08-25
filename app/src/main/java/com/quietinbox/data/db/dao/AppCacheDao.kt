package com.quietinbox.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quietinbox.data.db.entity.AppCacheEntity

@Dao
interface AppCacheDao {

    @Query("SELECT * FROM app_cache ORDER BY label ASC")
    suspend fun getAll(): List<AppCacheEntity>

    @Query("SELECT * FROM app_cache WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): AppCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(app: AppCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(apps: List<AppCacheEntity>)

    @Query("DELETE FROM app_cache WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
