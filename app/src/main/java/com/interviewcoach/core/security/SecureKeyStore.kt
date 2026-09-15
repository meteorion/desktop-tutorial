package com.interviewcoach.core.security

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds secrets that must never touch the Room database or logs: the LLM
 * API key and the user's backup-export password. Reads/writes are
 * synchronous — EncryptedSharedPreferences has no async API, unlike
 * flutter_secure_storage's platform-channel round trip.
 */
@Singleton
class SecureKeyStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "interview_coach_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun setApiKey(key: String) = prefs.edit { putString(KEY_API_KEY, key) }
    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)
    fun clearApiKey() = prefs.edit { remove(KEY_API_KEY) }

    fun setBackupPassword(password: String) = prefs.edit { putString(KEY_BACKUP_PASSWORD, password) }
    fun getBackupPassword(): String? = prefs.getString(KEY_BACKUP_PASSWORD, null)

    companion object {
        private const val KEY_API_KEY = "llm_api_key"
        private const val KEY_BACKUP_PASSWORD = "backup_password"
    }
}
