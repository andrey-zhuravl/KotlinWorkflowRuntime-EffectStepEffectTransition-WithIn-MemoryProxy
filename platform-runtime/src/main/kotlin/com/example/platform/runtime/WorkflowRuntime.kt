package com.example.platform.runtime

import com.example.platform.core.TimerCommand
import com.example.platform.core.Workflow
import com.example.platform.human.HumanTaskManager
import com.example.platform.human.HumanTaskResume
import com.example.platform.persistence.jdbc.WorkflowPersistence
import com.example.platform.projections.ProjectionEngine
import com.example.platform.security.AuthorizationContext
import com.example.platform.security.PolicyEngine
import com.example.platform.runtime.ensureAllowed
import com.example.platform.timers.ScheduledTimer
import com.example.platform.timers.TimerScheduler
import com.example.platform.transport.kafka.OutboxDispatcher
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkflowRuntime(
    private val persistence: WorkflowPersistence,
    private val timerScheduler: TimerScheduler,
    private val outboxDispatcher: OutboxDispatcher,
    private val humanTaskManager: HumanTaskManager,
    private val projectionEngine: ProjectionEngine,
    private val policyEngine: PolicyEngine,
    private val clock: Clock = Clock.systemUTC(),
    private val loggerFactory: WorkflowLoggerFactory = WorkflowLoggerFactory.stdout(),
) {
    private val executors = ConcurrentHashMap<String, WorkflowExecutor<*, *, *, *>>()

    init {
        timerScheduler.registerConsumer { timer ->
            handleTimer(timer)
        }
        humanTaskManager.registerListener { resume ->
            handleHumanResume(resume)
        }
    }

    fun <S : Any, C : Any, E : Any, R : Any> register(workflow: Workflow<S, C, E, R>) {
        executors[workflow.name] = WorkflowExecutor(
            workflow = workflow,
            workflowType = workflow.name,
            persistence = persistence,
            timerScheduler = timerScheduler,
            outboxDispatcher = outboxDispatcher,
            humanTaskManager = humanTaskManager,
            projectionEngine = projectionEngine,
            clock = clock,
            loggerFactory = loggerFactory,
            dispatch = { envelope -> dispatch(envelope) },
        )
    }

    suspend fun <C : Any> dispatch(envelope: CommandEnvelope<C>): Any? = withContext(Dispatchers.Default) {
        policyEngine.authorize(
            AuthorizationContext(
                workflowType = envelope.workflowType,
                workflowId = envelope.workflowId,
                command = envelope.command,
                metadata = envelope.metadata,
            ),
        ).ensureAllowed()
        @Suppress("UNCHECKED_CAST")
        val executor = executors[envelope.workflowType] as? WorkflowExecutor<Any, C, Any, Any>
            ?: error("Workflow ${'$'}{envelope.workflowType} is not registered")
        executor.execute(envelope)
    }

    suspend fun registeredWorkflows(): Set<String> = executors.keys

    private suspend fun handleTimer(timer: ScheduledTimer) {
        val payload = timer.payload
        if (payload is TimerCommand<*>) {
            val targetType = payload.targetWorkflowType ?: timer.workflowType
            val targetId = payload.targetWorkflowId ?: timer.workflowId
            @Suppress("UNCHECKED_CAST")
            val command = payload.command as Any
            dispatch(
                CommandEnvelope(
                    workflowType = targetType,
                    workflowId = targetId,
                    command = command,
                    metadata = payload.metadata,
                ),
            )
        }
    }

    private suspend fun handleHumanResume(resume: HumanTaskResume) {
        val payload = resume.payload ?: return
        @Suppress("UNCHECKED_CAST")
        val command = payload as? Any ?: return
        dispatch(
            CommandEnvelope(
                workflowType = resume.workflowType,
                workflowId = resume.workflowId,
                command = command,
                metadata = resume.metadata,
            ),
        )
    }
}
