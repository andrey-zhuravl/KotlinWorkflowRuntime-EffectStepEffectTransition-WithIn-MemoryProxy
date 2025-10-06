package com.example.platform.human

fun interface HumanTaskListener {
    suspend fun onResume(resume: HumanTaskResume)
}
