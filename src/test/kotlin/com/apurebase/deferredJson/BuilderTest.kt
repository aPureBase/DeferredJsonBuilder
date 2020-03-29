package com.apurebase.deferredJson

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.json
import org.amshove.kluent.shouldEqualUnordered
import org.junit.jupiter.api.RepeatedTest

class BuilderTest {

    @RepeatedTest(100)
    fun `basic test`() = runBlockingTest {
        val def1 = CompletableDeferred<JsonElement>()
        val def2 = CompletableDeferred<JsonElement>()
        val def3 = CompletableDeferred<JsonElement>()

        val deferredMap = async {
            deferredJsonBuilder {
                "hello" toDeferredValue def1
                "extra" toDeferredObj {
                    "v1" toDeferredValue def2
                    "v2" toDeferredValue def3
                }
            }
        }


        launch {
            def1.complete(JsonPrimitive("world"))
            def2.complete(JsonPrimitive("new world"))
            launch {
                def3.complete(JsonPrimitive(""))
            }
        }

        deferredMap.await() shouldEqualUnordered json {
            "hello" to "world"
            "extra" to json {
                "v1" to "new world"
                "v2" to ""
            }
        }
        Unit
    }

}
