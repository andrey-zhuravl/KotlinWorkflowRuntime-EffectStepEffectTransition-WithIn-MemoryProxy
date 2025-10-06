package com.example.platform.ksp.codegen

import com.example.platform.ksp.model.WorkflowBinding
import java.io.File

internal fun generateBinding(binding: WorkflowBinding, outputRoot: File) {
    val packageName = binding.packageName
    val generatedPackage = if (packageName.isBlank()) "generated" else "$packageName.generated"
    val packagePath = generatedPackage.replace('.', File.separatorChar)
    val outputDir = outputRoot.resolve(packagePath)
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    val fileName = "${binding.className}Generated.kt"
    val file = outputDir.resolve(fileName)
    file.writeText(renderFile(binding, generatedPackage))
}

private fun renderFile(binding: WorkflowBinding, packageName: String): String {
    val builder = StringBuilder()
    builder.appendLine("package $packageName")
    builder.appendLine()
    builder.appendLine("import com.example.platform.core.descriptor.ServiceDescriptor")
    builder.appendLine("import com.example.platform.core.descriptor.SimpleServiceDescriptor")
    builder.appendLine("import com.example.platform.runtime.local.WorkflowEngine")
    builder.appendLine("import com.example.platform.runtime.local.WorkflowProxy")
    builder.appendLine("import ${binding.packageName}.${binding.className}")
    builder.appendLine("import ${binding.stateFqn}")
    builder.appendLine("import ${binding.commandFqn}")
    builder.appendLine("import ${binding.eventFqn}")
    builder.appendLine("import ${binding.replyFqn}")
    builder.appendLine()
    builder.appendLine("/** Generated workflow bindings for ${binding.className}. */")
    builder.appendLine("public object ${binding.className}Generated {")
    builder.appendLine("    public fun descriptor(): ServiceDescriptor = SimpleServiceDescriptor(")
    builder.appendLine("        name = \"${binding.serviceName}\",")
    builder.appendLine("        stateFqn = \"${binding.stateFqn}\",")
    builder.appendLine("        commandFqn = \"${binding.commandFqn}\",")
    builder.appendLine("        eventFqn = \"${binding.eventFqn}\",")
    builder.appendLine("        replyFqn = \"${binding.replyFqn}\"")
    builder.appendLine("    )")
    builder.appendLine()
    builder.appendLine("    public fun register(")
    builder.appendLine("        engine: WorkflowEngine<${binding.stateType}, ${binding.commandType}, ${binding.eventType}, ${binding.replyType}>,")
    builder.appendLine("        workflow: ${binding.className}")
    builder.appendLine("    ) {")
    builder.appendLine("        engine.register(workflow)")
    builder.appendLine("    }")
    builder.appendLine()
    builder.appendLine("    public fun proxy(")
    builder.appendLine("        engine: WorkflowEngine<${binding.stateType}, ${binding.commandType}, ${binding.eventType}, ${binding.replyType}>,")
    builder.appendLine("        id: String,")
    builder.appendLine("        workflow: ${binding.className} = ${binding.className}()")
    builder.appendLine("    ): WorkflowProxy<${binding.stateType}, ${binding.commandType}, ${binding.eventType}, ${binding.replyType}> {")
    builder.appendLine("        engine.register(workflow)")
    builder.appendLine("        return engine.createProxy(workflow, id)")
    builder.appendLine("    }")
    builder.appendLine("}")
    return builder.toString()
}
