package com.interviewcoach.core.llm

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ClaudeLlmProviderTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() { server = MockWebServer().apply { start() } }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun `retries once on failure, then succeeds`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(
            MockResponse().setBody("""{"content":[{"type":"text","text":"{\"score\":80,\"feedback\":\"ok\"}"}]}"""),
        )
        val provider = ClaudeLlmProvider(apiKey = "test-key", baseUrl = server.url("/").toString())

        val result = provider.gradeAnswer("q", "ref", "answer")

        assertThat(result.score).isEqualTo(80.0)
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test(expected = LlmRequestFailure::class)
    fun `throws LlmRequestFailure after the retry also fails`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))
        val provider = ClaudeLlmProvider(apiKey = "test-key", baseUrl = server.url("/").toString())

        provider.gradeAnswer("q", "ref", "answer")
    }
}
