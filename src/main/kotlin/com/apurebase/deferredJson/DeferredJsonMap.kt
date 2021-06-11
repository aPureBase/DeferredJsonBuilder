package com.apurebase.deferredJson

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext

class DeferredJsonMap internal constructor(
    val ctx: CoroutineContext
): CoroutineScope {

    private val job = Job()
    override val coroutineContext = (ctx + job)

    private val dmLock = Mutex()
    private val deferredMap = mutableMapOf<String, Deferred<JsonElement>>()
    private val uddmLock = Mutex()
    private val unDefinedDeferredMap = mutableMapOf<String, DeferredJsonMap>()
    private val mjLock = Mutex()
    private val moreJobs = mutableListOf<Deferred<Unit>>()

    private var completedMap: Map<String, JsonElement>? = null

    suspend infix fun String.toValue(element: JsonElement) {
        this toDeferredValue CompletableDeferred(element)
    }

    suspend infix fun String.toDeferredValue(element: Deferred<JsonElement>) = dmLock.withLock {
        deferredMap[this] = element
    }

    suspend infix fun String.toDeferredObj(block: suspend DeferredJsonMap.() -> Unit) {
        val mapToUse = uddmLock.withLock {
            val map = unDefinedDeferredMap[this]
            if (map == null) {
                val newMap = DeferredJsonMap(job)
                unDefinedDeferredMap[this] = newMap
                newMap
            } else map
        }
        block(mapToUse)
    }

    suspend infix fun String.toDeferredArray(block: suspend DeferredJsonArray.() -> Unit) {
        val array = DeferredJsonArray(job)
        block(array)
        this@toDeferredArray toDeferredValue array.asDeferred()
    }

    private fun asDeferred() : Deferred<JsonElement> = async(job + CoroutineName("$ctx - create as deferred"), LAZY) {
        println("$job | map:asDeferred #1")
        awaitAll()
        println("$job | map:asDeferred #2")
        val res = build()
        println("$job | map:asDeferred #3")
        res
    }

    private suspend fun awaitAll() {
        check(completedMap == null) { "The deferred tree has already been awaited!" }

        println("$job | map:awaitAll #1")

        do {
            println("$job | map:awaitAll #do")
            unDefinedDeferredMap.map { (key, map) ->
                deferredLaunch {
                    key toDeferredValue map.asDeferred()
                }
            }
            moreJobs.awaitAll()
         } while (moreJobs.any { !it.isCompleted })

        println("$job | map:awaitAll #2")
        job.complete()
        println("$job | map:awaitAll #3")
        GlobalScope.launch {
            while (this@DeferredJsonMap.job.isActive) {
                println("  -> $job | mj   : ${moreJobs.filter { it.isCompleted }.size} / ${moreJobs.size}")
                println("  -> $job | uddm : ${unDefinedDeferredMap.filter { it.value.isActive }.size} / ${unDefinedDeferredMap.size}")
                delay(100)
            }
        }
        job.join()

        println("$job | map:awaitAll #4")
        completedMap = deferredMap.mapValues { it.value.await() }
    }

    suspend fun deferredLaunch(block: suspend DeferredJsonMap.() -> Unit) = coroutineScope {
        mjLock.withLock {
            println("$job | ")
            moreJobs.add(async(job + CoroutineName("$ctx deferredLaunch")) {
                block(this@DeferredJsonMap)
            })
        }
    }

    private fun build(): JsonObject {
        checkNotNull(completedMap) { "The deferred tree has not been awaited!" }
        check(job.isCompleted) { "Please call 'awaitAll' before calling this" }
        return JsonObject(completedMap!!)
    }

    suspend fun awaitAndBuild():JsonObject { awaitAll(); return build() }
}
