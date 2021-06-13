package com.apurebase.deferredJson

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.amshove.kluent.shouldEqualUnordered
import org.junit.jupiter.api.Test

class HugeComplexTest {

    @Test
    fun theTest() = runBlocking<Unit> {
        val (rootFields, compute1, validation1) = basic()
        val (otherFields, compute2, validation2) = basic()

        val data = deferredJsonBuilder(15_000) {
            rootFields()
            deferredLaunch {
                Thread.sleep(10)
                delay(15)
                "other" toDeferredObj { otherFields() }
            }
        }

        launch { compute1() }
        launch { compute2() }

        println(data.await().toString())

        data.await() shouldEqualUnordered buildJsonObject {
            validation1()
            putJsonObject("other") { validation2() }
        }
    }

    private fun basic(): Triple<suspend DeferredJsonMap.() -> Unit, suspend () -> Unit, JsonObjectBuilder.() -> Unit> {
        val v1 = CompletableDeferred<JsonElement>()
        val v2 = CompletableDeferred<JsonElement>()
        val v3 = CompletableDeferred<JsonElement>()
        val v4 = CompletableDeferred<JsonElement>()

        return Triple({
            "number" toDeferredValue v1
            "text" toDeferredValue v2
            "basicArray" toDeferredArray {
                addDeferredValue(v3)
                addDeferredValue(v4)
                addValue(JsonPrimitive("string value"))
            }
        }, {
            delay(100)
            v1.complete(JsonPrimitive(1_000))
            delay(100)
            v2.complete(JsonPrimitive("Hello World"))
            delay(100)
            v3.complete(JsonPrimitive("delayed string value"))
            delay(100)
            v4.complete(JsonPrimitive(false))
        }, {
            put("number", 1_000)
            put("text", "Hello World")
            putJsonArray("basicArray") {
                add("delayed string value")
                add(false)
                add("string value")
            }
        })
    }

}
