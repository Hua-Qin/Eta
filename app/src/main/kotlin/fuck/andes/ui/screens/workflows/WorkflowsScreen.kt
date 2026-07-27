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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import fuck.andes.agent.workflow.model.WorkflowDefinition
import fuck.andes.ui.components.MiuixScaffoldPage
import fuck.andes.ui.components.PrefDivider
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

sealed interface WorkflowsAction {
    data object NavigateBack : WorkflowsAction
    data object CreateNew : WorkflowsAction
    data object ImportTemplate : WorkflowsAction
    data class Edit(val workflowId: String) : WorkflowsAction
    data class Run(val workflowId: String) : WorkflowsAction
    data class Delete(val workflowId: String) : WorkflowsAction
}

@Composable
fun WorkflowsScreen(
    workflows: List<WorkflowDefinition>,
    onAction: (WorkflowsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteTarget by remember { mutableStateOf<WorkflowDefinition?>(null) }

    MiuixScaffoldPage(
        title = "工作流管理",
        onBack = { onAction(WorkflowsAction.NavigateBack) },
        modifier = modifier,
        actions = {
            IconButton(onClick = { onAction(WorkflowsAction.CreateNew) }) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_plus),
                    contentDescription = "新建工作流",
                )
            }
        },
    ) {
        item(key = "actions-title") { SmallTitle("操作") }
        item(key = "actions-card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                BasicComponent(
                    title = "新建工作流",
                    summary = "创建一个新的 JSON 工作流",
                    startAction = {
                        WorkflowIcon(LucideR.drawable.lucide_ic_plus)
                    },
                    onClick = { onAction(WorkflowsAction.CreateNew) },
                )
                PrefDivider()
                BasicComponent(
                    title = "从模板创建",
                    summary = "使用内置模板快速开始",
                    startAction = {
                        WorkflowIcon(LucideR.drawable.lucide_ic_layout_template)
                    },
                    onClick = { onAction(WorkflowsAction.ImportTemplate) },
                )
            }
        }

        if (workflows.isNotEmpty()) {
            item(key = "list-title") { SmallTitle("我的工作流 (${workflows.size})") }
            item(key = "list-card") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    workflows.forEachIndexed { index, workflow ->
                        WorkflowItem(
                            workflow = workflow,
                            onEdit = { onAction(WorkflowsAction.Edit(workflow.id)) },
                            onRun = { onAction(WorkflowsAction.Run(workflow.id)) },
                            onDelete = { deleteTarget = workflow },
                        )
                        if (index < workflows.lastIndex) PrefDivider()
                    }
                }
            }
        } else {
            item(key = "empty-title") { SmallTitle("暂无工作流") }
            item(key = "empty-hint") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(LucideR.drawable.lucide_ic_layout_template),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "还没有工作流",
                                color = MiuixTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "点击上方按钮创建第一个工作流",
                                color = MiuixTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { workflow ->
        WindowDialog(
            show = true,
            title = "删除工作流？",
            summary = "删除「${workflow.name}」后无法恢复。",
            onDismissRequest = { deleteTarget = null },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onAction(WorkflowsAction.Delete(workflow.id))
                        deleteTarget = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary(
                        color = MiuixTheme.colorScheme.error,
                        contentColor = MiuixTheme.colorScheme.onError,
                    ),
                ) {
                    Text("删除")
                }
                TextButton(
                    text = "取消",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { deleteTarget = null },
                )
            }
        }
    }
}

@Composable
private fun WorkflowItem(
    workflow: WorkflowDefinition,
    onEdit: () -> Unit,
    onRun: () -> Unit,
    onDelete: () -> Unit,
) {
    BasicComponent(
        title = workflow.name,
        summary = workflow.description.ifBlank { "无描述" },
        startAction = {
            WorkflowIcon(LucideR.drawable.lucide_ic_layout_template)
        },
        endActions = {
            IconButton(
                onClick = onRun,
                minWidth = 36.dp,
                minHeight = 36.dp,
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_play),
                    contentDescription = "运行",
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onEdit,
                minWidth = 36.dp,
                minHeight = 36.dp,
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_pencil),
                    contentDescription = "编辑",
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onDelete,
                minWidth = 36.dp,
                minHeight = 36.dp,
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_trash_2),
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun WorkflowIcon(iconRes: Int) {
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(36.dp)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
}
