package com.apurebase.deferredJson

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.amshove.kluent.shouldEqualUnordered
import org.junit.jupiter.api.Test

class HugeComplexTest {

    @Test
    fun theTest() = runBlocking<Unit> {
//        val otherJob = Job()

        val data = deferredJsonBuilder(15_000) {
            basic()
//            coroutineScope {
//                deferredLaunch {
                    Thread.sleep(10)
//                    launch(otherJob) {
                        delay(15)
                        "other" toDeferredObj { basic() }
//                    }
//                }
//            }
        }

        println(data.await().toString())

        data.await() shouldEqualUnordered buildJsonObject {
            basicValidation()
            putJsonObject("other") { basicValidation() }
        }
    }


    private suspend fun DeferredJsonMap.basic() = deferredLaunch {
        val v1 = CompletableDeferred<JsonElement>()
        val v2 = CompletableDeferred<JsonElement>()
        val v3 = CompletableDeferred<JsonElement>()
        val v4 = CompletableDeferred<JsonElement>()

        "number" toDeferredValue v1
        "text" toDeferredValue v2
        "basicArray" toDeferredArray {
            addDeferredValue(v3)
            addDeferredValue(v4)
            addValue(JsonPrimitive("string value"))
        }

        deferredLaunch {
            delay(100)
            v1.complete(JsonPrimitive(1_000))
            delay(100)
            v2.complete(JsonPrimitive("Hello World"))
            delay(100)
            v3.complete(JsonPrimitive("delayed string value"))
            delay(100)
            v4.complete(JsonPrimitive(false))
        }
    }

    private fun JsonObjectBuilder.basicValidation() {
//        put("number", 1_000)
//        put("text", "Hello World")
        putJsonArray("basicArray") {
            add("delayed string value")
//            add(false)
//            add("string value")
        }
    }

}
