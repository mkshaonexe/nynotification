package com.quietinbox.feature.rules.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO interface for the Rules UI (Allow Rules and Muted Apps).
 */
@Dao
interface RulesUiDao {

    @Query("SELECT * FROM allow_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<AllowRuleEntity>>

    @Query("SELECT * FROM allow_rules WHERE type = :type ORDER BY createdAt DESC")
    fun getRulesByType(type: String): Flow<List<AllowRuleEntity>>

    @Query("SELECT * FROM allow_rules WHERE enabled = 1")
    fun getEnabledRulesFlow(): Flow<List<AllowRuleEntity>>

    @Query("SELECT * FROM allow_rules WHERE enabled = 1")
    suspend fun getEnabledRules(): List<AllowRuleEntity>

    @Query("SELECT * FROM allow_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: Long): AllowRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AllowRuleEntity): Long

    @Update
    suspend fun updateRule(rule: AllowRuleEntity)

    @Delete
    suspend fun deleteRule(rule: AllowRuleEntity)

    @Query("DELETE FROM allow_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("SELECT * FROM muted_apps ORDER BY mutedAt DESC")
    fun getAllMutedApps(): Flow<List<MutedAppEntity>>

    @Query("SELECT packageName FROM muted_apps")
    suspend fun getMutedPackageNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM muted_apps WHERE packageName = :packageName)")
    suspend fun isAppMuted(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun muteApp(app: MutedAppEntity)

    @Query("DELETE FROM muted_apps WHERE packageName = :packageName")
    suspend fun unmuteApp(packageName: String)
}
