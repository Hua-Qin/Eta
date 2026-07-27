package fuck.andes.agent.workflow.runner

import fuck.andes.agent.workflow.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

typealias ToolExecutor = suspend (toolName: String, params: JSONObject) -> String
typealias ScreenObserver = suspend () -> String
typealias ScreenTextProvider = suspend () -> String

class WorkflowRunner(
    private val definition: WorkflowDefinition,
    private val inputs: Map<String, String> = emptyMap(),
    private val toolExecutor: ToolExecutor,
    private val screenObserver: ScreenObserver,
    private val screenTextProvider: ScreenTextProvider,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    private val _state = MutableStateFlow(
        WorkflowRunState(
            workflowId = definition.id,
            stepStates = definition.steps.associate { it.id to StepRunState() },
            variables = inputs.toMutableMap().apply {
                definition.inputs.forEach { input ->
                    putIfAbsent(input.name, input.default)
                }
            }.toMap(),
        )
    )
    val state: StateFlow<WorkflowRunState> = _state.asStateFlow()

    fun start() {
        if (job != null) return
        job = scope.launch {
            runWorkflow()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = _state.value.copy(
            status = RunStatus.CANCELLED,
            currentStepId = null,
        )
    }

    private suspend fun runWorkflow() {
        _state.value = _state.value.copy(status = RunStatus.RUNNING)
        appendLog(LogLevel.INFO, null, "工作流启动: ${definition.name}")

        val firstStep = definition.steps.firstOrNull()?.id
            ?: run {
                _state.value = _state.value.copy(status = RunStatus.COMPLETED)
                appendLog(LogLevel.INFO, null, "工作流无步骤，直接完成")
                return
            }

        var currentStepId: String? = firstStep

        while (currentStepId != null) {
            ensureActive()

            val step = definition.steps.find { it.id == currentStepId }
            if (step == null) {
                appendLog(LogLevel.ERROR, currentStepId, "步骤不存在: $currentStepId")
                _state.value = _state.value.copy(
                    status = RunStatus.FAILED,
                    error = "Step not found: $currentStepId",
                    currentStepId = null,
                )
                return
            }

            _state.value = _state.value.copy(currentStepId = step.id)
            markStepRunning(step.id)
            appendLog(LogLevel.INFO, step.id, "执行步骤 [${step.type}] ${step.id}")

            val result = runCatching {
                executeStep(step)
            }

            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                markStepFailed(step.id, error)
                appendLog(LogLevel.ERROR, step.id, "步骤失败: $error")
                _state.value = _state.value.copy(
                    status = RunStatus.FAILED,
                    error = "Step ${step.id} failed: $error",
                    currentStepId = null,
                )
                return
            }

            val stepOutput = result.getOrNull() ?: ""
            markStepCompleted(step.id, stepOutput)

            val nextStepId = resolveNextStep(step, stepOutput)
            if (nextStepId == null) {
                appendLog(LogLevel.INFO, step.id, "工作流完成")
                _state.value = _state.value.copy(
                    status = RunStatus.COMPLETED,
                    currentStepId = null,
                )
                return
            }

            currentStepId = nextStepId
        }
    }

    private suspend fun executeStep(step: WorkflowStep): String {
        return when (step.type) {
            StepType.tool -> executeToolStep(step)
            StepType.observe -> executeObserveStep(step)
            StepType.condition_code -> executeConditionCodeStep(step)
            StepType.condition_ai -> {
                appendLog(LogLevel.WARN, step.id, "condition_ai 暂未实现，跳过")
                "skipped"
            }
            StepType.ai_orchestrate -> {
                appendLog(LogLevel.WARN, step.id, "ai_orchestrate 暂未实现，跳过")
                "skipped"
            }
        }
    }

    private suspend fun executeToolStep(step: WorkflowStep): String {
        val toolName = step.tool
            ?: throw IllegalArgumentException("Tool step missing 'tool' field")
        val resolvedParams = resolveVariables(step.params)
        val jsonParams = JSONObject(resolvedParams)
        appendLog(LogLevel.DEBUG, step.id, "调用工具: $toolName, params=$jsonParams")
        return toolExecutor(toolName, jsonParams)
    }

    private suspend fun executeObserveStep(step: WorkflowStep): String {
        appendLog(LogLevel.DEBUG, step.id, "观察屏幕...")
        return screenObserver()
    }

    private suspend fun executeConditionCodeStep(step: WorkflowStep): String {
        val rule = step.rule
        if (rule.isEmpty()) {
            throw IllegalArgumentException("Condition step missing 'rule' field")
        }
        val screenText = screenTextProvider()
        val result = evaluateRule(rule, screenText)
        appendLog(LogLevel.DEBUG, step.id, "条件判断结果: $result, rule=$rule")
        return if (result) "true" else "false"
    }

    private fun evaluateRule(rule: Map<String, String>, screenText: String): Boolean {
        rule["screen_contains"]?.let { text ->
            return text in screenText
        }
        rule["screen_not_contains"]?.let { text ->
            return text !in screenText
        }
        rule["package_equals"]?.let { pkg ->
            return pkg == currentPackageName()
        }
        rule["element_exists_text"]?.let { text ->
            return text in screenText
        }
        return false
    }

    private fun currentPackageName(): String {
        return _state.value.variables["current_package"] ?: ""
    }

    private fun resolveNextStep(step: WorkflowStep, stepOutput: String): String? {
        return when (step.type) {
            StepType.condition_code, StepType.condition_ai -> {
                if (stepOutput == "true") step.true_next else step.false_next
            }
            else -> step.next
        }
    }

    private fun resolveVariables(params: Map<String, String>): Map<String, String> {
        val vars = _state.value.variables
        return params.mapValues { (_, value) ->
            var result = value
            vars.forEach { (k, v) ->
                result = result.replace("{$k}", v)
            }
            result
        }
    }

    private fun markStepRunning(stepId: String) {
        _state.value = _state.value.copy(
            stepStates = _state.value.stepStates.toMutableMap().apply {
                this[stepId] = this[stepId]?.copy(
                    status = StepStatus.RUNNING,
                    startedAt = System.currentTimeMillis(),
                ) ?: StepRunState(status = StepStatus.RUNNING, startedAt = System.currentTimeMillis())
            }
        )
    }

    private fun markStepCompleted(stepId: String, output: String) {
        _state.value = _state.value.copy(
            stepStates = _state.value.stepStates.toMutableMap().apply {
                this[stepId] = this[stepId]?.copy(
                    status = StepStatus.COMPLETED,
                    finishedAt = System.currentTimeMillis(),
                    output = output.take(500),
                ) ?: StepRunState(status = StepStatus.COMPLETED, finishedAt = System.currentTimeMillis(), output = output.take(500))
            }
        )
    }

    private fun markStepFailed(stepId: String, error: String) {
        _state.value = _state.value.copy(
            stepStates = _state.value.stepStates.toMutableMap().apply {
                this[stepId] = this[stepId]?.copy(
                    status = StepStatus.FAILED,
                    finishedAt = System.currentTimeMillis(),
                    error = error,
                ) ?: StepRunState(status = StepStatus.FAILED, finishedAt = System.currentTimeMillis(), error = error)
            }
        )
    }

    private fun appendLog(level: LogLevel, stepId: String?, message: String) {
        _state.value = _state.value.copy(
            logs = _state.value.logs + WorkflowLogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                stepId = stepId,
                message = message,
            ),
        )
    }

    private fun ensureActive() {
        if (!scope.isActive) throw CancellationException()
    }

    fun destroy() {
        job?.cancel()
        job = null
        scope.cancel()
    }
}
