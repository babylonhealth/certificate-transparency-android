package org.certificatetransparency.ctlog.okhttp

import org.certificatetransparency.ctlog.LogSignatureVerifier
import org.certificatetransparency.ctlog.data.loglist.LogListNetworkDataSource
import org.certificatetransparency.ctlog.data.loglist.LogListService
import org.certificatetransparency.ctlog.domain.datasource.DataSource
import org.certificatetransparency.ctlog.domain.datasource.InMemoryDataSource
import retrofit2.Retrofit

object LogListDataSourceFactory {
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
