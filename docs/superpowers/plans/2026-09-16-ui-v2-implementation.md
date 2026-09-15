# UI v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Roll the [v2 UI design](../../2026-09-16-ai-interview-assistant-ui-design-v2.md) (emerald "clean/professional" visual system + 3-tab information architecture) into the Kotlin/Compose app, replacing the v1 gradient/5-tab implementation.

**Architecture:** This is a visual + navigation-structure change, not new business logic — almost every task edits existing Composables and DI-injected ViewModels in place. Two small pieces of *real* logic get introduced along the way (mastery→color banding, and the "why is mock interview still locked" gap text); both are pure functions with red/green unit tests. Everything else is Compose UI with no test harness in this repo today (no androidTest source set, no Compose UI test rule, no snapshot testing) — those tasks are verified by `./gradlew :app:compileDebugKotlin` (compiles) plus running the app and comparing the screen to its v2 mockup in `docs/ui-mockups/*-v2.html`, not by a failing/passing test. Don't invent test infrastructure to force TDD onto pure layout/color changes; that would be scope creep this task doesn't need.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Hilt, Room, Robolectric + JUnit4 + Truth (existing JVM unit test stack).

**Known, deliberate gaps (do not try to "fix" these mid-task):**
- The design doc's dashboard streak badge ("🔥 连续 N 天") is **not implemented** — there is no streak/attempt-history service anywhere in this codebase, and building one is a real domain-logic feature, not a UI reskin. Dashboard ships without it.
- The Profile "模拟面试" row shows only a coarse "已解锁 · N 次记录" / "未解锁" status, not the detailed "还差 X%" gap text — that gap computation already lives on the Dashboard (Task 4) and duplicating it into `ProfileViewModel` just to repeat the same sentence in two places isn't worth the duplication.
- Onboarding's "简历导入" is a pasted-text field, not a real PDF/Word file picker — `ResumeParseService.parseResumeText(resumeText: String)` (existing code) only ever accepted resume text, and there is no file-picker/PDF-extraction code anywhere in the app to reuse. Building that is a separate feature.
- Onboarding's salary selection is **not persisted** — `PlanService.generateAndPersistPlan` has no salary parameter and there is no user/profile table to store it in. This matches the app's pre-existing behavior (the salary state already existed in `OnboardingViewModel` before this plan and was already discarded); this plan does not silently regress anything, it just makes the already-collected value visible in the UI.
- `TaskListScreen`'s rows stay plain `ListItem`s (Tasks 1 and 3 only fix their colors and remove the screen's own Scaffold) rather than the individually-bordered cards with an accent-highlighted "current" row shown in [task-list-v2.html](../../ui-mockups/task-list-v2.html). The existing icon-tint/strikethrough treatment already carries the done/current/todo distinction; wrapping each row in its own `Card` is a nice-to-have, not in any task below — add it as a follow-up if it's wanted.
- Profile shows only the existing "API Key" row, not a separate "LLM 提供商" row from [profile-unconfigured-v2.html](../../ui-mockups/profile-unconfigured-v2.html) — there is no provider-selection mechanism anywhere in this codebase (`LlmProviderRegistry` tracks whether a key is configured, not a chosen provider among several), and building one is a separate feature, not a visual reskin.

---

## Task 1: Emerald color system rollout

**Files:**
- Modify: `app/src/main/java/com/interviewcoach/app/theme/Color.kt`
- Modify: `app/src/main/java/com/interviewcoach/app/theme/Theme.kt`
- Create: `app/src/main/java/com/interviewcoach/app/widgets/ScreenHeader.kt`
- Create: `app/src/main/java/com/interviewcoach/app/widgets/PrimaryButton.kt`
- Delete: `app/src/main/java/com/interviewcoach/app/widgets/GradientHeader.kt`
- Delete: `app/src/main/java/com/interviewcoach/app/widgets/GradientPrimaryButton.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/learning/TaskListScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/mockinterview/MockInterviewTabScreen.kt`

This task is a mechanical, whole-codebase rename: every place that referenced the old purple/teal gradient tokens gets repointed at the new emerald tokens so the project compiles again. Later tasks build real v2 layouts on top of this; this task's only job is "nothing is broken and nothing says `PrimaryPurple` anymore."

- [ ] **Step 1: Replace the color token object**

Overwrite `app/src/main/java/com/interviewcoach/app/theme/Color.kt`:

```kotlin
package com.interviewcoach.app.theme

import androidx.compose.ui.graphics.Color

/** Color tokens from the UI design doc v2 (docs/2026-09-16-ai-interview-assistant-ui-design-v2.md) §5.1. */
object AppColors {
    val Accent = Color(0xFF0E9F6E)
    val AccentDisabled = Color(0xFFEEEEEE)
    val AccentSoftBackground = Color(0xFFE6F7F1)
    val MasteryLow = Color(0xFFE8735C)
    val MasteryMid = Color(0xFFD9A441)
    val MasteryHigh = Color(0xFF0E9F6E)
    val PageBackground = Color(0xFFF7F7F8)
    val CardBackground = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFECECEE)
    val TextPrimary = Color(0xFF1A1A1F)
    val TextSecondary = Color(0xFF8A8F98)
    val TextLabel = Color(0xFF8A8F98)
    val TextMuted = Color(0xFFB0B3BB)
    val WarningBackground = Color(0xFFFFF4E5)
    val WarningText = Color(0xFF8A5A00)
    val ErrorBackground = Color(0xFFFFF0F0)
    val ErrorText = Color(0xFFC0392B)
}
```

- [ ] **Step 2: Repoint the Material3 color scheme**

In `app/src/main/java/com/interviewcoach/app/theme/Theme.kt`, replace the `AppColorScheme` definition:

```kotlin
private val AppColorScheme = lightColorScheme(
    primary = AppColors.Accent,
    onPrimary = Color.White,
    primaryContainer = AppColors.AccentSoftBackground,
    onPrimaryContainer = AppColors.Accent,
    secondary = AppColors.Accent,
    onSecondary = Color.White,
    background = AppColors.PageBackground,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.CardBackground,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.CardBorder,
    outlineVariant = Color(0xFFEEEEEE),
    error = AppColors.ErrorText,
    onError = Color.White,
    errorContainer = AppColors.ErrorBackground,
    onErrorContainer = AppColors.ErrorText,
)
```

- [ ] **Step 3: Replace `GradientHeader` with a solid-accent `ScreenHeader`**

Delete the old file and create the new one:

```bash
git rm app/src/main/java/com/interviewcoach/app/widgets/GradientHeader.kt
```

Create `app/src/main/java/com/interviewcoach/app/widgets/ScreenHeader.kt`:

```kotlin
package com.interviewcoach.app.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interviewcoach.app.theme.AppColors

/**
 * Solid-accent hero header (UI design doc v2 §5.1), used atop key screens
 * instead of a flat TopAppBar. Replaces the retired gradient GradientHeader.
 */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Accent)
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        if (subtitle != null) {
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
```

- [ ] **Step 4: Replace `GradientPrimaryButton` with a solid-fill `PrimaryButton`**

```bash
git rm app/src/main/java/com/interviewcoach/app/widgets/GradientPrimaryButton.kt
```

Create `app/src/main/java/com/interviewcoach/app/widgets/PrimaryButton.kt`:

```kotlin
package com.interviewcoach.app.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interviewcoach.app.theme.AppColors

/** Primary action button per UI design doc v2 §5.2: rounded 8dp, solid accent fill, bold white text. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) AppColors.Accent else AppColors.AccentDisabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) Color.White else AppColors.TextMuted, fontWeight = FontWeight.Bold)
    }
}
```

- [ ] **Step 5: Fix `OnboardingScreen.kt`'s widget imports**

In `app/src/main/java/com/interviewcoach/feature/onboarding/OnboardingScreen.kt`:

```kotlin
// old
import com.interviewcoach.app.widgets.GradientHeader
import com.interviewcoach.app.widgets.GradientPrimaryButton
// new
import com.interviewcoach.app.widgets.PrimaryButton
import com.interviewcoach.app.widgets.ScreenHeader
```

```kotlin
// old
GradientHeader(title = "设定目标", subtitle = "告诉我们你的目标岗位,AI 来定制专属学习计划")
// new
ScreenHeader(title = "设定目标", subtitle = "告诉我们你的目标岗位,AI 来定制专属学习计划")
```

```kotlin
// old
GradientPrimaryButton(
// new
PrimaryButton(
```

(This screen gets fully rebuilt in Task 6 — this step only needs to keep it compiling.)

- [ ] **Step 6: Fix `ProfileScreen.kt`'s renamed color tokens**

```kotlin
// old
if (isConfigured) "已配置 ✓" else "未配置",
color = if (isConfigured) AppColors.MasteryHighEnd else AppColors.MasteryLowStart,
// new
if (isConfigured) "已配置 ✓" else "未配置",
color = if (isConfigured) AppColors.MasteryHigh else AppColors.MasteryLow,
```

(This screen gets fully rebuilt in Task 5 — this step only needs to keep it compiling.)

- [ ] **Step 7: Fix `TaskListScreen.kt`'s renamed color tokens**

```kotlin
// old
if (task.completed) Icon(Icons.Filled.CheckCircle, null, tint = AppColors.MasteryHighEnd)
else Icon(if (task.taskType == "practice") Icons.Outlined.MenuBook else Icons.Outlined.Style, null, tint = AppColors.PrimaryPurple)
// new
if (task.completed) Icon(Icons.Filled.CheckCircle, null, tint = AppColors.MasteryHigh)
else Icon(if (task.taskType == "practice") Icons.Outlined.MenuBook else Icons.Outlined.Style, null, tint = AppColors.Accent)
```

(This screen loses its own `Scaffold`/`TopAppBar` in Task 3 — this step only needs to keep it compiling.)

- [ ] **Step 8: Fix `MockInterviewTabScreen.kt`'s renamed color token**

```kotlin
// old
Card(border = BorderStroke(2.dp, AppColors.ButtonPurpleLight)) {
// new
Card(border = BorderStroke(1.dp, AppColors.CardBorder)) {
```

- [ ] **Step 9: Build**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`, no references to `PrimaryPurple`, `PrimaryTeal`, `ButtonPurpleLight`, `MasteryLowStart/End`, `MasteryMidEnd`, `MasteryHighStart/End`, `GradientHeader`, or `GradientPrimaryButton` remain (`grep -rn "PrimaryPurple\|PrimaryTeal\|ButtonPurpleLight\|MasteryLowStart\|MasteryLowEnd\|MasteryMidEnd\|MasteryHighStart\|MasteryHighEnd\|GradientHeader\|GradientPrimaryButton" app/src/main` prints nothing).

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: roll out emerald v2 color system, replace gradient widgets"
```

---

## Task 2: `masteryColor()` pure function (TDD)

**Files:**
- Create: `app/src/main/java/com/interviewcoach/app/theme/MasteryColor.kt`
- Test: `app/src/test/java/com/interviewcoach/app/theme/MasteryColorTest.kt`

The v2 design grades every knowledge-point/dimension score into a red/amber/green band (design doc §5.1: 低于阈值 / 中 / 已掌握). This function is the single source of truth for that banding so Dashboard (Task 4) and the review report (Task 8) can't disagree with each other about what counts as "low."

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/interviewcoach/app/theme/MasteryColorTest.kt`:

```kotlin
package com.interviewcoach.app.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MasteryColorTest {
    @Test
    fun `below 60 is low`() {
        assertThat(masteryColor(0.0)).isEqualTo(AppColors.MasteryLow)
        assertThat(masteryColor(59.9)).isEqualTo(AppColors.MasteryLow)
    }

    @Test
    fun `60 up to just under 80 is mid`() {
        assertThat(masteryColor(60.0)).isEqualTo(AppColors.MasteryMid)
        assertThat(masteryColor(79.9)).isEqualTo(AppColors.MasteryMid)
    }

    @Test
    fun `80 and above is high`() {
        assertThat(masteryColor(80.0)).isEqualTo(AppColors.MasteryHigh)
        assertThat(masteryColor(100.0)).isEqualTo(AppColors.MasteryHigh)
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.interviewcoach.app.theme.MasteryColorTest"
```

Expected: FAIL — `masteryColor` is unresolved.

- [ ] **Step 3: Implement**

Create `app/src/main/java/com/interviewcoach/app/theme/MasteryColor.kt`:

```kotlin
package com.interviewcoach.app.theme

import androidx.compose.ui.graphics.Color

/**
 * Maps a 0-100 mastery/interview-dimension score to the low/mid/high accent
 * from the UI design doc v2 §5.1.
 */
fun masteryColor(score: Double): Color = when {
    score < 60.0 -> AppColors.MasteryLow
    score < 80.0 -> AppColors.MasteryMid
    else -> AppColors.MasteryHigh
}
```

- [ ] **Step 4: Run it to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.interviewcoach.app.theme.MasteryColorTest"
```

Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interviewcoach/app/theme/MasteryColor.kt app/src/test/java/com/interviewcoach/app/theme/MasteryColorTest.kt
git commit -m "feat: add masteryColor() low/mid/high banding with tests"
```

---

## Task 3: Navigation restructure — 3 tabs, Learning segmented control, Mock Interview relocated

**Files:**
- Modify: `app/src/main/java/com/interviewcoach/app/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/learning/TaskListScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/freelearning/FreeLearningScreen.kt`
- Create: `app/src/main/java/com/interviewcoach/feature/learning/LearningTabScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/mockinterview/MockInterviewTabScreen.kt`

This is the information-architecture change from design doc §2: bottom nav goes from 5 tabs to 3 (首页/学习/我的), Free Learning becomes a segmented mode inside Learning instead of its own tab, and Mock Interview becomes a pushed route reachable from Dashboard/Profile instead of a tab. Dashboard and Profile's *own* call sites for the new "mock_interview" route get wired up in Tasks 4 and 5 — this task only needs to establish the route and keep the two tabs Dashboard/Profile compiling with their pre-existing signatures.

- [ ] **Step 1: Strip `TaskListScreen`'s own Scaffold/TopAppBar**

It will be hosted inside `LearningTabScreen`'s single Scaffold from now on, so its own app bar has to go. Replace the whole function body in `app/src/main/java/com/interviewcoach/feature/learning/TaskListScreen.kt`:

```kotlin
package com.interviewcoach.feature.learning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.core.storage.entity.DailyTaskEntity

@Composable
fun TaskListScreen(planId: String, onTaskSelected: (DailyTaskEntity) -> Unit, viewModel: TaskListViewModel = hiltViewModel()) {
    LaunchedEffect(planId) { viewModel.load(planId) }
    val tasks by viewModel.tasks.collectAsState()
    val done = tasks.count { it.completed }

    LinearProgressIndicator(
        progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = AppColors.Accent,
        trackColor = AppColors.CardBorder,
    )
    LazyColumn {
        items(tasks) { task ->
            ListItem(
                modifier = Modifier.clickable(enabled = !task.completed) { onTaskSelected(task) },
                leadingContent = {
                    if (task.completed) Icon(Icons.Filled.CheckCircle, null, tint = AppColors.MasteryHigh)
                    else Icon(if (task.taskType == "practice") Icons.Outlined.MenuBook else Icons.Outlined.Style, null, tint = AppColors.Accent)
                },
                headlineContent = {
                    Text(
                        task.knowledgePointId,
                        color = if (task.completed) AppColors.TextMuted else Color.Unspecified,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    )
                },
            )
        }
    }
}
```

- [ ] **Step 2: Strip `FreeLearningScreen`'s own Scaffold/TopAppBar**

Replace the whole function body in `app/src/main/java/com/interviewcoach/feature/freelearning/FreeLearningScreen.kt`:

```kotlin
package com.interviewcoach.feature.freelearning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    Box(Modifier.fillMaxSize()) {
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
```

(Yes, the "🎯" emoji and default text colors are untouched here — that cosmetic pass is Task 7. This step is structural only: no more `Scaffold`/`TopAppBar`.)

- [ ] **Step 3: Create `LearningTabScreen`**

Create `app/src/main/java/com/interviewcoach/feature/learning/LearningTabScreen.kt`:

```kotlin
package com.interviewcoach.feature.learning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.core.storage.entity.DailyTaskEntity
import com.interviewcoach.feature.freelearning.FreeLearningScreen

private enum class LearningMode(val label: String) { DailyTasks("每日任务"), FreeLearning("自由学习") }

/**
 * Hosts the two independent learning entry points (product doc §2.4 daily
 * tasks and §2.5 free learning) behind one segmented control, per UI design
 * doc v2 §2: they merge into a single bottom-nav tab, but stay separate
 * screens/ViewModels underneath — free learning must not touch daily-task
 * progress, and vice versa.
 */
@Composable
fun LearningTabScreen(planId: String, positionId: String, onTaskSelected: (DailyTaskEntity) -> Unit) {
    var mode by remember { mutableStateOf(LearningMode.DailyTasks) }

    Scaffold(containerColor = AppColors.PageBackground) { padding ->
        Column(Modifier.padding(padding)) {
            Text(
                "学习",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                LearningMode.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = mode == entry,
                        onClick = { mode = entry },
                        shape = SegmentedButtonDefaults.itemShape(index, LearningMode.entries.size),
                    ) { Text(entry.label) }
                }
            }
            when (mode) {
                LearningMode.DailyTasks -> TaskListScreen(planId = planId, onTaskSelected = onTaskSelected)
                LearningMode.FreeLearning -> FreeLearningScreen(positionId = positionId)
            }
        }
    }
}
```

- [ ] **Step 4: Add a back button to `MockInterviewTabScreen`**

It's reached by pushing a route now (from Dashboard/Profile) instead of being a bottom tab, so it needs a way back. In `app/src/main/java/com/interviewcoach/feature/mockinterview/MockInterviewTabScreen.kt`, add the parameter and wire the app bar:

```kotlin
// old signature
@Composable
fun MockInterviewTabScreen(
    planId: String,
    isUnlocked: Boolean, // derived from PlanEntity.status by the caller, never recomputed live here
    onStartNewSession: () -> Unit,
    onGoLearn: () -> Unit,
    onOpenReport: (ReviewReportEntity) -> Unit,
    viewModel: MockInterviewTabViewModel = hiltViewModel(),
) {
// new signature
@Composable
fun MockInterviewTabScreen(
    planId: String,
    isUnlocked: Boolean, // derived from PlanEntity.status by the caller, never recomputed live here
    onBack: () -> Unit,
    onStartNewSession: () -> Unit,
    onGoLearn: () -> Unit,
    onOpenReport: (ReviewReportEntity) -> Unit,
    viewModel: MockInterviewTabViewModel = hiltViewModel(),
) {
```

```kotlin
// old
Scaffold(topBar = { TopAppBar(title = { Text("模拟面试") }) }) { padding ->
// new
Scaffold(topBar = {
    TopAppBar(
        title = { Text("模拟面试") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
    )
}) { padding ->
```

Add the two new imports:

```kotlin
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
```

- [ ] **Step 5: Rewrite `AppNavHost.kt`'s tab set and routing**

Overwrite `app/src/main/java/com/interviewcoach/app/navigation/AppNavHost.kt` in full:

```kotlin
package com.interviewcoach.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.interviewcoach.core.storage.entity.KnowledgePointEntity
import com.interviewcoach.core.storage.entity.PlanEntity
import com.interviewcoach.core.storage.entity.ReviewReportEntity
import com.interviewcoach.domain.service.PlanService
import com.interviewcoach.feature.dashboard.DashboardScreen
import com.interviewcoach.feature.learning.CardScreen
import com.interviewcoach.feature.learning.DailyTaskScreen
import com.interviewcoach.feature.learning.LearningTabScreen
import com.interviewcoach.feature.mockinterview.MockInterviewChatScreen
import com.interviewcoach.feature.mockinterview.MockInterviewTabScreen
import com.interviewcoach.feature.mockinterview.ReviewReportScreen
import com.interviewcoach.feature.onboarding.OnboardingScreen
import com.interviewcoach.feature.planconfirm.PlanConfirmScreen
import com.interviewcoach.feature.profile.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun RootShell(rootViewModel: AppRootViewModel = hiltViewModel()) {
    val state by rootViewModel.state.collectAsState()

    when (val s = state) {
        is RootUiState.Loading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        is RootUiState.NeedsOnboarding -> OnboardingFlow(onPlanConfirmed = { rootViewModel.refresh() })
        is RootUiState.MainShell -> MainShellNav(plan = s.plan, onPlanChanged = { rootViewModel.refresh() })
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

private enum class MainTab(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "首页", Icons.Outlined.Home),
    Learning("learning", "学习", Icons.Outlined.MenuBook),
    Profile("profile", "我的", Icons.Outlined.Person),
}

/** Resolves a knowledge point by id before rendering its (task/card) destination. */
@Composable
private fun ResolvedKnowledgePoint(
    shellViewModel: MainShellViewModel,
    positionId: String,
    knowledgePointId: String,
    content: @Composable (KnowledgePointEntity) -> Unit,
) {
    var kp by remember(knowledgePointId) { mutableStateOf<KnowledgePointEntity?>(null) }
    LaunchedEffect(knowledgePointId) { kp = shellViewModel.getKnowledgePoint(positionId, knowledgePointId) }
    val currentKp = kp
    if (currentKp != null) {
        content(currentKp)
    } else {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
    }
}

@Composable
private fun MainShellNav(plan: PlanEntity, onPlanChanged: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val shellViewModel: MainShellViewModel = hiltViewModel()
    var selectedReport by remember { mutableStateOf<ReviewReportEntity?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(MainTab.Dashboard.route) { saveState = true }
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
        NavHost(navController, startDestination = MainTab.Dashboard.route, modifier = Modifier.padding(padding)) {
            composable(MainTab.Dashboard.route) {
                DashboardScreen(positionId = plan.positionId) // Task 4 expands this call
            }

            composable(MainTab.Learning.route) {
                LearningTabScreen(
                    planId = plan.id,
                    positionId = plan.positionId,
                    onTaskSelected = { task ->
                        val routeName = if (task.taskType == "card") "card" else "daily_task"
                        navController.navigate("$routeName/${task.id}/${task.knowledgePointId}")
                    },
                )
            }

            composable(MainTab.Profile.route) { ProfileScreen() } // Task 5 expands this call

            composable("mock_interview") {
                MockInterviewTabScreen(
                    planId = plan.id,
                    isUnlocked = plan.status == "unlocked_mock_interview",
                    onBack = { navController.popBackStack() },
                    onStartNewSession = {
                        scope.launch {
                            val sessionId = shellViewModel.startMockInterviewSession(plan.id, plan.positionId)
                            navController.navigate("mock_interview_chat/$sessionId")
                        }
                    },
                    onGoLearn = {
                        navController.navigate(MainTab.Learning.route) { popUpTo(MainTab.Dashboard.route) { saveState = true } }
                    },
                    onOpenReport = { report ->
                        selectedReport = report
                        navController.navigate("review_report")
                    },
                )
            }

            composable("daily_task/{taskId}/{kpId}") { entry ->
                val taskId = entry.arguments?.getString("taskId") ?: return@composable
                val kpId = entry.arguments?.getString("kpId") ?: return@composable
                ResolvedKnowledgePoint(shellViewModel, plan.positionId, kpId) { kp ->
                    DailyTaskScreen(
                        knowledgePointId = kp.id,
                        knowledgePointName = kp.name,
                        isCore = kp.isCore,
                        onTaskCompleted = {
                            scope.launch {
                                val wasUnlocked = plan.status == "unlocked_mock_interview"
                                val nowUnlocked = shellViewModel.completeTask(taskId, plan.id, plan.positionId)
                                if (!wasUnlocked && nowUnlocked) onPlanChanged()
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }

            composable("card/{taskId}/{kpId}") { entry ->
                val taskId = entry.arguments?.getString("taskId") ?: return@composable
                val kpId = entry.arguments?.getString("kpId") ?: return@composable
                ResolvedKnowledgePoint(shellViewModel, plan.positionId, kpId) { kp ->
                    CardScreen(
                        knowledgePointName = kp.name,
                        onDone = {
                            scope.launch {
                                val wasUnlocked = plan.status == "unlocked_mock_interview"
                                val nowUnlocked = shellViewModel.completeTask(taskId, plan.id, plan.positionId)
                                if (!wasUnlocked && nowUnlocked) onPlanChanged()
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }

            composable("mock_interview_chat/{sessionId}") { entry ->
                val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
                var weakIds by remember(sessionId) { mutableStateOf<List<String>?>(null) }
                LaunchedEffect(sessionId) { weakIds = shellViewModel.weakKnowledgePointIds() }
                weakIds?.let { ids ->
                    MockInterviewChatScreen(
                        sessionId = sessionId,
                        positionId = plan.positionId,
                        weakKnowledgePointIds = ids,
                        onSessionEnded = { report ->
                            selectedReport = report
                            navController.navigate("review_report") { popUpTo("mock_interview") }
                        },
                    )
                } ?: Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            }

            composable("review_report") {
                selectedReport?.let { report ->
                    ReviewReportScreen(
                        report = report,
                        previousScore = null,
                        onReinforceWeakPoints = {
                            navController.navigate(MainTab.Learning.route) { popUpTo(MainTab.Dashboard.route) { saveState = true } }
                        },
                    )
                }
            }
        }
    }
}
```

The only functional diffs from the previous file: `MainTab` has 3 entries instead of 5 (no `Icons.Outlined.Chat`/`Icons.Outlined.Mic` imports), the `Learning` tab renders `LearningTabScreen` instead of `TaskListScreen` directly, there's no more `FreeLearning`/`MockInterview` tab composable, a plain `"mock_interview"` route replaces `MainTab.MockInterview.route` (with `onBack` wired to `popBackStack()`), and both `popUpTo(MainTab.MockInterview.route)` call sites become `popUpTo("mock_interview")`.

- [ ] **Step 6: Build**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: restructure nav to 3 tabs, merge free learning into Learning, move mock interview under a pushed route"
```

---

## Task 4: Dashboard rebuild — unlock-gap text (TDD) + header/mastery list/CTA

**Files:**
- Create: `app/src/main/java/com/interviewcoach/feature/dashboard/UnlockGapText.kt`
- Test: `app/src/test/java/com/interviewcoach/feature/dashboard/UnlockGapTextTest.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/dashboard/DashboardViewModel.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/app/navigation/AppNavHost.kt:133` (the `MainTab.Dashboard.route` composable)

This is the first real content for [dashboard-v2.html](../../ui-mockups/dashboard-v2.html): target position, overall mastery, a mastery-colored knowledge-point list, an unlock-gap card, and a "continue today's tasks" button.

- [ ] **Step 1: Write the failing test for the gap-text logic**

Create `app/src/test/java/com/interviewcoach/feature/dashboard/UnlockGapTextTest.kt`:

```kotlin
package com.interviewcoach.feature.dashboard

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnlockGapTextTest {
    @Test
    fun `a weak core knowledge point takes priority over the overall average`() {
        val items = listOf(
            KnowledgePointMastery("kp1", "系统设计", isCore = true, score = 54.0),
            KnowledgePointMastery("kp2", "Java 基础", isCore = false, score = 95.0),
        )
        assertThat(unlockGapText(items)).isEqualTo("核心知识点「系统设计」还需提升 16%")
    }

    @Test
    fun `falls back to the overall average gap once every core point clears its threshold`() {
        val items = listOf(
            KnowledgePointMastery("kp1", "系统设计", isCore = true, score = 75.0),
            KnowledgePointMastery("kp2", "Java 基础", isCore = false, score = 65.0),
        )
        assertThat(unlockGapText(items)).isEqualTo("距解锁模拟面试还差 10%")
    }

    @Test
    fun `returns null once both thresholds are already met`() {
        val items = listOf(
            KnowledgePointMastery("kp1", "系统设计", isCore = true, score = 85.0),
            KnowledgePointMastery("kp2", "Java 基础", isCore = false, score = 90.0),
        )
        assertThat(unlockGapText(items)).isNull()
    }

    @Test
    fun `returns null for an empty knowledge point list`() {
        assertThat(unlockGapText(emptyList())).isNull()
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.interviewcoach.feature.dashboard.UnlockGapTextTest"
```

Expected: FAIL — `unlockGapText` is unresolved (and `KnowledgePointMastery`'s constructor doesn't have named `isCore`/`score` args yet in the right order — that's fine, Step 3 fixes it).

- [ ] **Step 3: Implement, reusing `MockInterviewService`'s existing thresholds**

Create `app/src/main/java/com/interviewcoach/feature/dashboard/UnlockGapText.kt`:

```kotlin
package com.interviewcoach.feature.dashboard

import com.interviewcoach.domain.service.MockInterviewService

/**
 * Mirrors MockInterviewService.isUnlocked's two-threshold rule (every core
 * knowledge point >= CORE_THRESHOLD AND the overall average >= OVERALL_THRESHOLD)
 * to explain *why* the mock interview is still locked, reusing the same
 * constants so the displayed gap can never disagree with the actual unlock
 * decision.
 */
fun unlockGapText(items: List<KnowledgePointMastery>): String? {
    if (items.isEmpty()) return null
    val weakestCore = items.filter { it.isCore && it.score < MockInterviewService.CORE_THRESHOLD }
        .minByOrNull { it.score }
    if (weakestCore != null) {
        val gap = (MockInterviewService.CORE_THRESHOLD - weakestCore.score).toInt()
        return "核心知识点「${weakestCore.name}」还需提升 $gap%"
    }
    val overall = items.sumOf { it.score } / items.size
    if (overall < MockInterviewService.OVERALL_THRESHOLD) {
        val gap = (MockInterviewService.OVERALL_THRESHOLD - overall).toInt()
        return "距解锁模拟面试还差 $gap%"
    }
    return null
}
```

- [ ] **Step 4: Run it to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.interviewcoach.feature.dashboard.UnlockGapTextTest"
```

Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Expand `DashboardViewModel`'s state**

Overwrite `app/src/main/java/com/interviewcoach/feature/dashboard/DashboardViewModel.kt`:

```kotlin
package com.interviewcoach.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.storage.dao.DailyTaskDao
import com.interviewcoach.core.storage.dao.MasteryDao
import com.interviewcoach.core.storage.dao.PositionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class KnowledgePointMastery(val id: String, val name: String, val isCore: Boolean, val score: Double)

data class DashboardUiState(
    val positionName: String = "",
    val items: List<KnowledgePointMastery> = emptyList(),
    val overallMastery: Int = 0,
    val completedTaskCount: Int = 0,
    val totalTaskCount: Int = 0,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val positionDao: PositionDao,
    private val masteryDao: MasteryDao,
    private val dailyTaskDao: DailyTaskDao,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    fun load(planId: String, positionId: String) {
        viewModelScope.launch {
            val position = positionDao.getAllPositions().firstOrNull { it.id == positionId }
            val items = positionDao.getKnowledgePointsForPosition(positionId).map { kp ->
                KnowledgePointMastery(kp.id, kp.name, kp.isCore, masteryDao.getMasteryForKnowledgePoint(kp.id)?.score ?: 0.0)
            }
            val overall = if (items.isEmpty()) 0 else (items.sumOf { it.score } / items.size).toInt()
            val startOfDay = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val todaysTasks = dailyTaskDao.getTasksForDate(planId, startOfDay)
            _state.value = DashboardUiState(
                positionName = position?.name ?: "",
                items = items,
                overallMastery = overall,
                completedTaskCount = todaysTasks.count { it.completed },
                totalTaskCount = todaysTasks.size,
            )
        }
    }
}
```

(`KnowledgePointMastery` keeps living here, in the same file/package as `unlockGapText`, so Step 3 needed no import for it.)

- [ ] **Step 6: Rebuild `DashboardScreen`**

Overwrite `app/src/main/java/com/interviewcoach/feature/dashboard/DashboardScreen.kt`:

```kotlin
package com.interviewcoach.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.app.theme.masteryColor
import com.interviewcoach.app.widgets.PrimaryButton

@Composable
fun DashboardScreen(
    planId: String,
    positionId: String,
    onContinueTasks: () -> Unit,
    onOpenMockInterview: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    LaunchedEffect(planId, positionId) { viewModel.load(planId, positionId) }
    val state by viewModel.state.collectAsState()
    val gapText = unlockGapText(state.items)

    Scaffold(containerColor = AppColors.PageBackground) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                Column(Modifier.fillMaxWidth().background(AppColors.CardBackground).padding(18.dp)) {
                    Text("目标岗位", color = AppColors.TextSecondary, fontSize = 12.sp)
                    Text(state.positionName, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("整体掌握度 ${state.overallMastery}%", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (gapText != null) {
                        Text(gapText, color = AppColors.TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            item {
                Text(
                    "知识点掌握度",
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 10.dp),
                )
            }
            items(state.items) { kp ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                    border = BorderStroke(1.dp, AppColors.CardBorder),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (kp.isCore) "${kp.name}  核心" else kp.name, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${kp.score.toInt()}%", color = masteryColor(kp.score), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (kp.score / 100).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = masteryColor(kp.score),
                            trackColor = AppColors.CardBorder,
                        )
                    }
                }
            }
            item {
                if (gapText != null) {
                    Card(
                        onClick = onOpenMockInterview,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                        border = BorderStroke(1.dp, AppColors.CardBorder),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("模拟面试待解锁", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(gapText, color = AppColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                PrimaryButton(
                    text = "继续今日任务(${state.completedTaskCount}/${state.totalTaskCount})",
                    onClick = onContinueTasks,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 7: Wire the new params into `AppNavHost`**

In `app/src/main/java/com/interviewcoach/app/navigation/AppNavHost.kt`, replace the Dashboard composable:

```kotlin
// old
composable(MainTab.Dashboard.route) {
    DashboardScreen(positionId = plan.positionId) // Task 4 expands this call
}
// new
composable(MainTab.Dashboard.route) {
    DashboardScreen(
        planId = plan.id,
        positionId = plan.positionId,
        onContinueTasks = {
            navController.navigate(MainTab.Learning.route) { popUpTo(MainTab.Dashboard.route) { saveState = true } }
        },
        onOpenMockInterview = { navController.navigate("mock_interview") },
    )
}
```

- [ ] **Step 8: Build**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Manual check**

Run the app (see Task 8's manual-check step for how this repo runs — same command), open the app, and compare the 首页 tab against [dashboard-v2.html](../../ui-mockups/dashboard-v2.html): target position + overall mastery text at top, a bordered card per knowledge point with a colored score/progress bar (red under 60, amber 60-79, green 80+), and — only while locked — an unlock-gap card above the "继续今日任务" button that navigates to Mock Interview when tapped.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: rebuild Dashboard with mastery-colored list, unlock-gap card, and continue-tasks CTA"
```

---

## Task 5: Profile screen — Mock Interview entry row

**Files:**
- Modify: `app/src/main/java/com/interviewcoach/feature/profile/ProfileViewModel.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/app/navigation/AppNavHost.kt:167` (the `MainTab.Profile.route` composable)

Adds the "模拟面试" row from [profile-v2.html](../../ui-mockups/profile-v2.html) that the design doc's §2 relocation depends on, and reskins the rest of the screen to the bordered-card v2 look.

- [ ] **Step 1: Give `ProfileViewModel` the report count**

Overwrite `app/src/main/java/com/interviewcoach/feature/profile/ProfileViewModel.kt`:

```kotlin
package com.interviewcoach.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interviewcoach.core.llm.LlmProviderRegistry
import com.interviewcoach.core.security.SecureKeyStore
import com.interviewcoach.core.storage.dao.MockInterviewDao
import com.interviewcoach.core.storage.dao.ReviewReportDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(val isConfigured: Boolean = false, val mockInterviewReportCount: Int = 0)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val llmProviderRegistry: LlmProviderRegistry,
    private val mockInterviewDao: MockInterviewDao,
    private val reviewReportDao: ReviewReportDao,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState(isConfigured = llmProviderRegistry.isConfigured()))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load(planId: String) {
        viewModelScope.launch {
            val sessionIds = mockInterviewDao.getSessionsForPlan(planId).map { it.id }
            val count = reviewReportDao.getReportsForSessions(sessionIds).size
            _state.value = _state.value.copy(mockInterviewReportCount = count)
        }
    }

    fun saveApiKey(key: String) {
        if (key.isEmpty()) return
        secureKeyStore.setApiKey(key)
        _state.value = _state.value.copy(isConfigured = llmProviderRegistry.isConfigured())
    }
}
```

- [ ] **Step 2: Rebuild `ProfileScreen`**

Overwrite `app/src/main/java/com/interviewcoach/feature/profile/ProfileScreen.kt`:

```kotlin
package com.interviewcoach.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors

@Composable
fun ProfileScreen(
    planId: String,
    isMockInterviewUnlocked: Boolean,
    onOpenMockInterview: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    LaunchedEffect(planId) { viewModel.load(planId) }
    val state by viewModel.state.collectAsState()
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

    Scaffold(containerColor = AppColors.PageBackground) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (!state.isConfigured) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppColors.WarningBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("还没配置 AI 服务", color = AppColors.WarningText, style = MaterialTheme.typography.titleSmall)
                        Text("配置后才能生成计划、批改作答、进行模拟面试", color = AppColors.WarningText)
                        TextButton(onClick = { showDialog = true }) { Text("立即配置 →", color = AppColors.Accent) }
                    }
                }
            }
            Text("以下设置项均可点击进入编辑", color = AppColors.TextMuted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Text("求职进展", color = AppColors.TextLabel, style = MaterialTheme.typography.titleSmall)
            Card(
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AppColors.CardBorder),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            ) {
                ListItem(
                    headlineContent = { Text("模拟面试") },
                    trailingContent = {
                        Text(
                            if (isMockInterviewUnlocked) "已解锁 · ${state.mockInterviewReportCount} 次记录 ›" else "未解锁 ›",
                            color = if (isMockInterviewUnlocked) AppColors.Accent else AppColors.TextMuted,
                        )
                    },
                    modifier = Modifier.clickable(onClick = onOpenMockInterview),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("AI 服务", color = AppColors.TextLabel, style = MaterialTheme.typography.titleSmall)
            Card(
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AppColors.CardBorder),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            ) {
                ListItem(
                    headlineContent = { Text("API Key") },
                    trailingContent = {
                        Text(
                            if (state.isConfigured) "已配置" else "未配置",
                            color = if (state.isConfigured) AppColors.MasteryHigh else AppColors.ErrorText,
                        )
                    },
                    modifier = Modifier.clickable { showDialog = true },
                )
            }
        }
    }
}
```

- [ ] **Step 3: Wire the new params into `AppNavHost`**

```kotlin
// old
composable(MainTab.Profile.route) { ProfileScreen() } // Task 5 expands this call
// new
composable(MainTab.Profile.route) {
    ProfileScreen(
        planId = plan.id,
        isMockInterviewUnlocked = plan.status == "unlocked_mock_interview",
        onOpenMockInterview = { navController.navigate("mock_interview") },
    )
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Mock Interview entry row to Profile, reskin Profile cards"
```

---

## Task 6: Onboarding screen — salary selector, resume paste, parse confirmation

**Files:**
- Modify: `app/src/main/java/com/interviewcoach/feature/onboarding/OnboardingViewModel.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/onboarding/OnboardingScreen.kt`

Fills in the two product-doc §2.1 fields ([onboarding-v2.html](../../ui-mockups/onboarding-v2.html)) that existed in `OnboardingViewModel` but were never exposed by the screen: salary range and resume text. See this plan's "Known, deliberate gaps" note at the top for why resume input is pasted text, not a file picker, and why salary isn't persisted.

- [ ] **Step 1: Split "type resume text" from "trigger parse", add salary selection**

Overwrite `app/src/main/java/com/interviewcoach/feature/onboarding/OnboardingViewModel.kt`:

```kotlin
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
    val salaryRange: String = OnboardingViewModel.SALARY_PRESETS.first(),
    val resumeText: String = "",
    val parsing: Boolean = false,
    val parseOutcome: ResumeParseOutcome? = null,
    val showConfirmation: Boolean = false,
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

    fun onSalaryRangeSelected(range: String) { _state.value = _state.value.copy(salaryRange = range) }

    fun onResumeTextChanged(resumeText: String) { _state.value = _state.value.copy(resumeText = resumeText) }

    /** Triggered by an explicit "解析简历" tap, not on every keystroke. */
    fun parseResume() {
        val resumeText = _state.value.resumeText
        if (resumeText.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(parsing = true)
            val outcome = ResumeParseService(llmProviderRegistry.current()).parseResumeText(resumeText)
            _state.value = _state.value.copy(
                parseOutcome = outcome,
                salaryRange = outcome.suggestedSalaryRange ?: _state.value.salaryRange,
                parsing = false,
                showConfirmation = true,
            )
        }
    }

    fun onAdjustAgain() { _state.value = _state.value.copy(showConfirmation = false) }

    companion object {
        val SALARY_PRESETS = listOf("10K-15K", "15K-25K", "25K-35K", "35K-50K", "50K 以上")
    }
}
```

- [ ] **Step 2: Rebuild `OnboardingScreen`**

Overwrite `app/src/main/java/com/interviewcoach/feature/onboarding/OnboardingScreen.kt`:

```kotlin
package com.interviewcoach.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.app.widgets.PrimaryButton
import com.interviewcoach.app.widgets.ScreenHeader

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

@Composable
private fun DropdownField(label: String, selectedText: String?, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        SectionLabel(label)
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
            border = BorderStroke(1.dp, AppColors.CardBorder),
        ) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                Row(
                    modifier = Modifier.fillMaxWidth().menuAnchor().clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedText ?: "请选择",
                        color = if (selectedText != null) AppColors.TextPrimary else AppColors.TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = AppColors.TextMuted)
                }
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (id, name) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(id); expanded = false })
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onConfirmed: (positionId: String, salaryRange: String) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(containerColor = AppColors.PageBackground) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (state.parseOutcome != null && state.showConfirmation) {
                val outcome = state.parseOutcome!!
                ScreenHeader(title = "确认解析结果", subtitle = "已根据你的简历生成以下信息,请确认")
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("目标薪资", color = AppColors.TextSecondary, fontSize = 12.sp)
                                if (outcome.needsUserConfirmation) {
                                    Text(
                                        "推测填写,请确认",
                                        color = AppColors.WarningText,
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .background(AppColors.WarningBackground, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(state.salaryRange, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    if (outcome.demonstratedSkills.isNotEmpty()) {
                        Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("已识别的能力项(${outcome.demonstratedSkills.size})", color = AppColors.TextSecondary, fontSize = 12.sp)
                                Text(outcome.demonstratedSkills.joinToString(" · "), color = AppColors.TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                    PrimaryButton(
                        text = "确认无误,生成计划",
                        enabled = state.selectedPositionId != null,
                        onClick = { state.selectedPositionId?.let { onConfirmed(it, state.salaryRange) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { viewModel.onAdjustAgain() }, modifier = Modifier.fillMaxWidth()) {
                        Text("重新调整", color = AppColors.TextSecondary)
                    }
                }
            } else {
                ScreenHeader(title = "设定目标", subtitle = "告诉我们你的目标岗位,AI 来定制专属学习计划")
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DropdownField(
                        label = "目标岗位",
                        selectedText = state.positions.firstOrNull { it.id == state.selectedPositionId }?.name,
                        options = state.positions.map { it.id to it.name },
                        onSelected = viewModel::onPositionSelected,
                    )
                    DropdownField(
                        label = "目标薪资",
                        selectedText = state.salaryRange,
                        options = OnboardingViewModel.SALARY_PRESETS.map { it to it },
                        onSelected = viewModel::onSalaryRangeSelected,
                    )
                    Column {
                        SectionLabel("导入简历(可选)")
                        TextField(
                            value = state.resumeText,
                            onValueChange = viewModel::onResumeTextChanged,
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("粘贴简历文本,AI 会据此预填薪资并识别你已掌握的知识点") },
                        )
                        TextButton(onClick = { viewModel.parseResume() }, enabled = state.resumeText.isNotBlank() && !state.parsing) {
                            Text(if (state.parsing) "AI 正在解析…" else "解析简历")
                        }
                        Text("不上传也可以,直接从零开始评估", color = AppColors.TextMuted, fontSize = 12.sp)
                    }
                    PrimaryButton(
                        text = "生成面试计划",
                        enabled = state.selectedPositionId != null,
                        onClick = { state.selectedPositionId?.let { onConfirmed(it, state.salaryRange) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Build**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual check**

Uninstall/reinstall the app (or clear its data) so `RootUiState.NeedsOnboarding` triggers, run it, and compare against [onboarding-v2.html](../../ui-mockups/onboarding-v2.html): position + salary dropdowns, a resume text box with a "解析简历" action, and — after parsing non-empty text — the confirmation panel with the "推测填写,请确认" badge and the skills list.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add salary selection and resume-paste parsing flow to Onboarding"
```

---

## Task 7: Visual polish A — shared AI-state widget + Learning-flow screens (DailyTask, Card, FreeLearning)

**Files:**
- Modify: `app/src/main/java/com/interviewcoach/app/widgets/AiActionButton.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/learning/DailyTaskScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/learning/CardScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/freelearning/FreeLearningScreen.kt`

Applies the v2 look (bordered cards, `PrimaryButton`, no emoji icons per design doc §5.3) to the three screens reachable from the Learning tab, plus the shared `AiActionButton` (design doc §5.3 [ai-request-states-v2.html](../../ui-mockups/ai-request-states-v2.html)) that both `DailyTaskScreen` and `FreeLearningScreen` use for submit/loading/error. Structural changes already landed in Task 3 — this task is purely cosmetic.

- [ ] **Step 1: Reskin the shared `AiActionButton`**

Overwrite `app/src/main/java/com/interviewcoach/app/widgets/AiActionButton.kt`:

```kotlin
package com.interviewcoach.app.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
                OutlinedButton(onClick = onRetry, colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.ErrorText)) {
                    Text("重试")
                }
            }
        }
        return
    }

    Button(
        onClick = onClick,
        enabled = state != AiActionState.LOADING,
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent, disabledContainerColor = AppColors.Accent.copy(alpha = 0.5f)),
        modifier = modifier,
    ) {
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

- [ ] **Step 2: Reskin `DailyTaskScreen`**

Overwrite `app/src/main/java/com/interviewcoach/feature/learning/DailyTaskScreen.kt`:

```kotlin
package com.interviewcoach.feature.learning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.app.theme.masteryColor
import com.interviewcoach.app.widgets.AiActionButton
import com.interviewcoach.app.widgets.PrimaryButton

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

    Scaffold(topBar = { TopAppBar(title = { Text(knowledgePointName) }) }, containerColor = AppColors.PageBackground) { padding ->
        val question = state.question
        if (question == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(Modifier.padding(padding).padding(16.dp)) {
            Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
                Text(question.content, modifier = Modifier.padding(16.dp))
            }
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
                Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
                    Row(Modifier.padding(16.dp)) {
                        Text("${feedback.score.toInt()}", color = masteryColor(feedback.score), fontWeight = FontWeight.Bold)
                        Text("  ${feedback.feedback}")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("参考答案:${question.referenceAnswer}", color = AppColors.TextSecondary)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { viewModel.flagQuestion() }) { Text("标记有误", color = AppColors.TextMuted) }
                Spacer(Modifier.height(8.dp))
                PrimaryButton(text = "下一题 →", onClick = onTaskCompleted, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
```

- [ ] **Step 3: Reskin `CardScreen`**

Overwrite `app/src/main/java/com/interviewcoach/feature/learning/CardScreen.kt`:

```kotlin
package com.interviewcoach.feature.learning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.app.widgets.PrimaryButton

@Composable
fun CardScreen(knowledgePointName: String, onDone: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(knowledgePointName) }) }, containerColor = AppColors.PageBackground) { padding ->
        Column(Modifier.padding(padding).padding(20.dp)) {
            Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
                Text(
                    "「$knowledgePointName」核心要点速览:\n\n" +
                        "在正式练习前,先快速回顾这个知识点的定义、常见应用场景，" +
                        "以及面试中最容易被追问的细节，做到心里有数再作答。",
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = "我已了解,继续", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}
```

- [ ] **Step 4: Reskin `FreeLearningScreen`'s content (drop the emoji, add accent color)**

```kotlin
// old
Text("🎯 AI 为你选了「${kp.name}」做回顾")
// new
Text("AI 为你选了「${kp.name}」做回顾", color = AppColors.Accent, fontWeight = FontWeight.Bold)
```

Add the two new imports:

```kotlin
import androidx.compose.ui.text.font.FontWeight
import com.interviewcoach.app.theme.AppColors
```

And replace the plain feedback text with a bordered card, matching `DailyTaskScreen`'s feedback treatment:

```kotlin
// old
} else {
    Text("得分:${feedback.score.toInt()}")
    Text(feedback.feedback)
}
// new
} else {
    Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
        Column(Modifier.padding(16.dp)) {
            Text("${feedback.score.toInt()}", color = masteryColor(feedback.score), fontWeight = FontWeight.Bold)
            Text(feedback.feedback)
        }
    }
}
```

Add the remaining new imports this needs:

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import com.interviewcoach.app.theme.masteryColor
```

- [ ] **Step 5: Build**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Manual check**

Run the app, go to 学习 → tap into a practice question, then a knowledge card, then switch to 自由学习 — compare against [daily-task-v2.html](../../ui-mockups/daily-task-v2.html) and [free-learning-v2.html](../../ui-mockups/free-learning-v2.html): bordered question/feedback cards, emerald `PrimaryButton`s, no 🎯/🚩 emoji, and the shared `AiActionButton`'s loading/error states against [ai-request-states-v2.html](../../ui-mockups/ai-request-states-v2.html).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "style: reskin AiActionButton, DailyTask/Card/FreeLearning screens to v2 bordered-card look"
```

---

## Task 8: Visual polish B — Mock-interview-flow screens (Chat, Tab list, Review report)

**Files:**
- Modify: `app/src/main/java/com/interviewcoach/feature/mockinterview/MockInterviewChatScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/mockinterview/MockInterviewTabScreen.kt`
- Modify: `app/src/main/java/com/interviewcoach/feature/mockinterview/ReviewReportScreen.kt`

Applies the v2 look to the interview flow: solid-accent chat bubbles, a plain-text "开始新的模拟面试" button (no 🎤), and — the one real upgrade here — mastery-colored dimension score bars on the review report, which didn't exist at all before (the old screen was plain `Text` lines).

- [ ] **Step 1: Reskin `MockInterviewChatScreen`'s bubbles**

```kotlin
// old
Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (turn.role == "ai") Arrangement.Start else Arrangement.End) {
    Card {
        Text(turn.text, modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp))
    }
}
// new
Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (turn.role == "ai") Arrangement.Start else Arrangement.End) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (turn.role == "ai") AppColors.CardBackground else AppColors.Accent,
        ),
        border = if (turn.role == "ai") BorderStroke(1.dp, AppColors.CardBorder) else null,
    ) {
        Text(
            turn.text,
            color = if (turn.role == "ai") AppColors.TextPrimary else Color.White,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
        )
    }
}
```

Add the new imports:

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import com.interviewcoach.app.theme.AppColors
```

And give the Scaffold the page background:

```kotlin
// old
Scaffold(topBar = { TopAppBar(title = { Text("模拟面试进行中") }) }) { padding ->
// new
Scaffold(topBar = { TopAppBar(title = { Text("模拟面试进行中") }) }, containerColor = AppColors.PageBackground) { padding ->
```

- [ ] **Step 2: Reskin `MockInterviewTabScreen`'s cards and drop the 🎤 emoji**

```kotlin
// old
Card(border = BorderStroke(1.dp, AppColors.CardBorder)) {
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Lock, contentDescription = null)
        Text("模拟面试待解锁")
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onGoLearn) { Text("去学习 →") }
    }
}
// new
Card(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppColors.TextMuted)
        Text("模拟面试待解锁", color = AppColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onGoLearn) { Text("去学习 →") }
    }
}
```

```kotlin
// old
Button(onClick = onStartNewSession, modifier = Modifier.fillMaxWidth()) { Text("🎤 开始新的模拟面试") }
// new
PrimaryButton(text = "开始新的模拟面试", onClick = onStartNewSession, modifier = Modifier.fillMaxWidth())
```

```kotlin
// old
Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    ListItem(
        headlineContent = { Text(dateFormat.format(report.createdAt)) },
        trailingContent = { Text(report.overallScore.toInt().toString()) },
        modifier = Modifier.clickable { onOpenReport(report) },
    )
}
// new
Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    ListItem(
        headlineContent = { Text(dateFormat.format(report.createdAt)) },
        trailingContent = { Text(report.overallScore.toInt().toString(), color = masteryColor(report.overallScore), fontWeight = FontWeight.Bold) },
        modifier = Modifier.clickable { onOpenReport(report) },
    )
}
```

Add the new imports:

```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.interviewcoach.app.theme.masteryColor
import com.interviewcoach.app.widgets.PrimaryButton
```

Remove the now-unused `androidx.compose.material3.Button` import (replaced by `PrimaryButton`), and give the Scaffold the page background:

```kotlin
// old
Scaffold(topBar = {
    TopAppBar(
        title = { Text("模拟面试") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
    )
}) { padding ->
// new
Scaffold(
    topBar = {
        TopAppBar(
            title = { Text("模拟面试") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
        )
    },
    containerColor = AppColors.PageBackground,
) { padding ->
```

- [ ] **Step 3: Rebuild `ReviewReportScreen` with mastery-colored dimension bars**

Overwrite `app/src/main/java/com/interviewcoach/feature/mockinterview/ReviewReportScreen.kt`:

```kotlin
package com.interviewcoach.feature.mockinterview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interviewcoach.app.theme.AppColors
import com.interviewcoach.app.theme.masteryColor
import com.interviewcoach.app.widgets.PrimaryButton
import com.interviewcoach.core.storage.entity.ReviewReportEntity

@Composable
private fun DimensionBar(label: String, score: Double?) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = if (score != null) AppColors.TextSecondary else AppColors.TextMuted, modifier = Modifier.weight(1f))
            Text(
                score?.toInt()?.toString() ?: "敬请期待",
                color = if (score != null) masteryColor(score) else AppColors.TextMuted,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ((score ?: 0.0) / 100).toFloat() },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = if (score != null) masteryColor(score) else AppColors.CardBorder,
            trackColor = AppColors.CardBorder,
        )
    }
}

@Composable
fun ReviewReportScreen(report: ReviewReportEntity, previousScore: Double?, onReinforceWeakPoints: () -> Unit) {
    val delta = previousScore?.let { report.overallScore - it }

    Scaffold(topBar = { TopAppBar(title = { Text("本次模拟面试报告") }) }, containerColor = AppColors.PageBackground) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text(report.overallScore.toInt().toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            delta?.let { Text("比上次 ${if (it >= 0) "+" else ""}${it.toInt()}", color = AppColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(16.dp))

            Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
                Column(Modifier.padding(14.dp)) {
                    DimensionBar("专业知识/技能准确度", report.knowledgeScore)
                    DimensionBar("表达与逻辑结构", report.expressionScore)
                    DimensionBar("非语言信号(语调/自信度)", null)
                }
            }
            Spacer(Modifier.height(12.dp))

            Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
                Column(Modifier.padding(14.dp)) {
                    Text("亮点", color = AppColors.MasteryHigh, fontWeight = FontWeight.Bold)
                    Text(report.highlights)
                }
            }
            Spacer(Modifier.height(8.dp))

            Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
                Column(Modifier.padding(14.dp)) {
                    Text("不足", color = AppColors.MasteryLow, fontWeight = FontWeight.Bold)
                    Text(report.weaknesses)
                }
            }
            Spacer(Modifier.height(8.dp))

            Card(shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, AppColors.CardBorder)) {
                Column(Modifier.padding(14.dp)) {
                    Text("针对性建议", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text(report.suggestions)
                }
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "去强化薄弱知识点 →", onClick = onReinforceWeakPoints, modifier = Modifier.fillMaxWidth())
        }
    }
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manual check**

Run the app, complete enough daily tasks to unlock mock interview (or seed data so `plan.status == "unlocked_mock_interview"`), start a session, answer a couple of turns, end it, and compare the chat bubbles / tab list / report screen against [mock-interview-v2.html](../../ui-mockups/mock-interview-v2.html), [mock-interview-list-v2.html](../../ui-mockups/mock-interview-list-v2.html), and [review-report-v2.html](../../ui-mockups/review-report-v2.html).

- [ ] **Step 6: Full test suite + commit**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, all existing tests plus `MasteryColorTest` and `UnlockGapTextTest` pass.

```bash
git add -A
git commit -m "style: reskin mock-interview chat/list/report screens, add dimension score bars"
```
