package com.babylonhealth.certificatetransparency

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Enables TLS v1.2 when creating SSLSockets.
 *
 *
 * For some reason, android supports TLS v1.2 from API 16, but enables it by
 * default only from API 20.
 *
 * @link https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
 */
class Tls12SocketFactory(private val delegate: SSLSocketFactory) : SSLSocketFactory() {

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? {
        return delegate.createSocket(s, host, port, autoClose).patch()
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket? {
        return delegate.createSocket(host, port).patch()
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket? {
        return delegate.createSocket(host, port, localHost, localPort).patch()
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return delegate.createSocket(host, port).patch()
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket? {
        return delegate.createSocket(address, port, localAddress, localPort).patch()
    }

    private fun Socket.patch() = apply {
        if (this is SSLSocket) {
            enabledProtocols = TLS_V12_ONLY
        }
    }

    companion object {
        private val TLS_V12_ONLY = arrayOf("TLSv1.2")
    }
}
