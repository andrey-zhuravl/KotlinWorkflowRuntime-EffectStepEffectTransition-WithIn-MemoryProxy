import org.gradle.api.tasks.JavaExec
import java.io.File

val gradleHomeDir = gradle.gradleHomeDir ?: error("Gradle home unavailable")
val libDir = gradleHomeDir.resolve("lib")

val kotlinJars = libDir.listFiles { file ->
    file.isFile && file.name.startsWith("kotlin-") && file.name.endsWith(".jar")
}?.toList() ?: error("Kotlin jars not found in $libDir")
val compilerJars = kotlinJars.filter { jar ->
    jar.name.contains("compiler") || jar.name.contains("scripting") || jar.name.contains("daemon")
}
val runtimeJars = kotlinJars.filterNot { jar ->
    jar.name.contains("compiler") || jar.name.contains("scripting") || jar.name.contains("daemon")
}
val coroutinesJar = libDir.resolve("kotlinx-coroutines-core-jvm-1.6.4.jar")
require(coroutinesJar.exists()) { "kotlinx-coroutines-core jar not found at $coroutinesJar" }
val serializationCoreJar = libDir.resolve("kotlinx-serialization-core-jvm-1.6.2.jar")
require(serializationCoreJar.exists()) { "kotlinx-serialization-core jar not found at $serializationCoreJar" }
val serializationJsonJar = libDir.resolve("kotlinx-serialization-json-jvm-1.6.2.jar")
require(serializationJsonJar.exists()) { "kotlinx-serialization-json jar not found at $serializationJsonJar" }
val troveJar = libDir.resolve("trove4j-1.0.20200330.jar")
require(troveJar.exists()) { "trove4j jar not found at $troveJar" }
val annotationsJar = libDir.resolve("annotations-24.0.1.jar")
require(annotationsJar.exists()) { "annotations jar not found at $annotationsJar" }

val runtimeClasspathBase = files(runtimeJars + listOf(coroutinesJar, serializationCoreJar, serializationJsonJar, annotationsJar))
val compilerClasspathBase = files(compilerJars + runtimeClasspathBase.files + troveJar)

val moduleDependencies = mapOf(
    "platform-core" to emptyList<String>(),
    "platform-ksp" to listOf(":platform-core"),
    "platform-runtime-local" to listOf(":platform-core"),
    "sample-order" to listOf(":platform-core", ":platform-runtime-local", ":platform-ksp")
)

subprojects {
    val dependenciesPaths = moduleDependencies[name] ?: emptyList()
    val mainSourceDir = file("src/main/kotlin")
    val testSourceDir = file("src/test/kotlin")
    val generatedDir = layout.buildDirectory.dir("generated/ksp/main").get().asFile
    val mainSources = fileTree(mainSourceDir) { include("**/*.kt") }
    val generatedSources = fileTree(generatedDir) { include("**/*.kt") }
    val allMainSources = mainSources + generatedSources
    val testSources = fileTree(testSourceDir) { include("**/*.kt") }
    val mainOutput = layout.buildDirectory.dir("classes/kotlin/main")
    val testOutput = layout.buildDirectory.dir("classes/kotlin/test")

    tasks.register("clean") {
        group = "build"
        delete(buildDir)
    }

    val generateSources = if (name == "sample-order") {
        tasks.register<JavaExec>("generateSources") {
            group = "build"
            description = "Generates workflow bindings."
            inputs.files(mainSources)
            outputs.dir(generatedDir)
            dependsOn(project(":platform-ksp").tasks.named("compileMain"))
            mainClass.set("com.example.platform.ksp.WorkflowProcessorProviderKt")
            classpath = files(
                compilerClasspathBase.files +
                    runtimeClasspathBase.files +
                    project(":platform-ksp").layout.buildDirectory.dir("classes/kotlin/main").get().asFile
            )
            doFirst {
                generatedDir.mkdirs()
            }
            args = listOf(mainSourceDir.absolutePath, generatedDir.absolutePath)
        }
    } else {
        null
    }

    val compileMain = tasks.register<JavaExec>("compileMain") {
        group = "build"
        description = "Compiles Kotlin main sources."
        inputs.files(allMainSources)
        outputs.dir(mainOutput)
        enabled = allMainSources.files.isNotEmpty()
        dependsOn(dependenciesPaths.map { project(it).tasks.named("compileMain") })
        generateSources?.let { dependsOn(it) }
        mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
        classpath = compilerClasspathBase
        doFirst {
            mainOutput.get().asFile.mkdirs()
            val cp = mutableListOf<File>()
            cp += runtimeClasspathBase.files
            dependenciesPaths.forEach { path ->
                cp += project(path).layout.buildDirectory.dir("classes/kotlin/main").get().asFile
            }
            val argsList = mutableListOf("-no-stdlib", "-no-reflect", "-d", mainOutput.get().asFile.absolutePath)
            if (cp.isNotEmpty()) {
                argsList += listOf("-classpath", cp.joinToString(File.pathSeparator) { it.absolutePath })
            }
            val sourceFiles = allMainSources.files.filter { it.isFile }
            argsList += sourceFiles.map { it.absolutePath }
            args = argsList
        }
    }

    val compileTest = tasks.register<JavaExec>("compileTest") {
        group = "build"
        description = "Compiles Kotlin test sources."
        inputs.files(testSources)
        outputs.dir(testOutput)
        enabled = testSources.files.isNotEmpty()
        dependsOn(compileMain)
        dependsOn(dependenciesPaths.map { project(it).tasks.named("compileMain") })
        mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
        classpath = compilerClasspathBase
        doFirst {
            testOutput.get().asFile.mkdirs()
            val cp = mutableListOf<File>()
            cp += runtimeClasspathBase.files
            dependenciesPaths.forEach { path ->
                cp += project(path).layout.buildDirectory.dir("classes/kotlin/main").get().asFile
            }
            cp += mainOutput.get().asFile
            val argsList = mutableListOf("-no-stdlib", "-no-reflect", "-d", testOutput.get().asFile.absolutePath)
            if (cp.isNotEmpty()) {
                argsList += listOf("-classpath", cp.joinToString(File.pathSeparator) { it.absolutePath })
            }
            val sourceFiles = testSources.files.filter { it.isFile }
            argsList += sourceFiles.map { it.absolutePath }
            args = argsList
        }
    }

    val testTask = tasks.register<JavaExec>("test") {
        group = "verification"
        description = "Runs module tests."
        enabled = testSources.files.isNotEmpty()
        dependsOn(compileTest)
        doFirst {
            val cp = mutableListOf<File>()
            cp += runtimeClasspathBase.files
            dependenciesPaths.forEach { path ->
                cp += project(path).layout.buildDirectory.dir("classes/kotlin/main").get().asFile
            }
            cp += mainOutput.get().asFile
            cp += testOutput.get().asFile
            classpath = files(cp)
        }
        mainClass.set("com.example.sample.order.TestRunnerKt")
        args = listOf(testOutput.get().asFile.absolutePath)
    }

    tasks.register("build") {
        group = "build"
        dependsOn(compileMain)
        dependsOn(testTask)
    }

    if (name == "sample-order") {
        tasks.register<JavaExec>("run") {
            group = "application"
            description = "Runs the sample application."
            dependsOn(compileMain)
            dependsOn(dependenciesPaths.map { project(it).tasks.named("compileMain") })
            doFirst {
                val cp = mutableListOf<File>()
                cp += runtimeClasspathBase.files
                dependenciesPaths.forEach { path ->
                    cp += project(path).layout.buildDirectory.dir("classes/kotlin/main").get().asFile
                }
                cp += mainOutput.get().asFile
                classpath = files(cp)
            }
            mainClass.set("com.example.sample.order.MainKt")
        }
    }
}

tasks.register("clean") {
    group = "build"
    delete(buildDir)
    dependsOn(subprojects.map { it.path + ":clean" })
}

tasks.register("build") {
    group = "build"
    dependsOn(subprojects.map { it.path + ":build" })
}

tasks.register("test") {
    group = "verification"
    dependsOn(subprojects.map { it.path + ":test" })
}
