package com.interviewcoach.core.storage.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `setSetting then getSetting round-trips a value`() = runTest {
        db.settingsDao().setSetting("llm_provider", "claude")
        assertThat(db.settingsDao().getSetting("llm_provider")).isEqualTo("claude")
    }

    @Test
    fun `getSetting returns null for a key that was never set`() = runTest {
        assertThat(db.settingsDao().getSetting("missing_key")).isNull()
    }
}
