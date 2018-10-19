package org.certificatetransparency.ctlog.utils

import com.google.gson.GsonBuilder
import kotlinx.coroutines.GlobalScope
import org.certificatetransparency.ctlog.TestData
import org.certificatetransparency.ctlog.datasource.DataSource
import org.certificatetransparency.ctlog.internal.loglist.model.LogList
import org.certificatetransparency.ctlog.internal.utils.Base64
import org.certificatetransparency.ctlog.internal.utils.PublicKeyFactory
import org.certificatetransparency.ctlog.internal.verifier.LogSignatureVerifier
import org.certificatetransparency.ctlog.internal.verifier.model.LogInfo
import org.certificatetransparency.ctlog.verifier.SignatureVerifier
import java.security.MessageDigest

object LogListDataSourceTestFactory {

    val logListDataSource: DataSource<Map<String, SignatureVerifier>> by lazy {
        val hasher = MessageDigest.getInstance("SHA-256")

        // Collection of CT logs that are trusted from https://www.gstatic.com/ct/log_list/log_list.json
        val json = TestData.file(TestData.TEST_LOG_LIST_JSON).readText()
        val trustedLogKeys = GsonBuilder().create().fromJson(json, LogList::class.java).logs.map { it.key }

        val map = trustedLogKeys.map { Base64.decode(it) }.associateBy({
            Base64.toBase64String(hasher.digest(it))
        }) {
            LogSignatureVerifier(LogInfo(PublicKeyFactory.fromByteArray(it)))
        }

        object : DataSource<Map<String, SignatureVerifier>> {
            override suspend fun get() = map

            override suspend fun set(value: Map<String, SignatureVerifier>) = Unit

            override val coroutineContext = GlobalScope.coroutineContext
        }
    }

    val emptySource: DataSource<Map<String, SignatureVerifier>> by lazy {
        object : DataSource<Map<String, SignatureVerifier>> {
            override suspend fun get() = emptyMap<String, SignatureVerifier>()

            override suspend fun set(value: Map<String, SignatureVerifier>) = Unit

            override val coroutineContext = GlobalScope.coroutineContext
        }
    }

    val nullSource: DataSource<Map<String, SignatureVerifier>> by lazy {
        object : DataSource<Map<String, SignatureVerifier>> {
            override suspend fun get() = null

            override suspend fun set(value: Map<String, SignatureVerifier>) = Unit

            override val coroutineContext = GlobalScope.coroutineContext
        }
    }
}
