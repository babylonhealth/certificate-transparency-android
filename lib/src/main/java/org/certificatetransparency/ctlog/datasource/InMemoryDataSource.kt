package org.certificatetransparency.ctlog.datasource

import kotlinx.coroutines.GlobalScope

class InMemoryDataSource<Value : Any> : DataSource<Value> {
    private var cachedValue: Value? = null

    override suspend fun get(): Value? = cachedValue

    override suspend fun set(value: Value) {
        cachedValue = value
    }

    override val coroutineContext = GlobalScope.coroutineContext
}
