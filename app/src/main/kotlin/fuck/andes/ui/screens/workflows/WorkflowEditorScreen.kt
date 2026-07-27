package fuck.andes.ui.screens.workflows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import fuck.andes.agent.workflow.model.WorkflowDefinition
import fuck.andes.ui.components.MiuixScaffold
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlinx.serialization.json.Json

sealed interface WorkflowEditorAction {
    data object NavigateBack : WorkflowEditorAction
    data class Save(val definition: WorkflowDefinition) : WorkflowEditorAction
    data class Run(val workflowId: String) : WorkflowEditorAction
    data object Delete : WorkflowEditorAction
}

@Composable
fun WorkflowEditorScreen(
    workflow: WorkflowDefinition?,
    isNew: Boolean,
    onAction: (WorkflowEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var jsonText by remember(workflow) {
        mutableStateOf(
            workflow?.let { Json { prettyPrint = true; encodeDefaults = true }.encodeToString(WorkflowDefinition.serializer(), it) }
                ?: defaultWorkflowJson()
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun validate(): WorkflowDefinition? {
        return try {
            val def = Json { ignoreUnknownKeys = true; isLenient = true }
                .decodeFromString(WorkflowDefinition.serializer(), jsonText)
            error = null
            def
        } catch (e: Exception) {
            error = e.message ?: "解析失败"
            null
        }
    }

    MiuixScaffold(
        title = if (isNew) "新建工作流" else "编辑工作流",
        onBack = { onAction(WorkflowEditorAction.NavigateBack) },
        modifier = modifier,
        actions = {
            if (!isNew) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_trash_2),
                        contentDescription = "删除",
                        tint = MiuixTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = {
                validate()?.let { def ->
                    onAction(WorkflowEditorAction.Save(def))
                }
            }) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_save),
                    contentDescription = "保存",
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        },
    ) { paddingValues, scrollBehavior ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            val def = validate()
            val canSave = def != null
            val canRun = def != null && !isNew

            if (error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_alert_circle),
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JSON 错误: ${error ?: ""}",
                            color = MiuixTheme.colorScheme.error,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(
                        MiuixTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(12.dp),
                    ),
            ) {
                TextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    ),
                    minLines = 40,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { jsonText = formatJson(jsonText) }) {
                    Text("格式化")
                }
                Spacer(modifier = Modifier.size(8.dp))
                if (canRun) {
                    Button(
                        onClick = {
                            validate()?.let { def ->
                                onAction(WorkflowEditorAction.Run(def.id))
                            }
                        },
                        enabled = canSave,
                    ) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_play),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("运行")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Button(
                    onClick = {
                        validate()?.let { def ->
                            onAction(WorkflowEditorAction.Save(def))
                        }
                    },
                    enabled = canSave,
                ) {
                    Text("保存")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        WindowDialog(onDismissRequest = { showDeleteConfirm = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text("删除工作流", style = MiuixTheme.typography.title2)
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    "确定要删除这个工作流吗？此操作不可撤销。",
                    color = MiuixTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = {
                            onAction(WorkflowEditorAction.Delete)
                            showDeleteConfirm = false
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

private fun defaultWorkflowJson(): String {
    return """
{
  "id": "workflow_${System.currentTimeMillis()}",
  "name": "新建工作流",
  "description": "请描述这个工作流的用途",
  "trigger": "manual",
  "inputs": [],
  "steps": [
    {
      "id": "step_1",
      "type": "observe",
      "next": null
    }
  ]
}
    """.trimIndent()
}

private fun formatJson(text: String): String {
    return try {
        val json = Json { prettyPrint = true; prettyPrintIndent = "  " }
        val element = json.parseToJsonElement(text)
        json.encodeToString(element)
    } catch (e: Exception) {
        text
    }
}
