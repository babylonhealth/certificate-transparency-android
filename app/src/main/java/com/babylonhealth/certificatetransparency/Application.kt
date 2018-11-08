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

package com.babylonhealth.certificatetransparency

import androidx.multidex.MultiDexApplication
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller

// Used by AndroidManifest.xml
@Suppress("unused")
class Application : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        updateSecurityProvider()
    }

    /**
     * See https://developer.android.com/training/articles/security-gms-provider. This also updates the ciphers that can
     * be used for an HTTPS connection. For example old devices would not be able to connect to our backend because
     * there aren't any common ciphers between the device and the backend. All ciphers available in our backend exist
     * in Android devices from API 20 onwards. Note that there's an asynchronous method as well, but we are using the
     * synchronous one to make sure this is applied on time, before the http stack is initialized inside the dagger
     * graph.
     */
    private fun updateSecurityProvider() {
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: GooglePlayServicesRepairableException) {
            println("Failed to update security provider")
        } catch (e: GooglePlayServicesNotAvailableException) {
            println("Failed to update security provider")
        }
    }
}
