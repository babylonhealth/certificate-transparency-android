package com.babylonhealth.certificatetransparency.sampleapp.examples.okhttp.java;

import com.babylonhealth.certificatetransparency.sampleapp.examples.BaseExampleViewModel;
import okhttp3.*;
import org.certificatetransparency.ctlog.InterceptorBuilder;
import org.certificatetransparency.ctlog.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;

public class OkHttpJavaExampleViewModel extends BaseExampleViewModel {
    // A normal client would create this ahead of time and share it between network requests
    // We create it dynamically as we allow the user to set the hosts for certificate transparency
    private OkHttpClient createOkHttpClient(Set<String> hosts, boolean isFailOnError, Logger defaultLogger) {
        // Create a network interceptor
        InterceptorBuilder builder = new InterceptorBuilder()
                .setFailOnError(isFailOnError)
                .setLogger(defaultLogger);

        for (String host : hosts) {
            builder.addHost(host);
        }

        Interceptor networkInterceptor = builder.build();

        // Set the interceptor when creating the OkHttp client
        return new OkHttpClient.Builder()
                .addNetworkInterceptor(networkInterceptor)
                .build();
    }

    @Override
    public void openConnection(@NotNull String connectionHost, @NotNull Set<String> hosts, boolean isFailOnError, @NotNull Logger defaultLogger) {
        OkHttpClient client = createOkHttpClient(hosts, isFailOnError, defaultLogger);

        Request request = new Request.Builder().url("https://$connectionHost").build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                // Failure. Send message to the UI as logger won't catch generic network exceptions
                sendException(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                // Success. Reason will have been sent to the logger
            }
        });
    }

    @NotNull
    @Override
    public String generateSourceCode(@NotNull Set<String> hosts, boolean failOnError) {
        StringBuilder bob = new StringBuilder();

        bob.append("InterceptorBuilder builder = new InterceptorBuilder()");

        for (String host : hosts) {
            bob.append("\n        .addHost(\"");
            bob.append(host);
            bob.append("\")");
        }

        if (!failOnError) {
            bob.append("\n        .setFailOnError(false)");
        }
        bob.append(";\n\n");


        bob.append("OkHttpClient client = new OkHttpClient.Builder()\n");
        bob.append("        .addNetworkInterceptor(networkInterceptor)\n");
        bob.append("        .build();");

        return bob.toString();
    }
}
