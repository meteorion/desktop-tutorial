package com.interviewcoach.core.storage

import android.content.Context
import androidx.room.Room
import com.interviewcoach.core.storage.dao.AttemptDao
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.MockInterviewDao
import com.interviewcoach.core.storage.dao.PlanDao
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.dao.QuestionDao
import com.interviewcoach.core.storage.dao.ReviewReportDao
import com.interviewcoach.core.storage.dao.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "interview_coach.db").build()

    @Provides fun providePositionDao(db: AppDatabase): PositionDao = db.positionDao()
    @Provides fun providePlanDao(db: AppDatabase): PlanDao = db.planDao()
    @Provides fun provideDailyTaskDao(db: AppDatabase): DailyTaskDao = db.dailyTaskDao()
    @Provides fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()
    @Provides fun provideAttemptDao(db: AppDatabase): AttemptDao = db.attemptDao()
    @Provides fun provideMasteryDao(db: AppDatabase): MasteryDao = db.masteryDao()
    @Provides fun provideMockInterviewDao(db: AppDatabase): MockInterviewDao = db.mockInterviewDao()
    @Provides fun provideReviewReportDao(db: AppDatabase): ReviewReportDao = db.reviewReportDao()
    @Provides fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
}
