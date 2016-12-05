package com.benjaminearley.cod3

sealed class Sum<out A : Any, out B : Any> {
    class kindA<out A : Any, out B : Any>(val value: A) : Sum<A, B>() {
        override fun <C> with(l: (A) -> C, r: (B) -> C): C = l(value)
    }

    class kindB<out A : Any, out B : Any>(val value: B) : Sum<A, B>() {
        override fun <C> with(l: (A) -> C, r: (B) -> C): C = r(value)
    }

    abstract fun <C> with(l: (A) ->  C, r: (B) -> C): C
}
