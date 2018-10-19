/*
 * Copyright 2018 Babylon Healthcare Services Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.certificatetransparency.ctlog.datasource

import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.certificatetransparency.ctlog.internal.loglist.InMemoryDataSource
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.util.concurrent.atomic.AtomicInteger

class DataSourceReuseInflightTest {

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    private val cache = spy(InMemoryDataSource<Any>())

    private lateinit var reuseInflightCache: DataSource<Any>

    @Before
    fun before() {
        reuseInflightCache = cache.reuseInflight()
    }

    // get
    @Test
    fun `single call to get returns the value`() = runBlocking {
        // given value available in first cache only
        whenever(cache.get()).then { "value" }

        // when we get the value
        val result = reuseInflightCache.get()

        // then we return the value
        verify(cache).get()
        assertEquals("value", result)
    }

    @Test
    fun `execute get only once whilst a call is in flight`() = runBlocking {
        val count = AtomicInteger(0)

        // given value available in first cache only
        whenever(cache.get()).then {
            runBlocking(coroutineContext) {
                delay(100)
            }

            count.getAndIncrement()
        }

        // when we get the same key 5 times
        val jobs = arrayListOf<Deferred<Any?>>()
        for (i in 1..5) {
            jobs.add(async { reuseInflightCache.get() })
        }
        jobs.forEach { it.await() }

        // then get is only called once
        assertEquals(1, count.get())
    }

    @Test
    fun `execute get twice if a call is made once the first one has finished`() = runBlocking {
        val count = AtomicInteger(0)

        // given value available in first cache only
        whenever(cache.get()).then {
            runBlocking(coroutineContext) {
                delay(100)
            }

            count.getAndIncrement()
        }

        reuseInflightCache.get()

        // we yield here as the map that stores the reuse may not have been cleared yet
        delay(100)

        // when we get the same key 5 times
        val jobs = arrayListOf<Deferred<Any?>>()
        for (i in 1..5) {
            jobs.add(async { reuseInflightCache.get() })
        }
        jobs.forEach { it.await() }

        // then get is only called once
        assertEquals(2, count.get())

    }

    @Test(expected = TestException::class)
    fun `propogate exception on get`() = runBlocking {
        // given value available in first cache only
        whenever(cache.get()).then { throw TestException() }

        // when we get the value
        reuseInflightCache.get()

        // then we throw an exception
        Unit
    }

    // set
    @Test
    fun `call set from cache`() = runBlocking {
        // given value available in first cache only
        whenever(cache.set("value")).then { "value" }

        // when we get the value
        reuseInflightCache.set("value")

        // then we return the value
        //Assert.assertEquals("value", result)
        verify(cache).set("value")
    }

    @Test(expected = TestException::class)
    fun `propagate exception on set`() = runBlocking {
        // given value available in first cache only
        whenever(cache.set("value")).then { throw TestException() }

        // when we get the value
        reuseInflightCache.set("value")

        // then we throw an exception
    }

    class TestException : Exception()
}
