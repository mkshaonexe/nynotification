package com.quietinbox.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.quietinbox.data.db.entity.ScheduleAppEntity
import com.quietinbox.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

data class ScheduleWithApps(
    val schedule: ScheduleEntity,
    val extraApps: List<String>
)

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules ORDER BY createdAt ASC")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE enabled = 1")
    suspend fun getEnabledSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    @Query("SELECT packageName FROM schedule_apps WHERE scheduleId = :scheduleId")
    suspend fun getExtraAppsForSchedule(scheduleId: Long): List<String>

    @Query("SELECT packageName FROM schedule_apps WHERE scheduleId = :scheduleId")
    fun getExtraAppsForScheduleFlow(scheduleId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleApps(apps: List<ScheduleAppEntity>)

    @Query("DELETE FROM schedule_apps WHERE scheduleId = :scheduleId")
    suspend fun deleteExtraAppsForSchedule(scheduleId: Long)

    @Transaction
    suspend fun replaceScheduleApps(scheduleId: Long, packageNames: List<String>) {
        deleteExtraAppsForSchedule(scheduleId)
        if (packageNames.isNotEmpty()) {
            insertScheduleApps(packageNames.map { ScheduleAppEntity(scheduleId, it) })
        }
    }
}
