package com.apurebase.deferredJson

import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext

class DeferredJsonMap internal constructor(
    ctx: CoroutineContext
): CoroutineScope {

    internal val job = Job()
    override val coroutineContext = ctx + job

    private val deferredMap = mutableMapOf<String, Deferred<JsonElement>>()
    private var completedMap: Map<String, JsonElement>? = null

    infix fun String.toValue(element: JsonElement) {
        this toDeferredValue CompletableDeferred(element)
    }

    infix fun String.toDeferredValue(element: Deferred<JsonElement>) {
        deferredMap[this] = element
    }

    suspend infix fun String.toDeferredObj(block: suspend DeferredJsonMap.() -> Unit) {
        val map = DeferredJsonMap(coroutineContext)
        block(map)
        this@toDeferredObj toDeferredValue map.asDeferred()
    }

    suspend infix fun String.toDeferredArray(block: suspend DeferredJsonArray.() -> Unit) {
        val array = DeferredJsonArray(coroutineContext)
        block(array)
        this@toDeferredArray toDeferredValue array.asDeferred()
    }

    @Suppress("DeferredIsResult")
    fun asDeferred(): Deferred<JsonElement> {
        return async(coroutineContext, start = CoroutineStart.LAZY) {
            awaitAll()
            build()
        }
    }

    suspend fun awaitAll() {
        check(completedMap == null) { "The deferred tree has already been awaited!" }
        completedMap = deferredMap.mapValues { it.value.await() }


        job.complete()
    }

    fun build(): JsonObject {
        checkNotNull(completedMap) { "The deferred tree has not been awaited!" }
        return JsonObject(completedMap!!)
    }
}
