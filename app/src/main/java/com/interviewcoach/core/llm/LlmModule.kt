package com.interviewcoach.core.llm

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {
    /**
     * Deliberately NOT @Singleton: every domain service that takes an
     * LlmProvider (PlanService, LearningSessionService, MockInterviewService,
     * ReviewService, ResumeParseService) is itself unscoped, so each fresh
     * injection (e.g. a new screen's ViewModel via hiltViewModel()) re-resolves
     * the current provider from LlmProviderRegistry — picking up a
     * newly-configured API key the next time a screen is (re)created, without
     * requiring a process restart.
     */
    @Provides
    fun provideLlmProvider(registry: LlmProviderRegistry): LlmProvider = registry.current()
}
