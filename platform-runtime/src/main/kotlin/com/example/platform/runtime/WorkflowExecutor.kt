package com.example.platform.runtime

import com.example.platform.core.ChildWorkflowStart
import com.example.platform.core.CommandMetadata
import com.example.platform.core.Effect
import com.example.platform.core.EffectStep
import com.example.platform.core.EffectsDSL
import com.example.platform.core.HumanInteraction
import com.example.platform.core.TimerCommand
import com.example.platform.core.TimerInstruction
import com.example.platform.core.Workflow
import com.example.platform.persistence.jdbc.WorkflowPersistence
import com.example.platform.projections.ProjectionEngine
import com.example.platform.projections.ProjectionEvent
import com.example.platform.timers.TimerMetadata
import com.example.platform.timers.TimerRequest
import com.example.platform.timers.TimerScheduler
import com.example.platform.transport.kafka.OutboxDispatcher
import com.example.platform.transport.kafka.OutboxMessageEnvelope
import com.example.platform.human.HumanTaskManager
import com.example.platform.human.HumanTaskRequest
import java.time.Clock

internal class WorkflowExecutor<S : Any, C : Any, E : Any, R : Any>(
    private val workflow: Workflow<S, C, E, R>,
    private val workflowType: String,
    private val persistence: WorkflowPersistence,
    private val timerScheduler: TimerScheduler,
    private val outboxDispatcher: OutboxDispatcher,
    private val humanTaskManager: HumanTaskManager,
    private val projectionEngine: ProjectionEngine,
    private val clock: Clock,
    private val loggerFactory: WorkflowLoggerFactory,
    private val dispatch: suspend (CommandEnvelope<Any>) -> Any?,
) {
    suspend fun execute(envelope: CommandEnvelope<C>): R? {
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
        val effect = workflow.onCommand(initialState, envelope.command, context)
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
                is EffectStep.Schedule -> handleTimerInstruction(step.instruction, context)
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
