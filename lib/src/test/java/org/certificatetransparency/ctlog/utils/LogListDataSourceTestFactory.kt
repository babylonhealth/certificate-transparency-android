package org.certificatetransparency.ctlog.utils

import com.google.gson.GsonBuilder
import kotlinx.coroutines.GlobalScope
import org.certificatetransparency.ctlog.Base64
import org.certificatetransparency.ctlog.LogInfo
import org.certificatetransparency.ctlog.PublicKeyFactory
import org.certificatetransparency.ctlog.TestData
import org.certificatetransparency.ctlog.data.loglist.model.LogList
import org.certificatetransparency.ctlog.data.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.domain.datasource.DataSource
import java.security.MessageDigest

object LogListDataSourceTestFactory {

    val logListDataSource: DataSource<Map<String, LogSignatureVerifier>> by lazy {
        val hasher = MessageDigest.getInstance("SHA-256")

        // Collection of CT logs that are trusted from https://www.gstatic.com/ct/log_list/log_list.json
        val json = TestData.file(TestData.TEST_LOG_LIST_JSON).readText()
        val trustedLogKeys = GsonBuilder().create().fromJson(json, LogList::class.java).logs.map { it.key }

        val map = trustedLogKeys.map { Base64.decode(it) }.associateBy({
            Base64.toBase64String(hasher.digest(it))
        }) {
            LogSignatureVerifier(LogInfo(PublicKeyFactory.fromByteArray(it)))
        }

        object : DataSource<Map<String, LogSignatureVerifier>> {
            override suspend fun get() = map

            override suspend fun set(value: Map<String, LogSignatureVerifier>) = Unit

            override val coroutineContext = GlobalScope.coroutineContext
        }
    }

    val emptySource: DataSource<Map<String, LogSignatureVerifier>> by lazy {
        object : DataSource<Map<String, LogSignatureVerifier>> {
            override suspend fun get() = emptyMap<String, LogSignatureVerifier>()

            override suspend fun set(value: Map<String, LogSignatureVerifier>) = Unit

            override val coroutineContext = GlobalScope.coroutineContext
        }
    }

    val nullSource: DataSource<Map<String, LogSignatureVerifier>> by lazy {
        object : DataSource<Map<String, LogSignatureVerifier>> {
            override suspend fun get() = null

            override suspend fun set(value: Map<String, LogSignatureVerifier>) = Unit

            override val coroutineContext = GlobalScope.coroutineContext
        }
    }
}
