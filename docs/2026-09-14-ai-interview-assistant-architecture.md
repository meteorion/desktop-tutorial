# 自适应引导式 AI 面试助手 —— 技术架构设计文档

- 状态:V1 草案
- 日期:2026-09-14
- 关联文档:[产品设计文档](2026-09-14-ai-interview-assistant-design.md)

## 1. 范围与前提

当前架构针对**个人使用、单设备**场景:用户自备 LLM API Key,在自己的手机上使用,不涉及多用户、多设备同步或对外服务。因此本架构**不引入后端服务**,采用纯客户端 App 方案。若未来需要支持多设备同步或多用户场景,需引入后端(见第 8 节)。

**目标平台**:iOS / Android(Flutter 单代码库双端出包)。

## 2. 总体架构

```
┌─────────────────────────────────────────┐
│              UI 层(Widgets)              │  目标设定/计划确认/学习/自由学习/看板/模拟面试/审核报告
├─────────────────────────────────────────┤
│         状态管理层(Riverpod)              │  UI 状态与领域服务之间的桥接
├─────────────────────────────────────────┤
│   领域服务层(纯 Dart,不依赖 UI/框架)       │
│  PlanService / MasteryService /          │
│  AdjustmentService / MockInterviewService│
│  / ReviewService / ResumeParseService     │
├───────────────┬─────────────────────────┤
│  本地存储层     │      LLM 抽象层          │
│ (drift+SQLite) │  LlmProvider 接口        │
│                │  ├─ ClaudeProvider       │
│                │  ├─ OpenAIProvider       │
│                │  └─ ...可扩展            │
└───────────────┴─────────────┬───────────┘
                                │ HTTPS 直连(设备直接出网)
                        LLM 提供商 API(用户自备 Key)
```

App 内没有自建服务端,所有"智能"能力(计划生成、出题、点评、模拟面试对话、审核报告)均通过 LLM 抽象层向外部 LLM 提供商发起请求;所有持久化数据留在设备本地。

## 3. 技术栈选型

| 层 | 选型 | 说明 |
|---|---|---|
| UI/框架 | Flutter | 单代码库覆盖 iOS/Android,个人开发者产出效率高 |
| 状态管理 | Riverpod | 与领域服务解耦,便于测试 |
| 本地数据库 | drift(基于 SQLite,启用 sqlcipher 加密) | 类型安全的查询构建、迁移管理成熟;加密见第 7 节 |
| 安全存储 | flutter_secure_storage | 存放 LLM API Key,底层用 Keychain/Keystore |
| 网络请求 | dio / http | 直接调用 LLM 提供商 REST API |
| 文档解析 | syncfusion_flutter_pdf / docx 等本地解析库 | 简历 PDF/Word 先在本地提取纯文本,再交给 LLM,不依赖各 Provider 不一致的文件理解能力 |

## 4. 分层设计与模块边界

- **UI 层**:仅依赖领域服务层暴露的方法和状态,不直接访问数据库或网络请求对象。
- **领域服务层**:每个服务对应产品文档中的一个功能模块,服务之间通过显式方法调用交互,不共享可变状态:
  - `PlanService`:计划生成(调用 `LlmProvider` + 本地岗位种子数据)、计划确认与保存。
  - `MasteryService`:掌握度重算(消费 Attempt,更新 Mastery),供每日任务和自由学习模式共用。
  - `AdjustmentService`:依据掌握度/进度/主观反馈三类信号重排 DailyTask。
  - `MockInterviewService`:模拟面试会话管理、解锁条件判定、动态出题。
  - `ReviewService`:面试结束后生成 ReviewReport,并将薄弱知识点回写给 `AdjustmentService`。
  - `ResumeParseService`:简历解析,输出岗位/薪资推荐与知识点差异比对结果。
- **本地存储层**:`drift` 的表结构与产品文档「3. 数据模型」一一对应,领域服务通过 DAO 读写,不直接写 SQL。
- **LLM 抽象层**:统一接口 `LlmProvider`,声明领域服务需要的能力(而非通用 chat 接口),例如:
  ```
  abstract class LlmProvider {
    Future<ResumeParseResult> parseResume(String resumeText);
    Future<PlanDraft> generatePlan(PlanGenerationInput input);
    Future<AnswerFeedback> gradeAnswer(Question q, String answer);
    Future<InterviewTurn> nextInterviewQuestion(InterviewContext ctx);
    Future<ReviewReportDraft> generateReview(InterviewTranscript transcript);
  }
  ```
  各 Provider(Claude/OpenAI/...)实现该接口,内部负责各自的 prompt 组装和响应解析;领域服务只面向接口编程,替换/新增提供商不影响上层。

这样划分的好处:UI、存储、LLM 三者互相独立、通过接口通信,替换任意一层(例如未来把本地存储换成远程同步,或新增一个 LLM 提供商)不需要改动其他层。

## 5. 本地数据模型

沿用产品设计文档「3. 数据模型」中的实体,补充落地到 SQLite 表的关键说明:

| 表 | 对应产品实体 | 落地说明 |
|---|---|---|
| positions / knowledge_points | Position / KnowledgePoint | 随 App 内置 JSON 种子数据导入,版本升级时按版本号增量覆盖 |
| plans | Plan | |
| daily_tasks | DailyTask | 动态调整时更新当天及未来日期的记录,不改写历史记录 |
| questions | Question | LLM 生成后落库持久化,避免重复调用产生的成本和不一致 |
| attempts | Attempt | 新增 `source` 字段(`daily_task` / `free_learning`),见产品文档 2.5/3 |
| mastery | Mastery | 按 `(user, knowledge_point)` 唯一,随 Attempt 增量重算 |
| mock_interview_sessions | MockInterviewSession | |
| review_reports | ReviewReport | |
| app_settings | (架构新增,产品文档未涉及) | 存用户选择的 LLM Provider、模型名等本地配置,单行记录 |

**数据库迁移策略**:使用 drift 的 schema 版本管理,种子数据(岗位库)与用户数据(计划/掌握度等)分开迁移,避免种子数据升级时误清空用户进度。

## 6. 关键流程的架构落地

**动态调整闭环**(对应产品文档 2.7)
```
UI 提交作答 → AdjustmentService 调用 LlmProvider.gradeAnswer()
   → 写入 attempts 表 → MasteryService 重算相关 mastery 记录
   → AdjustmentService 读取最新 mastery + daily_tasks 完成情况
   → 重新生成/覆盖未来 daily_tasks 记录
   → 若 mastery 汇总达标 → 更新 plans.status = unlocked_mock_interview
```

**模拟面试与审核闭环**(对应产品文档 2.9/2.10)
```
MockInterviewService 依据低掌握度知识点组装 InterviewContext
   → 循环调用 LlmProvider.nextInterviewQuestion() 驱动多轮对话
   → 会话结束 → ReviewService 调用 LlmProvider.generateReview()
   → 落库 review_reports → 薄弱知识点交给 AdjustmentService 生成新一轮 daily_tasks
```

两条闭环均在设备本地完整执行,唯一的外部依赖是 LLM 提供商的 API 调用。

**LLM 调用失败处理**:移动网络环境不稳定是常态,所有 `LlmProvider` 调用统一走一层轻量重试(失败自动重试 1 次),仍失败则向 UI 抛出可识别的错误态,由用户手动触发重试;已落库的部分结果(如已生成过的部分 daily_tasks)保留,不因单次调用失败回滚已有数据。

## 7. 安全与密钥管理

- LLM API Key 由用户在设置页手动输入,存入 `flutter_secure_storage`(iOS Keychain / Android Keystore),不写入代码、不随安装包分发、不出现在日志中。
- 网络请求仅面向用户配置的 LLM 提供商域名,不经过任何自建中转服务器。
- 简历文件(PDF/Word)统一先在本地用文档解析库提取纯文本,再把文本发给 `LlmProvider.parseResume()`,不依赖各 LLM 提供商不一致的文件上传/理解能力,原始文件本身不上传到任何第三方。
- 本地数据库(drift/SQLite)存有简历、作答内容等个人信息,启用 sqlcipher 做数据库级加密(而非仅依赖系统级全盘加密),加密密钥同样存于 `flutter_secure_storage`。

## 8. 数据备份与迁移(单设备局限的应对)

当前方案没有云同步,用户仅能在单一设备上使用。为降低"换设备/重装丢数据"的影响,提供:

- **导出**:将本地数据库文件(或导出为 JSON)生成单一备份文件,用户设置备份密码后加密写出(与数据库加密复用同一套本地加密能力),用户可自行保存(如存入自己的网盘)。
- **导入**:在新设备上输入密码解密并恢复计划、掌握度、面试记录等全部数据。

该功能不依赖任何服务端,纯本地文件读写即可实现;备份文件加密是因为其中包含简历等个人信息,不应以明文形式脱离设备存放。

## 9. 非目标与未来演进路径

**当前不实现**:
- 多设备自动同步、多用户账号体系。
- 服务端统一管理 API Key、控制调用成本。
- 云端题库/岗位库的集中更新分发(当前靠 App 版本更新携带)。

**未来若需引入后端**,得益于第 4 节的分层设计,改造路径是:新增一个实现相同数据访问接口的「远程存储层」(替换/补充本地 drift 实现),LLM 调用也可迁移为「客户端 → 自建网关 → LLM 提供商」以实现密钥托管和多用户成本控制;UI 层与领域服务层基本不需要改动。
