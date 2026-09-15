package com.interviewcoach.core.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp1", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false},
    {"id": "kp2", "name": "系统设计", "weight": 4, "difficulty": 3, "isCore": true}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class SeedLoaderTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `loadSeedIfEmpty populates positions and knowledge points from JSON`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)

        val positions = db.positionDao().getAllPositions()
        val points = db.positionDao().getAllKnowledgePoints()
        assertThat(positions).hasSize(1)
        assertThat(points).hasSize(2)
        assertThat(points.first { it.id == "kp2" }.isCore).isTrue()

        // Calling it again must not duplicate rows.
        loadSeedIfEmpty(db, SEED_JSON)
        assertThat(db.positionDao().getAllKnowledgePoints()).hasSize(2)
    }
}
