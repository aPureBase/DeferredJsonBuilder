package com.apurebase.deferredJson

import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject

fun CoroutineScope.deferredJsonBuilder(
    timeout: Long? = null,
    init: suspend DeferredJsonMap.() -> Unit
): Deferred<JsonObject> = async {
    val block: suspend CoroutineScope.() -> JsonObject = {
        try {
            val builder = DeferredJsonMap(coroutineContext, true)
            println("Root: ${builder.job}")
            builder.init()
            builder.awaitAndBuild()
        } catch (e: CancellationException) {
            throw e.cause ?: e
        }
    }

    timeout?.let {
        withTimeout(it) {
            block()
        }
    } ?: block()
}
