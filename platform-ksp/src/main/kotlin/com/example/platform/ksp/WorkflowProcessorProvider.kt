package com.example.platform.ksp

import java.io.File

internal object WorkflowProcessorProvider {
    fun generate(sourceRoots: List<File>, outputRoot: File) {
        val processor = WorkflowProcessor()
        sourceRoots.forEach(processor::scan)
        processor.write(outputRoot)
    }
}

fun main(args: Array<String>) {
    require(args.size >= 2) { "Expected <inputDir> <outputDir>" }
    val input = File(args[0])
    val output = File(args[1])
    WorkflowProcessorProvider.generate(listOf(input), output)
}
