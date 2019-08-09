package com.babylon.certificaterevocation

import java.io.IOException
import java.security.cert.X509Certificate

/**
 * Abstract class providing the results of performing certificate revocation checks
 */
sealed class RevocationResult {
    /**
     * Abstract class representing certificate revocation checks passed
     */
    sealed class Success : RevocationResult() {

        /**
         * Certificate revocation checks passed
         */
        object Trusted : Success() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Success: Certificates not in revocation list"
        }

        /**
         * Insecure connection so no certificate to check revocation
         */
        object InsecureConnection : Success() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Success: Revocation not enabled for insecure connection"
        }
    }

    /**
     * Abstract class representing certificate revocation checks failed
     */
    sealed class Failure : RevocationResult() {

        /**
         * Certificate revocation checks failed as no certificates are present
         */
        object NoCertificates : Failure() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Failure: No certificates"
        }

        /**
         * Certificate revocation checks failed as server not trusted
         */
        data class CertificateRevoked(val certificate: X509Certificate) : Failure() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Failure: Certificate is revoked"
        }

        /**
         * Certificate revocation checks failed due to an unknown [IOException]
         * @property ioException The [IOException] that occurred
         */
        data class UnknownIoException(val ioException: IOException) : Failure() {
            /**
             * Returns a string representation of the object.
             */
            override fun toString() = "Failure: IOException $ioException"
        }
    }
}
