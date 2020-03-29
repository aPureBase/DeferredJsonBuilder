package com.apurebase.deferredJson

import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class DeferredJsonMap internal constructor(
    private val dispatcher: CoroutineDispatcher,
    val level: Int = 0
): CoroutineScope {

    internal val job = Job()
    override val coroutineContext = (dispatcher + job)

    private val deferredMap = mutableMapOf<String, Deferred<JsonElement>>()
    private var completedMap: Map<String, JsonElement>? = null


    private fun log(msg: String) {
        println("[DeferredJsonMap-$level]: $msg")
    }

    infix fun String.toValue(element: JsonElement) {
        log("toValue")
        this toDeferredValue CompletableDeferred(element)
    }

    infix fun String.toDeferredValue(element: Deferred<JsonElement>) {
        log("toDeferredValue")
        deferredMap[this] = element
    }

    suspend infix fun String.toDeferredObj(block: suspend DeferredJsonMap.() -> Unit) {
        log("toDeferredObj")
        val map = DeferredJsonMap(dispatcher, level + 1)
        block(map)
        this@toDeferredObj toDeferredValue map.asDeferred()
    }

    suspend infix fun String.toDeferredArray(block: suspend DeferredJsonArray.() -> Unit) {
        log("toDeferredArray")
        val array = DeferredJsonArray(dispatcher)
        block(array)
        this@toDeferredArray toDeferredValue array.asDeferred()
    }

    @Suppress("DeferredIsResult")
    fun asDeferred(): Deferred<JsonElement> {
        log("asDeferred")
        return async(coroutineContext, start = CoroutineStart.LAZY) {
            awaitAll()
            build()
        }
    }

    suspend fun awaitAll() {
        log("awaitAll")
        check(completedMap == null) { "The deferred tree has already been awaited!" }
        completedMap = deferredMap.mapValues { it.value.await() }

        log("awaitAll:done")

        job.complete()
    }

    fun build(): JsonObject {
        log("build")
        checkNotNull(completedMap) { "The deferred tree has not been awaited!" }
        return JsonObject(completedMap!!)
    }
}
