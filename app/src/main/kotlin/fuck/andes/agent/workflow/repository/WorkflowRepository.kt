package fuck.andes.agent.workflow.repository

import android.content.Context
import fuck.andes.agent.workflow.model.WorkflowDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class WorkflowRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val workflowsDir: File by lazy {
        File(context.filesDir, "workflows").apply { mkdirs() }
    }

    private val _workflows = MutableStateFlow<List<WorkflowDefinition>>(emptyList())
    val workflows: StateFlow<List<WorkflowDefinition>> = _workflows.asStateFlow()

    suspend fun loadAll() = withContext(Dispatchers.IO) {
        val list = workflowsDir.listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull { file ->
                runCatching {
                    json.decodeFromString<WorkflowDefinition>(file.readText())
                }.getOrNull()
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
        _workflows.value = list
    }

    suspend fun get(id: String): WorkflowDefinition? = withContext(Dispatchers.IO) {
        val file = File(workflowsDir, "$id.json")
        if (file.exists()) {
            runCatching { json.decodeFromString<WorkflowDefinition>(file.readText()) }.getOrNull()
        } else {
            null
        }
    }

    suspend fun save(definition: WorkflowDefinition) = withContext(Dispatchers.IO) {
        val toSave = definition.copy(updatedAt = System.currentTimeMillis())
        val file = File(workflowsDir, "${toSave.id}.json")
        file.writeText(json.encodeToString(toSave))
        loadAll()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val file = File(workflowsDir, "$id.json")
        if (file.exists()) file.delete()
        loadAll()
    }

    suspend fun importFromJson(jsonString: String): Result<WorkflowDefinition> =
        withContext(Dispatchers.IO) {
            runCatching {
                val def = json.decodeFromString<WorkflowDefinition>(jsonString)
                save(def)
                def
            }
        }

    suspend fun loadTemplate(name: String): WorkflowDefinition? = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.assets.open("workflow-templates/$name.json")
            val text = stream.bufferedReader().use { it.readText() }
            json.decodeFromString<WorkflowDefinition>(text)
        }.getOrNull()
    }

    fun listTemplateNames(): List<String> = runCatching {
        context.assets.list("workflow-templates")
            ?.filter { it.endsWith(".json") }
            ?.map { it.removeSuffix(".json") }
            ?: emptyList()
    }.getOrDefault(emptyList())

    fun toJson(definition: WorkflowDefinition): String = json.encodeToString(definition)

    fun fromJson(jsonString: String): Result<WorkflowDefinition> = runCatching {
        json.decodeFromString<WorkflowDefinition>(jsonString)
    }
}
