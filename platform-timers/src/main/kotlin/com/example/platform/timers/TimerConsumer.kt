package com.example.platform.timers

fun interface TimerConsumer {
    suspend fun onTimer(timer: ScheduledTimer)
}
