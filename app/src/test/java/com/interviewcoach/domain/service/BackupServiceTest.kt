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
import javax.crypto.AEADBadTagException

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp1", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class BackupServiceTest {
    private lateinit var sourceDb: AppDatabase
    private lateinit var targetDb: AppDatabase

    @Before
    fun setUp() {
        sourceDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        targetDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() { sourceDb.close(); targetDb.close() }

    @Test
    fun `exportEncrypted then importEncrypted round-trips plan data with the right password`() = runTest {
        loadSeedIfEmpty(sourceDb, SEED_JSON)
        val service = BackupService()

        val encrypted = service.exportEncrypted(sourceDb, password = "correct horse")
        service.importEncrypted(targetDb, encrypted, password = "correct horse")

        val positions = targetDb.positionDao().getAllPositions()
        assertThat(positions).hasSize(1)
        assertThat(positions.first().name).isEqualTo("后端开发-Java")
    }

    @Test(expected = AEADBadTagException::class)
    fun `importEncrypted throws when the password is wrong`() = runTest {
        loadSeedIfEmpty(sourceDb, SEED_JSON)
        val service = BackupService()
        val encrypted = service.exportEncrypted(sourceDb, password = "right password")

        service.importEncrypted(targetDb, encrypted, password = "wrong password")
    }
}
