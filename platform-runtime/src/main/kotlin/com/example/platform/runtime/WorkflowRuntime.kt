package com.example.platform.runtime

import com.example.platform.core.CommandMetadata
import com.example.platform.core.Effect
import com.example.platform.core.EffectStep
import com.example.platform.core.Effects
import com.example.platform.core.EffectsDSL
import com.example.platform.core.HumanInteraction
import com.example.platform.core.TimerCommand
import com.example.platform.core.TimerInstruction
import com.example.platform.core.Workflow
import com.example.platform.core.WorkflowContext
import com.example.platform.core.WorkflowLogger
import com.example.platform.persistence.jdbc.WorkflowPersistence
import com.example.platform.projections.ProjectionEngine
import com.example.platform.projections.ProjectionEvent
import com.example.platform.security.AuthorizationContext
import com.example.platform.security.PolicyDecision
import com.example.platform.security.PolicyEngine
import com.example.platform.timers.ScheduledTimer
import com.example.platform.timers.TimerMetadata
import com.example.platform.timers.TimerRequest
import com.example.platform.timers.TimerScheduler
import com.example.platform.transport.kafka.OutboxDispatcher
import com.example.platform.transport.kafka.OutboxMessageEnvelope
import com.example.platform.human.HumanTaskManager
import com.example.platform.human.HumanTaskRequest
import com.example.platform.human.HumanTaskResume
import java.time.Clock
import java.time.Instant
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
        executors[workflow.name] = WorkflowExecutor(workflow, workflow.name)
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
        executor.execute(envelope, envelope.command)
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

    private inner class WorkflowExecutor<S : Any, C : Any, E : Any, R : Any>(
        private val workflow: Workflow<S, C, E, R>,
        private val workflowType: String,
    ) {
        suspend fun execute(envelope: CommandEnvelope<C>, command: C): R? {
            val metadata = envelope.metadata
            if (metadata.commandId != null && persistence.isCommandProcessed(workflowType, envelope.workflowId, metadata.commandId)) {
                return null
            }

            val loaded = persistence.loadWorkflow(workflowType, envelope.workflowId) {
                workflow.initialState(envelope.workflowId)
            }

            val logger = loggerFactory.create(workflowType, envelope.workflowId)
            val context = DefaultWorkflowContext(
                workflowType = workflowType,
                workflowId = envelope.workflowId,
                effects = EffectsDSL.create(),
                logger = logger,
                clock = clock,
                metadata = metadata,
            )

            val initialState = loaded.state
            val effect = workflow.onCommand(initialState, command, context)
            val execution = interpretEffect(effect, initialState, context)

            val newState = execution.state
            val eventsToPersist = execution.events

            persistence.commitWorkflow(
                workflowType = workflowType,
                workflowId = envelope.workflowId,
                expectedSequence = loaded.lastSequence,
                newState = newState,
                newEvents = eventsToPersist,
                metadata = metadata,
            )

            if (eventsToPersist.isNotEmpty()) {
                publishToProjections(
                    workflowId = envelope.workflowId,
                    events = eventsToPersist,
                    metadata = metadata,
                    baseSequence = loaded.lastSequence,
                )
            }

            return execution.reply
        }

        private suspend fun publishToProjections(
            workflowId: String,
            events: List<E>,
            metadata: CommandMetadata,
            baseSequence: Long,
        ) {
            events.forEachIndexed { index, event ->
                projectionEngine.publish(
                    ProjectionEvent(
                        workflowType = workflowType,
                        workflowId = workflowId,
                        sequence = baseSequence + index + 1,
                        event = event,
                        tenantId = metadata.tenantId,
                        recordedAt = clock.instant(),
                    ),
                )
            }
        }

        private suspend fun interpretEffect(
            effect: Effect<S, E, R>,
            startingState: S,
            context: DefaultWorkflowContext<S, C, E, R>,
        ): ExecutionResult<S, E, R> {
            var state = startingState
            val events = mutableListOf<E>()
            var reply: R? = null
            var replySet = false

            for (step in effect.steps) {
                when (step) {
                    is EffectStep.Persist -> {
                        step.events.forEach { event ->
                            val applied = workflow.applyEvent(state, event)
                            state = applied
                            events += event
                        }
                    }
                    is EffectStep.Transition -> {
                        state = step.transition(state)
                    }
                    is EffectStep.Run -> {
                        step.block(state)
                    }
                    is EffectStep.Reply -> {
                        reply = step.builder(state)
                        replySet = true
                    }
                    is EffectStep.NoReply -> {
                        reply = null
                        replySet = true
                    }
                    is EffectStep.Schedule -> handleTimerInstruction(step.instruction, context, state)
                    is EffectStep.Outbox -> handleOutbox(step, context)
                    is EffectStep.ChildWorkflow -> handleChild(step.start, context)
                    is EffectStep.Human -> handleHuman(step.interaction, context)
                }
            }

            if (!replySet) {
                reply = null
            }

            return ExecutionResult(state, events, reply)
        }

        private suspend fun handleTimerInstruction(
            instruction: TimerInstruction,
            context: DefaultWorkflowContext<S, C, E, R>,
            state: S,
        ) {
            when (instruction) {
                is TimerInstruction.At -> timerScheduler.schedule(
                    TimerRequest(
                        workflowType = workflowType,
                        workflowId = context.workflowId,
                        key = instruction.key,
                        fireAt = instruction.fireAt,
                        payload = instruction.payload,
                        metadata = TimerMetadata(
                            tenantId = context.metadata.tenantId,
                            correlationId = context.metadata.correlationId,
                        ),
                    ),
                )
                is TimerInstruction.After -> timerScheduler.schedule(
                    TimerRequest(
                        workflowType = workflowType,
                        workflowId = context.workflowId,
                        key = instruction.key,
                        fireAt = context.now().plus(instruction.delay),
                        payload = instruction.payload,
                        metadata = TimerMetadata(
                            tenantId = context.metadata.tenantId,
                            correlationId = context.metadata.correlationId,
                        ),
                    ),
                )
                is TimerInstruction.Cron -> timerScheduler.schedule(
                    TimerRequest(
                        workflowType = workflowType,
                        workflowId = context.workflowId,
                        key = instruction.key,
                        fireAt = context.now(),
                        payload = instruction.payload,
                        metadata = TimerMetadata(
                            tenantId = context.metadata.tenantId,
                            correlationId = context.metadata.correlationId,
                        ),
                        cronExpression = instruction.expression,
                        jitter = instruction.jitter,
                    ),
                )
                is TimerInstruction.Cancel -> timerScheduler.cancel(
                    workflowType = workflowType,
                    workflowId = context.workflowId,
                    key = instruction.key,
                )
            }
        }

        private suspend fun handleOutbox(
            step: EffectStep.Outbox<S, E, R>,
            context: DefaultWorkflowContext<S, C, E, R>,
        ) {
            outboxDispatcher.publish(
                OutboxMessageEnvelope(
                    workflowType = workflowType,
                    workflowId = context.workflowId,
                    message = step.message,
                    channel = step.channel,
                    headers = step.headers,
                    tenantId = context.metadata.tenantId,
                    correlationId = context.metadata.correlationId,
                ),
            )
        }

        private suspend fun handleChild(
            start: ChildWorkflowStart,
            context: DefaultWorkflowContext<S, C, E, R>,
        ) {
            val metadata = context.metadata.copy()
            val delay = start.options.startDelay
            if (delay != null) {
                timerScheduler.schedule(
                    TimerRequest(
                        workflowType = start.workflowType,
                        workflowId = start.workflowId,
                        key = "child:${'$'}{start.workflowType}:${'$'}{start.workflowId}",
                        fireAt = context.now().plus(delay),
                        payload = TimerCommand(
                            command = start.initialCommand,
                            targetWorkflowType = start.workflowType,
                            targetWorkflowId = start.workflowId,
                            metadata = metadata,
                        ),
                        metadata = TimerMetadata(
                            tenantId = metadata.tenantId,
                            correlationId = metadata.correlationId,
                        ),
                    ),
                )
            } else {
                dispatch(
                    CommandEnvelope(
                        workflowType = start.workflowType,
                        workflowId = start.workflowId,
                        command = start.initialCommand,
                        metadata = metadata,
                    ),
                )
            }
        }

        private suspend fun handleHuman(
            interaction: HumanInteraction,
            context: DefaultWorkflowContext<S, C, E, R>,
        ) {
            humanTaskManager.register(
                HumanTaskRequest(
                    workflowType = workflowType,
                    workflowId = context.workflowId,
                    interaction = interaction,
                    metadata = context.metadata,
                ),
            )
        }
    }
}

data class CommandEnvelope<C : Any>(
    val workflowType: String,
    val workflowId: String,
    val command: C,
    val metadata: CommandMetadata = CommandMetadata(),
)

data class ExecutionResult<S, E, R>(
    val state: S,
    val events: List<E>,
    val reply: R?,
)

private class DefaultWorkflowContext<S, C, E, R>(
    val workflowType: String,
    override val workflowId: String,
    override val effects: Effects<S, E, R>,
    override val logger: WorkflowLogger,
    private val clock: Clock,
    val metadata: CommandMetadata,
) : WorkflowContext<S, C, E, R> {
    override fun now(): Instant = clock.instant()
    override val correlationId: String? get() = metadata.correlationId
    override val tenantId: String? get() = metadata.tenantId
}

data class PolicyViolationException(val decision: PolicyDecision) : RuntimeException(
    "Command rejected: ${'$'}decision",
)

fun PolicyDecision.ensureAllowed() {
    if (!allowed) {
        throw PolicyViolationException(this)
    }
}

fun interface WorkflowLoggerFactory {
    fun create(workflowType: String, workflowId: String): WorkflowLogger

    companion object {
        fun stdout(): WorkflowLoggerFactory = WorkflowLoggerFactory { type, id ->
            StdoutWorkflowLogger(type, id)
        }
    }
}

private class StdoutWorkflowLogger(
    private val workflowType: String,
    private val workflowId: String,
) : WorkflowLogger {
    override fun debug(message: String) {
        println("[${'$'}workflowType:${'$'}workflowId] DEBUG ${'$'}message")
    }

    override fun info(message: String) {
        println("[${'$'}workflowType:${'$'}workflowId] INFO ${'$'}message")
    }

    override fun warn(message: String, throwable: Throwable?) {
        println("[${'$'}workflowType:${'$'}workflowId] WARN ${'$'}message")
        throwable?.printStackTrace()
    }

    override fun error(message: String, throwable: Throwable?) {
        System.err.println("[${'$'}workflowType:${'$'}workflowId] ERROR ${'$'}message")
        throwable?.printStackTrace()
    }
}
