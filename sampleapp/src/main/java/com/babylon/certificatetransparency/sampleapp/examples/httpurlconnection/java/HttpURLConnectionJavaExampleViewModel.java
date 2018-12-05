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

package com.babylon.certificatetransparency.sampleapp.examples.httpurlconnection.java;

import android.content.Context;
import com.babylon.certificatetransparency.sampleapp.examples.BaseExampleViewModel;
import com.babylon.certificatetransparency.HostnameVerifierBuilder;
import com.babylon.certificatetransparency.Logger;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

public class HttpURLConnectionJavaExampleViewModel extends BaseExampleViewModel {

    private final Context applicationContext;

    public HttpURLConnectionJavaExampleViewModel(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @NotNull
    @Override
    public String getSampleCodeTemplate() {
        return "httpurlconnection-java.txt";
    }

    private void enableCertificateTransparencyChecks(
            HttpURLConnection connection,
            Set<String> hosts,
            boolean isFailOnError,
            Logger defaultLogger
    ) {
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

            // Create a hostname verifier wrapping the original
            HostnameVerifierBuilder builder = new HostnameVerifierBuilder(httpsConnection.getHostnameVerifier())
                    .setFailOnError(isFailOnError)
                    .setLogger(defaultLogger);

            for (String host : hosts) {
                builder.addHost(host);
            }

            httpsConnection.setHostnameVerifier(builder.build());
        }
    }

    @Override
    public void openConnection(@NotNull String connectionHost, @NotNull Set<String> hosts, boolean isFailOnError, @NotNull Logger defaultLogger) {
        // Quick and dirty way to push the network call onto a background thread, don't do this is a real app
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL("https://" + connectionHost).openConnection();
                //noinspection unchecked
                enableCertificateTransparencyChecks(connection, hosts, isFailOnError, defaultLogger);

                connection.connect();
            } catch (IOException e) {
                sendException(e);
            }
        }).start();
    }
}
