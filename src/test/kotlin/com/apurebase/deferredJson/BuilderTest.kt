package com.apurebase.deferredJson

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEqualUnordered
import org.junit.jupiter.api.Test

class BuilderTest {

    @Test
    fun `basic test`() = runBlocking<Unit> {
        val def1 = CompletableDeferred<JsonElement>()
        val def2 = CompletableDeferred<JsonElement>()
        val def3 = CompletableDeferred<JsonElement>()

        val deferredMap = deferredJsonBuilder(25_000) {
            "hello" toDeferredValue def1
            "extra" toDeferredObj {
                "v1" toDeferredValue def2
                "v2" toDeferredValue def3
            }
        }

        launch {
            def1.complete(JsonPrimitive("world"))
            def2.complete(JsonPrimitive("new world"))
            launch {
                def3.complete(JsonPrimitive(""))
            }
        }

        deferredMap.await() shouldBeEqualTo buildJsonObject {
            put("hello", "world")
            putJsonObject("extra") {
                put("v1", "new world")
                put("v2", "")
            }
        }
    }


    @Test
    fun `basic deferred launches`() = runBlocking<Unit> {
        var def1: CompletableDeferred<JsonElement>? = null
        var def2: CompletableDeferred<JsonElement>? = null
        var def3: CompletableDeferred<JsonElement>? = null

        val deferredMap = deferredJsonBuilder(15_000) {
            def1 = CompletableDeferred()

            "hello" toDeferredValue def1!!
            delay(25)
            def2 = CompletableDeferred()
            "last" toDeferredValue def2!!


            deferredLaunch {
                delay(750)
                def3 = CompletableDeferred()
                "onTheFly" toDeferredValue def3!!
            }
        }

        launch {
            def1!!.complete(JsonPrimitive("world"))
            launch {
                delay(75)
                def2!!.complete(JsonPrimitive("takes long"))
            }
            while (def3 == null) {
                delay(10)
            }
            def3!!.complete(JsonPrimitive("value"))
        }

        deferredMap.await() shouldEqualUnordered buildJsonObject {
            put("hello", "world")
            put("last", "takes long")
            put("onTheFly", "value")
        }
    }

}
