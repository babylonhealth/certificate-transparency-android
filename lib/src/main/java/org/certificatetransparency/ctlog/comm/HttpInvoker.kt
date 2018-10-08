package org.certificatetransparency.ctlog.comm

import org.apache.http.NameValuePair
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.impl.client.HttpClientBuilder
import java.io.IOException

/** Simple delegator to HttpClient, so it can be mocked  */
class HttpInvoker {
    /**
     * Make an HTTP POST method call to the given URL with the provided JSON payload.
     *
     * @param url URL for POST method
     * @param jsonPayload Serialized JSON payload.
     * @return Server's response body.
     */
    fun makePostRequest(url: String, jsonPayload: String): String {
        try {
            HttpClientBuilder.create().build().use { httpClient ->
                val post = HttpPost(url)
                post.entity = StringEntity(jsonPayload, "utf-8")
                post.addHeader("Content-Type", "application/json; charset=utf-8")

                return httpClient.execute(post, BasicResponseHandler())
            }
        } catch (e: IOException) {
            throw LogCommunicationException("Error making POST request to $url", e)
        }
    }

    /**
     * Makes an HTTP GET method call to the given URL with the provides parameters.
     *
     * @param url URL for GET method.
     * @param params query parameters.
     * @return Server's response body.
     */
    @JvmOverloads
    fun makeGetRequest(url: String, params: List<NameValuePair>? = null): String {
        try {
            HttpClientBuilder.create().build().use { httpClient ->
                var paramsStr = ""
                if (params != null) {
                    paramsStr = "?${URLEncodedUtils.format(params, "UTF-8")}"
                }
                val get = HttpGet(url + paramsStr)
                get.addHeader("Content-Type", "application/json; charset=utf-8")

                return httpClient.execute(get, BasicResponseHandler())
            }
        } catch (e: IOException) {
            throw LogCommunicationException("Error making GET request to $url", e)
        }
    }
}
