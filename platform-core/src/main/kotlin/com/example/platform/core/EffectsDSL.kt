package com.example.platform.core

object EffectsDSL {
    fun <S, E, R> create(): Effects<S, E, R> = Effects()
}
