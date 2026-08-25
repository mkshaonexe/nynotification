package com.quietinbox.feature.schedules.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.quietinbox.data.db.entity.ScheduleAppEntity
import com.quietinbox.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * DAO interface providing Schedule UI queries and multi-table operations.
 */
@Dao
abstract class SchedulesUiDao {

    @Query("SELECT * FROM schedules ORDER BY createdAt DESC")
    abstract fun observeAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE enabled = 1 ORDER BY createdAt DESC")
    abstract suspend fun getEnabledSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    abstract suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Query("SELECT packageName FROM schedule_apps WHERE scheduleId = :scheduleId")
    abstract suspend fun getAppsForSchedule(scheduleId: Long): List<String>

    @Query("SELECT * FROM schedule_apps")
    abstract fun observeAllScheduleApps(): Flow<List<ScheduleAppEntity>>

    @Query("SELECT * FROM schedule_apps")
    abstract suspend fun getAllScheduleApps(): List<ScheduleAppEntity>

    open fun observeAllWithApps(): Flow<List<ScheduleWithApps>> {
        return combine(
            observeAllSchedules(),
            observeAllScheduleApps()
        ) { schedules, apps ->
            val appsMap = apps.groupBy({ it.scheduleId }, { it.packageName })
            schedules.map { schedule ->
                ScheduleWithApps(
                    schedule = schedule,
                    extraAllowedApps = appsMap[schedule.id]?.toSet() ?: emptySet()
                )
            }
        }
    }

    open suspend fun getEnabledWithApps(): List<ScheduleWithApps> {
        val schedules = getEnabledSchedules()
        val apps = getAllScheduleApps().groupBy({ it.scheduleId }, { it.packageName })
        return schedules.map { schedule ->
            ScheduleWithApps(
                schedule = schedule,
                extraAllowedApps = apps[schedule.id]?.toSet() ?: emptySet()
            )
        }
    }

    open suspend fun getByIdWithApps(id: Long): ScheduleWithApps? {
        val schedule = getScheduleById(id) ?: return null
        val apps = getAppsForSchedule(id).toSet()
        return ScheduleWithApps(schedule = schedule, extraAllowedApps = apps)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertScheduleApps(apps: List<ScheduleAppEntity>)

    @Query("DELETE FROM schedule_apps WHERE scheduleId = :scheduleId")
    abstract suspend fun deleteAppsForSchedule(scheduleId: Long)

    @Transaction
    open suspend fun saveScheduleWithApps(schedule: ScheduleEntity, extraAllowedApps: Set<String>): Long {
        val scheduleId = insertSchedule(schedule)
        deleteAppsForSchedule(scheduleId)
        if (extraAllowedApps.isNotEmpty()) {
            val appEntities = extraAllowedApps.map { ScheduleAppEntity(scheduleId = scheduleId, packageName = it) }
            insertScheduleApps(appEntities)
        }
        return scheduleId
    }

    @Query("UPDATE schedules SET enabled = :enabled WHERE id = :id")
    abstract suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM schedules WHERE id = :id")
    abstract suspend fun deleteScheduleById(id: Long)
}
