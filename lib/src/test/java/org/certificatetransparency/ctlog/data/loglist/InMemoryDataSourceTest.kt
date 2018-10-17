package org.certificatetransparency.ctlog.data.loglist

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryDataSourceTest {

    @Test
    fun emptyDataSourceReturnsNull() = runBlocking {
        // given a new data source
        val dataSource = InMemoryDataSource<String>()

        // when we get the current value
        val result = dataSource.get()

        // then it returns null
        assertNull(result)
    }

    @Test
    fun dataSourceReturnsSetValue() = runBlocking {
        // given a new data source populated with a value
        val dataSource = InMemoryDataSource<String>()
        dataSource.set("1")

        // when we get the current value
        val result = dataSource.get()

        // then it returns the value
        assertEquals("1", result)
    }

    @Test
    fun dataSourceReturnsLatestSetValue() = runBlocking {
        // given a new data source populated with a value
        val dataSource = InMemoryDataSource<String>()
        dataSource.set("1")

        // when we set a second value
        dataSource.set("2")

        // then the second value is returned when retrieved
        assertEquals("2", dataSource.get())
    }
}
