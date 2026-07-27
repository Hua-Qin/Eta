# 工作流系统设计规范

## 1. 概述

在现有 LLM 驱动的 Agent 系统之上，增加 JSON 工作流编排层和 UI 管理页面，支持预定义自动化任务和条件分支，同时保留 AI 自由编排能力。

## 2. 架构

```
┌─────────────────────────────────────────────┐
│        WorkflowScreen (UI)                  │
│  - 工作流列表页                              │
│  - 工作流编辑页（JSON 编辑器）              │
│  - 运行状态页（实时查看步骤执行）            │
└───────────────┬─────────────────────────────┘
                ↓
┌─────────────────────────────────────────────┐
│        WorkflowRepository                    │
│  - JSON 工作流 CRUD                          │
│  - 持久化到内部存储                          │
└───────────────┬─────────────────────────────┘
                ↓
┌─────────────────────────────────────────────┐
│        WorkflowRunner                        │
│  - 解析 JSON → 步骤状态机                    │
│  - 条件判断（code / ai）                     │
│  - 步骤执行器（tool / ai / observe）         │
└───────────────┬─────────────────────────────┘
                ↓
┌─────────────────────────────────────────────┐
│       现有系统（不改动）                     │
│  AgentLoop / AgentLocalTools / AgentModelClient│
└─────────────────────────────────────────────┘
```

## 3. JSON 工作流格式

```json
{
  "id": "send_wechat_msg",
  "name": "发送微信消息",
  "description": "自动打开微信并给指定联系人发消息",
  "trigger": "manual",
  "inputs": [
    { "name": "contact", "label": "联系人", "type": "text", "default": "" },
    { "name": "message", "label": "消息内容", "type": "text", "default": "" }
  ],
  "steps": [
    {
      "id": "open_wechat",
      "type": "tool",
      "tool": "launch_app",
      "params": { "package_name": "com.tencent.mm" },
      "next": "observe_home"
    },
    {
      "id": "observe_home",
      "type": "observe",
      "next": "check_home"
    },
    {
      "id": "check_home",
      "type": "condition_code",
      "rule": { "screen_contains": "微信" },
      "true_next": "ai_navigate_to_contact",
      "false_next": "ai_handle_login"
    },
    {
      "id": "ai_handle_login",
      "type": "ai_orchestrate",
      "task": "完成微信登录流程，进入微信主界面",
      "max_steps": 10,
      "next": "ai_navigate_to_contact"
    },
    {
      "id": "ai_navigate_to_contact",
      "type": "ai_orchestrate",
      "task": "找到并打开与「{contact}」的聊天窗口",
      "max_steps": 15,
      "next": "ai_send_message"
    },
    {
      "id": "ai_send_message",
      "type": "ai_orchestrate",
      "task": "在当前聊天窗口发送消息：「{message}」",
      "max_steps": 5
    }
  ]
}
```

### 3.1 步骤类型

| 类型 | 说明 | 必填字段 |
|------|------|---------|
| `tool` | 调用单个设备工具 | `tool`, `params` |
| `observe` | 观察屏幕 | - |
| `condition_code` | 代码判断（精确规则） | `rule`, `true_next`, `false_next` |
| `condition_ai` | AI 判断（自然语言） | `prompt`, `true_next`, `false_next` |
| `ai_orchestrate` | AI 自由编排子任务 | `task`, `max_steps` |

### 3.2 条件规则（condition_code）

```json
{
  "rule": { "screen_contains": "登录" }
}
```

支持的规则：
- `screen_contains: string` — 屏幕文本包含指定内容
- `screen_not_contains: string` — 屏幕文本不包含指定内容
- `package_equals: string` — 当前包名等于指定值
- `element_exists: { text?: string, id?: string }` — 存在匹配的 UI 元素
- `and: [rule1, rule2]` — 逻辑与
- `or: [rule1, rule2]` — 逻辑或
- `not: rule` — 逻辑非

### 3.3 变量替换

步骤中的字符串支持 `{variable_name}` 格式的变量替换，变量来源：
- `inputs` 中定义的输入参数
- 前序步骤的输出结果

## 4. 数据模型

### 4.1 WorkflowDefinition
```kotlin
data class WorkflowDefinition(
    val id: String,
    val name: String,
    val description: String,
    val trigger: String = "manual",
    val inputs: List<WorkflowInput> = emptyList(),
    val steps: List<WorkflowStep> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)
```

### 4.2 WorkflowStep
```kotlin
data class WorkflowStep(
    val id: String,
    val type: StepType,
    val next: String? = null,
    // tool 类型
    val tool: String? = null,
    val params: Map<String, Any?>? = null,
    // condition 类型
    val rule: Map<String, Any?>? = null,
    val trueNext: String? = null,
    val falseNext: String? = null,
    val prompt: String? = null,
    // ai_orchestrate 类型
    val task: String? = null,
    val maxSteps: Int = 10,
)

enum class StepType { TOOL, OBSERVE, CONDITION_CODE, CONDITION_AI, AI_ORCHESTRATE }
```

### 4.3 WorkflowRunState
```kotlin
data class WorkflowRunState(
    val workflowId: String,
    val status: RunStatus,
    val currentStepId: String?,
    val steps: Map<String, StepRunState>,
    val logs: List<WorkflowLogEntry>,
    val variables: Map<String, String>,
    val error: String? = null,
)

enum class RunStatus { IDLE, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }

data class StepRunState(
    val status: StepStatus,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val output: String? = null,
    val error: String? = null,
)

enum class StepStatus { PENDING, RUNNING, COMPLETED, FAILED, SKIPPED }
```

## 5. 组件设计

### 5.1 WorkflowRepository
- 职责：工作流定义的持久化和 CRUD 操作
- 存储位置：`Context.filesDir/workflows/` 目录，每个工作流一个 JSON 文件
- 方法：`list()`, `get(id)`, `save(definition)`, `delete(id)`, `importJson(json)`

### 5.2 WorkflowRunner
- 职责：解析工作流定义，按步骤执行，维护运行状态
- 输入：`workflowId`, `inputs: Map<String, String>`
- 输出：`StateFlow<WorkflowRunState>` 流式状态
- 关键方法：`start()`, `pause()`, `resume()`, `cancel()`

### 5.3 步骤执行器
- **ToolStepExecutor**：调用 `AgentLocalTools.execute()` 执行单个工具
- **ObserveStepExecutor**：调用 `deviceController.observe()` 观察屏幕
- **ConditionCodeExecutor**：基于屏幕快照执行代码条件判断
- **ConditionAiExecutor**：调用 LLM 判断条件
- **AiOrchestrateExecutor**：启动子 `AgentLoop` 执行子任务

### 5.4 UI 页面
- **WorkflowListScreen**：工作流列表，支持新建、编辑、删除、运行、导入
- **WorkflowEditorScreen**：JSON 编辑器，支持语法校验、格式化、保存
- **WorkflowRunScreen**：运行状态页，显示步骤进度、实时日志、暂停/停止

## 6. 导航集成

在 `AppRoute` 中新增：
- `Workflows` — 列表页
- `WorkflowEditor(val workflowId: String?)` — 编辑页（null 为新建）
- `WorkflowRun(val workflowId: String)` — 运行页

入口：设置页 → 工作流管理

## 7. 错误处理

- JSON 解析失败：在编辑页显示错误行号和信息
- 步骤执行失败：标记该步骤为 FAILED，整体状态 FAILED，记录错误日志
- AI 判断超时：超时视为 false 分支或可配置
- 运行中断：支持从断点恢复（基于持久化的运行状态）

## 8. 预置模板

随应用预置几个常用工作流模板：
1. `send_wechat_msg` — 发送微信消息
2. `take_screenshot_share` — 截图并分享
3. `open_settings_page` — 打开指定设置页
