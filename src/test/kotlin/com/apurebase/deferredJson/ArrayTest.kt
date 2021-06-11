package com.apurebase.deferredJson

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.amshove.kluent.shouldEqualUnordered
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Basic builder tests need to work first")
class ArrayTest {

    @Test
    fun `basic array`() = runBlocking<Unit> {
        val v = CompletableDeferred<JsonPrimitive>()
        val data = deferredJsonBuilder {
            "data" toDeferredArray {
                addDeferredValue(v)
                addValue(JsonPrimitive("World"))
            }
        }

        launch {
            delay(500)
            v.complete(JsonPrimitive("Hello"))
        }

        data.await() shouldEqualUnordered buildJsonObject {
            putJsonArray("data") {
                add("Hello")
                add("World")
            }
        }
    }

    @Test
    fun `basic object array`() = runBlocking<Unit> {
        val v1 = CompletableDeferred<JsonPrimitive>()
        val v2 = CompletableDeferred<JsonPrimitive>()

        val data = deferredJsonBuilder {
            "data" toDeferredArray {
                addDeferredObj {
                    delay(50)
                    "first" toDeferredValue v1
                    "second" toDeferredValue v2
                }
            }
        }

        launch {
            delay(500)
            v1.complete(JsonPrimitive("Hello"))
            v2.complete(JsonPrimitive("World"))
        }

        data.await() shouldEqualUnordered buildJsonObject {
            putJsonArray("data") {
                addJsonObject {
                    put("first", "Hello")
                    put("second", "World")
                }
            }
        }
    }

    @Test
    fun `basic mixed types in array`() = runBlocking<Unit> {
        val v1 = CompletableDeferred<JsonPrimitive>()
        val v2 = CompletableDeferred<JsonPrimitive>()
        val v3 = CompletableDeferred<JsonPrimitive>()

        val data = deferredJsonBuilder {
            "data" toDeferredArray {
                addValue(JsonPrimitive("Hello"))
                addValue(JsonPrimitive(false))
                addDeferredValue(v2)
                addDeferredObj {
                    delay(50)
                    "first" toDeferredValue v1
                    "second" toDeferredValue v2
                }
                addDeferredArray {
                    addDeferredValue(v1)
                    addDeferredValue(v2)
                    addDeferredValue(v3)
                }
            }
        }

        launch {
            delay(500)
            v1.complete(JsonPrimitive("Hello"))
            v2.complete(JsonPrimitive(100))
            launch {
                v3.complete(JsonPrimitive("Second World"))
            }
        }

        data.await() shouldEqualUnordered buildJsonObject {
            putJsonArray("data") {
                add("Hello")
                add(false)
                add(100)
                addJsonObject {
                    put("first", "Hello")
                    put("second", 100)
                }
                addJsonArray {
                    add("Hello")
                    add(100)
                    add("Second World")
                }
            }
        }
    }

}
