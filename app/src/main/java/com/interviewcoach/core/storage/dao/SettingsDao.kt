package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.interviewcoach.core.storage.entity.AppSettingEntity

@Dao
interface SettingsDao {
    @Upsert suspend fun upsertSetting(entity: AppSettingEntity)

    @Query("SELECT settingValue FROM app_settings WHERE settingKey = :key")
    suspend fun getSetting(key: String): String?

    suspend fun setSetting(key: String, value: String) = upsertSetting(AppSettingEntity(key, value))
}
