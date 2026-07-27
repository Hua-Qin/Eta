package fuck.andes.ui.screens.workflows

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fuck.andes.agent.workflow.model.WorkflowDefinition
import fuck.andes.ui.components.MiuixScaffoldPage
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val context = LocalContext.current
    var jsonText by remember(workflow) {
        mutableStateOf(
            workflow?.let { w ->
                Json { prettyPrint = true; encodeDefaults = true }
                    .encodeToString(WorkflowDefinition.serializer(), w)
            } ?: buildDefaultJson()
        )
    }
    var name by remember(workflow) { mutableStateOf(workflow?.name ?: "") }
    var description by remember(workflow) { mutableStateOf(workflow?.description ?: "") }
    var parseError by remember { mutableStateOf<String?>(null) }

    fun validateJson(): WorkflowDefinition? {
        return try {
            val def = Json { ignoreUnknownKeys = true }
                .decodeFromString(WorkflowDefinition.serializer(), jsonText)
            parseError = null
            def
        } catch (e: Exception) {
            parseError = e.message
            null
        }
    }

    MiuixScaffoldPage(
        title = if (isNew) "新建工作流" else "编辑工作流",
        onBack = { onAction(WorkflowEditorAction.NavigateBack) },
        modifier = modifier,
    ) {
        item(key = "basic-title") { SmallTitle("基本信息") }
        item(key = "basic-card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "工作流名称",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "描述",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item(key = "json-title") { SmallTitle("JSON 定义") }
        item(key = "json-card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        label = "工作流 JSON",
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    )
                    if (parseError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "JSON 解析错误: ${parseError}",
                            color = MiuixTheme.colorScheme.error,
                        )
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
                            onClick = {
                                val def = validateJson()
                                if (def != null) {
                                    val toSave = def.copy(
                                        id = workflow?.id ?: "workflow_${System.currentTimeMillis()}",
                                        name = name.ifBlank { def.name },
                                        description = description.ifBlank { def.description },
                                    )
                                    onAction(WorkflowEditorAction.Save(toSave))
                                    Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "JSON 格式错误", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary(),
                        ) {
                            Text("保存")
                        }
                        Button(
                            onClick = {
                                val def = validateJson()
                                if (def != null) {
                                    val id = workflow?.id ?: "workflow_${System.currentTimeMillis()}"
                                    val toSave = def.copy(
                                        id = id,
                                        name = name.ifBlank { def.name },
                                        description = description.ifBlank { def.description },
                                    )
                                    onAction(WorkflowEditorAction.Save(toSave))
                                    onAction(WorkflowEditorAction.Run(id))
                                } else {
                                    Toast.makeText(context, "JSON 格式错误", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("运行")
                        }
                    }
                    if (!isNew) {
                        Button(
                            onClick = { onAction(WorkflowEditorAction.Delete) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColorsPrimary(
                                color = MiuixTheme.colorScheme.error,
                                contentColor = MiuixTheme.colorScheme.onError,
                            ),
                        ) {
                            Text("删除")
                        }
                    }
                }
            }
        }
    }
}

private fun buildDefaultJson(): String {
    return """{
  "name": "新建工作流",
  "description": "",
  "steps": [
    {
      "id": "step_1",
      "type": "tool",
      "name": "示例步骤",
      "tool_name": "device_info",
      "description": "获取设备信息"
    }
  ]
}"""
}
