package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp-weak", "name": "系统设计", "weight": 4, "difficulty": 3, "isCore": true},
    {"id": "kp-strong", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false},
    {"id": "kp-untouched", "name": "并发编程", "weight": 3, "difficulty": 2, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class FreeLearningServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun service() = FreeLearningService(db.masteryDao(), db.positionDao())

    @Test
    fun `picks the lowest-mastery already-learned knowledge point`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak", 40.0)
        db.masteryDao().upsertMastery("kp-strong", 90.0)
        // kp-untouched has no mastery record — never attempted, must be ignored.

        val selected = service().selectKnowledgePointToReview("p1")

        assertThat(selected?.id).isEqualTo("kp-weak")
    }

    @Test
    fun `returns null when the user has not attempted anything yet`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)

        assertThat(service().selectKnowledgePointToReview("p1")).isNull()
    }

    @Test
    fun `excludeId lets 换一个 pick a different already-learned point`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak", 40.0)
        db.masteryDao().upsertMastery("kp-strong", 90.0)

        val selected = service().selectKnowledgePointToReview("p1", excludeId = "kp-weak")

        assertThat(selected?.id).isEqualTo("kp-strong")
    }
}
