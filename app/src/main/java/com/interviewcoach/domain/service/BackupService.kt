package com.interviewcoach.domain.service

import androidx.room.withTransaction
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.entity.AttemptEntity
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.MasteryRecordEntity
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.core.storage.entity.PositionEntity
import com.interviewcoach.core.storage.entity.QuestionEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

@Serializable
private data class BackupDump(
    val positions: List<PositionEntity>,
    val knowledgePoints: List<KnowledgePointEntity>,
    val plans: List<PlanEntity>,
    val dailyTasks: List<DailyTaskEntity>,
    val questions: List<QuestionEntity>,
    val attempts: List<AttemptEntity>,
    val masteryRecords: List<MasteryRecordEntity>,
)

@Serializable
private data class EncryptedEnvelope(val salt: String, val iv: String, val cipherText: String)

class BackupService @Inject constructor() {
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, 100_000, 256)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    suspend fun exportEncrypted(db: AppDatabase, password: String): ByteArray {
        val plans = db.planDao().getAllPlans()
        val dump = BackupDump(
            positions = db.positionDao().getAllPositions(),
            knowledgePoints = db.positionDao().getAllKnowledgePoints(),
            plans = plans,
            dailyTasks = plans.flatMap { db.dailyTaskDao().getTasksForPlan(it.id) },
            questions = emptyList(),
            attempts = emptyList(),
            masteryRecords = db.masteryDao().getAllMastery(),
        )
        val plaintext = Json.encodeToString(dump).toByteArray()

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        val cipherText = cipher.doFinal(plaintext)

        val envelope = EncryptedEnvelope(
            salt = java.util.Base64.getEncoder().encodeToString(salt),
            iv = java.util.Base64.getEncoder().encodeToString(cipher.iv),
            cipherText = java.util.Base64.getEncoder().encodeToString(cipherText),
        )
        return Json.encodeToString(envelope).toByteArray()
    }

    suspend fun importEncrypted(db: AppDatabase, encrypted: ByteArray, password: String) {
        val envelope = Json.decodeFromString<EncryptedEnvelope>(String(encrypted))
        val salt = java.util.Base64.getDecoder().decode(envelope.salt)
        val iv = java.util.Base64.getDecoder().decode(envelope.iv)
        val cipherText = java.util.Base64.getDecoder().decode(envelope.cipherText)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv)) }
        // Throws javax.crypto.AEADBadTagException on a wrong password — that's the
        // desired "reject bad password" behavior, not caught here.
        val plaintext = cipher.doFinal(cipherText)
        val dump = Json.decodeFromString<BackupDump>(String(plaintext))

        db.withTransaction {
            dump.positions.forEach { db.positionDao().upsertPosition(it) }
            dump.knowledgePoints.forEach { db.positionDao().upsertKnowledgePoint(it) }
            dump.plans.forEach { db.planDao().insertPlan(it) }
            dump.dailyTasks.forEach { db.dailyTaskDao().insertTask(it) }
            dump.questions.forEach { db.questionDao().insertQuestion(it) }
            dump.attempts.forEach { db.attemptDao().insertAttempt(it) }
            dump.masteryRecords.forEach { db.masteryDao().upsertMastery(it) }
        }
    }
}
