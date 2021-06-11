package com.apurebase.deferredJson

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.CoroutineContext


class DeferredJsonArray internal constructor(
    ctx: CoroutineContext
): CoroutineScope, Mutex by Mutex() {

    private val job = SupervisorJob()
    override val coroutineContext = (ctx + job)

    private val deferredArray = mutableListOf<Deferred<JsonElement>>()
    private var completedArray: List<JsonElement>? = null

    suspend fun addValue(element: JsonElement) {
        addDeferredValue(CompletableDeferred(element))
    }

    suspend fun addDeferredValue(element: Deferred<JsonElement>) = withLock {
        deferredArray.add(element)
    }

    suspend fun addDeferredObj(block: suspend DeferredJsonMap.() -> Unit) {
        val map = DeferredJsonMap(job)
        block(map)
        addDeferredValue(async(job, LAZY) { map.awaitAndBuild() })
    }

    suspend fun addDeferredArray(block: suspend DeferredJsonArray.() -> Unit) {
        val array = DeferredJsonArray(job)
        block(array)
        addDeferredValue(array.asDeferred())
    }

    fun asDeferred() : Deferred<JsonElement> {
        return async(job, LAZY) {
            awaitAll()
            build()
        }
    }

    suspend fun awaitAll() {
        check(completedArray == null) { "The deferred tree has already been awaited!" }
        completedArray = deferredArray.awaitAll()
        job.complete()
    }

    fun build(): JsonArray {
        checkNotNull(completedArray) { "The deferred tree has not been awaited!" }
        return JsonArray(completedArray!!)
    }
}
