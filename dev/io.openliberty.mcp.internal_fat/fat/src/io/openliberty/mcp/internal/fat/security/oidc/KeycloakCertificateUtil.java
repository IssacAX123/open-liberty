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

import static org.junit.Assert.assertNotNull;

import java.security.cert.Certificate;
import java.util.Arrays;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Certificate helper for local Keycloak FAT support.
 */
public final class KeycloakCertificateUtil {

    private static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";

    private KeycloakCertificateUtil() {
        // Utility class.
    }

    public static Certificate[] getServerCertificates(String endpoint) throws Exception {
        final String methodName = "getServerCertificates";
        try (CloseableHttpClient httpclient = getInsecureHttpsClient()) {
            HttpGet httpGet = new HttpGet(endpoint);
            HttpContext context = new BasicHttpContext();
            try (CloseableHttpResponse response = httpclient.execute(httpGet, context)) {
                logHttpResponse(methodName, httpGet, response);
                Certificate[] certificates = (Certificate[]) context.getAttribute(PEER_CERTIFICATES);
                Log.info(KeycloakCertificateUtil.class, methodName, "Certificates: " + Arrays.toString(certificates));
                assertNotNull("Expected TLS certificates in the HttpContext.", certificates);
                return certificates;
            }
        }
    }

    private static CloseableHttpClient getInsecureHttpsClient() throws Exception {
        HttpRequestInterceptor certificateInterceptor = (request, context) -> {
            ManagedHttpClientConnection connection = (ManagedHttpClientConnection) context.getAttribute("http.connection");
            if (connection != null && connection.getSSLSession() != null) {
                context.setAttribute(PEER_CERTIFICATES, connection.getSSLSession().getPeerCertificates());
            }
        };
        return KeycloakSupportUtils.getInsecureHttpClient()
                        .getClass()
                        .cast(org.apache.http.impl.client.HttpClientBuilder.create()
                                        .setSSLHostnameVerifier(new NoopHostnameVerifier())
                                        .addInterceptorLast(certificateInterceptor)
                                        .build());
    }

    private static void logHttpResponse(String methodName, HttpRequestBase request, CloseableHttpResponse response) {
        Log.info(KeycloakCertificateUtil.class, methodName,
                        request.getMethod() + " " + request.getURI() + " ---> "
                                        + response.getStatusLine().getStatusCode() + " "
                                        + response.getStatusLine().getReasonPhrase());
    }
}

// Made with Bob
