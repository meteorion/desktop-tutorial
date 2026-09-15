package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.llm.FakeLlmProvider
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.entity.MockInterviewSessionEntity
import com.interviewcoach.domain.model.InterviewTurn
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `finalizeSession backfills per-turn scores and persists an overall report`() = runTest {
        val service = ReviewService(FakeLlmProvider(), db.mockInterviewDao(), db.reviewReportDao(), db.settingsDao())
        val transcript = listOf(
            InterviewTurn(role = "ai", text = "q1"),
            InterviewTurn(role = "user", text = "a short answer"),
            InterviewTurn(role = "ai", text = "q2"),
            InterviewTurn(role = "user", text = "a much longer and more detailed answer with specifics"),
        )
        db.mockInterviewDao().insertSession(
            MockInterviewSessionEntity(id = "session1", planId = "plan1", startedAt = 0L, transcriptJson = Json.encodeToString(transcript)),
        )

        val report = service.finalizeSession("session1")

        assertThat(report.overallScore).isGreaterThan(0.0)

        val session = db.mockInterviewDao().getSessionById("session1")!!
        assertThat(session.endedAt).isNotNull()
        val scoredTranscript = Json.decodeFromString<List<InterviewTurn>>(session.transcriptJson)
        val userTurns = scoredTranscript.filter { it.role == "user" }
        userTurns.forEach { assertThat(it.turnScore).isNotNull() }
        // The longer, more detailed answer should score at least as high as the short one.
        assertThat(userTurns[1].turnScore!!).isAtLeast(userTurns[0].turnScore!!)

        // Weak knowledge points from the review flow into a settings-backed priority
        // list AdjustmentService reads (same mechanism as the "too hard" difficulty pref).
        assertThat(db.settingsDao().getSetting("weak_points_from_last_review")).isNotNull()
    }
}
