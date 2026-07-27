package fuck.andes.ui.screens.workflows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import fuck.andes.agent.workflow.model.RunStatus
import fuck.andes.agent.workflow.model.StepStatus
import fuck.andes.agent.workflow.model.WorkflowDefinition
import fuck.andes.agent.workflow.model.WorkflowLogEntry
import fuck.andes.agent.workflow.model.WorkflowRunState
import fuck.andes.ui.components.MiuixScaffoldPage
import fuck.andes.ui.components.SectionHeader
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
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
    val steps = workflow?.steps ?: emptyList()

    MiuixScaffoldPage(
        title = "运行中",
        onBack = { onAction(WorkflowRunAction.NavigateBack) },
        modifier = modifier,
        actions = {
            if (runState?.status == RunStatus.RUNNING) {
                IconButton(onClick = { onAction(WorkflowRunAction.Cancel) }) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_square),
                        contentDescription = "停止",
                        tint = MiuixTheme.colorScheme.error,
                    )
                }
            }
            if (runState?.status == RunStatus.COMPLETED || runState?.status == RunStatus.FAILED || runState?.status == RunStatus.CANCELLED) {
                IconButton(onClick = { onAction(WorkflowRunAction.Restart) }) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_rotate_cw),
                        contentDescription = "重新运行",
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        },
    ) {
        item(key = "status_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val statusColor = when (runState?.status) {
                        RunStatus.RUNNING -> MiuixTheme.colorScheme.primary
                        RunStatus.COMPLETED -> MiuixTheme.colorScheme.primary
                        RunStatus.FAILED -> MiuixTheme.colorScheme.error
                        RunStatus.CANCELLED -> MiuixTheme.colorScheme.onSurfaceVariant
                        else -> MiuixTheme.colorScheme.onSurfaceVariant
                    }
                    val statusText = when (runState?.status) {
                        RunStatus.RUNNING -> "运行中"
                        RunStatus.COMPLETED -> "已完成"
                        RunStatus.FAILED -> "失败"
                        RunStatus.CANCELLED -> "已取消"
                        RunStatus.PAUSED -> "已暂停"
                        else -> "等待中"
                    }
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_activity),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                statusColor.copy(alpha = 0.15f),
                                CircleShape,
                            )
                            .padding(6.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(workflow?.name ?: "工作流", style = MiuixTheme.typography.title3)
                        Text(
                            statusText,
                            color = statusColor,
                            style = MiuixTheme.typography.callout,
                        )
                    }
                    val completed = runState?.stepStates?.values?.count {
                        it.status == StepStatus.COMPLETED || it.status == StepStatus.FAILED
                    } ?: 0
                    Text(
                        "$completed / ${steps.size}",
                        color = MiuixTheme.colorScheme.onSurfaceVariant,
                        style = MiuixTheme.typography.title3,
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
        }

        item(key = "section_steps") {
            SectionHeader("步骤")
        }

        item(key = "steps_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                steps.forEachIndexed { index, step ->
                    val stepState = runState?.stepStates?.get(step.id)
                    val isCurrent = runState?.currentStepId == step.id
                    StepRow(
                        index = index + 1,
                        stepId = step.id,
                        stepType = step.type.name,
                        status = stepState?.status ?: StepStatus.PENDING,
                        isCurrent = isCurrent,
                        error = stepState?.error,
                    )
                    if (index < steps.size - 1) {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
        }

        item(key = "section_logs") {
            SectionHeader("日志")
        }

        item(key = "logs_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                val logs = runState?.logs ?: emptyList()
                if (logs.isEmpty()) {
                    Text(
                        "暂无日志",
                        modifier = Modifier.padding(24.dp),
                        color = MiuixTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(modifier = Modifier.padding(12.dp)) {
                        logs.takeLast(50).forEach { log ->
                            LogRow(log = log)
                        }
                    }
                }
            }
        }

        if (runState?.error != null) {
            item(key = "error_card") {
                Spacer(modifier = Modifier.size(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_alert_circle),
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("错误", style = MiuixTheme.typography.title3)
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            runState.error ?: "",
                            color = MiuixTheme.colorScheme.error,
                            style = MiuixTheme.typography.callout,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    index: Int,
    stepId: String,
    stepType: String,
    status: StepStatus,
    isCurrent: Boolean,
    error: String?,
) {
    val statusColor = when (status) {
        StepStatus.COMPLETED -> MiuixTheme.colorScheme.primary
        StepStatus.RUNNING -> MiuixTheme.colorScheme.primary
        StepStatus.FAILED -> MiuixTheme.colorScheme.error
        StepStatus.SKIPPED -> MiuixTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        StepStatus.PENDING -> MiuixTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = String.format("%02d", index),
            color = statusColor,
            style = MiuixTheme.typography.title3,
            modifier = Modifier.width(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(stepId, style = MiuixTheme.typography.callout)
            Text(
                stepType,
                color = MiuixTheme.colorScheme.onSurfaceVariant,
                style = MiuixTheme.typography.caption1,
            )
            if (error != null) {
                Text(
                    error,
                    color = MiuixTheme.colorScheme.error,
                    style = MiuixTheme.typography.caption1,
                )
            }
        }
        when (status) {
            StepStatus.COMPLETED -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_check_circle),
                contentDescription = "已完成",
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            StepStatus.RUNNING -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_loader),
                contentDescription = "运行中",
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            StepStatus.FAILED -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_x_circle),
                contentDescription = "失败",
                tint = MiuixTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            StepStatus.SKIPPED -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_skip_forward),
                contentDescription = "已跳过",
                tint = MiuixTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
            StepStatus.PENDING -> Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_circle),
                contentDescription = "等待中",
                tint = MiuixTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LogRow(log: WorkflowLogEntry) {
    val color = when (log.level) {
        fuck.andes.agent.workflow.model.LogLevel.ERROR -> MiuixTheme.colorScheme.error
        fuck.andes.agent.workflow.model.LogLevel.WARN -> MiuixTheme.colorScheme.primary
        else -> MiuixTheme.colorScheme.onSurfaceVariant
    }
    val time = remember(log.timestamp) {
        val ms = log.timestamp % 1000
        val totalSecs = log.timestamp / 1000
        val secs = totalSecs % 60
        val mins = (totalSecs / 60) % 60
        val hrs = totalSecs / 3600 % 24
        String.format("%02d:%02d:%02d.%03d", hrs, mins, secs, ms.toInt())
    }
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            time,
            color = MiuixTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.width(90.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            log.message,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
