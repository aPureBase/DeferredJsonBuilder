package com.apurebase.deferredJson

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.CoroutineContext

val all = mutableListOf<DeferredJsonMap>()

class DeferredJsonMap internal constructor(
    val ctx: CoroutineContext,
    val isRoot: Boolean = false
): CoroutineScope {

    internal val job = Job()
    override val coroutineContext = ctx + job + CoroutineName("isRoot: $isRoot")

    private val dmLock = Mutex()
    private val deferredMap = mutableMapOf<String, Deferred<JsonElement>>()
    private val uddmLock = Mutex()
    private val unDefinedDeferredMap = mutableMapOf<String, DeferredJsonMap>()
    private val mjLock = Mutex()
    private val moreJobs = mutableListOf<Deferred<Unit>>()

    private var completedMap: Map<String, JsonElement>? = null

    init {
        all.add(this)
    }

    suspend infix fun String.toValue(element: JsonElement) {
        this toDeferredValue CompletableDeferred(element)
    }

    infix fun String.toDeferredValue(element: Deferred<JsonElement>) {
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

    private fun asDeferredAsync(startIn: Job = job) : Deferred<JsonElement> = async(startIn + CoroutineName("$ctx - create as deferred"), LAZY) {
        awaitAll()
        build()
    }

    private suspend fun awaitAll() {
        check(completedMap == null) { "The deferred tree has already been awaited!" }

        println("$job | map:awaitAll #1")


        do {
            println("$job | map:awaitAll #do")
            moreJobs.awaitAll()
            unDefinedDeferredMap.map { (key, map) ->
                key toDeferredValue map.asDeferredAsync(job)
            }
         } while (moreJobs.any { !it.isCompleted })

        println("$job | map:awaitAll #2")
        job.children.toList().joinAll()
        println("$job | map:awaitAll #3")
        job.complete()


        println("$job | map:awaitAll #4")
        job.join()
        completedMap = deferredMap.mapValues { it.value.await() }
    }

    suspend fun deferredLaunch(block: suspend DeferredJsonMap.() -> Unit) = mjLock.withLock {
        println("$job | ")
        moreJobs.add(async(job + CoroutineName("$ctx deferredLaunch"), LAZY) {
            block(this@DeferredJsonMap)
        })
    }

    private fun build(): JsonObject {
        checkNotNull(completedMap) { "The deferred tree has not been awaited!" }
        check(job.isCompleted) { "Please call 'awaitAll' before calling this" }
        return JsonObject(completedMap!!)
    }

    suspend fun awaitAndBuild():JsonObject { awaitAll(); return build() }
}
