package org.certificatetransparency.ctlog.data.loglist

import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.domain.datasource.DataSource
import retrofit2.Retrofit

internal object LogListDataSourceFactory {
    fun create(): DataSource<Map<String, LogSignatureVerifier>> {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.gstatic.com/ct/log_list/")
            .build()

        val logService = retrofit.create(LogListService::class.java)

        return InMemoryDataSource<Map<String, LogSignatureVerifier>>()
            .compose(LogListNetworkDataSource(logService).oneWayTransform { logServers -> logServers.associateBy({ it.logId }) { it.verifier } })
            .reuseInflight()
    }
}
