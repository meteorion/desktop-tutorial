package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingEntity(@PrimaryKey val settingKey: String, val settingValue: String)
