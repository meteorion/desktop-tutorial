package com.interviewcoach.core.storage

import android.database.Cursor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.PositionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `inserts and reads back a position with a knowledge point`() = runTest {
        db.positionDao().upsertPosition(PositionEntity(id = "p1", name = "后端开发-Java"))
        db.positionDao().upsertKnowledgePoint(
            KnowledgePointEntity(id = "kp1", positionId = "p1", name = "Java 基础", weight = 1, difficulty = 1, isCore = false),
        )

        val points = db.positionDao().getAllKnowledgePoints()
        assertThat(points).hasSize(1)
        assertThat(points.first().name).isEqualTo("Java 基础")
        assertThat(points.first().positionId).isEqualTo("p1")
    }

    @Test
    fun `schema has all ten tables required by the architecture doc`() {
        val tableNames = mutableSetOf<String>()
        val cursor: Cursor = db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'")
        cursor.use { while (it.moveToNext()) tableNames.add(it.getString(0)) }
        assertThat(tableNames).containsAtLeastElementsIn(
            setOf(
                "positions", "knowledge_points", "plans", "daily_tasks", "questions",
                "attempts", "mastery_records", "mock_interview_sessions", "review_reports", "app_settings",
            ),
        )
    }
}
