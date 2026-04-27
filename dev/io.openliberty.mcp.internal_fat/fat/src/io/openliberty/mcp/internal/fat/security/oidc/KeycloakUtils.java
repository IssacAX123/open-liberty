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

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

/**
 * Local Keycloak FAT utility methods for the MCP module.
 */
@SuppressWarnings("restriction")
public final class KeycloakUtils {

    private KeycloakUtils() {
        // Utility class.
    }

    public static JsonArray getJsonArray(String json) {
        JsonReader jsonReader = null;
        try {
            jsonReader = Json.createReader(new StringReader(json));
            return jsonReader.readArray();
        } finally {
            if (jsonReader != null) {
                jsonReader.close();
            }
        }
    }

    public static JsonObject getJsonObject(String json) {
        JsonReader jsonReader = null;
        try {
            jsonReader = Json.createReader(new StringReader(json));
            return jsonReader.readObject();
        } finally {
            if (jsonReader != null) {
                jsonReader.close();
            }
        }
    }

    public static String getStringResponse(CloseableHttpResponse response) throws ParseException, IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return "";
        }
        Header encodingHeader = entity.getContentEncoding();
        Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charset.forName(encodingHeader.getValue());
        return EntityUtils.toString(entity, encoding);
    }

    public static CloseableHttpClient getInsecureHttpClient() throws Exception {
        SSLContext sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(null, (certificate, authType) -> true)
                        .build();
        return HttpClients.custom()
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(new NoopHostnameVerifier())
                        .build();
    }
}

// Made with Bob
