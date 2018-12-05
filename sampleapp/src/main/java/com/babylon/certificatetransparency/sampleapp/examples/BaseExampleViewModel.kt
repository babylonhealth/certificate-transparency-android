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

package com.babylon.certificatetransparency.sampleapp.examples

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mustachejava.DefaultMustacheFactory
import okhttp3.HttpUrl
import org.certificatetransparency.ctlog.Logger
import org.certificatetransparency.ctlog.VerificationResult
import java.io.StringWriter
import javax.net.ssl.SSLPeerUnverifiedException

abstract class BaseExampleViewModel : ViewModel() {

    abstract val sampleCodeTemplate: String

    private var state = State(
        hosts = setOf("*.babylonhealth.com"),
        failOnError = true,
        sampleCode = "",
        message = null
    )

    private val _liveData = MutableLiveData<State>()

    init {
        updateSourceCode()
        _liveData.postValue(state)
    }

    val liveData: LiveData<State>
        get() = _liveData

    fun addHost(title: String) {
        state = if (isValidHost(title)) {
            state.copy(hosts = state.hosts.toMutableSet().apply { add(title) }.toSet())
        } else {
            state.copy(message = State.Message.Failure("Invalid host"))
        }

        updateSourceCode()
        _liveData.postValue(state)
    }

    fun removeHost(title: String) {
        state = state.copy(hosts = state.hosts.toMutableSet().apply { remove(title) }.toSet())
        updateSourceCode()
        _liveData.postValue(state)
    }

    fun dismissMessage() {
        state = state.copy(message = null)
        _liveData.postValue(state)
    }

    private fun generateSourceCode(hosts: Set<String>, failOnError: Boolean): String {
        val scopes = mapOf("hosts" to hosts.toSet(), "failOnError" to failOnError)

        return StringWriter().use {
            DefaultMustacheFactory().compile(sampleCodeTemplate).execute(it, scopes)
        }.toString()
    }

    private fun updateSourceCode() {
        val source = generateSourceCode(state.hosts, state.failOnError)
        state = state.copy(sampleCode = source)
    }

    fun setFailOnError(failOnError: Boolean) {
        state = state.copy(failOnError = failOnError)
        updateSourceCode()
        _liveData.postValue(state)
    }

    private val defaultLogger = object : Logger {
        override fun log(host: String, result: VerificationResult) {
            val message = when (result) {
                is VerificationResult.Success -> State.Message.Success(result.toString())
                is VerificationResult.Failure -> State.Message.Failure(result.toString())
            }

            state = state.copy(message = message)
            _liveData.postValue(state)
        }
    }

    fun sendException(e: Throwable?) {
        if (e?.message != "Certificate transparency failed" && e?.cause !is SSLPeerUnverifiedException) {
            state = state.copy(message = State.Message.Failure(e?.message ?: e.toString()))
            _liveData.postValue(state)
        }
    }

    abstract fun openConnection(connectionHost: String, hosts: Set<String>, isFailOnError: Boolean, defaultLogger: Logger)

    fun openConnection(connectionHost: String) {
        openConnection(connectionHost, state.hosts, state.failOnError, defaultLogger)
    }

    companion object {
        private const val WILDCARD = "*."

        private fun isValidHost(pattern: CharSequence): Boolean {
            val host = if (pattern.startsWith(WILDCARD)) {
                HttpUrl.parse("http://" + pattern.substring(WILDCARD.length))?.host()
            } else {
                HttpUrl.parse("http://$pattern")?.host()
            }

            return host != null
        }
    }
}
