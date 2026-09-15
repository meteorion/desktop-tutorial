package com.interviewcoach.feature.profile

import androidx.lifecycle.ViewModel
import com.interviewcoach.core.llm.LlmProviderRegistry
import com.interviewcoach.core.security.SecureKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val llmProviderRegistry: LlmProviderRegistry,
) : ViewModel() {
    private val _isConfigured = MutableStateFlow(llmProviderRegistry.isConfigured())
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    fun saveApiKey(key: String) {
        if (key.isEmpty()) return
        secureKeyStore.setApiKey(key)
        _isConfigured.value = llmProviderRegistry.isConfigured()
    }
}
