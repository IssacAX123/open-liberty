/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.security.oidc;

import static junit.framework.Assert.assertNotNull;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContextBuilder;

import com.ibm.websphere.simplicity.log.Log;

/**
 * HTTP convenience methods for local Keycloak FAT support.
 */
public class HttpUtils {
    public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";

    public static CloseableHttpClient getInsecureHttpsClient() throws Exception {
        HttpResponseInterceptor certificateInterceptor = (httpResponse, context) -> {
            if (context != null) {
                ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection) context
                                .getAttribute(HttpCoreContext.HTTP_CONNECTION);
                try {
                    SSLSession sslSession = routedConnection.getSSLSession();
                    if (sslSession != null) {
                        Certificate[] certificates = sslSession.getPeerCertificates();
                        if (certificates != null) {
                            context.setAttribute(PEER_CERTIFICATES, certificates);
                        }
                    }
                } catch (ConnectionShutdownException e) {
                    Log.warning(HttpUtils.class,
                                    "Unable to save the connection's TLS certificates to the HTTP context since the connection was closed.");
                }
            }
        };

        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(MyTrustAllStrategy.INSTANCE).build();
        SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        return HttpClients.custom()
                        .setSSLSocketFactory(connectionFactory)
                        .addInterceptorLast(certificateInterceptor)
                        .build();
    }

    public static Certificate[] getServerCertificates(String endpoint) throws Exception {
        final String methodName = "getServerCertificates";

        try (CloseableHttpClient httpclient = getInsecureHttpsClient()) {
            HttpGet httpGet = new HttpGet(endpoint);
            HttpContext context = new BasicHttpContext();

            try (final CloseableHttpResponse response = httpclient.execute(httpGet, context)) {
                logHttpResponse(HttpUtils.class, methodName, httpGet, response);

                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    Log.info(HttpUtils.class, methodName, "Expected response 200, but received response: " + statusLine + ". " + response);
                }

                Certificate[] certificates = (Certificate[]) context.getAttribute(PEER_CERTIFICATES);
                Log.info(HttpUtils.class, methodName, "Certificates: " + Arrays.toString(certificates));
                assertNotNull("Expected there to be TLS certificates in the HttpContext. Did the connection abort before we could retrieve them?",
                                certificates);
                return certificates;
            }
        }
    }

    public static void logHttpResponse(Class<?> clazz, String methodName, HttpRequestBase request,
                    CloseableHttpResponse response) {
        StatusLine statusLine = response.getStatusLine();
        Log.info(clazz, methodName, request.getMethod() + " " + request.getURI() + " ---> " + statusLine.getStatusCode()
                        + " " + statusLine.getReasonPhrase());
    }

    private static class MyTrustAllStrategy implements TrustStrategy {
        public static final MyTrustAllStrategy INSTANCE = new MyTrustAllStrategy();

        @Override
        public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            return true;
        }
    }
}

// Made with Bob
