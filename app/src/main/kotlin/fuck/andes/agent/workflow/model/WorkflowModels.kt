package fuck.andes.agent.workflow.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class WorkflowDefinition(
    val id: String,
    val name: String,
    val description: String = "",
    val trigger: String = "manual",
    val inputs: List<WorkflowInput> = emptyList(),
    val steps: List<WorkflowStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Immutable
@Serializable
data class WorkflowInput(
    val name: String,
    val label: String = name,
    val type: String = "text",
    val default: String = "",
)

@Immutable
@Serializable
data class WorkflowStep(
    val id: String,
    val type: StepType,
    val next: String? = null,
    val tool: String? = null,
    val params: Map<String, String> = emptyMap(),
    val rule: Map<String, String> = emptyMap(),
    val true_next: String? = null,
    val false_next: String? = null,
    val prompt: String? = null,
    val task: String? = null,
    val max_steps: Int = 10,
)

@Serializable
enum class StepType {
    tool,
    observe,
    condition_code,
    condition_ai,
    ai_orchestrate,
}

@Immutable
data class WorkflowRunState(
    val workflowId: String,
    val status: RunStatus = RunStatus.IDLE,
    val currentStepId: String? = null,
    val stepStates: Map<String, StepRunState> = emptyMap(),
    val logs: List<WorkflowLogEntry> = emptyList(),
    val variables: Map<String, String> = emptyMap(),
    val error: String? = null,
)

@Immutable
enum class RunStatus {
    IDLE, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
}

@Immutable
data class StepRunState(
    val status: StepStatus = StepStatus.PENDING,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val output: String? = null,
    val error: String? = null,
)

@Immutable
enum class StepStatus {
    PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
}

@Immutable
data class WorkflowLogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val stepId: String? = null,
    val message: String,
)

@Immutable
enum class LogLevel {
    INFO, WARN, ERROR, DEBUG
}
