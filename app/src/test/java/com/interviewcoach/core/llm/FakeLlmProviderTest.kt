package com.interviewcoach.core.llm

import com.google.common.truth.Truth.assertThat
import com.interviewcoach.domain.model.PlanGenerationInput
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeLlmProviderTest {
    @Test
    fun `gradeAnswer gives a higher score for longer, non-empty answers`() = runTest {
        val provider = FakeLlmProvider()

        val empty = provider.gradeAnswer("q", "ref", "")
        val substantive = provider.gradeAnswer("q", "ref", "a reasonably detailed answer covering the key points")

        assertThat(empty.score).isLessThan(substantive.score)
        assertThat(substantive.feedback).isNotEmpty()
    }

    @Test
    fun `generatePlan produces the requested number of days with at least one task each`() = runTest {
        val provider = FakeLlmProvider()

        val plan = provider.generatePlan(PlanGenerationInput(positionId = "p1", demonstratedSkillKnowledgePointIds = emptyList()))

        assertThat(plan.tasksByDayIndex).hasSize(plan.periodDays)
        plan.tasksByDayIndex.values.forEach { assertThat(it).isNotEmpty() }
    }
}
