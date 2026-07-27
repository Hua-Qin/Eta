package fuck.andes.agent.workflow

import android.content.Context
import fuck.andes.agent.workflow.model.WorkflowDefinition
import fuck.andes.agent.workflow.model.WorkflowRunState
import fuck.andes.agent.workflow.repository.WorkflowRepository
import fuck.andes.agent.workflow.runner.WorkflowRunner
import kotlinx.coroutines.flow.StateFlow

object WorkflowManager {

    private var repository: WorkflowRepository? = null
    private var currentRunner: WorkflowRunner? = null
    private var context: Context? = null

    fun init(appContext: Context) {
        if (repository != null) return
        context = appContext.applicationContext
        repository = WorkflowRepository(appContext.applicationContext)
    }

    fun getRepository(): WorkflowRepository {
        return repository ?: throw IllegalStateException("WorkflowManager not initialized")
    }

    suspend fun startWorkflow(
        workflowId: String,
        inputs: Map<String, String> = emptyMap(),
        toolExecutor: suspend (String, org.json.JSONObject) -> String,
        screenObserver: suspend () -> String,
        screenTextProvider: suspend () -> String,
    ): StateFlow<WorkflowRunState>? {
        val repo = getRepository()
        val def = repo.get(workflowId) ?: return null
        return startWorkflow(def, inputs, toolExecutor, screenObserver, screenTextProvider)
    }

    fun startWorkflow(
        definition: WorkflowDefinition,
        inputs: Map<String, String> = emptyMap(),
        toolExecutor: suspend (String, org.json.JSONObject) -> String,
        screenObserver: suspend () -> String,
        screenTextProvider: suspend () -> String,
    ): StateFlow<WorkflowRunState> {
        currentRunner?.destroy()
        val runner = WorkflowRunner(
            definition = definition,
            inputs = inputs,
            toolExecutor = toolExecutor,
            screenObserver = screenObserver,
            screenTextProvider = screenTextProvider,
        )
        currentRunner = runner
        runner.start()
        return runner.state
    }

    fun cancelCurrent() {
        currentRunner?.cancel()
        currentRunner = null
    }

    fun currentState(): StateFlow<WorkflowRunState>? = currentRunner?.state
}
