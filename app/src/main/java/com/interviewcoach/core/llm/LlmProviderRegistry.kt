package com.interviewcoach.core.llm

import com.interviewcoach.core.security.SecureKeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the active LlmProvider from the stored API key. Falls back to
 * FakeLlmProvider when none is configured yet (profile "未配置" state, UI doc
 * 3.8), so the rest of the app never has to null-check "is there a provider".
 */
@Singleton
class LlmProviderRegistry @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val fakeLlmProvider: FakeLlmProvider,
) {
    fun isConfigured(): Boolean = !secureKeyStore.getApiKey().isNullOrEmpty()

    fun current(): LlmProvider {
        val apiKey = secureKeyStore.getApiKey()
        return if (apiKey.isNullOrEmpty()) fakeLlmProvider else ClaudeLlmProvider(apiKey)
    }
}
