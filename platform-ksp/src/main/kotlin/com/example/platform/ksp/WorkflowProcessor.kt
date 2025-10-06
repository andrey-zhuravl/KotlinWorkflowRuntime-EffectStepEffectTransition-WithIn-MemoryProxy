package com.example.platform.ksp

import com.example.platform.ksp.codegen.generateBinding
import com.example.platform.ksp.model.Symbols
import com.example.platform.ksp.model.WorkflowBinding
import java.io.File

internal class WorkflowProcessor {
    private val bindings = mutableListOf<WorkflowBinding>()

    fun scan(root: File) {
        if (!root.exists()) return
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { parseFile(it) }
    }

    fun write(outputRoot: File) {
        for (binding in bindings) {
            generateBinding(binding, outputRoot)
        }
    }

    private fun parseFile(file: File) {
        val text = file.readText()
        val packageName = findPackage(text)
        val matches = WORKFLOW_PATTERN.findAll(text)
        for (match in matches) {
            val serviceName = match.groupValues[1]
            val className = match.groupValues[2]
            val typeList = match.groupValues[3].split(',').map { it.trim() }
            if (typeList.size != 4) {
                continue
            }
            val (stateType, commandType, eventType, replyType) = typeList
            val binding = WorkflowBinding(
                packageName = packageName,
                className = className,
                serviceName = serviceName,
                stateType = stateType,
                commandType = commandType,
                eventType = eventType,
                replyType = replyType,
                stateFqn = qualifyType(packageName, stateType),
                commandFqn = qualifyType(packageName, commandType),
                eventFqn = qualifyType(packageName, eventType),
                replyFqn = qualifyType(packageName, replyType),
                sourceFile = file
            )
            bindings += binding
        }
    }

    private fun findPackage(source: String): String {
        val packageMatch = PACKAGE_PATTERN.find(source)
        return packageMatch?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun qualifyType(packageName: String, typeName: String): String {
        return if (typeName.contains('.')) typeName else listOfNotNull(packageName.takeIf { it.isNotBlank() }, typeName).joinToString(".")
    }

    companion object {
        private val PACKAGE_PATTERN = Regex("^\\s*package\\s+([\\w.]+)", RegexOption.MULTILINE)
        private val WORKFLOW_PATTERN = Regex(
            """@${Symbols.WORKFLOW_SERVICE}\("([^"]+)"\)\s*(?:@[^
]*\n\s*)*(?:public\s+|internal\s+|private\s+|protected\s+)?class\s+(\w+)\s*:\s*${Symbols.WORKFLOW_INTERFACE}\s*<([^>]+)>""",
            RegexOption.MULTILINE
        )
    }
}
