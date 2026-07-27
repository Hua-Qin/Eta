package fuck.andes.ui.screens.workflows

import androidx.compose.foundation.layout.Arrangement
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
import fuck.andes.ui.components.SectionHeader
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

private val CardHorizontalPadding = 12.dp
private val CardBottomPadding = 12.dp

sealed interface WorkflowsAction {
    data object NavigateBack : WorkflowsAction
    data class CreateNew(val template: String? = null) : WorkflowsAction
    data class Edit(val workflowId: String) : WorkflowsAction
    data class Run(val workflowId: String) : WorkflowsAction
    data class Delete(val workflowId: String) : WorkflowsAction
    data object ImportTemplate : WorkflowsAction
}

@Composable
fun WorkflowsScreen(
    workflows: List<WorkflowDefinition>,
    onAction: (WorkflowsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteTarget by remember { mutableStateOf<WorkflowDefinition?>(null) }

    MiuixScaffoldPage(
        title = "工作流",
        onBack = { onAction(WorkflowsAction.NavigateBack) },
        modifier = modifier,
        actions = {
            IconButton(onClick = { onAction(WorkflowsAction.CreateNew()) }) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_plus),
                    contentDescription = "新建",
                )
            }
        },
    ) {
        item(key = "section_user") {
            SectionHeader("我的工作流")
        }

        if (workflows.isEmpty()) {
            item(key = "empty_state") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CardHorizontalPadding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "还没有工作流",
                            color = MiuixTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Button(
                            onClick = { onAction(WorkflowsAction.CreateNew()) },
                        ) {
                            Text("新建工作流")
                        }
                    }
                }
            }
        } else {
            item(key = "workflows_list") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CardHorizontalPadding),
                ) {
                    workflows.forEachIndexed { index, workflow ->
                        WorkflowItem(
                            workflow = workflow,
                            onEdit = { onAction(WorkflowsAction.Edit(workflow.id)) },
                            onRun = { onAction(WorkflowsAction.Run(workflow.id)) },
                            onDelete = { deleteTarget = workflow },
                        )
                        if (index < workflows.size - 1) {
                            PrefDivider(startIndent = 56.dp)
                        }
                    }
                }
                Spacer(modifier = Modifier.size(CardBottomPadding))
            }
        }

        item(key = "section_templates") {
            SectionHeader("模板")
        }

        item(key = "templates_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CardHorizontalPadding),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_layout_template),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MiuixTheme.colorScheme.primaryContainer,
                                CircleShape,
                            )
                            .padding(6.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("从模板创建", style = MiuixTheme.typography.title3)
                        Text(
                            "使用预置模板快速开始",
                            color = MiuixTheme.colorScheme.onSurfaceVariant,
                            style = MiuixTheme.typography.callout,
                        )
                    }
                    TextButton(onClick = { onAction(WorkflowsAction.ImportTemplate) }) {
                        Text("查看")
                    }
                }
            }
        }
    }

    if (deleteTarget != null) {
        WindowDialog(onDismissRequest = { deleteTarget = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text("删除工作流", style = MiuixTheme.typography.title2)
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    "确定要删除「${deleteTarget?.name}」吗？此操作不可撤销。",
                    color = MiuixTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = {
                            deleteTarget?.let { onAction(WorkflowsAction.Delete(it.id)) }
                            deleteTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MiuixTheme.colorScheme.error,
                        ),
                    ) {
                        Text("删除")
                    }
                }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(LucideR.drawable.lucide_ic_workflow),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .size(32.dp)
                .background(
                    MiuixTheme.colorScheme.primaryContainer,
                    CircleShape,
                )
                .padding(6.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(workflow.name, style = MiuixTheme.typography.title3)
            if (workflow.description.isNotBlank()) {
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    workflow.description,
                    color = MiuixTheme.colorScheme.onSurfaceVariant,
                    style = MiuixTheme.typography.callout,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                "${workflow.steps.size} 个步骤",
                color = MiuixTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = MiuixTheme.typography.caption1,
            )
        }
        IconButton(onClick = onRun) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_play),
                contentDescription = "运行",
                tint = MiuixTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_pencil),
                contentDescription = "编辑",
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_trash_2),
                contentDescription = "删除",
                tint = MiuixTheme.colorScheme.error,
            )
        }
    }
}
