package com.interviewcoach.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.interviewcoach.core.storage.dao.AttemptDao
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.MockInterviewDao
import com.interviewcoach.core.storage.dao.PlanDao
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.dao.QuestionDao
import com.interviewcoach.core.storage.dao.ReviewReportDao
import com.interviewcoach.core.storage.dao.SettingsDao
import com.interviewcoach.core.storage.entity.AppSettingEntity
import com.interviewcoach.core.storage.entity.AttemptEntity
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.MasteryRecordEntity
import com.interviewcoach.core.storage.entity.MockInterviewSessionEntity
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.core.storage.entity.PositionEntity
import com.interviewcoach.core.storage.entity.QuestionEntity
import com.interviewcoach.core.storage.entity.ReviewReportEntity

@Database(
    entities = [
        PositionEntity::class, KnowledgePointEntity::class, PlanEntity::class,
        DailyTaskEntity::class, QuestionEntity::class, AttemptEntity::class,
        MasteryRecordEntity::class, MockInterviewSessionEntity::class,
        ReviewReportEntity::class, AppSettingEntity::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao
    abstract fun planDao(): PlanDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun questionDao(): QuestionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun masteryDao(): MasteryDao
    abstract fun mockInterviewDao(): MockInterviewDao
    abstract fun reviewReportDao(): ReviewReportDao
    abstract fun settingsDao(): SettingsDao
}
