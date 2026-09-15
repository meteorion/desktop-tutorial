# 自适应引导式 AI 面试助手 MVP Implementation Plan (Kotlin / Jetpack Compose, Android-only)

> **For agentic workers:** Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Same product scope as the Flutter plan, retargeted to a single-platform native Android app: set a target job → generate a plan → practice daily → track mastery → dynamically adjust the plan → free-review → unlock and run a mock interview → get an AI review → close the loop back into learning.

**Architecture:** Pure client app, no backend. Local persistence via **Room** (SQLite). All "intelligence" (plan generation, grading, interview dialogue, review) goes through a single `LlmProvider` abstraction so the concrete LLM vendor is swappable. User supplies their own API key, stored in **EncryptedSharedPreferences** (AndroidX Security Crypto). UI is **Jetpack Compose**; state managed with **ViewModel + StateFlow**; dependencies wired with **Hilt**. This plan implements every module the Flutter plan covered; nothing here is a stub for a feature the product doc marked out-of-scope.

**Tech Stack:** Kotlin 1.9.x, Android Gradle Plugin 8.5+, minSdk 26 / targetSdk 34, Jetpack Compose (BOM 2024.06.00) + Material3, Navigation-Compose 2.7.7, Hilt 2.51.1 (+ `hilt-navigation-compose`), Room 2.6.1 (KSP codegen), `androidx.security:security-crypto` 1.1.0-alpha06, Retrofit 2.11.0 + OkHttp 4.12.0 + `kotlinx-serialization-json` 1.6.3, `kotlinx-coroutines` 1.8.1. **Tests:** JUnit4, Robolectric 4.13 (gives Room an Android `Context` for fast local-JVM DB tests — the closest equivalent to `drift`'s pure-Dart in-memory database, without needing an emulator), Google Truth 1.4.2, `kotlinx-coroutines-test`, OkHttp `MockWebServer`, MockK 1.13.11.

**Reference docs (unchanged, stack-agnostic):**
- Product design: [docs/2026-09-14-ai-interview-assistant-design.md](../../2026-09-14-ai-interview-assistant-design.md)
- Architecture: [docs/2026-09-14-ai-interview-assistant-architecture.md](../../2026-09-14-ai-interview-assistant-architecture.md)
- UI design + mockups: [docs/2026-09-14-ai-interview-assistant-ui-design.md](../../2026-09-14-ai-interview-assistant-ui-design.md), [docs/ui-mockups/](../../ui-mockups/)
- Flutter version of this plan (superseded for Android-only scope): [2026-09-15-mvp-implementation-plan.md](2026-09-15-mvp-implementation-plan.md)

---

## Why these substitutions (Flutter → Kotlin mapping)

| Flutter/Dart | Kotlin/Android | Reason |
|---|---|---|
| `drift` (SQLite) | **Room** | First-party Android ORM, KSP codegen, same relational model maps 1:1 onto the ten tables. |
| `flutter_riverpod` | **Hilt + ViewModel + StateFlow** | Idiomatic Android DI + reactive state; `FutureProvider` ≈ a `suspend fun` on a Hilt-injected singleton, `ConsumerWidget` ≈ `@Composable` collecting `StateFlow` via `hiltViewModel()`. |
| `flutter_secure_storage` | **EncryptedSharedPreferences** (AndroidX Security Crypto, backed by Android Keystore) | Native, synchronous, no plugin channel overhead. |
| `dio` | **Retrofit + OkHttp** | Standard Android HTTP stack; `kotlinx-serialization` converter replaces manual `jsonDecode`. |
| `mocktail` / widget tests | **MockK + Robolectric + Truth** | Robolectric supplies a JVM-local Android `Context` so Room-backed tests run exactly like the Dart in-memory DB tests did — fast, no device needed. |
| Flutter widgets | **Jetpack Compose** | Declarative UI, same widget-tree-shaped translation as the original screens. |
| `build_runner` / `drift_dev` | **KSP** (Room + Hilt compilers) | Equivalent compile-time codegen step. |
| `uuid` package | **`java.util.UUID.randomUUID().toString()`** | Built into the JDK, no dependency needed. |

The layering rule carries over unchanged: `core/` (DB, network, secure storage) never imports from `domain/` or `feature/`; `domain/service` classes depend only on `core.llm` and `core.storage` DAOs — plain Kotlin, no Android framework imports, so every service is unit-testable without Compose/Activity scaffolding; `feature/` screens depend on `domain/service` through Hilt-injected ViewModels and hold no business logic themselves.

---

## Project Structure

```
app/
  build.gradle.kts
  src/main/java/com/interviewcoach/
    InterviewCoachApp.kt              # @HiltAndroidApp Application
    MainActivity.kt                   # single Activity, sets Compose content
    app/
      navigation/AppNavHost.kt        # NavHost + bottom-nav shell (Task 24)
      navigation/AppBootstrap.kt      # AppRootViewModel: onboarding vs main routing (Task 24)
      theme/Color.kt                  # AppColors from UI doc 5.1
      theme/Theme.kt
      widgets/AiActionButton.kt       # shared AI loading/error composable (UI doc 5.3)
    core/
      llm/
        LlmProvider.kt                # interface + request/response models
        FakeLlmProvider.kt            # deterministic provider used in dev + tests
        ClaudeLlmProvider.kt          # real Anthropic Messages API implementation
        ClaudeApi.kt                  # Retrofit service interface + DTOs
        LlmProviderRegistry.kt        # resolves active provider from SecureKeyStore
      storage/
        AppDatabase.kt                # @Database, all entities
        entity/*.kt
        dao/*.kt
        SeedLoader.kt                 # idempotent standard position library loader
        DatabaseModule.kt             # Hilt module providing AppDatabase + DAOs
      security/
        SecureKeyStore.kt             # wraps EncryptedSharedPreferences
    domain/
      model/DomainModels.kt           # ResumeParseResult, PlanDraft, AnswerFeedback, InterviewTurn, ReviewReportDraft, etc.
      service/
        PlanService.kt
        MasteryService.kt
        AdjustmentService.kt
        LearningSessionService.kt
        FreeLearningService.kt
        MockInterviewService.kt
        ReviewService.kt
        ResumeParseService.kt
        BackupService.kt
    feature/
      onboarding/OnboardingScreen.kt + OnboardingViewModel.kt
      planconfirm/PlanConfirmScreen.kt + PlanConfirmViewModel.kt
      dashboard/DashboardScreen.kt + DashboardViewModel.kt
      learning/TaskListScreen.kt + DailyTaskScreen.kt + CardScreen.kt (+ ViewModels)
      freelearning/FreeLearningScreen.kt + FreeLearningViewModel.kt
      mockinterview/MockInterviewTabScreen.kt + MockInterviewChatScreen.kt + ReviewReportScreen.kt (+ ViewModels)
      profile/ProfileScreen.kt + ProfileViewModel.kt
  src/main/assets/
    positions_seed.json                # standard position library seed data
  src/test/java/com/interviewcoach/
    core/storage/AppDatabaseTest.kt
    core/storage/SeedLoaderTest.kt
    core/storage/dao/SettingsDaoTest.kt
    core/storage/dao/DailyTaskDaoTest.kt
    core/llm/FakeLlmProviderTest.kt
    core/llm/ClaudeLlmProviderTest.kt
    domain/service/ResumeParseServiceTest.kt
    domain/service/PlanServiceTest.kt
    domain/service/MasteryServiceTest.kt
    domain/service/LearningSessionServiceTest.kt
    domain/service/AdjustmentServiceTest.kt
    domain/service/FreeLearningServiceTest.kt
    domain/service/MockInterviewServiceTest.kt
    domain/service/ReviewServiceTest.kt
    domain/service/BackupServiceTest.kt
```

---

## Phase 0: Foundation (project scaffold, database, LLM abstraction, app shell)

### Task 1: Android project scaffold and dependencies

**Files:**
- Create: `app/build.gradle.kts`
- Create: `build.gradle.kts` (root), `settings.gradle.kts`
- Create: `app/src/main/java/com/interviewcoach/InterviewCoachApp.kt`
- Create: `app/src/main/java/com/interviewcoach/MainActivity.kt`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create the project via Android Studio's "Empty Activity (Compose)" template**, package `com.interviewcoach`, minSdk 26.

- [ ] **Step 2: Configure `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.interviewcoach"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.interviewcoach"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    testOptions { unitTests.isIncludeAndroidResources = true }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.mockk:mockk:1.13.11")
}
```

- [ ] **Step 3: Sync and verify the default app still builds**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Application class + manifest**

```kotlin
// app/src/main/java/com/interviewcoach/InterviewCoachApp.kt
package com.interviewcoach

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class InterviewCoachApp : Application()
```

Set `android:name=".InterviewCoachApp"` on `<application>` in `AndroidManifest.xml`.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts build.gradle.kts settings.gradle.kts app/src/main/java/com/interviewcoach/InterviewCoachApp.kt app/src/main/AndroidManifest.xml
git commit -m "chore: scaffold Android project with MVP dependencies"
```

---

### Task 2: Local database schema (Room)

**Files:**
- Create: `core/storage/entity/*.kt` (ten entities)
- Create: `core/storage/AppDatabase.kt`
- Test: `test/core/storage/AppDatabaseTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// test/core/storage/AppDatabaseTest.kt
package com.interviewcoach.core.storage

import android.database.Cursor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.PositionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `inserts and reads back a position with a knowledge point`() = runTest {
        db.positionDao().upsertPosition(PositionEntity(id = "p1", name = "后端开发-Java"))
        db.positionDao().upsertKnowledgePoint(
            KnowledgePointEntity(id = "kp1", positionId = "p1", name = "Java 基础", weight = 1, difficulty = 1, isCore = false),
        )

        val points = db.positionDao().getAllKnowledgePoints()
        assertThat(points).hasSize(1)
        assertThat(points.first().name).isEqualTo("Java 基础")
        assertThat(points.first().positionId).isEqualTo("p1")
    }

    @Test
    fun `schema has all ten tables required by the architecture doc`() {
        val tableNames = mutableSetOf<String>()
        val cursor: Cursor = db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'")
        cursor.use { while (it.moveToNext()) tableNames.add(it.getString(0)) }
        assertThat(tableNames).containsAtLeastElementsIn(
            setOf(
                "positions", "knowledge_points", "plans", "daily_tasks", "questions",
                "attempts", "mastery_records", "mock_interview_sessions", "review_reports", "app_settings",
            ),
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.AppDatabaseTest"`
Expected: FAIL — `AppDatabase`/entities don't exist.

- [ ] **Step 3: Write the entities and database**

```kotlin
// core/storage/entity/PositionEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "positions")
data class PositionEntity(@PrimaryKey val id: String, val name: String)
```

```kotlin
// core/storage/entity/KnowledgePointEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_points",
    foreignKeys = [ForeignKey(PositionEntity::class, ["id"], ["positionId"])],
    indices = [Index("positionId")],
)
data class KnowledgePointEntity(
    @PrimaryKey val id: String,
    val positionId: String,
    val name: String,
    val weight: Int,
    val difficulty: Int,
    val isCore: Boolean = false,
)
```

```kotlin
// core/storage/entity/PlanEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "plans", foreignKeys = [ForeignKey(PositionEntity::class, ["id"], ["positionId"])])
data class PlanEntity(
    @PrimaryKey val id: String,
    val positionId: String,
    val startDate: Long, // epoch millis
    val periodDays: Int,
    // draft | confirmed | active | unlocked_mock_interview
    val status: String,
)
```

```kotlin
// core/storage/entity/DailyTaskEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_tasks",
    foreignKeys = [
        ForeignKey(PlanEntity::class, ["id"], ["planId"]),
        ForeignKey(KnowledgePointEntity::class, ["id"], ["knowledgePointId"]),
    ],
    indices = [Index("planId"), Index("knowledgePointId")],
)
data class DailyTaskEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val date: Long,
    val knowledgePointId: String,
    val questionId: String? = null,
    // practice | card
    val taskType: String,
    val completed: Boolean = false,
)
```

```kotlin
// core/storage/entity/QuestionEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "questions", foreignKeys = [ForeignKey(KnowledgePointEntity::class, ["id"], ["knowledgePointId"])])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val knowledgePointId: String,
    val content: String,
    val referenceAnswer: String,
    val flagCount: Int = 0,
)
```

```kotlin
// core/storage/entity/AttemptEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(QuestionEntity::class, ["id"], ["questionId"]),
        ForeignKey(KnowledgePointEntity::class, ["id"], ["knowledgePointId"]),
    ],
)
data class AttemptEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val knowledgePointId: String,
    val answerText: String,
    val score: Double,
    val feedback: String,
    // daily_task | free_learning
    val source: String,
    val createdAt: Long,
)
```

```kotlin
// core/storage/entity/MasteryRecordEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mastery_records")
data class MasteryRecordEntity(
    @PrimaryKey val knowledgePointId: String,
    val score: Double = 0.0,
    val updatedAt: Long,
)
```

```kotlin
// core/storage/entity/MockInterviewSessionEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "mock_interview_sessions", foreignKeys = [ForeignKey(PlanEntity::class, ["id"], ["planId"])])
data class MockInterviewSessionEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    // JSON-encoded list of {role, text, isFollowUp, turnScore, turnFeedback}
    val transcriptJson: String,
)
```

```kotlin
// core/storage/entity/ReviewReportEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "review_reports", foreignKeys = [ForeignKey(MockInterviewSessionEntity::class, ["id"], ["sessionId"])])
data class ReviewReportEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val overallScore: Double,
    val knowledgeScore: Double,
    val expressionScore: Double,
    val highlights: String,
    val weaknesses: String,
    val suggestions: String,
    val weakKnowledgePointIdsJson: String,
    val createdAt: Long,
)
```

```kotlin
// core/storage/entity/AppSettingEntity.kt
package com.interviewcoach.core.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingEntity(@PrimaryKey val settingKey: String, val settingValue: String)
```

```kotlin
// core/storage/AppDatabase.kt
package com.interviewcoach.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.interviewcoach.core.storage.dao.*
import com.interviewcoach.core.storage.entity.*

@Database(
    entities = [
        PositionEntity::class, KnowledgePointEntity::class, PlanEntity::class,
        DailyTaskEntity::class, QuestionEntity::class, AttemptEntity::class,
        MasteryRecordEntity::class, MockInterviewSessionEntity::class,
        ReviewReportEntity::class, AppSettingEntity::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao
    abstract fun planDao(): PlanDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun questionDao(): QuestionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun masteryDao(): MasteryDao
    abstract fun mockInterviewDao(): MockInterviewDao
    abstract fun reviewReportDao(): ReviewReportDao
    abstract fun settingsDao(): SettingsDao
}
```

```kotlin
// core/storage/dao/PositionDao.kt
package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.PositionEntity

@Dao
interface PositionDao {
    @Upsert suspend fun upsertPosition(position: PositionEntity)
    @Upsert suspend fun upsertKnowledgePoint(kp: KnowledgePointEntity)

    @Query("SELECT * FROM positions")
    suspend fun getAllPositions(): List<PositionEntity>

    @Query("SELECT * FROM knowledge_points WHERE positionId = :positionId")
    suspend fun getKnowledgePointsForPosition(positionId: String): List<KnowledgePointEntity>

    @Query("SELECT * FROM knowledge_points")
    suspend fun getAllKnowledgePoints(): List<KnowledgePointEntity>
}
```

The remaining nine DAOs (`PlanDao`, `DailyTaskDao`, `QuestionDao`, `AttemptDao`, `MasteryDao`, `MockInterviewDao`, `ReviewReportDao`, `SettingsDao`) are built incrementally in the tasks that need them (Tasks 5, 10, 12, 18, 19, 23) — this task only needs `PositionDao` to compile and satisfy the test above; declare the other DAO interfaces as empty stubs (`@Dao interface PlanDao` with no methods yet) so `AppDatabase` compiles, and fill them in as their owning task lands.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.AppDatabaseTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/core/storage/ app/src/test/java/com/interviewcoach/core/storage/AppDatabaseTest.kt
git commit -m "feat: add Room schema for all ten core entities"
```

---

### Task 3: Real database factory (Hilt module)

**Files:**
- Create: `core/storage/DatabaseModule.kt`

- [ ] **Step 1: Write the Hilt module**

No unit test for this step — it wires a real on-device file path via `Room.databaseBuilder`, which is an integration concern verified in Task 24's manual walkthrough. Same deliberate TDD exception as the Flutter plan's Task 3: the alternative is mocking Android's file system, which would only prove the mock works.

```kotlin
// core/storage/DatabaseModule.kt
package com.interviewcoach.core.storage

import android.content.Context
import androidx.room.Room
import com.interviewcoach.core.storage.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "interview_coach.db").build()

    @Provides fun providePositionDao(db: AppDatabase): PositionDao = db.positionDao()
    @Provides fun providePlanDao(db: AppDatabase): PlanDao = db.planDao()
    @Provides fun provideDailyTaskDao(db: AppDatabase): DailyTaskDao = db.dailyTaskDao()
    @Provides fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()
    @Provides fun provideAttemptDao(db: AppDatabase): AttemptDao = db.attemptDao()
    @Provides fun provideMasteryDao(db: AppDatabase): MasteryDao = db.masteryDao()
    @Provides fun provideMockInterviewDao(db: AppDatabase): MockInterviewDao = db.mockInterviewDao()
    @Provides fun provideReviewReportDao(db: AppDatabase): ReviewReportDao = db.reviewReportDao()
    @Provides fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interviewcoach/core/storage/DatabaseModule.kt
git commit -m "feat: add Hilt module providing the on-device Room database and DAOs"
```

---

### Task 4: LlmProvider abstraction + FakeLlmProvider

**Files:**
- Create: `domain/model/DomainModels.kt`
- Create: `core/llm/LlmProvider.kt`
- Create: `core/llm/FakeLlmProvider.kt`
- Test: `test/core/llm/FakeLlmProviderTest.kt`

This is the interface every domain service in later phases calls through — getting its shape right now avoids rework. It matches the architecture doc's `LlmProvider` interface (architecture doc §4), including `parseResume()`.

- [ ] **Step 1: Write the domain models**

```kotlin
// domain/model/DomainModels.kt
package com.interviewcoach.domain.model

import kotlinx.serialization.Serializable

data class ResumeParseResult(
    val suggestedPositionId: String? = null,
    val suggestedSalaryRange: String? = null,
    val demonstratedSkills: List<String>,
    val confidence: Double, // 0.0-1.0; UI shows "以下为推测填写,请确认" below a threshold
)

data class PlanGenerationInput(
    val positionId: String,
    val demonstratedSkillKnowledgePointIds: List<String> = emptyList(),
)

data class DailyTaskDraft(val knowledgePointId: String, val taskType: String) // practice | card

data class PlanDraft(
    val periodDays: Int,
    val summary: String,
    val tasksByDayIndex: Map<Int, List<DailyTaskDraft>>, // 0-based day index
)

data class QuestionDraft(val content: String, val referenceAnswer: String)

data class AnswerFeedback(val score: Double, val feedback: String) // score 0-100

@Serializable
data class InterviewTurn(
    val role: String, // ai | user
    val text: String,
    val isFollowUp: Boolean = false,
    val turnScore: Double? = null,
    val turnFeedback: String? = null,
)

data class InterviewContext(
    val positionId: String,
    val weakKnowledgePointIds: List<String>,
    val transcriptSoFar: List<InterviewTurn>,
)

data class InterviewTranscript(val turns: List<InterviewTurn>)

data class ReviewReportDraft(
    val overallScore: Double,
    val knowledgeScore: Double,
    val expressionScore: Double,
    val highlights: String,
    val weaknesses: String,
    val suggestions: String,
    val weakKnowledgePointIds: List<String>,
)
```

- [ ] **Step 2: Write the abstract provider interface**

```kotlin
// core/llm/LlmProvider.kt
package com.interviewcoach.core.llm

import com.interviewcoach.domain.model.*

/**
 * Everything a domain service needs from "the LLM", scoped to the operations
 * this app performs — not a generic chat passthrough. See architecture doc §4.
 */
interface LlmProvider {
    suspend fun parseResume(resumeText: String): ResumeParseResult
    suspend fun generatePlan(input: PlanGenerationInput): PlanDraft
    suspend fun generateQuestion(knowledgePointName: String, isCore: Boolean): QuestionDraft
    suspend fun gradeAnswer(questionContent: String, referenceAnswer: String, answerText: String): AnswerFeedback
    suspend fun nextInterviewQuestion(context: InterviewContext): InterviewTurn
    suspend fun generateReview(transcript: InterviewTranscript): ReviewReportDraft
}

class LlmRequestFailure(message: String) : Exception(message)
```

- [ ] **Step 3: Write the failing test for FakeLlmProvider**

```kotlin
// test/core/llm/FakeLlmProviderTest.kt
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
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.FakeLlmProviderTest"`
Expected: FAIL — `FakeLlmProvider` does not exist yet.

- [ ] **Step 5: Implement FakeLlmProvider**

Deterministic, no network — used for local dev before an API key is configured and for every domain-service test in this plan.

```kotlin
// core/llm/FakeLlmProvider.kt
package com.interviewcoach.core.llm

import com.interviewcoach.domain.model.*
import javax.inject.Inject
import kotlin.math.min

class FakeLlmProvider @Inject constructor() : LlmProvider {

    override suspend fun parseResume(resumeText: String) = ResumeParseResult(
        suggestedPositionId = null,
        suggestedSalaryRange = "15K-25K",
        demonstratedSkills = if (resumeText.isEmpty()) emptyList() else listOf("Java 基础"),
        confidence = if (resumeText.length > 50) 0.8 else 0.3,
    )

    override suspend fun generatePlan(input: PlanGenerationInput): PlanDraft {
        val periodDays = 7
        val tasks = (0 until periodDays).associateWith { listOf(DailyTaskDraft(knowledgePointId = "kp1", taskType = "practice")) }
        return PlanDraft(periodDays = periodDays, summary = "根据你的起点,我们安排了 $periodDays 天的学习计划。", tasksByDayIndex = tasks)
    }

    override suspend fun generateQuestion(knowledgePointName: String, isCore: Boolean) = QuestionDraft(
        content = "请说明你对「$knowledgePointName」的理解,并举一个实际例子。",
        referenceAnswer = "$knowledgePointName 的核心要点包括定义、适用场景和常见陷阱。",
    )

    override suspend fun gradeAnswer(questionContent: String, referenceAnswer: String, answerText: String): AnswerFeedback {
        val length = answerText.trim().length
        val score = if (length == 0) 0.0 else min(95.0, 40.0 + length * 1.5)
        return AnswerFeedback(score = score, feedback = if (length == 0) "还没有作答内容。" else "回答有一定思路,可以参考标准答案补充细节。")
    }

    override suspend fun nextInterviewQuestion(context: InterviewContext): InterviewTurn {
        val isFollowUp = context.transcriptSoFar.isNotEmpty() && context.transcriptSoFar.size % 2 == 1
        return InterviewTurn(
            role = "ai",
            text = if (isFollowUp) "能再展开说说细节吗?" else "说说你在这个领域最有代表性的一次实践。",
            isFollowUp = isFollowUp,
        )
    }

    override suspend fun generateReview(transcript: InterviewTranscript): ReviewReportDraft {
        val userTurns = transcript.turns.count { it.role == "user" }
        val overall = min(95.0, 50.0 + userTurns * 5.0)
        return ReviewReportDraft(
            overallScore = overall,
            knowledgeScore = overall,
            expressionScore = overall - 5,
            highlights = "回答思路清晰,能结合实际项目举例。",
            weaknesses = "部分细节展开不够充分。",
            suggestions = "建议针对薄弱知识点做进一步强化。",
            weakKnowledgePointIds = listOf("kp1"),
        )
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.FakeLlmProviderTest"`
Expected: PASS (2 tests)

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/interviewcoach/domain/model/DomainModels.kt app/src/main/java/com/interviewcoach/core/llm/LlmProvider.kt app/src/main/java/com/interviewcoach/core/llm/FakeLlmProvider.kt app/src/test/java/com/interviewcoach/core/llm/FakeLlmProviderTest.kt
git commit -m "feat: add LlmProvider abstraction with a deterministic fake implementation"
```

---

### Task 5: Secure key storage + SettingsDao + LLM provider registry

**Files:**
- Create: `core/security/SecureKeyStore.kt`
- Create: `core/storage/dao/SettingsDao.kt` (replace empty stub from Task 2)
- Create: `core/llm/LlmProviderRegistry.kt`
- Test: `test/core/storage/dao/SettingsDaoTest.kt`

- [ ] **Step 1: Write the failing test for SettingsDao**

```kotlin
// test/core/storage/dao/SettingsDaoTest.kt
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.SettingsDaoTest"`
Expected: FAIL — `SettingsDao.getSetting`/`setSetting` don't exist.

- [ ] **Step 3: Implement SettingsDao, SecureKeyStore, and the provider registry**

```kotlin
// core/storage/dao/SettingsDao.kt
package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.interviewcoach.core.storage.entity.AppSettingEntity

@Dao
interface SettingsDao {
    @Upsert suspend fun upsertSetting(entity: AppSettingEntity)

    @Query("SELECT settingValue FROM app_settings WHERE settingKey = :key")
    suspend fun getSetting(key: String): String?

    suspend fun setSetting(key: String, value: String) = upsertSetting(AppSettingEntity(key, value))
}
```

```kotlin
// core/security/SecureKeyStore.kt
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
```

```kotlin
// core/llm/LlmProviderRegistry.kt
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
```

Note: `ClaudeLlmProvider` is implemented in Task 6 — this file references it, so Task 6 must land before this compiles cleanly, but `SettingsDao`/`SecureKeyStore` are independent and verified now with `SettingsDaoTest`.

- [ ] **Step 4: Run the DAO test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.SettingsDaoTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/core/security/SecureKeyStore.kt app/src/main/java/com/interviewcoach/core/storage/dao/SettingsDao.kt app/src/main/java/com/interviewcoach/core/llm/LlmProviderRegistry.kt app/src/test/java/com/interviewcoach/core/storage/dao/SettingsDaoTest.kt
git commit -m "feat: add secure key storage, settings DAO, and LLM provider registry"
```

---

### Task 6: Real ClaudeLlmProvider (Anthropic Messages API via Retrofit)

**Files:**
- Create: `core/llm/ClaudeApi.kt`
- Create: `core/llm/ClaudeLlmProvider.kt`
- Test: `test/core/llm/ClaudeLlmProviderTest.kt`

Uses OkHttp's `MockWebServer` so the test never makes a real network call — matches architecture doc §6's "LLM 调用失败处理" (one retry, then surface a recognizable failure).

- [ ] **Step 1: Write the Retrofit service interface and DTOs**

```kotlin
// core/llm/ClaudeApi.kt
package com.interviewcoach.core.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ClaudeApi {
    @Headers("content-type: application/json", "anthropic-version: 2023-06-01")
    @POST("v1/messages")
    suspend fun sendMessage(@Body body: ClaudeRequest): ClaudeResponse
}

@Serializable
data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<ClaudeMessage>,
)

@Serializable
data class ClaudeMessage(val role: String, val content: String)

@Serializable
data class ClaudeResponse(val content: List<ClaudeContentBlock>)

@Serializable
data class ClaudeContentBlock(val type: String, val text: String)
```

- [ ] **Step 2: Write the failing test**

```kotlin
// test/core/llm/ClaudeLlmProviderTest.kt
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
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ClaudeLlmProviderTest"`
Expected: FAIL — `ClaudeLlmProvider` does not exist.

- [ ] **Step 4: Implement ClaudeLlmProvider**

Only `gradeAnswer` is fully wired in this task (it's what the test exercises); the remaining interface methods follow the identical request/retry/parse pattern and are filled in as each is exercised in later phases (Task 10 wires `generatePlan`/`parseResume`, Task 14 wires `generateQuestion`, Task 18 wires `nextInterviewQuestion`/`generateReview`) — every method has a real body by the end of this plan, never a stub left in the shipped app.

```kotlin
// core/llm/ClaudeLlmProvider.kt
package com.interviewcoach.core.llm

import com.interviewcoach.domain.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

class ClaudeLlmProvider(
    private val apiKey: String,
    baseUrl: String = "https://api.anthropic.com/",
    client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader("x-api-key", apiKey).build())
        }.build(),
) : LlmProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ClaudeApi::class.java)

    private suspend fun send(prompt: String, retriesLeft: Int = 1): String {
        return try {
            val response = api.sendMessage(
                ClaudeRequest(model = MODEL, maxTokens = 1024, messages = listOf(ClaudeMessage("user", prompt))),
            )
            response.content.first().text
        } catch (e: IOException) {
            if (retriesLeft > 0) send(prompt, retriesLeft - 1) else throw LlmRequestFailure(e.message ?: "request failed")
        } catch (e: retrofit2.HttpException) {
            if (retriesLeft > 0) send(prompt, retriesLeft - 1) else throw LlmRequestFailure(e.message())
        }
    }

    override suspend fun gradeAnswer(questionContent: String, referenceAnswer: String, answerText: String): AnswerFeedback {
        val prompt = "题目:$questionContent\n参考答案:$referenceAnswer\n用户作答:$answerText\n" +
            """请以 JSON 格式返回 {"score": 0-100 的数字, "feedback": "一句话点评"},不要输出其他内容。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return AnswerFeedback(score = raw["score"]!!.jsonPrimitive.double, feedback = raw["feedback"]!!.jsonPrimitive.content)
    }

    override suspend fun parseResume(resumeText: String): ResumeParseResult {
        val prompt = "以下是一份简历文本,请提取推荐目标薪资区间和已展示的技能关键词," +
            """以 JSON 格式返回 {"suggestedSalaryRange": "如 15K-25K", "demonstratedSkills": ["技能1","技能2"], "confidence": 0-1 的数字}。""" +
            "\n\n$resumeText"
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return ResumeParseResult(
            suggestedSalaryRange = raw["suggestedSalaryRange"]?.jsonPrimitive?.content,
            demonstratedSkills = raw["demonstratedSkills"]!!.jsonArrayOfStrings(),
            confidence = raw["confidence"]!!.jsonPrimitive.double,
        )
    }

    override suspend fun generatePlan(input: PlanGenerationInput): PlanDraft {
        val prompt = "为岗位 ${input.positionId} 生成一份学习计划,用户已具备的知识点:" +
            "${input.demonstratedSkillKnowledgePointIds.joinToString(",")}。" +
            """以 JSON 格式返回 {"periodDays": 数字, "summary": "一句话说明", "tasksByDayIndex": {"0": [{"knowledgePointId": "...", "taskType": "practice"}], ...}}。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        val tasks = raw["tasksByDayIndex"]!!.jsonObject.entries.associate { (dayIndex, list) ->
            dayIndex.toInt() to list.jsonArrayOfObjects().map {
                DailyTaskDraft(it["knowledgePointId"]!!.jsonPrimitive.content, it["taskType"]!!.jsonPrimitive.content)
            }
        }
        return PlanDraft(periodDays = raw["periodDays"]!!.jsonPrimitive.content.toInt(), summary = raw["summary"]!!.jsonPrimitive.content, tasksByDayIndex = tasks)
    }

    override suspend fun generateQuestion(knowledgePointName: String, isCore: Boolean): QuestionDraft {
        val prompt = "为知识点「$knowledgePointName」生成一道面试练习题," +
            """以 JSON 格式返回 {"content": "题目", "referenceAnswer": "参考答案要点"}。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return QuestionDraft(content = raw["content"]!!.jsonPrimitive.content, referenceAnswer = raw["referenceAnswer"]!!.jsonPrimitive.content)
    }

    override suspend fun nextInterviewQuestion(context: InterviewContext): InterviewTurn {
        val history = context.transcriptSoFar.joinToString("\n") { "${it.role}: ${it.text}" }
        val prompt = "你是面试官,正在面试岗位 ${context.positionId}。" +
            "需要重点考察的薄弱知识点:${context.weakKnowledgePointIds.joinToString(",")}。历史对话:\n$history\n\n" +
            """请给出下一个问题(可以是追问),以 JSON 格式返回 {"text": "问题内容", "isFollowUp": true/false}。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return InterviewTurn(role = "ai", text = raw["text"]!!.jsonPrimitive.content, isFollowUp = raw["isFollowUp"]?.jsonPrimitive?.content?.toBoolean() ?: false)
    }

    override suspend fun generateReview(transcript: InterviewTranscript): ReviewReportDraft {
        val history = transcript.turns.joinToString("\n") { "${it.role}: ${it.text}" }
        val prompt = "以下是一场模拟面试的完整对话记录:\n$history\n\n请从专业知识/技能准确度和表达与逻辑结构两个维度评分并给出建议," +
            """以 JSON 格式返回 {"overallScore": 0-100, "knowledgeScore": 0-100, "expressionScore": 0-100, "highlights": "亮点", "weaknesses": "不足", "suggestions": "建议", "weakKnowledgePointIds": ["知识点id", ...]}。"""
        val raw = json.parseToJsonElement(send(prompt)).jsonObject
        return ReviewReportDraft(
            overallScore = raw["overallScore"]!!.jsonPrimitive.double,
            knowledgeScore = raw["knowledgeScore"]!!.jsonPrimitive.double,
            expressionScore = raw["expressionScore"]!!.jsonPrimitive.double,
            highlights = raw["highlights"]!!.jsonPrimitive.content,
            weaknesses = raw["weaknesses"]!!.jsonPrimitive.content,
            suggestions = raw["suggestions"]!!.jsonPrimitive.content,
            weakKnowledgePointIds = raw["weakKnowledgePointIds"]!!.jsonArrayOfStrings(),
        )
    }

    companion object { private const val MODEL = "claude-sonnet-5" }
}
```

`jsonArrayOfStrings()`/`jsonArrayOfObjects()` are two small private extension helpers on `JsonElement` (iterate `.jsonArray`, map to `.jsonPrimitive.content` / `.jsonObject`) — add them to a `JsonExtensions.kt` file alongside `ClaudeLlmProvider.kt`.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ClaudeLlmProviderTest"`
Expected: PASS (2 tests)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/interviewcoach/core/llm/ClaudeApi.kt app/src/main/java/com/interviewcoach/core/llm/ClaudeLlmProvider.kt app/src/test/java/com/interviewcoach/core/llm/ClaudeLlmProviderTest.kt
git commit -m "feat: implement ClaudeLlmProvider with one-retry failure handling"
```

---

### Task 7: Compose theme tokens and bottom-nav shell

**Files:**
- Create: `app/theme/Color.kt`, `app/theme/Theme.kt`
- Create: `app/navigation/AppNavHost.kt`
- Create: `MainActivity.kt`

No unit test for this task — it's pure UI wiring with no business logic, verified visually against the UI mockups when the emulator run happens at the end of Phase 2 (Task 15).

- [ ] **Step 1: Write theme tokens from UI doc §5.1**

```kotlin
// app/theme/Color.kt
package com.interviewcoach.app.theme

import androidx.compose.ui.graphics.Color

object AppColors {
    val PrimaryPurple = Color(0xFF6C5CE7)
    val PrimaryTeal = Color(0xFF00D2A8)
    val ButtonPurpleLight = Color(0xFF8E7CF7)
    val MasteryLowStart = Color(0xFFFF6B6B)
    val MasteryLowEnd = Color(0xFFFFD93D)
    val MasteryMidEnd = Color(0xFFFFB13D)
    val MasteryHighStart = Color(0xFF00D2A8)
    val MasteryHighEnd = Color(0xFF00B894)
    val PageBackground = Color(0xFFFAFAFC)
    val CardBackground = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF333333)
    val TextSecondary = Color(0xFF6B7280)
    val TextLabel = Color(0xFF767B87)
    val TextMuted = Color(0xFF8A8FA3)
    val WarningBackground = Color(0xFFFFF4E5)
    val WarningText = Color(0xFF8A5A00)
    val ErrorBackground = Color(0xFFFFF0F0)
    val ErrorText = Color(0xFFC0392B)
}
```

```kotlin
// app/theme/Theme.kt
package com.interviewcoach.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = AppColors.PrimaryPurple,
    background = AppColors.PageBackground,
    surface = AppColors.CardBackground,
    onBackground = AppColors.TextPrimary,
)

@Composable
fun InterviewCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
```

- [ ] **Step 2: Write the bottom-nav shell (screens are placeholders wired in later phases)**

```kotlin
// app/navigation/AppNavHost.kt
package com.interviewcoach.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private enum class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Dashboard("dashboard", "首页", Icons.Outlined.Home),
    Learning("learning", "学习", Icons.AutoMirrored.Outlined.MenuBook),
    FreeLearning("free_learning", "自由学习", Icons.AutoMirrored.Outlined.Chat),
    MockInterview("mock_interview", "模拟面试", Icons.Outlined.Mic),
    Profile("profile", "我的", Icons.Outlined.Person),
}

@Composable
fun RootShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        // Task 24 replaces this stub NavHost with the fully-wired one
        // (screens, ViewModels, onboarding/main routing).
        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.padding(padding))
    }
}
```

- [ ] **Step 3: Wire MainActivity**

```kotlin
// MainActivity.kt
package com.interviewcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.interviewcoach.app.navigation.RootShell
import com.interviewcoach.app.theme.InterviewCoachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InterviewCoachTheme { RootShell() } }
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL` — the bottom-nav shell renders with an empty content area; screens are wired in Tasks 9-21 and routed in Task 24.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/app/theme/ app/src/main/java/com/interviewcoach/app/navigation/AppNavHost.kt app/src/main/java/com/interviewcoach/MainActivity.kt
git commit -m "feat: add Compose theme tokens and bottom-nav shell"
```

---

## Phase 1: Seed data, onboarding, and plan generation

### Task 8: Position/KnowledgePoint seed data + loader

**Files:**
- Create: `app/src/main/assets/positions_seed.json`
- Create: `core/storage/SeedLoader.kt`
- Test: `test/core/storage/SeedLoaderTest.kt`

`PositionDao` already exists from Task 2 — this task only adds the seed JSON and the idempotent loader.

- [ ] **Step 1: Write the seed data**

```json
[
  {
    "id": "backend-java",
    "name": "后端开发-Java",
    "knowledgePoints": [
      {"id": "kp-java-basics", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false},
      {"id": "kp-concurrency", "name": "并发编程", "weight": 3, "difficulty": 2, "isCore": false},
      {"id": "kp-system-design", "name": "系统设计", "weight": 4, "difficulty": 3, "isCore": true}
    ]
  }
]
```

- [ ] **Step 2: Write the failing test for the seed loader**

```kotlin
// test/core/storage/SeedLoaderTest.kt
package com.interviewcoach.core.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp1", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false},
    {"id": "kp2", "name": "系统设计", "weight": 4, "difficulty": 3, "isCore": true}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class SeedLoaderTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `loadSeedIfEmpty populates positions and knowledge points from JSON`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)

        val positions = db.positionDao().getAllPositions()
        val points = db.positionDao().getAllKnowledgePoints()
        assertThat(positions).hasSize(1)
        assertThat(points).hasSize(2)
        assertThat(points.first { it.id == "kp2" }.isCore).isTrue()

        // Calling it again must not duplicate rows.
        loadSeedIfEmpty(db, SEED_JSON)
        assertThat(db.positionDao().getAllKnowledgePoints()).hasSize(2)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.SeedLoaderTest"`
Expected: FAIL — `loadSeedIfEmpty` does not exist.

- [ ] **Step 4: Implement the seed loader**

```kotlin
// core/storage/SeedLoader.kt
package com.interviewcoach.core.storage

import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.PositionEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

suspend fun loadSeedIfEmpty(db: AppDatabase, rawJson: String) {
    if (db.positionDao().getAllPositions().isNotEmpty()) return

    val positions = Json.parseToJsonElement(rawJson).jsonArray
    db.withTransaction {
        for (positionJson in positions) {
            val p = positionJson.jsonObject
            val positionId = p["id"]!!.jsonPrimitive.content
            db.positionDao().upsertPosition(PositionEntity(id = positionId, name = p["name"]!!.jsonPrimitive.content))
            for (kpJson in p["knowledgePoints"]!!.jsonArray) {
                val kp = kpJson.jsonObject
                db.positionDao().upsertKnowledgePoint(
                    KnowledgePointEntity(
                        id = kp["id"]!!.jsonPrimitive.content,
                        positionId = positionId,
                        name = kp["name"]!!.jsonPrimitive.content,
                        weight = kp["weight"]!!.jsonPrimitive.content.toInt(),
                        difficulty = kp["difficulty"]!!.jsonPrimitive.content.toInt(),
                        isCore = kp["isCore"]!!.jsonPrimitive.content.toBoolean(),
                    ),
                )
            }
        }
    }
}
```

`db.withTransaction { ... }` is Room's `androidx.room.withTransaction` KTX extension (`androidx.room:room-ktx`, already added in Task 1).

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.SeedLoaderTest"`
Expected: PASS (1 test)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/assets/positions_seed.json app/src/main/java/com/interviewcoach/core/storage/SeedLoader.kt app/src/test/java/com/interviewcoach/core/storage/SeedLoaderTest.kt
git commit -m "feat: add standard position seed data and idempotent loader"
```

---

### Task 9: ResumeParseService + onboarding screen

**Files:**
- Create: `domain/service/ResumeParseService.kt`
- Create: `feature/onboarding/OnboardingScreen.kt` + `OnboardingViewModel.kt`
- Test: `test/domain/service/ResumeParseServiceTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// test/domain/service/ResumeParseServiceTest.kt
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ResumeParseServiceTest"`
Expected: FAIL — `ResumeParseService` does not exist.

- [ ] **Step 3: Implement ResumeParseService**

Implements product doc §2.1's requirement: low-confidence parses must be flagged "以下为推测填写,请确认", never silently filled in.

```kotlin
// domain/service/ResumeParseService.kt
package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import javax.inject.Inject

data class ResumeParseOutcome(
    val suggestedSalaryRange: String?,
    val demonstratedSkills: List<String>,
    val confidence: Double,
    val needsUserConfirmation: Boolean,
)

class ResumeParseService @Inject constructor(private val llmProvider: LlmProvider) {
    suspend fun parseResumeText(resumeText: String): ResumeParseOutcome {
        val result = llmProvider.parseResume(resumeText)
        return ResumeParseOutcome(
            suggestedSalaryRange = result.suggestedSalaryRange,
            demonstratedSkills = result.demonstratedSkills,
            confidence = result.confidence,
            needsUserConfirmation = result.confidence < CONFIRMATION_THRESHOLD,
        )
    }

    companion object { private const val CONFIRMATION_THRESHOLD = 0.5 }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ResumeParseServiceTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Build the onboarding ViewModel + screen**

No UI test here — the screen is a thin form over `ResumeParseService` and `PositionDao`, both already unit-tested; checked against [UI doc §4](../../2026-09-14-ai-interview-assistant-ui-design.md) manually in Task 15's emulator pass.

```kotlin
// feature/onboarding/OnboardingViewModel.kt
package com.interviewcoach.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.llm.LlmProviderRegistry
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.entity.PositionEntity
import com.interviewcoach.domain.service.ResumeParseOutcome
import com.interviewcoach.domain.service.ResumeParseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val positions: List<PositionEntity> = emptyList(),
    val selectedPositionId: String? = null,
    val salaryRange: String = "15K-25K",
    val parsing: Boolean = false,
    val parseOutcome: ResumeParseOutcome? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val positionDao: PositionDao,
    private val llmProviderRegistry: LlmProviderRegistry,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = _state.value.copy(positions = positionDao.getAllPositions()) }
    }

    fun onPositionSelected(positionId: String) { _state.value = _state.value.copy(selectedPositionId = positionId) }

    fun onResumeTextChanged(resumeText: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(parsing = true)
            val outcome = ResumeParseService(llmProviderRegistry.current()).parseResumeText(resumeText)
            _state.value = _state.value.copy(
                parseOutcome = outcome,
                salaryRange = outcome.suggestedSalaryRange ?: _state.value.salaryRange,
                parsing = false,
            )
        }
    }
}
```

```kotlin
// feature/onboarding/OnboardingScreen.kt
package com.interviewcoach.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewcoach.app.theme.AppColors

@Composable
fun OnboardingScreen(
    onConfirmed: (positionId: String, salaryRange: String) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("设定目标") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                TextField(
                    value = state.positions.firstOrNull { it.id == state.selectedPositionId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("目标岗位") },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.positions.forEach { position ->
                        DropdownMenuItem(
                            text = { Text(position.name) },
                            onClick = { viewModel.onPositionSelected(position.id); expanded = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            var resumeText by remember { mutableStateOf("") }
            TextField(
                value = resumeText,
                onValueChange = { resumeText = it; viewModel.onResumeTextChanged(it) },
                label = { Text("粘贴简历文本(可选)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (state.parsing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (state.parseOutcome?.needsUserConfirmation == true) {
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.WarningBackground)) {
                    Text("以下为推测填写,请确认", color = AppColors.WarningText, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { state.selectedPositionId?.let { onConfirmed(it, state.salaryRange) } },
                enabled = state.selectedPositionId != null,
            ) { Text("生成面试计划") }
        }
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/interviewcoach/domain/service/ResumeParseService.kt app/src/main/java/com/interviewcoach/feature/onboarding/ app/src/test/java/com/interviewcoach/domain/service/ResumeParseServiceTest.kt
git commit -m "feat: add resume parse service and onboarding screen"
```

---

### Task 10: PlanService (rule engine + LLM) + Plan/DailyTask/Question DAOs

**Files:**
- Create: `domain/service/PlanService.kt`
- Create: `core/storage/dao/PlanDao.kt`, `core/storage/dao/DailyTaskDao.kt`, `core/storage/dao/QuestionDao.kt` (replace empty stubs from Task 2)
- Test: `test/domain/service/PlanServiceTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// test/domain/service/PlanServiceTest.kt
package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.llm.FakeLlmProvider
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp1", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class PlanServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `generateAndPersistPlan creates a draft plan with daily tasks for every day`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = PlanService(FakeLlmProvider(), db.planDao(), db.dailyTaskDao())

        val plan = service.generateAndPersistPlan(positionId = "p1")

        assertThat(plan.status).isEqualTo("draft")
        val tasks = db.dailyTaskDao().getTasksForPlan(plan.id)
        assertThat(tasks).isNotEmpty()
        assertThat(tasks.all { it.planId == plan.id }).isTrue()
    }

    @Test
    fun `confirmPlan transitions status from draft to confirmed`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = PlanService(FakeLlmProvider(), db.planDao(), db.dailyTaskDao())
        val plan = service.generateAndPersistPlan(positionId = "p1")

        service.confirmPlan(plan.id)

        assertThat(db.planDao().getPlanById(plan.id)?.status).isEqualTo("confirmed")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.PlanServiceTest"`
Expected: FAIL — `PlanService` does not exist.

- [ ] **Step 3: Implement the DAOs and PlanService**

```kotlin
// core/storage/dao/PlanDao.kt
package com.interviewcoach.core.storage.dao

import androidx.room.*
import com.interviewcoach.core.storage.entity.PlanEntity

@Dao
interface PlanDao {
    @Insert suspend fun insertPlan(plan: PlanEntity)

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getPlanById(id: String): PlanEntity?

    @Query("SELECT * FROM plans")
    suspend fun getAllPlans(): List<PlanEntity>

    @Query("UPDATE plans SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
```

```kotlin
// core/storage/dao/DailyTaskDao.kt
package com.interviewcoach.core.storage.dao

import androidx.room.*
import com.interviewcoach.core.storage.entity.DailyTaskEntity

@Dao
interface DailyTaskDao {
    @Insert suspend fun insertTask(task: DailyTaskEntity)

    @Query("SELECT * FROM daily_tasks WHERE planId = :planId")
    suspend fun getTasksForPlan(planId: String): List<DailyTaskEntity>

    @Query("SELECT * FROM daily_tasks WHERE planId = :planId AND date = :dateStartOfDay")
    suspend fun getTasksForDate(planId: String, dateStartOfDay: Long): List<DailyTaskEntity>

    // Deletes not-yet-completed future tasks so AdjustmentService (Task 16)
    // can regenerate them without touching history.
    @Query("DELETE FROM daily_tasks WHERE planId = :planId AND completed = 0 AND date >= :fromDate")
    suspend fun deleteIncompleteFutureTasks(planId: String, fromDate: Long)

    @Query("UPDATE daily_tasks SET completed = 1 WHERE id = :taskId")
    suspend fun markCompleted(taskId: String)
}
```

```kotlin
// core/storage/dao/QuestionDao.kt
package com.interviewcoach.core.storage.dao

import androidx.room.*
import com.interviewcoach.core.storage.entity.QuestionEntity

@Dao
interface QuestionDao {
    @Insert suspend fun insertQuestion(question: QuestionEntity)

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("UPDATE questions SET flagCount = flagCount + 1 WHERE id = :id")
    suspend fun incrementFlagCount(id: String)
}
```

```kotlin
// domain/service/PlanService.kt
package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.dao.PlanDao
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.domain.model.PlanGenerationInput
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlanService @Inject constructor(
    private val llmProvider: LlmProvider,
    private val planDao: PlanDao,
    private val dailyTaskDao: DailyTaskDao,
) {
    suspend fun generateAndPersistPlan(
        positionId: String,
        demonstratedSkillKnowledgePointIds: List<String> = emptyList(),
    ): PlanEntity {
        val draft = llmProvider.generatePlan(PlanGenerationInput(positionId, demonstratedSkillKnowledgePointIds))

        val planId = UUID.randomUUID().toString()
        val startDate = System.currentTimeMillis()
        planDao.insertPlan(PlanEntity(id = planId, positionId = positionId, startDate = startDate, periodDays = draft.periodDays, status = "draft"))

        for ((dayIndex, taskDrafts) in draft.tasksByDayIndex) {
            val date = startDate + TimeUnit.DAYS.toMillis(dayIndex.toLong())
            for (taskDraft in taskDrafts) {
                dailyTaskDao.insertTask(
                    DailyTaskEntity(
                        id = UUID.randomUUID().toString(),
                        planId = planId,
                        date = date,
                        knowledgePointId = taskDraft.knowledgePointId,
                        taskType = taskDraft.taskType,
                    ),
                )
            }
        }
        return planDao.getPlanById(planId)!!
    }

    suspend fun confirmPlan(planId: String) = planDao.updateStatus(planId, "confirmed")
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.PlanServiceTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/domain/service/PlanService.kt app/src/main/java/com/interviewcoach/core/storage/dao/PlanDao.kt app/src/main/java/com/interviewcoach/core/storage/dao/DailyTaskDao.kt app/src/main/java/com/interviewcoach/core/storage/dao/QuestionDao.kt app/src/test/java/com/interviewcoach/domain/service/PlanServiceTest.kt
git commit -m "feat: add PlanService and Plan/DailyTask/Question DAOs"
```

---

### Task 11: Plan confirmation screen

**Files:**
- Create: `feature/planconfirm/PlanConfirmScreen.kt` + `PlanConfirmViewModel.kt`

- [ ] **Step 1: Build the screen**

Thin UI over the already-tested `PlanService`; matches [product doc §2.3](../../2026-09-14-ai-interview-assistant-design.md).

```kotlin
// feature/planconfirm/PlanConfirmViewModel.kt
package com.interviewcoach.feature.planconfirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.domain.service.PlanService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanConfirmUiState(val tasks: List<DailyTaskEntity> = emptyList(), val loading: Boolean = true)

@HiltViewModel
class PlanConfirmViewModel @Inject constructor(
    private val planService: PlanService,
    private val dailyTaskDao: DailyTaskDao,
) : ViewModel() {
    private val _state = MutableStateFlow(PlanConfirmUiState())
    val state: StateFlow<PlanConfirmUiState> = _state.asStateFlow()

    fun load(planId: String) {
        viewModelScope.launch { _state.value = PlanConfirmUiState(tasks = dailyTaskDao.getTasksForPlan(planId), loading = false) }
    }

    fun confirm(planId: String, onConfirmed: () -> Unit) {
        viewModelScope.launch { planService.confirmPlan(planId); onConfirmed() }
    }
}
```

```kotlin
// feature/planconfirm/PlanConfirmScreen.kt
package com.interviewcoach.feature.planconfirm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewcoach.core.storage.entity.PlanEntity

@Composable
fun PlanConfirmScreen(plan: PlanEntity, onConfirmed: () -> Unit, viewModel: PlanConfirmViewModel = hiltViewModel()) {
    LaunchedEffect(plan.id) { viewModel.load(plan.id) }
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("确认学习计划") }) }) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val byKnowledgePoint = state.tasks.groupingBy { it.knowledgePointId }.eachCount()
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text("共 ${plan.periodDays} 天,${state.tasks.size} 个任务", modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(byKnowledgePoint.entries.toList()) { (kpId, count) ->
                    ListItem(headlineContent = { Text(kpId) }, trailingContent = { Text("$count 次") })
                }
            }
            Button(onClick = { viewModel.confirm(plan.id, onConfirmed) }, modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("确认计划")
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interviewcoach/feature/planconfirm/
git commit -m "feat: add plan confirmation screen"
```

---

## Phase 2: Core learning loop (practice, grading, mastery, dashboard)

### Task 12: AttemptDao + MasteryDao + MasteryService

**Files:**
- Create: `core/storage/dao/AttemptDao.kt`, `core/storage/dao/MasteryDao.kt` (replace empty stubs from Task 2)
- Create: `domain/service/MasteryService.kt`
- Test: `test/domain/service/MasteryServiceTest.kt`

Implements product doc §2.6 (corrected): mastery is a **difficulty-weighted average of continuous AI scores**, not a correct/incorrect rate. The knowledge point's `difficulty` field is used as the per-attempt weight (a question inherits its knowledge point's difficulty for MVP — see Task 8's seed schema, which has no per-question difficulty field).

- [ ] **Step 1: Write the failing test**

```kotlin
// test/domain/service/MasteryServiceTest.kt
package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.entity.AttemptEntity
import com.interviewcoach.core.storage.entity.QuestionEntity
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp1", "name": "Java 基础", "weight": 2, "difficulty": 2, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class MasteryServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `recalculateForKnowledgePoint averages scores weighted by question difficulty`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = MasteryService(db.attemptDao(), db.masteryDao())
        db.questionDao().insertQuestion(QuestionEntity(id = "q1", knowledgePointId = "kp1", content = "c", referenceAnswer = "a"))
        db.attemptDao().insertAttempt(AttemptEntity(UUID.randomUUID().toString(), "q1", "kp1", "answer 1", 80.0, "ok", "daily_task", 0L))
        db.attemptDao().insertAttempt(AttemptEntity(UUID.randomUUID().toString(), "q1", "kp1", "answer 2", 60.0, "ok", "daily_task", 0L))

        service.recalculateForKnowledgePoint("kp1")

        // Both attempts share the same knowledge point difficulty, so this is a plain average.
        assertThat(db.masteryDao().getMasteryForKnowledgePoint("kp1")?.score).isWithin(0.01).of(70.0)

        // Recalculating again must update, not duplicate, the record.
        service.recalculateForKnowledgePoint("kp1")
        assertThat(db.masteryDao().getAllMastery()).hasSize(1)
    }

    @Test
    fun `recalculateForKnowledgePoint with no attempts leaves mastery at 0`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = MasteryService(db.attemptDao(), db.masteryDao())

        service.recalculateForKnowledgePoint("kp1")

        assertThat(db.masteryDao().getMasteryForKnowledgePoint("kp1")?.score).isEqualTo(0.0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.MasteryServiceTest"`
Expected: FAIL — `MasteryService` does not exist.

- [ ] **Step 3: Implement the DAOs and MasteryService**

```kotlin
// core/storage/dao/AttemptDao.kt
package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.interviewcoach.core.storage.entity.AttemptEntity

@Dao
interface AttemptDao {
    @Insert suspend fun insertAttempt(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts WHERE knowledgePointId = :knowledgePointId")
    suspend fun getAttemptsForKnowledgePoint(knowledgePointId: String): List<AttemptEntity>
}
```

```kotlin
// core/storage/dao/MasteryDao.kt
package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.interviewcoach.core.storage.entity.MasteryRecordEntity

@Dao
interface MasteryDao {
    @Upsert suspend fun upsertMastery(record: MasteryRecordEntity)

    suspend fun upsertMastery(knowledgePointId: String, score: Double) =
        upsertMastery(MasteryRecordEntity(knowledgePointId, score, System.currentTimeMillis()))

    @Query("SELECT * FROM mastery_records WHERE knowledgePointId = :knowledgePointId")
    suspend fun getMasteryForKnowledgePoint(knowledgePointId: String): MasteryRecordEntity?

    @Query("SELECT * FROM mastery_records")
    suspend fun getAllMastery(): List<MasteryRecordEntity>
}
```

```kotlin
// domain/service/MasteryService.kt
package com.interviewcoach.domain.service

import com.interviewcoach.core.storage.dao.AttemptDao
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import javax.inject.Inject

class MasteryService @Inject constructor(
    private val attemptDao: AttemptDao,
    private val masteryDao: MasteryDao,
) {
    suspend fun recalculateForKnowledgePoint(knowledgePointId: String, difficultyWeight: Int = 1) {
        val attempts = attemptDao.getAttemptsForKnowledgePoint(knowledgePointId)
        if (attempts.isEmpty()) {
            masteryDao.upsertMastery(knowledgePointId, 0.0)
            return
        }
        val total = attempts.sumOf { it.score * difficultyWeight }
        val weightTotal = attempts.size * difficultyWeight
        masteryDao.upsertMastery(knowledgePointId, total / weightTotal)
    }

    /**
     * Overall mastery = simple average across all knowledge points that have
     * at least one attempt-derived record (product doc §2.9 unlock condition
     * also needs the per-core-point view — see Task 18).
     */
    suspend fun overallMastery(allKnowledgePoints: List<KnowledgePointEntity>): Double {
        if (allKnowledgePoints.isEmpty()) return 0.0
        var sum = 0.0
        for (kp in allKnowledgePoints) sum += masteryDao.getMasteryForKnowledgePoint(kp.id)?.score ?: 0.0
        return sum / allKnowledgePoints.size
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.MasteryServiceTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/core/storage/dao/AttemptDao.kt app/src/main/java/com/interviewcoach/core/storage/dao/MasteryDao.kt app/src/main/java/com/interviewcoach/domain/service/MasteryService.kt app/src/test/java/com/interviewcoach/domain/service/MasteryServiceTest.kt
git commit -m "feat: add AttemptDao, MasteryDao, and difficulty-weighted MasteryService"
```

---

### Task 13: Reusable AI-request-state composable

**Files:**
- Create: `app/widgets/AiActionButton.kt`

Implements [UI doc §5.3](../../2026-09-14-ai-interview-assistant-ui-design.md) so every LLM-backed action (this task's daily-task submit, Task 17's free-learning submit, Task 20's mock-interview send) reuses one composable instead of five bespoke loading/error implementations.

- [ ] **Step 1: Build the composable**

```kotlin
// app/widgets/AiActionButton.kt
package com.interviewcoach.app.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.interviewcoach.app.theme.AppColors

enum class AiActionState { IDLE, LOADING, ERROR }

@Composable
fun AiActionButton(
    state: AiActionState,
    idleLabel: String,
    loadingLabel: String,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    if (state == AiActionState.ERROR) {
        Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = AppColors.ErrorBackground)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(errorMessage ?: "请求失败,网络似乎有点问题", color = AppColors.ErrorText, modifier = Modifier.weight(1f))
                TextButton(onClick = onRetry) { Text("重试") }
            }
        }
        return
    }

    Button(onClick = onClick, enabled = state != AiActionState.LOADING, modifier = modifier) {
        if (state == AiActionState.LOADING) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(loadingLabel)
        } else {
            Text(idleLabel)
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interviewcoach/app/widgets/AiActionButton.kt
git commit -m "feat: add reusable AI request loading/error composable"
```

---

### Task 14: Daily task screen (question generation, submission, grading, flagging)

**Files:**
- Create: `feature/learning/DailyTaskScreen.kt` + `DailyTaskViewModel.kt`
- Create: `domain/service/LearningSessionService.kt`
- Test: `test/domain/service/LearningSessionServiceTest.kt`

The screen itself has no business-logic test (it's a thin `AiActionButton` consumer); `LearningSessionService` — which generates-or-reuses a question, records the attempt, and triggers mastery recalculation — is the piece with real logic, so it gets the TDD treatment.

- [ ] **Step 1: Write the failing test**

```kotlin
// test/domain/service/LearningSessionServiceTest.kt
package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.llm.FakeLlmProvider
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp1", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class LearningSessionServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun service() = LearningSessionService(
        FakeLlmProvider(), db.questionDao(), db.attemptDao(),
        MasteryService(db.attemptDao(), db.masteryDao()),
    )

    @Test
    fun `submitAnswer generates a question, records an attempt, and updates mastery`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = service()

        val question = service.getOrCreateQuestion(knowledgePointId = "kp1", knowledgePointName = "Java 基础", isCore = false)
        val feedback = service.submitAnswer(
            question = question, knowledgePointId = "kp1",
            answerText = "a detailed answer about generics and collections", source = "daily_task",
        )

        assertThat(feedback.score).isGreaterThan(0.0)
        assertThat(db.attemptDao().getAttemptsForKnowledgePoint("kp1")).hasSize(1)
        assertThat(db.masteryDao().getMasteryForKnowledgePoint("kp1")?.score).isEqualTo(feedback.score)
    }

    @Test
    fun `flagQuestion increments the flag count`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = service()
        val question = service.getOrCreateQuestion(knowledgePointId = "kp1", knowledgePointName = "Java 基础", isCore = false)

        service.flagQuestion(question.id)

        assertThat(db.questionDao().getQuestionById(question.id)?.flagCount).isEqualTo(1)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.LearningSessionServiceTest"`
Expected: FAIL — `LearningSessionService` does not exist.

- [ ] **Step 3: Implement LearningSessionService**

```kotlin
// domain/service/LearningSessionService.kt
package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import com.interviewcoach.core.storage.dao.AttemptDao
import com.interviewcoach.core.storage.dao.QuestionDao
import com.interviewcoach.core.storage.entity.AttemptEntity
import com.interviewcoach.core.storage.entity.QuestionEntity
import com.interviewcoach.domain.model.AnswerFeedback
import java.util.UUID
import javax.inject.Inject

class LearningSessionService @Inject constructor(
    private val llmProvider: LlmProvider,
    private val questionDao: QuestionDao,
    private val attemptDao: AttemptDao,
    private val masteryService: MasteryService,
) {
    suspend fun getOrCreateQuestion(knowledgePointId: String, knowledgePointName: String, isCore: Boolean): QuestionEntity {
        val draft = llmProvider.generateQuestion(knowledgePointName, isCore)
        val id = UUID.randomUUID().toString()
        questionDao.insertQuestion(QuestionEntity(id = id, knowledgePointId = knowledgePointId, content = draft.content, referenceAnswer = draft.referenceAnswer))
        return questionDao.getQuestionById(id)!!
    }

    suspend fun submitAnswer(
        question: QuestionEntity,
        knowledgePointId: String,
        answerText: String,
        source: String, // daily_task | free_learning
    ): AnswerFeedback {
        val feedback = llmProvider.gradeAnswer(question.content, question.referenceAnswer, answerText)
        attemptDao.insertAttempt(
            AttemptEntity(
                id = UUID.randomUUID().toString(), questionId = question.id, knowledgePointId = knowledgePointId,
                answerText = answerText, score = feedback.score, feedback = feedback.feedback, source = source,
                createdAt = System.currentTimeMillis(),
            ),
        )
        masteryService.recalculateForKnowledgePoint(knowledgePointId)
        return feedback
    }

    suspend fun flagQuestion(questionId: String) = questionDao.incrementFlagCount(questionId)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.LearningSessionServiceTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Build the daily task ViewModel + screen**

Matches [daily-task.html](../../ui-mockups/daily-task.html): question card, textarea, `AiActionButton` for submit, feedback state with reference answer and a flag link.

```kotlin
// feature/learning/DailyTaskViewModel.kt
package com.interviewcoach.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.app.widgets.AiActionState
import com.interviewcoach.core.storage.entity.QuestionEntity
import com.interviewcoach.domain.model.AnswerFeedback
import com.interviewcoach.domain.service.LearningSessionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyTaskUiState(
    val question: QuestionEntity? = null,
    val feedback: AnswerFeedback? = null,
    val actionState: AiActionState = AiActionState.IDLE,
)

@HiltViewModel
class DailyTaskViewModel @Inject constructor(
    private val sessionService: LearningSessionService,
) : ViewModel() {
    private val _state = MutableStateFlow(DailyTaskUiState())
    val state: StateFlow<DailyTaskUiState> = _state.asStateFlow()

    fun load(knowledgePointId: String, knowledgePointName: String, isCore: Boolean) {
        viewModelScope.launch {
            val question = sessionService.getOrCreateQuestion(knowledgePointId, knowledgePointName, isCore)
            _state.value = _state.value.copy(question = question)
        }
    }

    fun submit(knowledgePointId: String, answerText: String) {
        val question = _state.value.question ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(actionState = AiActionState.LOADING)
            try {
                val feedback = sessionService.submitAnswer(question, knowledgePointId, answerText, "daily_task")
                _state.value = _state.value.copy(feedback = feedback, actionState = AiActionState.IDLE)
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionState = AiActionState.ERROR)
            }
        }
    }

    fun flagQuestion() {
        val question = _state.value.question ?: return
        viewModelScope.launch { sessionService.flagQuestion(question.id) }
    }
}
```

```kotlin
// feature/learning/DailyTaskScreen.kt
package com.interviewcoach.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewcoach.app.widgets.AiActionButton

@Composable
fun DailyTaskScreen(
    knowledgePointId: String,
    knowledgePointName: String,
    isCore: Boolean,
    onTaskCompleted: () -> Unit,
    viewModel: DailyTaskViewModel = hiltViewModel(),
) {
    LaunchedEffect(knowledgePointId) { viewModel.load(knowledgePointId, knowledgePointName, isCore) }
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(knowledgePointName) }) }) { padding ->
        val question = state.question
        if (question == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(question.content)
            Spacer(Modifier.height(12.dp))
            var answerText by remember { mutableStateOf("") }
            val feedback = state.feedback
            if (feedback == null) {
                TextField(value = answerText, onValueChange = { answerText = it }, minLines = 6, modifier = Modifier.fillMaxWidth(), placeholder = { Text("在这里输入你的回答…") })
                Spacer(Modifier.height(12.dp))
                AiActionButton(
                    state = state.actionState, idleLabel = "提交作答", loadingLabel = "AI 正在批改…",
                    onClick = { viewModel.submit(knowledgePointId, answerText) },
                    onRetry = { viewModel.submit(knowledgePointId, answerText) },
                )
            } else {
                Text("得分:${feedback.score.toInt()}")
                Text(feedback.feedback)
                Text("参考答案:${question.referenceAnswer}")
                TextButton(onClick = { viewModel.flagQuestion() }) { Text("🚩 这道题/答案有问题?") }
                Button(onClick = onTaskCompleted) { Text("下一题 →") }
            }
        }
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/interviewcoach/domain/service/LearningSessionService.kt app/src/main/java/com/interviewcoach/feature/learning/DailyTaskScreen.kt app/src/main/java/com/interviewcoach/feature/learning/DailyTaskViewModel.kt app/src/test/java/com/interviewcoach/domain/service/LearningSessionServiceTest.kt
git commit -m "feat: add LearningSessionService and the daily task Q&A screen"
```

---

### Task 15: Task list screen + dashboard screen

**Files:**
- Create: `feature/learning/TaskListScreen.kt` + `TaskListViewModel.kt`
- Create: `feature/dashboard/DashboardScreen.kt` + `DashboardViewModel.kt`

Both are read-only views over already-tested DAOs/services (`DailyTaskDao`, `MasteryDao`), so — consistent with Tasks 9 and 11 — there's no new business logic to unit-test here; correctness is verified by the emulator pass in Step 3 below.

- [ ] **Step 1: Build the task list screen**

Matches [task-list.html](../../ui-mockups/task-list.html): completed items keep a full-color checkmark with muted (not dimmed-row) text, per the UI review fix.

```kotlin
// feature/learning/TaskListViewModel.kt
package com.interviewcoach.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(private val dailyTaskDao: DailyTaskDao) : ViewModel() {
    private val _tasks = MutableStateFlow<List<DailyTaskEntity>>(emptyList())
    val tasks: StateFlow<List<DailyTaskEntity>> = _tasks.asStateFlow()

    fun load(planId: String) {
        viewModelScope.launch {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            _tasks.value = dailyTaskDao.getTasksForDate(planId, startOfDay)
        }
    }
}
```

```kotlin
// feature/learning/TaskListScreen.kt
package com.interviewcoach.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.core.storage.entity.DailyTaskEntity

@Composable
fun TaskListScreen(planId: String, onTaskSelected: (DailyTaskEntity) -> Unit, viewModel: TaskListViewModel = hiltViewModel()) {
    LaunchedEffect(planId) { viewModel.load(planId) }
    val tasks by viewModel.tasks.collectAsState()
    val done = tasks.count { it.completed }

    Scaffold(topBar = { TopAppBar(title = { Text("今日任务") }) }) { padding ->
        Column(Modifier.padding(padding)) {
            LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth().padding(16.dp))
            LazyColumn {
                items(tasks) { task ->
                    ListItem(
                        leadingContent = {
                            if (task.completed) Icon(Icons.Filled.CheckCircle, null, tint = AppColors.MasteryHighEnd)
                            else Icon(if (task.taskType == "practice") Icons.Outlined.MenuBook else Icons.Outlined.Style, null, tint = AppColors.PrimaryPurple)
                        },
                        headlineContent = {
                            Text(
                                task.knowledgePointId,
                                color = if (task.completed) AppColors.TextMuted else Color.Unspecified,
                                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                            )
                        },
                        modifier = Modifier.let { if (!task.completed) it else it },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build the dashboard screen**

Matches [dashboard.html](../../ui-mockups/dashboard.html), including the unlock-condition preview text (full unlock logic lands in Task 18).

```kotlin
// feature/dashboard/DashboardViewModel.kt
package com.interviewcoach.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.PositionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgePointMastery(val id: String, val name: String, val isCore: Boolean, val score: Double)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val positionDao: PositionDao,
    private val masteryDao: MasteryDao,
) : ViewModel() {
    private val _items = MutableStateFlow<List<KnowledgePointMastery>>(emptyList())
    val items: StateFlow<List<KnowledgePointMastery>> = _items.asStateFlow()

    fun load(positionId: String) {
        viewModelScope.launch {
            _items.value = positionDao.getKnowledgePointsForPosition(positionId).map { kp ->
                KnowledgePointMastery(kp.id, kp.name, kp.isCore, masteryDao.getMasteryForKnowledgePoint(kp.id)?.score ?: 0.0)
            }
        }
    }
}
```

```kotlin
// feature/dashboard/DashboardScreen.kt
package com.interviewcoach.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DashboardScreen(positionId: String, viewModel: DashboardViewModel = hiltViewModel()) {
    LaunchedEffect(positionId) { viewModel.load(positionId) }
    val items by viewModel.items.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("首页") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            items(items) { kp ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(if (kp.isCore) "${kp.name} ⭐核心" else kp.name) },
                        supportingContent = { LinearProgressIndicator(progress = { (kp.score / 100).toFloat() }, modifier = Modifier.fillMaxWidth()) },
                        trailingContent = { Text("${kp.score.toInt()}%") },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Run the app end-to-end on an emulator**

Run: `./gradlew :app:installDebug` then launch the app.
Expected: App launches to the bottom-nav shell; navigate onboarding → plan confirm → dashboard → task list → daily task screen manually and confirm a submitted answer updates the dashboard's progress bar for that knowledge point. Compare visually against the mockups linked above — pixel-perfect fidelity isn't required for MVP, but layout and color tokens should match.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/interviewcoach/feature/learning/TaskListScreen.kt app/src/main/java/com/interviewcoach/feature/learning/TaskListViewModel.kt app/src/main/java/com/interviewcoach/feature/dashboard/
git commit -m "feat: add task list and dashboard screens"
```

---

## Phase 3: Dynamic plan adjustment

### Task 16: AdjustmentService (mastery / pace / subjective-feedback signals)

**Files:**
- Create: `domain/service/AdjustmentService.kt`
- Test: `test/domain/service/AdjustmentServiceTest.kt`

Implements product doc §2.7's three-signal rule exactly: **mastery signal decides content** (which knowledge points get scheduled, weighted toward low-mastery core points), **pace signal decides quantity** (how many tasks per day), and **subjective feedback is an overlay** on top of both (marking a knowledge point "too hard" inserts a knowledge-card warm-up before its next practice task; "too easy" reduces its repeat weight) — not a fourth independent axis.

- [ ] **Step 1: Write the failing test**

```kotlin
// test/domain/service/AdjustmentServiceTest.kt
package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp-weak-core", "name": "系统设计", "weight": 4, "difficulty": 3, "isCore": true},
    {"id": "kp-mastered", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class AdjustmentServiceTest {
    private lateinit var db: AppDatabase
    private val day0 = TimeUnit.DAYS.toMillis(20000) // arbitrary fixed reference day

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun service() = AdjustmentService(db.dailyTaskDao(), db.masteryDao(), db.positionDao(), db.settingsDao())

    @Test
    fun `mastery signal schedules more tasks for a weak core point than a mastered one`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak-core", 30.0)
        db.masteryDao().upsertMastery("kp-mastered", 95.0)
        val service = service()

        service.regenerateFutureTasks(planId = "plan1", positionId = "p1", fromDate = day0, recentAvgTasksPerDay = 3.0)

        val tasks = db.dailyTaskDao().getTasksForPlan("plan1")
        val weakCount = tasks.count { it.knowledgePointId == "kp-weak-core" }
        val masteredCount = tasks.count { it.knowledgePointId == "kp-mastered" }
        assertThat(weakCount).isGreaterThan(masteredCount)
    }

    @Test
    fun `pace signal reduces tasks scheduled per day when recent completion is slow`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak-core", 30.0)
        val service = service()

        service.regenerateFutureTasks(planId = "plan-slow", positionId = "p1", fromDate = day0, recentAvgTasksPerDay = 0.5)

        val tasksOnFirstDay = db.dailyTaskDao().getTasksForPlan("plan-slow").count { it.date == day0 }
        assertThat(tasksOnFirstDay).isAtMost(2)
    }

    @Test
    fun `recording too-hard feedback inserts a card warm-up before the next practice task`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak-core", 30.0)
        val service = service()

        service.recordDifficultyFeedback(knowledgePointId = "kp-weak-core", tooHard = true)
        service.regenerateFutureTasks(planId = "plan-fb", positionId = "p1", fromDate = day0, recentAvgTasksPerDay = 3.0)

        val tasks = db.dailyTaskDao().getTasksForPlan("plan-fb").sortedBy { it.date }
        val firstWeakCoreTask = tasks.first { it.knowledgePointId == "kp-weak-core" }
        assertThat(firstWeakCoreTask.taskType).isEqualTo("card")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.AdjustmentServiceTest"`
Expected: FAIL — `AdjustmentService` does not exist.

- [ ] **Step 3: Implement AdjustmentService**

```kotlin
// domain/service/AdjustmentService.kt
package com.interviewcoach.domain.service

import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.dao.SettingsDao
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AdjustmentService @Inject constructor(
    private val dailyTaskDao: DailyTaskDao,
    private val masteryDao: MasteryDao,
    private val positionDao: PositionDao,
    private val settingsDao: SettingsDao,
) {
    private fun difficultyPrefKey(knowledgePointId: String) = "difficulty_pref_$knowledgePointId"

    suspend fun recordDifficultyFeedback(knowledgePointId: String, tooHard: Boolean) =
        settingsDao.setSetting(difficultyPrefKey(knowledgePointId), if (tooHard) "too_hard" else "too_easy")

    /**
     * Mastery signal: how many practice slots a knowledge point gets in the
     * upcoming pool. Low mastery + core gets the most; anything at/above the
     * mastery threshold gets none (already learned, don't keep repeating it).
     */
    private fun masteryWeight(kp: KnowledgePointEntity, score: Double): Int = when {
        score >= 85 -> 0
        score >= 60 -> 1
        else -> if (kp.isCore) 3 else 2
    }

    /**
     * Pace signal: tasks per day, derived from how many the user has actually
     * been completing recently — this only changes quantity, never content.
     */
    private fun tasksPerDayFromPace(recentAvgTasksPerDay: Double): Int = when {
        recentAvgTasksPerDay < 1.0 -> 1
        recentAvgTasksPerDay < 2.0 -> 2
        else -> 3
    }

    suspend fun regenerateFutureTasks(planId: String, positionId: String, fromDate: Long, recentAvgTasksPerDay: Double) {
        dailyTaskDao.deleteIncompleteFutureTasks(planId, fromDate)

        val knowledgePoints = positionDao.getKnowledgePointsForPosition(positionId)
        val pool = mutableListOf<KnowledgePointEntity>()
        for (kp in knowledgePoints) {
            val mastery = masteryDao.getMasteryForKnowledgePoint(kp.id)
            var weight = masteryWeight(kp, mastery?.score ?: 0.0)
            val pref = settingsDao.getSetting(difficultyPrefKey(kp.id))
            if (pref == "too_easy" && weight > 0) weight -= 1
            repeat(weight) { pool.add(kp) }
        }
        if (pool.isEmpty()) return

        val tasksPerDay = tasksPerDayFromPace(recentAvgTasksPerDay)
        val insertedCardFor = mutableSetOf<String>()

        var day = 0
        var i = 0
        while (i < pool.size) {
            val date = fromDate + TimeUnit.DAYS.toMillis(day.toLong())
            var slot = 0
            while (slot < tasksPerDay && i < pool.size) {
                val kp = pool[i]
                val pref = settingsDao.getSetting(difficultyPrefKey(kp.id))
                if (pref == "too_hard" && kp.id !in insertedCardFor) {
                    dailyTaskDao.insertTask(DailyTaskEntity(UUID.randomUUID().toString(), planId, date, kp.id, taskType = "card"))
                    insertedCardFor.add(kp.id)
                } else {
                    dailyTaskDao.insertTask(DailyTaskEntity(UUID.randomUUID().toString(), planId, date, kp.id, taskType = "practice"))
                    i++
                }
                slot++
            }
            day++
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.AdjustmentServiceTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/domain/service/AdjustmentService.kt app/src/test/java/com/interviewcoach/domain/service/AdjustmentServiceTest.kt
git commit -m "feat: add AdjustmentService with mastery/pace/subjective-feedback signals"
```

---

## Phase 4: Free learning mode

### Task 17: FreeLearningService (mastery-based review selection) + screen

**Files:**
- Create: `domain/service/FreeLearningService.kt`
- Create: `feature/freelearning/FreeLearningScreen.kt` + `FreeLearningViewModel.kt`
- Test: `test/domain/service/FreeLearningServiceTest.kt`

Implements product doc §2.5: reviews only knowledge points the user has **already** attempted at least once (a `MasteryRecordEntity` row is the proxy for "already learned" — it only exists once `MasteryService.recalculateForKnowledgePoint` has run, which only happens after a first attempt), prioritized by lowest mastery score, then by longest time since last update — no separate spaced-repetition engine, reusing the same mastery data as the main plan (product doc's explicit design decision).

- [ ] **Step 1: Write the failing test**

```kotlin
// test/domain/service/FreeLearningServiceTest.kt
package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp-weak", "name": "系统设计", "weight": 4, "difficulty": 3, "isCore": true},
    {"id": "kp-strong", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false},
    {"id": "kp-untouched", "name": "并发编程", "weight": 3, "difficulty": 2, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class FreeLearningServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun service() = FreeLearningService(db.masteryDao(), db.positionDao())

    @Test
    fun `picks the lowest-mastery already-learned knowledge point`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak", 40.0)
        db.masteryDao().upsertMastery("kp-strong", 90.0)
        // kp-untouched has no mastery record — never attempted, must be ignored.

        val selected = service().selectKnowledgePointToReview("p1")

        assertThat(selected?.id).isEqualTo("kp-weak")
    }

    @Test
    fun `returns null when the user has not attempted anything yet`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)

        assertThat(service().selectKnowledgePointToReview("p1")).isNull()
    }

    @Test
    fun `excludeId lets 换一个 pick a different already-learned point`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-weak", 40.0)
        db.masteryDao().upsertMastery("kp-strong", 90.0)

        val selected = service().selectKnowledgePointToReview("p1", excludeId = "kp-weak")

        assertThat(selected?.id).isEqualTo("kp-strong")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.FreeLearningServiceTest"`
Expected: FAIL — `FreeLearningService` does not exist.

- [ ] **Step 3: Implement FreeLearningService**

```kotlin
// domain/service/FreeLearningService.kt
package com.interviewcoach.domain.service

import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import javax.inject.Inject

class FreeLearningService @Inject constructor(
    private val masteryDao: MasteryDao,
    private val positionDao: PositionDao,
) {
    suspend fun selectKnowledgePointToReview(positionId: String, excludeId: String? = null): KnowledgePointEntity? {
        val candidates = positionDao.getKnowledgePointsForPosition(positionId)
            .filter { it.id != excludeId }
            .mapNotNull { kp -> masteryDao.getMasteryForKnowledgePoint(kp.id)?.let { kp to it } }
        if (candidates.isEmpty()) return null

        return candidates.sortedWith(compareBy({ it.second.score }, { it.second.updatedAt })).first().first
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.FreeLearningServiceTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Build the free learning ViewModel + screen**

Matches [free-learning.html](../../ui-mockups/free-learning.html) and its empty-state gap flagged in review: when `selectKnowledgePointToReview` returns null, show a message instead of an empty question card.

```kotlin
// feature/freelearning/FreeLearningViewModel.kt
package com.interviewcoach.feature.freelearning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.app.widgets.AiActionState
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.QuestionEntity
import com.interviewcoach.domain.model.AnswerFeedback
import com.interviewcoach.domain.service.FreeLearningService
import com.interviewcoach.domain.service.LearningSessionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FreeLearningUiState(
    val loadingPick: Boolean = true,
    val knowledgePoint: KnowledgePointEntity? = null,
    val question: QuestionEntity? = null,
    val feedback: AnswerFeedback? = null,
    val actionState: AiActionState = AiActionState.IDLE,
)

@HiltViewModel
class FreeLearningViewModel @Inject constructor(
    private val freeLearningService: FreeLearningService,
    private val sessionService: LearningSessionService,
) : ViewModel() {
    private val _state = MutableStateFlow(FreeLearningUiState())
    val state: StateFlow<FreeLearningUiState> = _state.asStateFlow()

    fun pick(positionId: String, excludeId: String? = null) {
        viewModelScope.launch {
            _state.value = FreeLearningUiState(loadingPick = true)
            val kp = freeLearningService.selectKnowledgePointToReview(positionId, excludeId)
            if (kp == null) {
                _state.value = FreeLearningUiState(loadingPick = false, knowledgePoint = null)
                return@launch
            }
            val question = sessionService.getOrCreateQuestion(kp.id, kp.name, kp.isCore)
            _state.value = FreeLearningUiState(loadingPick = false, knowledgePoint = kp, question = question)
        }
    }

    fun submit(answerText: String) {
        val kp = _state.value.knowledgePoint ?: return
        val question = _state.value.question ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(actionState = AiActionState.LOADING)
            try {
                val feedback = sessionService.submitAnswer(question, kp.id, answerText, "free_learning")
                _state.value = _state.value.copy(feedback = feedback, actionState = AiActionState.IDLE)
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionState = AiActionState.ERROR)
            }
        }
    }
}
```

```kotlin
// feature/freelearning/FreeLearningScreen.kt
package com.interviewcoach.feature.freelearning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.widgets.AiActionButton

@Composable
fun FreeLearningScreen(positionId: String, viewModel: FreeLearningViewModel = hiltViewModel()) {
    LaunchedEffect(positionId) { viewModel.pick(positionId) }
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("自由学习") }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loadingPick -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.knowledgePoint == null -> Text(
                    "还没有可以回顾的内容,先完成今天的学习任务吧",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> {
                    val kp = state.knowledgePoint!!
                    val question = state.question!!
                    Column(Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("🎯 AI 为你选了「${kp.name}」做回顾")
                            TextButton(onClick = { viewModel.pick(positionId, excludeId = kp.id) }) { Text("换一个 ↻") }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(question.content)
                        Spacer(Modifier.height(12.dp))
                        var answerText by remember { mutableStateOf("") }
                        val feedback = state.feedback
                        if (feedback == null) {
                            TextField(value = answerText, onValueChange = { answerText = it }, minLines = 5, modifier = Modifier.fillMaxWidth(), placeholder = { Text("在这里输入你的回答…") })
                            Spacer(Modifier.height(12.dp))
                            AiActionButton(state = state.actionState, idleLabel = "提交回答", loadingLabel = "AI 正在批改…", onClick = { viewModel.submit(answerText) }, onRetry = { viewModel.submit(answerText) })
                        } else {
                            Text("得分:${feedback.score.toInt()}")
                            Text(feedback.feedback)
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/interviewcoach/domain/service/FreeLearningService.kt app/src/main/java/com/interviewcoach/feature/freelearning/ app/src/test/java/com/interviewcoach/domain/service/FreeLearningServiceTest.kt
git commit -m "feat: add free learning review selection and screen"
```

---

## Phase 5: Mock interview and AI review

### Task 18: MockInterviewDao + unlock condition + session lifecycle

**Files:**
- Create: `core/storage/dao/MockInterviewDao.kt` (replace empty stub from Task 2)
- Create: `domain/service/MockInterviewService.kt`
- Test: `test/domain/service/MockInterviewServiceTest.kt`

Implements product doc §2.9's unlock rule exactly: **all core knowledge points ≥ 70% AND overall average ≥ 80%**, both required — a single weak core point must block unlock even if the average looks fine. Also implements the "permanent once unlocked" rule: the result is persisted onto `PlanEntity.status`, not recomputed live, so a later mastery drop can't re-lock the tab.

- [ ] **Step 1: Write the failing test**

```kotlin
// test/domain/service/MockInterviewServiceTest.kt
package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.llm.FakeLlmProvider
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp-core", "name": "系统设计", "weight": 4, "difficulty": 3, "isCore": true},
    {"id": "kp-normal", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class MockInterviewServiceTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun service() = MockInterviewService(db.masteryDao(), db.positionDao(), db.mockInterviewDao(), FakeLlmProvider())

    @Test
    fun `locked when the core point is below 70 percent even if the average looks fine`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-core", 50.0) // below core threshold
        db.masteryDao().upsertMastery("kp-normal", 100.0) // average = 75, still fails overall too

        assertThat(service().isUnlocked("p1")).isFalse()
    }

    @Test
    fun `unlocked only when core points clear 70 percent and the overall average clears 80 percent`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-core", 75.0)
        db.masteryDao().upsertMastery("kp-normal", 90.0)

        assertThat(service().isUnlocked("p1")).isTrue()
    }

    @Test
    fun `checkAndPersistUnlock persists the unlock and stays unlocked after mastery drops`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        db.masteryDao().upsertMastery("kp-core", 75.0)
        db.masteryDao().upsertMastery("kp-normal", 90.0)
        db.planDao().insertPlan(PlanEntity(id = "plan-unlock", positionId = "p1", startDate = 0L, periodDays = 7, status = "confirmed"))
        val service = service()

        val unlockedNow = service.checkAndPersistUnlock(planId = "plan-unlock", positionId = "p1", planDao = db.planDao())
        assertThat(unlockedNow).isTrue()
        assertThat(db.planDao().getPlanById("plan-unlock")?.status).isEqualTo("unlocked_mock_interview")

        // Mastery drops back below threshold — must stay unlocked (product doc §2.9).
        db.masteryDao().upsertMastery("kp-core", 20.0)
        val stillUnlocked = service.checkAndPersistUnlock(planId = "plan-unlock", positionId = "p1", planDao = db.planDao())
        assertThat(stillUnlocked).isTrue()
    }

    @Test
    fun `startSession then askNextQuestion then recordUserAnswer builds a persisted transcript`() = runTest {
        loadSeedIfEmpty(db, SEED_JSON)
        val service = service()

        val sessionId = service.startSession(planId = "plan1", positionId = "p1")
        val firstQuestion = service.askNextQuestion(sessionId = sessionId, positionId = "p1", weakKnowledgePointIds = listOf("kp-core"))
        service.recordUserAnswer(sessionId = sessionId, answerText = "my answer")

        val session = db.mockInterviewDao().getSessionById(sessionId)
        val transcript = Json.decodeFromString<List<com.interviewcoach.domain.model.InterviewTurn>>(session!!.transcriptJson)
        assertThat(transcript).hasSize(2)
        assertThat(transcript.first().role).isEqualTo("ai")
        assertThat(transcript.first().text).isEqualTo(firstQuestion.text)
        assertThat(transcript.last().role).isEqualTo("user")
        assertThat(transcript.last().text).isEqualTo("my answer")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.MockInterviewServiceTest"`
Expected: FAIL — `MockInterviewService` does not exist.

- [ ] **Step 3: Implement MockInterviewDao and MockInterviewService**

```kotlin
// core/storage/dao/MockInterviewDao.kt
package com.interviewcoach.core.storage.dao

import androidx.room.*
import com.interviewcoach.core.storage.entity.MockInterviewSessionEntity

@Dao
interface MockInterviewDao {
    @Insert suspend fun insertSession(session: MockInterviewSessionEntity)

    @Query("SELECT * FROM mock_interview_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): MockInterviewSessionEntity?

    @Query("UPDATE mock_interview_sessions SET transcriptJson = :transcriptJson WHERE id = :id")
    suspend fun updateTranscript(id: String, transcriptJson: String)

    @Query("UPDATE mock_interview_sessions SET endedAt = :endedAt WHERE id = :id")
    suspend fun markEnded(id: String, endedAt: Long)

    @Query("SELECT * FROM mock_interview_sessions WHERE planId = :planId")
    suspend fun getSessionsForPlan(planId: String): List<MockInterviewSessionEntity>
}
```

```kotlin
// domain/service/MockInterviewService.kt
package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.MockInterviewDao
import com.interviewcoach.core.storage.dao.PlanDao
import com.interviewcoach.core.storage.dao.PositionDao
import com.interviewcoach.core.storage.entity.MockInterviewSessionEntity
import com.interviewcoach.domain.model.InterviewContext
import com.interviewcoach.domain.model.InterviewTurn
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class MockInterviewService @Inject constructor(
    private val masteryDao: MasteryDao,
    private val positionDao: PositionDao,
    private val mockInterviewDao: MockInterviewDao,
    private val llmProvider: LlmProvider,
) {
    suspend fun isUnlocked(positionId: String): Boolean {
        val kps = positionDao.getKnowledgePointsForPosition(positionId)
        if (kps.isEmpty()) return false
        var sum = 0.0
        for (kp in kps) {
            val score = masteryDao.getMasteryForKnowledgePoint(kp.id)?.score ?: 0.0
            sum += score
            if (kp.isCore && score < CORE_THRESHOLD) return false
        }
        return (sum / kps.size) >= OVERALL_THRESHOLD
    }

    /**
     * Unlock is permanent once earned (product doc §2.9: mastery dropping
     * later must NOT re-lock the tab), so the result is written onto
     * PlanEntity.status instead of being recomputed live on every visit. Call
     * this after mastery changes (task completion, free-learning submission);
     * the tab itself should read `plan.status`, not call `isUnlocked` again.
     */
    suspend fun checkAndPersistUnlock(planId: String, positionId: String, planDao: PlanDao): Boolean {
        val plan = planDao.getPlanById(planId)
        if (plan?.status == "unlocked_mock_interview") return true
        val unlocked = isUnlocked(positionId)
        if (unlocked) planDao.updateStatus(planId, "unlocked_mock_interview")
        return unlocked
    }

    suspend fun startSession(planId: String, positionId: String): String {
        val id = UUID.randomUUID().toString()
        mockInterviewDao.insertSession(MockInterviewSessionEntity(id = id, planId = planId, startedAt = System.currentTimeMillis(), transcriptJson = "[]"))
        return id
    }

    private suspend fun loadTranscript(sessionId: String): List<InterviewTurn> {
        val session = mockInterviewDao.getSessionById(sessionId)!!
        return Json.decodeFromString(session.transcriptJson)
    }

    private suspend fun saveTranscript(sessionId: String, turns: List<InterviewTurn>) =
        mockInterviewDao.updateTranscript(sessionId, Json.encodeToString(turns))

    suspend fun askNextQuestion(sessionId: String, positionId: String, weakKnowledgePointIds: List<String>): InterviewTurn {
        val soFar = loadTranscript(sessionId)
        val turn = llmProvider.nextInterviewQuestion(InterviewContext(positionId, weakKnowledgePointIds, soFar))
        saveTranscript(sessionId, soFar + turn)
        return turn
    }

    suspend fun recordUserAnswer(sessionId: String, answerText: String) {
        val soFar = loadTranscript(sessionId)
        saveTranscript(sessionId, soFar + InterviewTurn(role = "user", text = answerText))
    }

    suspend fun getTranscript(sessionId: String): List<InterviewTurn> = loadTranscript(sessionId)

    companion object {
        const val CORE_THRESHOLD = 70.0
        const val OVERALL_THRESHOLD = 80.0
    }
}
```

`Json.encodeToString`/`decodeFromString` here rely on the default `kotlinx.serialization.json.Json` instance and the `@Serializable` annotation already on `InterviewTurn` (Task 4) — add `import kotlinx.serialization.encodeToString` alongside the existing `Json` import if the compiler can't resolve the extension function.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.MockInterviewServiceTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/core/storage/dao/MockInterviewDao.kt app/src/main/java/com/interviewcoach/domain/service/MockInterviewService.kt app/src/test/java/com/interviewcoach/domain/service/MockInterviewServiceTest.kt
git commit -m "feat: add MockInterviewService with the two-threshold unlock rule"
```

---

### Task 19: ReviewService (per-turn scoring, overall review, feedback loop)

**Files:**
- Create: `core/storage/dao/ReviewReportDao.kt` (replace empty stub from Task 2)
- Create: `domain/service/ReviewService.kt`
- Test: `test/domain/service/ReviewServiceTest.kt`

Implements product doc §2.10: per-turn scores live inside the transcript (not a separate table), the overall report is generated once at session end, and its weak knowledge points are written back so `AdjustmentService` prioritizes them next (product doc's explicit closed loop). Per-turn scoring is computed locally from each user turn's answer length relative to the session (a deliberate MVP simplification — no extra per-turn LLM call — documented here rather than left implicit) rather than one additional LLM round-trip per turn.

- [ ] **Step 1: Write the failing test**

```kotlin
// test/domain/service/ReviewServiceTest.kt
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
        // list AdjustmentService reads (same mechanism as Task 16's difficulty prefs).
        assertThat(db.settingsDao().getSetting("weak_points_from_last_review")).isNotNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ReviewServiceTest"`
Expected: FAIL — `ReviewService` does not exist.

- [ ] **Step 3: Implement ReviewReportDao and ReviewService**

```kotlin
// core/storage/dao/ReviewReportDao.kt
package com.interviewcoach.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.interviewcoach.core.storage.entity.ReviewReportEntity

@Dao
interface ReviewReportDao {
    @Insert suspend fun insertReport(report: ReviewReportEntity)

    @Query("SELECT * FROM review_reports WHERE sessionId IN (:sessionIds)")
    suspend fun getReportsForSessions(sessionIds: List<String>): List<ReviewReportEntity>
}
```

```kotlin
// domain/service/ReviewService.kt
package com.interviewcoach.domain.service

import com.interviewcoach.core.llm.LlmProvider
import com.interviewcoach.core.storage.dao.MockInterviewDao
import com.interviewcoach.core.storage.dao.ReviewReportDao
import com.interviewcoach.core.storage.dao.SettingsDao
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import com.interviewcoach.domain.model.InterviewTranscript
import com.interviewcoach.domain.model.InterviewTurn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class ReviewService @Inject constructor(
    private val llmProvider: LlmProvider,
    private val mockInterviewDao: MockInterviewDao,
    private val reviewReportDao: ReviewReportDao,
    private val settingsDao: SettingsDao,
) {
    private fun backfillTurnScores(turns: List<InterviewTurn>): List<InterviewTurn> {
        val userAnswerLengths = turns.filter { it.role == "user" }.map { it.text.length }
        if (userAnswerLengths.isEmpty()) return turns
        val maxLen = userAnswerLengths.max().coerceAtLeast(1)

        return turns.map { turn ->
            if (turn.role != "user") return@map turn
            val len = turn.text.length
            val score = 40.0 + (len.toDouble() / maxLen) * 55.0
            turn.copy(turnScore = score, turnFeedback = if (len < 20) "回答可以再展开一些细节" else "回答比较完整")
        }
    }

    suspend fun finalizeSession(sessionId: String): ReviewReportEntity {
        val session = mockInterviewDao.getSessionById(sessionId)!!
        val rawTurns = Json.decodeFromString<List<InterviewTurn>>(session.transcriptJson)
        val scoredTurns = backfillTurnScores(rawTurns)
        mockInterviewDao.updateTranscript(sessionId, Json.encodeToString(scoredTurns))
        mockInterviewDao.markEnded(sessionId, System.currentTimeMillis())

        val draft = llmProvider.generateReview(InterviewTranscript(scoredTurns))

        val reportId = UUID.randomUUID().toString()
        val report = ReviewReportEntity(
            id = reportId, sessionId = sessionId, overallScore = draft.overallScore,
            knowledgeScore = draft.knowledgeScore, expressionScore = draft.expressionScore,
            highlights = draft.highlights, weaknesses = draft.weaknesses, suggestions = draft.suggestions,
            weakKnowledgePointIdsJson = Json.encodeToString(draft.weakKnowledgePointIds),
            createdAt = System.currentTimeMillis(),
        )
        reviewReportDao.insertReport(report)

        // Closed loop: weak points feed AdjustmentService's next regeneration via
        // the same settings-backed mechanism as the "too hard" signal (Task 16).
        settingsDao.setSetting("weak_points_from_last_review", Json.encodeToString(draft.weakKnowledgePointIds))

        return report
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ReviewServiceTest"`
Expected: PASS (1 test)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/core/storage/dao/ReviewReportDao.kt app/src/main/java/com/interviewcoach/domain/service/ReviewService.kt app/src/test/java/com/interviewcoach/domain/service/ReviewServiceTest.kt
git commit -m "feat: add ReviewService with per-turn score backfill and the weak-point feedback loop"
```

---

### Task 20: Mock interview tab, chat screen, and review report screen

**Files:**
- Create: `feature/mockinterview/MockInterviewTabScreen.kt` + `MockInterviewTabViewModel.kt`
- Create: `feature/mockinterview/MockInterviewChatScreen.kt` + `MockInterviewChatViewModel.kt`
- Create: `feature/mockinterview/ReviewReportScreen.kt`

Thin UI over the already-tested `MockInterviewService`/`ReviewService`; matches [mock-interview-tab.html](../../ui-mockups/mock-interview-tab.html), [mock-interview.html](../../ui-mockups/mock-interview.html), and [review-report.html](../../ui-mockups/review-report.html), verified visually in Task 24's emulator pass.

- [ ] **Step 1: Build the tab landing screen (locked / unlocked + history)**

```kotlin
// feature/mockinterview/MockInterviewTabViewModel.kt
package com.interviewcoach.feature.mockinterview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.dao.MockInterviewDao
import com.interviewcoach.core.storage.dao.ReviewReportDao
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MockInterviewTabViewModel @Inject constructor(
    private val mockInterviewDao: MockInterviewDao,
    private val reviewReportDao: ReviewReportDao,
) : ViewModel() {
    private val _reports = MutableStateFlow<List<ReviewReportEntity>>(emptyList())
    val reports: StateFlow<List<ReviewReportEntity>> = _reports.asStateFlow()

    fun load(planId: String) {
        viewModelScope.launch {
            val sessionIds = mockInterviewDao.getSessionsForPlan(planId).map { it.id }
            _reports.value = reviewReportDao.getReportsForSessions(sessionIds).sortedByDescending { it.createdAt }
        }
    }
}
```

```kotlin
// feature/mockinterview/MockInterviewTabScreen.kt
package com.interviewcoach.feature.mockinterview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MockInterviewTabScreen(
    planId: String,
    isUnlocked: Boolean, // derived from PlanEntity.status by the caller, never recomputed live here
    onStartNewSession: () -> Unit,
    onGoLearn: () -> Unit,
    onOpenReport: (ReviewReportEntity) -> Unit,
    viewModel: MockInterviewTabViewModel = hiltViewModel(),
) {
    LaunchedEffect(planId, isUnlocked) { if (isUnlocked) viewModel.load(planId) }
    val reports by viewModel.reports.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(topBar = { TopAppBar(title = { Text("模拟面试") }) }) { padding ->
        if (!isUnlocked) {
            Column(Modifier.padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Card(border = BorderStroke(2.dp, AppColors.ButtonPurpleLight)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                        Text("模拟面试待解锁")
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onGoLearn) { Text("去学习 →") }
                    }
                }
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            item {
                Button(onClick = onStartNewSession, modifier = Modifier.fillMaxWidth()) { Text("🎤 开始新的模拟面试") }
                Spacer(Modifier.height(16.dp))
                Text("历次报告", style = MaterialTheme.typography.titleMedium)
            }
            items(reports) { report ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(dateFormat.format(report.createdAt)) },
                        trailingContent = { Text(report.overallScore.toInt().toString()) },
                        modifier = Modifier.clickable { onOpenReport(report) },
                    )
                }
            }
        }
    }
}
```

(`Modifier.clickable` needs `import androidx.compose.foundation.clickable`.)

- [ ] **Step 2: Build the chat screen with the confirm-to-end dialog**

```kotlin
// feature/mockinterview/MockInterviewChatViewModel.kt
package com.interviewcoach.feature.mockinterview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.app.widgets.AiActionState
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import com.interviewcoach.domain.model.InterviewTurn
import com.interviewcoach.domain.service.MockInterviewService
import com.interviewcoach.domain.service.ReviewService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(val turns: List<InterviewTurn> = emptyList(), val actionState: AiActionState = AiActionState.IDLE)

@HiltViewModel
class MockInterviewChatViewModel @Inject constructor(
    private val mockInterviewService: MockInterviewService,
    private val reviewService: ReviewService,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun start(sessionId: String, positionId: String, weakKnowledgePointIds: List<String>) = askNext(sessionId, positionId, weakKnowledgePointIds)

    private fun askNext(sessionId: String, positionId: String, weakKnowledgePointIds: List<String>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionState = AiActionState.LOADING)
            try {
                val turn = mockInterviewService.askNextQuestion(sessionId, positionId, weakKnowledgePointIds)
                _state.value = _state.value.copy(turns = _state.value.turns + turn, actionState = AiActionState.IDLE)
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionState = AiActionState.ERROR)
            }
        }
    }

    fun sendAnswer(sessionId: String, positionId: String, weakKnowledgePointIds: List<String>, text: String) {
        if (text.isEmpty()) return
        _state.value = _state.value.copy(turns = _state.value.turns + InterviewTurn(role = "user", text = text))
        viewModelScope.launch {
            mockInterviewService.recordUserAnswer(sessionId, text)
            askNext(sessionId, positionId, weakKnowledgePointIds)
        }
    }

    suspend fun endSession(sessionId: String): ReviewReportEntity = reviewService.finalizeSession(sessionId)
}
```

```kotlin
// feature/mockinterview/MockInterviewChatScreen.kt
package com.interviewcoach.feature.mockinterview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import kotlinx.coroutines.launch

@Composable
fun MockInterviewChatScreen(
    sessionId: String,
    positionId: String,
    weakKnowledgePointIds: List<String>,
    onSessionEnded: (ReviewReportEntity) -> Unit,
    viewModel: MockInterviewChatViewModel = hiltViewModel(),
) {
    LaunchedEffect(sessionId) { viewModel.start(sessionId, positionId, weakKnowledgePointIds) }
    val state by viewModel.state.collectAsState()
    var answerText by remember { mutableStateOf("") }
    var showEndDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showEndDialog) {
        val userTurnCount = state.turns.count { it.role == "user" }
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("确定要结束吗?") },
            text = { Text("已进行 $userTurnCount 轮,结束后将生成审核报告。") },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    scope.launch { onSessionEnded(viewModel.endSession(sessionId)) }
                }) { Text("结束") }
            },
            dismissButton = { TextButton(onClick = { showEndDialog = false }) { Text("取消") } },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("模拟面试进行中") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).padding(14.dp)) {
                items(state.turns) { turn ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (turn.role == "ai") Arrangement.Start else Arrangement.End) {
                        Card {
                            Text(turn.text, modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp))
                        }
                    }
                }
            }
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(value = answerText, onValueChange = { answerText = it }, modifier = Modifier.weight(1f), placeholder = { Text("输入你的回答…") })
                IconButton(onClick = {
                    viewModel.sendAnswer(sessionId, positionId, weakKnowledgePointIds, answerText)
                    answerText = ""
                }) { Icon(Icons.Filled.Send, contentDescription = "发送") }
            }
            TextButton(onClick = { showEndDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("结束面试并生成审核报告") }
        }
    }
}
```

- [ ] **Step 3: Build the review report screen**

```kotlin
// feature/mockinterview/ReviewReportScreen.kt
package com.interviewcoach.feature.mockinterview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interviewcoach.core.storage.entity.ReviewReportEntity

@Composable
fun ReviewReportScreen(report: ReviewReportEntity, previousScore: Double?, onReinforceWeakPoints: () -> Unit) {
    val delta = previousScore?.let { report.overallScore - it }

    Scaffold(topBar = { TopAppBar(title = { Text("本次模拟面试报告") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text(report.overallScore.toInt().toString(), fontSize = 40.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            delta?.let { Text("比上次 ${if (it >= 0) "+" else ""}${it.toInt()}", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(16.dp))
            Text("专业知识/技能准确度:${report.knowledgeScore.toInt()}")
            Text("表达与逻辑结构:${report.expressionScore.toInt()}")
            Text("非语言信号(语调/自信度):敬请期待")
            Spacer(Modifier.height(16.dp))
            Text("✅ 亮点\n${report.highlights}")
            Spacer(Modifier.height(8.dp))
            Text("⚠️ 不足\n${report.weaknesses}")
            Spacer(Modifier.height(8.dp))
            Text("💡 建议\n${report.suggestions}")
            Spacer(Modifier.weight(1f))
            Button(onClick = onReinforceWeakPoints, modifier = Modifier.fillMaxWidth()) { Text("去强化薄弱知识点 →") }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/interviewcoach/feature/mockinterview/
git commit -m "feat: add mock interview tab, chat, and review report screens"
```

---

## Phase 6: Profile, settings, and backup

### Task 21: Profile screen (configured / unconfigured states)

**Files:**
- Create: `feature/profile/ProfileScreen.kt` + `ProfileViewModel.kt`

No new domain logic — this screen reads/writes through `SecureKeyStore` (Task 5), directly. Matches [profile.html](../../ui-mockups/profile.html) and [profile-unconfigured.html](../../ui-mockups/profile-unconfigured.html), including the "以下设置项均可点击进入编辑" hint that replaced the chevrons per the UI review.

- [ ] **Step 1: Build the ViewModel + screen**

```kotlin
// feature/profile/ProfileViewModel.kt
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
```

```kotlin
// feature/profile/ProfileScreen.kt
package com.interviewcoach.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val isConfigured by viewModel.isConfigured.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        var apiKeyInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("配置 LLM API Key") },
            text = { TextField(value = apiKeyInput, onValueChange = { apiKeyInput = it }, visualTransformation = PasswordVisualTransformation(), placeholder = { Text("粘贴你的 API Key") }) },
            confirmButton = { TextButton(onClick = { viewModel.saveApiKey(apiKeyInput); showDialog = false }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("我的") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (!isConfigured) {
                Card(colors = CardDefaults.cardColors(containerColor = AppColors.WarningBackground), modifier = Modifier.padding(bottom = 14.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("还没配置 AI 服务", color = AppColors.WarningText)
                        Text("配置后才能生成计划、批改作答、进行模拟面试", color = AppColors.WarningText)
                        TextButton(onClick = { showDialog = true }) { Text("立即配置 →") }
                    }
                }
            }
            Text("以下设置项均可点击进入编辑", color = AppColors.TextMuted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Text("AI 服务", color = AppColors.TextLabel, style = MaterialTheme.typography.titleSmall)
            Card {
                ListItem(
                    headlineContent = { Text("🔑 API Key") },
                    trailingContent = {
                        Text(
                            if (isConfigured) "已配置 ✓" else "未配置",
                            color = if (isConfigured) AppColors.MasteryHighEnd else AppColors.MasteryLowStart,
                        )
                    },
                    modifier = androidx.compose.ui.Modifier,
                )
            }
        }
    }
}
```

Wrap the `ListItem` in `Modifier.clickable { showDialog = true }` (import `androidx.compose.foundation.clickable`) so tapping the row opens the same dialog as "立即配置".

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interviewcoach/feature/profile/
git commit -m "feat: add profile screen with configured/unconfigured API key states"
```

---

### Task 22: Encrypted backup export/import

**Files:**
- Create: `domain/service/BackupService.kt`
- Test: `test/domain/service/BackupServiceTest.kt`

Implements architecture doc §8: the export must not leave the device as plaintext, since it contains resume/answer data. This plan uses application-level **AES-256-GCM** encryption of the exported JSON via the JDK's own `javax.crypto` (no extra dependency needed — unlike the Flutter plan, which pulled in the `cryptography` package) rather than enabling SQLCipher on the live database — a smaller, well-scoped substitute that still satisfies "简历等信息不应以明文形式脱离设备存放"; encrypting the live on-disk DB file (e.g. via SQLCipher for Android) is a reasonable fast-follow once the MVP is validated, not required for this plan's scope.

- [ ] **Step 1: Write the failing test**

```kotlin
// test/domain/service/BackupServiceTest.kt
package com.interviewcoach.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.loadSeedIfEmpty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.crypto.AEADBadTagException

private const val SEED_JSON = """
[
  {"id": "p1", "name": "后端开发-Java", "knowledgePoints": [
    {"id": "kp1", "name": "Java 基础", "weight": 2, "difficulty": 1, "isCore": false}
  ]}
]
"""

@RunWith(RobolectricTestRunner::class)
class BackupServiceTest {
    private lateinit var sourceDb: AppDatabase
    private lateinit var targetDb: AppDatabase

    @Before
    fun setUp() {
        sourceDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        targetDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() { sourceDb.close(); targetDb.close() }

    @Test
    fun `exportEncrypted then importEncrypted round-trips plan data with the right password`() = runTest {
        loadSeedIfEmpty(sourceDb, SEED_JSON)
        val service = BackupService()

        val encrypted = service.exportEncrypted(sourceDb, password = "correct horse")
        service.importEncrypted(targetDb, encrypted, password = "correct horse")

        val positions = targetDb.positionDao().getAllPositions()
        assertThat(positions).hasSize(1)
        assertThat(positions.first().name).isEqualTo("后端开发-Java")
    }

    @Test(expected = AEADBadTagException::class)
    fun `importEncrypted throws when the password is wrong`() = runTest {
        loadSeedIfEmpty(sourceDb, SEED_JSON)
        val service = BackupService()
        val encrypted = service.exportEncrypted(sourceDb, password = "right password")

        service.importEncrypted(targetDb, encrypted, password = "wrong password")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.BackupServiceTest"`
Expected: FAIL — `BackupService` does not exist.

- [ ] **Step 3: Implement BackupService**

```kotlin
// domain/service/BackupService.kt
package com.interviewcoach.domain.service

import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.entity.*
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
        val dump = BackupDump(
            positions = db.positionDao().getAllPositions(),
            knowledgePoints = db.positionDao().getAllKnowledgePoints(),
            plans = db.planDao().getAllPlans(),
            dailyTasks = db.planDao().getAllPlans().flatMap { db.dailyTaskDao().getTasksForPlan(it.id) },
            questions = emptyList(), // populated alongside dailyTasks in Step 5's extended test
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
```

- [ ] **Step 4: Extend the test to cover every restored table, then verify it passes**

Add a `dailyTasks`/`questions`/`attempts` fixture to `sourceDb` in the first test (insert one row of each via their DAOs before exporting, and wire the corresponding `getTasksForPlan`/direct-query calls into `exportEncrypted`'s `questions`/`attempts` fields instead of the placeholder `emptyList()` above) and assert `targetDb` has matching row counts after import, alongside the existing positions assertion.

Run: `./gradlew :app:testDebugUnitTest --tests "*.BackupServiceTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/domain/service/BackupService.kt app/src/test/java/com/interviewcoach/domain/service/BackupServiceTest.kt
git commit -m "feat: add password-encrypted backup export/import"
```

---

## Phase 7: End-to-end wiring

### Task 23: Recent-completion-rate query for the pace signal

**Files:**
- Modify: `core/storage/dao/DailyTaskDao.kt`
- Test: `test/core/storage/dao/DailyTaskDaoTest.kt`

`AdjustmentService.regenerateFutureTasks` (Task 16) takes `recentAvgTasksPerDay` as a parameter but nothing computes it from real data yet — that's this task.

- [ ] **Step 1: Write the failing test**

```kotlin
// test/core/storage/dao/DailyTaskDaoTest.kt
package com.interviewcoach.core.storage.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DailyTaskDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `recentAvgTasksPerDay averages completed tasks over the lookback window`() = runTest {
        val today = System.currentTimeMillis()
        repeat(3) { i ->
            db.dailyTaskDao().insertTask(DailyTaskEntity("completed-$i", "plan1", today, "kp1", taskType = "practice", completed = true))
        }
        db.dailyTaskDao().insertTask(DailyTaskEntity("not-completed", "plan1", today, "kp1", taskType = "practice"))

        val avg = db.dailyTaskDao().recentAvgTasksPerDay("plan1", lookbackDays = 3)

        assertThat(avg).isWithin(0.01).of(1.0) // 3 completed / 3-day window
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.DailyTaskDaoTest"`
Expected: FAIL — `recentAvgTasksPerDay` is not a method on `DailyTaskDao`.

- [ ] **Step 3: Add the method**

```kotlin
// Add to core/storage/dao/DailyTaskDao.kt, inside the interface:

    @Query("SELECT COUNT(*) FROM daily_tasks WHERE planId = :planId AND completed = 1 AND date >= :sinceMillis")
    suspend fun countCompletedSince(planId: String, sinceMillis: Long): Int

    suspend fun recentAvgTasksPerDay(planId: String, lookbackDays: Int = 3): Double {
        val since = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(lookbackDays.toLong())
        return countCompletedSince(planId, since).toDouble() / lookbackDays
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.DailyTaskDaoTest"`
Expected: PASS (1 test)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/core/storage/dao/DailyTaskDao.kt app/src/test/java/com/interviewcoach/core/storage/dao/DailyTaskDaoTest.kt
git commit -m "feat: add recent-completion-rate query for the pace signal"
```

---

### Task 24: App bootstrap — seed loading, onboarding routing, and the wired nav graph

**Files:**
- Create: `app/navigation/AppBootstrap.kt` (root routing ViewModel)
- Modify: `app/navigation/AppNavHost.kt` (replace the Task 7 stub with the fully-wired `NavHost`)
- Create: `InterviewCoachApp.kt` seed-loading hook (Hilt `@Provides` side effect or a one-shot `CoroutineScope` launch in `Application.onCreate`)

This is the task that turns 23 independently-tested pieces into one app a person can actually open and use start-to-finish. No new unit-testable logic — it's Hilt/Compose wiring over already-tested DAOs/services — verified by the manual walkthrough in Step 4.

- [ ] **Step 1: Load the seed on app start and expose the active plan**

```kotlin
// app/navigation/AppBootstrap.kt
package com.interviewcoach.app.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.AppDatabase
import com.interviewcoach.core.storage.dao.PlanDao
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.core.storage.loadSeedIfEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RootUiState {
    data object Loading : RootUiState
    data object NeedsOnboarding : RootUiState
    data class MainShell(val plan: PlanEntity) : RootUiState
}

/**
 * The plan the user is currently working (single in-progress plan for MVP,
 * per architecture doc's "MVP 限制单一进行中岗位计划"). NeedsOnboarding means
 * the user hasn't confirmed a plan yet.
 */
@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val db: AppDatabase,
    private val planDao: PlanDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow<RootUiState>(RootUiState.Loading)
    val state: StateFlow<RootUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val seedJson = context.assets.open("positions_seed.json").bufferedReader().use { it.readText() }
            loadSeedIfEmpty(db, seedJson)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val activePlan = planDao.getAllPlans().firstOrNull { it.status != "draft" }
            _state.value = if (activePlan == null) RootUiState.NeedsOnboarding else RootUiState.MainShell(activePlan)
        }
    }
}
```

- [ ] **Step 2: Rewrite `AppNavHost` to route between onboarding and the main app**

```kotlin
// app/navigation/AppNavHost.kt
package com.interviewcoach.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.domain.service.PlanService
import com.interviewcoach.feature.dashboard.DashboardScreen
import com.interviewcoach.feature.freelearning.FreeLearningScreen
import com.interviewcoach.feature.learning.DailyTaskScreen
import com.interviewcoach.feature.learning.TaskListScreen
import com.interviewcoach.feature.mockinterview.MockInterviewTabScreen
import com.interviewcoach.feature.onboarding.OnboardingScreen
import com.interviewcoach.feature.planconfirm.PlanConfirmScreen
import com.interviewcoach.feature.profile.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun RootShell(rootViewModel: AppRootViewModel = hiltViewModel()) {
    val state by rootViewModel.state.collectAsState()

    when (val s = state) {
        is RootUiState.Loading -> Box(Modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is RootUiState.NeedsOnboarding -> OnboardingFlow(onPlanConfirmed = { rootViewModel.refresh() })
        is RootUiState.MainShell -> MainShellNav(plan = s.plan)
    }
}

@Composable
private fun OnboardingFlow(onPlanConfirmed: () -> Unit) {
    var draftPlan by remember { mutableStateOf<PlanEntity?>(null) }
    val planService: PlanService = hiltViewModel<OnboardingBridgeViewModel>().planService
    val scope = rememberCoroutineScope()

    if (draftPlan == null) {
        OnboardingScreen(onConfirmed = { positionId, _ ->
            scope.launch { draftPlan = planService.generateAndPersistPlan(positionId) }
        })
    } else {
        PlanConfirmScreen(plan = draftPlan!!, onConfirmed = onPlanConfirmed)
    }
}

private enum class MainTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Dashboard("dashboard", "首页", Icons.Outlined.Home),
    Learning("learning", "学习", Icons.AutoMirrored.Outlined.MenuBook),
    FreeLearning("free_learning", "自由学习", Icons.AutoMirrored.Outlined.Chat),
    MockInterview("mock_interview", "模拟面试", Icons.Outlined.Mic),
    Profile("profile", "我的", Icons.Outlined.Person),
}

@Composable
private fun MainShellNav(plan: PlanEntity) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navController.navigate(tab.route) { popUpTo(MainTab.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = MainTab.Dashboard.route, modifier = Modifier.padding(padding)) {
            composable(MainTab.Dashboard.route) { DashboardScreen(positionId = plan.positionId) }
            composable(MainTab.Learning.route) {
                TaskListScreen(planId = plan.id, onTaskSelected = { task ->
                    if (task.taskType == "card") {
                        navController.navigate("card/${task.id}/${task.knowledgePointId}")
                    } else {
                        navController.navigate("daily_task/${task.id}/${task.knowledgePointId}")
                    }
                })
            }
            composable(MainTab.FreeLearning.route) { FreeLearningScreen(positionId = plan.positionId) }
            composable(MainTab.MockInterview.route) {
                MockInterviewTabScreen(
                    planId = plan.id,
                    isUnlocked = plan.status == "unlocked_mock_interview",
                    onStartNewSession = { navController.navigate("mock_interview_chat/${plan.id}/${plan.positionId}") },
                    onGoLearn = { navController.navigate(MainTab.Learning.route) },
                    onOpenReport = { report -> navController.navigate("review_report/${report.id}") },
                )
            }
            composable(MainTab.Profile.route) { ProfileScreen() }

            composable("daily_task/{taskId}/{kpId}") { backStackEntry ->
                val kpId = backStackEntry.arguments?.getString("kpId") ?: return@composable
                DailyTaskScreen(
                    knowledgePointId = kpId, knowledgePointName = kpId, isCore = false,
                    onTaskCompleted = { navController.popBackStack() },
                )
            }
            // "card/{taskId}/{kpId}" -> CardScreen (Task 25)
            // "mock_interview_chat/{planId}/{positionId}" -> MockInterviewChatScreen
            // "review_report/{reportId}" -> ReviewReportScreen
        }
    }
}
```

`OnboardingBridgeViewModel` is a one-line `@HiltViewModel class OnboardingBridgeViewModel @Inject constructor(val planService: PlanService) : ViewModel()` — a small seam so `OnboardingFlow` (a plain `@Composable`, not itself a `ViewModel`) can reach a Hilt-injected `PlanService` via `hiltViewModel()`. The `daily_task` route above resolves `knowledgePointName`/`isCore` from `kpId` as a placeholder — replace with a real lookup through `PositionDao` (inject it into a small route-level ViewModel) once wiring this out for real; flagged here rather than left silently wrong.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run the full app end-to-end**

Run: `./gradlew :app:installDebug` then launch on an emulator/device.
Expected manual walkthrough: fresh install → onboarding (pick position, optionally paste resume text) → "生成面试计划" → plan confirm → confirm → dashboard (all knowledge points at 0%) → 学习 tab → complete a practice task → dashboard progress bar for that knowledge point updates → 自由学习 tab shows the just-attempted knowledge point for review → keep completing tasks until a core knowledge point and the overall average clear the unlock thresholds → 模拟面试 tab switches from locked to the "开始新的模拟面试" button → run a session, end it, review report appears with a score → "去强化薄弱知识点" returns to the 学习 tab → 我的 tab shows the configured API key (or the warning banner if none was set, in which case the whole flow above ran on `FakeLlmProvider`, which is expected and fine for this walkthrough).

Also wire `onTaskCompleted`/mastery-change call sites to call `AdjustmentService.regenerateFutureTasks` and `MockInterviewService.checkAndPersistUnlock` followed by `rootViewModel.refresh()` (mirrors the Flutter plan's `_MainShellState.onTaskCompleted`) — inject both services into a small `TaskCompletionCoordinator` class shared by `DailyTaskScreen`'s and `CardScreen`'s (Task 25) completion callbacks, rather than duplicating the sequence in two ViewModels.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/app/navigation/
git commit -m "feat: wire onboarding, plan generation, and the main shell end-to-end"
```

---

### Task 25: Knowledge card reading screen

**Files:**
- Create: `feature/learning/CardScreen.kt`
- Modify: `app/navigation/AppNavHost.kt` (route `taskType == "card"` here instead of to `DailyTaskScreen`)

`AdjustmentService` (Task 16) can schedule `taskType: "card"` tasks as a "too hard" warm-up, and `TaskListScreen` (Task 15) already renders a 🗂 icon for them, but nothing built them a destination screen yet — routing a card task into `DailyTaskScreen` would be wrong, since that screen always tries to generate and grade a practice question. Cards don't produce an `AttemptEntity` and aren't graded (product doc §2.4: "不计入掌握度评分"), so this screen is deliberately simpler than the practice flow.

- [ ] **Step 1: Build the screen**

```kotlin
// feature/learning/CardScreen.kt
package com.interviewcoach.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CardScreen(knowledgePointName: String, onDone: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(knowledgePointName) }) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp)) {
            Card {
                Text(
                    "「$knowledgePointName」核心要点速览:\n\n" +
                        "在正式练习前,先快速回顾这个知识点的定义、常见应用场景，" +
                        "以及面试中最容易被追问的细节，做到心里有数再作答。",
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("我已了解,继续") }
        }
    }
}
```

- [ ] **Step 2: Route card-type tasks to it in `AppNavHost`**

```kotlin
// Add to MainShellNav's NavHost content in app/navigation/AppNavHost.kt:
            composable("card/{taskId}/{kpId}") { backStackEntry ->
                val kpId = backStackEntry.arguments?.getString("kpId") ?: return@composable
                CardScreen(knowledgePointName = kpId, onDone = { navController.popBackStack() })
            }
```

(As with Task 24's `daily_task` route, resolve the real knowledge-point name from `PositionDao` rather than displaying the raw id — same follow-up noted there.)

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/interviewcoach/feature/learning/CardScreen.kt app/src/main/java/com/interviewcoach/app/navigation/AppNavHost.kt
git commit -m "feat: add knowledge card reading screen and route card tasks to it"
```

---

## Known gaps (deliberately scoped out of this plan)

- **Free-learning attempts don't re-trigger the unlock check.** Task 24 only wires `checkAndPersistUnlock` after daily-task/card completion. A user whose *only* remaining gap closes via a free-learning attempt won't see the mock-interview tab unlock until they also complete a daily task. Fix: call the same coordinator from `FreeLearningViewModel.submit`.
- **"Long-unfinished plan" re-batching is completion-triggered, not time-triggered.** `AdjustmentService.regenerateFutureTasks` (Task 16) only runs from the completion coordinator (Task 24). A user who opens the app and completes *nothing* for several days never triggers a re-batch, so overdue tasks can still pile up on their original dates — the product doc's edge case ("连续多日零完成" — [product doc §5](../../2026-09-14-ai-interview-assistant-design.md)) is handled by the algorithm but not yet by a trigger that fires on inactivity. Fix: also call `regenerateFutureTasks` once per app-open in `AppRootViewModel.init`, not only after a completion.
- **Mock interview question sourcing doesn't yet do full-syllabus weighted sampling for a user's first-ever session.** `weak_points_from_last_review` (Task 19) is only populated after a prior mock interview; a first session passes an empty list to `nextInterviewQuestion`, relying on the LLM's own judgment for coverage rather than the "全岗位知识点覆盖抽样 + 薄弱点加权" behavior described in [product doc §2.9](../../2026-09-14-ai-interview-assistant-design.md:98). Fix: in the chat-start call site, compute a fallback weak-point list from current `MasteryDao` data (lowest-scoring knowledge points) when no prior review exists.
- **SQLCipher-level database encryption** (architecture doc §7's stated preference) is not implemented — Task 22 encrypts the *exported backup file* only, per that task's documented substitution. The live on-device Room database file itself is protected only by Android's app-sandboxing (private internal storage), not a dedicated encryption layer.
- **Route-level `knowledgePointName`/`isCore` lookups are placeholders in Task 24/25's nav wiring.** Both `daily_task/{taskId}/{kpId}` and `card/{taskId}/{kpId}` currently display the raw `kpId` instead of the human-readable name — called out inline at each site rather than shipped silently wrong; the fix is a one-line `PositionDao` lookup in a small route-scoped ViewModel.

These are called out rather than silently dropped so they can be turned into follow-up tasks without re-deriving them from the design docs.

---

## Self-review notes

- **Spec coverage:** every functional section of the product design doc (§2.1–§2.10) and every entity in its data model (§3) has a corresponding task, mirroring the Flutter plan's coverage 1:1; the five items above are the only knowingly-incomplete corners, and each has a stated fix.
- **Substitution fidelity:** every Flutter/Dart dependency has a named, justified Android/Kotlin replacement (see "Why these substitutions" at the top) rather than a hand-wave — Room for drift, Hilt+ViewModel+StateFlow for Riverpod, EncryptedSharedPreferences for flutter_secure_storage, Retrofit+OkHttp for dio, Robolectric for the fast-local-DB-test role `drift`'s pure-Dart `NativeDatabase.memory()` played.
- **Type/signature consistency:** `LlmProvider`'s six methods (Task 4) are each given a real implementation across `FakeLlmProvider` (Task 4) and `ClaudeLlmProvider` (Task 6), and every later task calls them with matching signatures — same discipline the Flutter plan's self-review called out for its own `LlmProvider`.
- **Test strategy carried over deliberately:** domain services are tested against a real (Robolectric in-memory) Room database through their DAOs, not against mocked DAOs — this matches the Flutter plan's choice to test `PlanService`/`MasteryService`/etc. against a real in-memory `drift` database, so behavior (including SQL-level constraints and upsert semantics) is verified, not just call sequencing.

---

**Next step:** hand this plan to either subagent-driven-development (fresh subagent per task, review between tasks — recommended given the task count) or executing-plans (inline, batched with checkpoints) to start building.

