package fuck.andes.ui.screens.workflows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import fuck.andes.agent.workflow.model.RunStatus
import fuck.andes.agent.workflow.model.StepStatus
import fuck.andes.agent.workflow.model.WorkflowDefinition
import fuck.andes.agent.workflow.model.WorkflowRunState
import fuck.andes.ui.components.MiuixScaffoldPage
import fuck.andes.ui.components.PrefDivider
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

sealed interface WorkflowRunAction {
    data object NavigateBack : WorkflowRunAction
    data object Cancel : WorkflowRunAction
    data object Restart : WorkflowRunAction
}

@Composable
fun WorkflowRunScreen(
    workflow: WorkflowDefinition?,
    runState: WorkflowRunState?,
    onAction: (WorkflowRunAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFinished = runState?.status.let { it == RunStatus.COMPLETED || it == RunStatus.FAILED || it == RunStatus.CANCELLED }

    MiuixScaffoldPage(
        title = "工作流运行",
        onBack = { onAction(WorkflowRunAction.NavigateBack) },
        modifier = modifier,
    ) {
        item(key = "status-title") { SmallTitle("运行状态") }
        item(key = "status-card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusIcon(status = runState?.status)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workflow?.name ?: "工作流",
                            )
                            Text(
                                text = statusText(runState?.status),
                                color = MiuixTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (runState?.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "错误: ${runState.error}",
                            color = MiuixTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        if (runState != null && workflow != null) {
            item(key = "steps-title") { SmallTitle("步骤 (${workflow.steps.size})") }
            item(key = "steps-card") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    workflow.steps.forEachIndexed { index, step ->
                        val stepState = runState.stepStates[step.id]
                        StepItem(
                            stepName = step.type.name,
                            stepId = step.id,
                            status = stepState?.status ?: StepStatus.PENDING,
                        )
                        if (index < workflow.steps.lastIndex) PrefDivider()
                    }
                }
            }

            if (runState.logs.isNotEmpty()) {
                item(key = "logs-title") { SmallTitle("日志") }
                item(key = "logs-card") {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            runState.logs.takeLast(50).forEach { log ->
                                Text(
                                    text = log.message,
                                    color = when (log.level) {
                                        fuck.andes.agent.workflow.model.LogLevel.ERROR -> MiuixTheme.colorScheme.error
                                        fuck.andes.agent.workflow.model.LogLevel.WARN -> MiuixTheme.colorScheme.primary
                                        else -> MiuixTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "actions-card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onAction(WorkflowRunAction.Restart) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            enabled = isFinished,
                        ) {
                            Text("重新运行")
                        }
                        Button(
                            onClick = { onAction(WorkflowRunAction.Cancel) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(
                                color = MiuixTheme.colorScheme.error,
                                contentColor = MiuixTheme.colorScheme.onError,
                            ),
                            enabled = !isFinished,
                        ) {
                            Text("停止")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    stepName: String,
    stepId: String,
    status: StepStatus,
) {
    BasicComponent(
        title = stepName,
        summary = stepId,
        startAction = {
            StepStatusIcon(status = status)
        },
    )
}

@Composable
private fun StatusIcon(status: RunStatus?) {
    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            RunStatus.IDLE, RunStatus.PAUSED -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_clock),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariant,
            )
            RunStatus.RUNNING -> InfiniteProgressIndicator(size = 24.dp)
            RunStatus.COMPLETED -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_check),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
            RunStatus.FAILED, RunStatus.CANCELLED -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_x),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.error,
            )
            null -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_clock),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StepStatusIcon(status: StepStatus) {
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(36.dp)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            StepStatus.PENDING -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_clock),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariant,
            )
            StepStatus.RUNNING -> InfiniteProgressIndicator(size = 20.dp)
            StepStatus.COMPLETED -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_check),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
            StepStatus.FAILED -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_x),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.error,
            )
            StepStatus.SKIPPED -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun statusText(status: RunStatus?): String = when (status) {
    RunStatus.IDLE -> "空闲"
    RunStatus.RUNNING -> "运行中"
    RunStatus.PAUSED -> "已暂停"
    RunStatus.COMPLETED -> "已完成"
    RunStatus.FAILED -> "失败"
    RunStatus.CANCELLED -> "已取消"
    null -> "未知"
}
