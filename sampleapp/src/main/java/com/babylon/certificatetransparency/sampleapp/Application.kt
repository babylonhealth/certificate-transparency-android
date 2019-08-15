/*
 * Copyright 2019 Babylon Partners Limited
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

package com.babylon.certificatetransparency.sampleapp

import androidx.multidex.*
import com.google.android.gms.common.*
import com.google.android.gms.security.*

// Used by AndroidManifest.xml
@Suppress("unused")
class Application : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        updateSecurityProvider()
        instance = this
    }

    /**
     * See https://developer.android.com/training/articles/security-gms-provider
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

    companion object {
        lateinit var instance: Application
            private set
    }
}
