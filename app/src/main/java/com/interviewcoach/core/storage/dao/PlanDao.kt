package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.interviewcoach.core.storage.entity.PlanEntity

@Dao
interface PlanDao {
    @Insert suspend fun insertPlan(plan: PlanEntity)

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getPlanById(id: String): PlanEntity?

    @Query("SELECT * FROM plans")
    suspend fun getAllPlans(): List<PlanEntity>

    @Query("UPDATE plans SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
