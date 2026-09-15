package com.interviewcoach.domain.service

import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.llm.FakeLlmProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResumeParseServiceTest {
    @Test
    fun `returns a low-confidence result for very short resume text`() = runTest {
        val service = ResumeParseService(FakeLlmProvider())

        val result = service.parseResumeText("too short")

        assertThat(result.confidence).isLessThan(0.5)
        assertThat(result.needsUserConfirmation).isTrue()
    }

    @Test
    fun `returns a high-confidence result for substantial resume text`() = runTest {
        val service = ResumeParseService(FakeLlmProvider())

        val result = service.parseResumeText(
            "五年 Java 后端开发经验,负责过高并发订单系统的设计与优化," +
                "熟悉分布式锁、消息队列和数据库分库分表方案,曾主导系统从单体到微服务的迁移。",
        )

        assertThat(result.confidence).isAtLeast(0.5)
        assertThat(result.needsUserConfirmation).isFalse()
        assertThat(result.demonstratedSkills).isNotEmpty()
    }
}
