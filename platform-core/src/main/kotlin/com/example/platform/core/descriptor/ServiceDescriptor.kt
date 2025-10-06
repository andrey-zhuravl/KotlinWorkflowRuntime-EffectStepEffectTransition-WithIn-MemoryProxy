package com.example.platform.core.descriptor

/** Describes a workflow service for runtime registration. */
public interface ServiceDescriptor {
    /** Unique service name. */
    public val name: String

    /** Fully qualified state type name. */
    public val stateFqn: String

    /** Fully qualified command type name. */
    public val commandFqn: String

    /** Fully qualified event type name. */
    public val eventFqn: String

    /** Fully qualified reply type name. */
    public val replyFqn: String
}

/** Simple immutable [ServiceDescriptor] implementation used by generated code. */
public data class SimpleServiceDescriptor(
    override val name: String,
    override val stateFqn: String,
    override val commandFqn: String,
    override val eventFqn: String,
    override val replyFqn: String
) : ServiceDescriptor
