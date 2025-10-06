package com.example.sample.order

import com.example.testing.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private data class TestResult(val className: String, val methodName: String, val success: Boolean, val failure: Throwable? = null)

private fun discoverTestClasses(root: Path): List<Class<*>> {
    if (!Files.exists(root)) return emptyList()
    val classes = mutableListOf<Class<*>>()
    Files.walk(root).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".class") }
            .forEach { path ->
                val relative = root.relativize(path).toString()
                if (relative.endsWith("Test.class") && !relative.contains('$')) {
                    val className = relative
                        .removeSuffix(".class")
                        .replace(File.separatorChar, '.')
                    classes += Thread.currentThread().contextClassLoader.loadClass(className)
                }
            }
    }
    return classes
}

public fun main(args: Array<String>) {
    val root = args.getOrNull(0) ?: error("Test classes directory argument missing")
    val outputDir = Paths.get(root)
    val classes = discoverTestClasses(outputDir)
    val results = mutableListOf<TestResult>()
    for (clazz in classes) {
        val testMethods = clazz.declaredMethods.filter { it.isAnnotationPresent(Test::class.java) }
        if (testMethods.isEmpty()) continue
        val instance = clazz.getDeclaredConstructor().newInstance()
        for (method in testMethods) {
            val result = try {
                method.isAccessible = true
                method.invoke(instance)
                TestResult(clazz.name, method.name, true)
            } catch (ex: Throwable) {
                val cause = ex.cause ?: ex
                TestResult(clazz.name, method.name, false, cause)
            }
            results += result
            if (result.success) {
                println("[PASS] ${result.className}.${result.methodName}")
            } else {
                println("[FAIL] ${result.className}.${result.methodName}: ${result.failure}")
            }
        }
    }
    val failed = results.filterNot { it.success }
    println("Executed ${results.size} tests: ${results.size - failed.size} passed, ${failed.size} failed")
    if (failed.isNotEmpty()) {
        throw AssertionError("${failed.size} tests failed")
    }
}
