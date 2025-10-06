package com.example.testing

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Test

public fun fail(message: String): Nothing = throw AssertionError(message)

public fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    if (expected != actual) {
        fail(message ?: "Expected <$expected> but was <$actual>")
    }
}

public fun assertNull(value: Any?, message: String? = null) {
    if (value != null) {
        fail(message ?: "Expected value to be null but was $value")
    }
}

public fun assertNotNull(value: Any?, message: String? = null) {
    if (value == null) {
        fail(message ?: "Expected value to be non-null")
    }
}

public inline fun <reified T> assertIs(value: Any?, message: String? = null) {
    if (value !is T) {
        fail(message ?: "Expected value to be of type ${T::class.java.name} but was ${value?.javaClass?.name}")
    }
}
