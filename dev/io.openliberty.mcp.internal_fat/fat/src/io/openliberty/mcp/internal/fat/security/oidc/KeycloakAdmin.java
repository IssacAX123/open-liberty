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

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Minimal local Keycloak admin REST helper for MCP FATs.
 */
@SuppressWarnings("restriction")
public class KeycloakAdmin {

    private static final Class<?> CLASS = KeycloakAdmin.class;

    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_ID = "id";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    public static final String ADMIN_CLIENT_ID = "admin-cli";

    public static final Map<String, String> BEARER_TOKENS_BY_REALM = new HashMap<String, String>();

    private final KeycloakContainer keycloak;

    public KeycloakAdmin(KeycloakContainer keycloak) {
        this.keycloak = keycloak;
    }

    private void addAdminBearerToken(HttpRequestBase request) throws Exception {
        request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminCliBearerToken()));
    }

    private HttpDelete buildHttpDeleteRequest(String endpoint) throws Exception {
        HttpDelete request = new HttpDelete(endpoint);
        addAdminBearerToken(request);
        return request;
    }

    private HttpGet buildHttpGetRequest(String endpoint, String... params) throws Exception {
        URIBuilder builder = new URIBuilder(endpoint);
        if (params != null && params.length > 0) {
            for (int idx = 0; idx < params.length; idx = idx + 2) {
                builder.addParameter(params[idx], params[idx + 1]);
            }
        }

        HttpGet request = new HttpGet(builder.build());
        addAdminBearerToken(request);
        return request;
    }

    private HttpPost buildHttpPostRequest(String endpoint, String jsonContent) throws Exception {
        HttpPost request = new HttpPost(endpoint);
        addAdminBearerToken(request);
        request.setEntity(new StringEntity(jsonContent, ContentType.APPLICATION_JSON));
        return request;
    }

    private String getAdminCliBearerToken() throws Exception {
        final String methodName = "getAdminCliBearerToken";

        HttpPost request = new HttpPost(getBearerTokenEndpoint(KeycloakContainer.DEFAULT_REALM));
        request.setEntity(new StringEntity("username=" + KeycloakContainer.ADMIN_USER
                        + "&password=" + KeycloakContainer.ADMIN_PASS
                        + "&grant_type=password&client_id=" + ADMIN_CLIENT_ID,
                        ContentType.APPLICATION_FORM_URLENCODED));

        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, methodName, "Requesting bearer token from " + request.getURI());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to retrieve bearer token for " + ADMIN_CLIENT_ID + ".");
                }
                String bearerToken = KeycloakUtils.getJsonObject(KeycloakUtils.getStringResponse(response)).getString(KEY_ACCESS_TOKEN);
                Log.info(CLASS, methodName, "Retrieved bearer token: " + bearerToken);
                return bearerToken;
            }
        }
    }

    private String getAdminRESTEndpoint() {
        return keycloak.getRootHttpsEndpoint() + "/admin/realms";
    }

    private String getBearerTokenEndpoint(String realm) {
        return keycloak.getRootHttpsEndpoint() + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public JsonObject getClient(String realm, String clientId) throws Exception {
        final String methodName = "getClient";

        HttpGet request = buildHttpGetRequest(getClientsRestEndpoint(realm), KEY_CLIENT_ID, clientId);

        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, methodName, "Requesting clients from " + request.getURI());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    return null;
                }

                JsonArray clients = KeycloakUtils.getJsonArray(KeycloakUtils.getStringResponse(response));
                if (clients.size() != 1) {
                    return null;
                }
                return clients.getJsonObject(0);
            }
        }
    }

    private String getClientsRestEndpoint(String realm) {
        return getAdminRESTEndpoint() + "/" + realm + "/clients";
    }

    public String registerOAuth20Client(LibertyServer libertyServer, String clientId, String realm, String oauth2LoginId) throws Exception {
        final String methodName = "registerOAuth20Client";

        String redirectHttpsUri = "https://" + libertyServer.getHostname() + ":" + libertyServer.getHttpDefaultSecurePort() + "/*";

        JsonArray redirectUris = Json.createArrayBuilder().add(redirectHttpsUri).build();
        JsonObject clientRep = Json.createObjectBuilder()
                        .add("clientId", clientId)
                        .add("protocol", "openid-connect")
                        .add("redirectUris", redirectUris)
                        .add("clientAuthenticatorType", "client-secret")
                        .add("publicClient", false)
                        .build();

        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            HttpPost postRequest = buildHttpPostRequest(getAdminRESTEndpoint() + "/" + realm + "/clients", clientRep.toString());

            Log.info(CLASS, methodName, "Posting client representation to " + postRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to create client " + clientId + ".");
                }
                KeycloakUtils.getStringResponse(response);
            }

            JsonObject client = getClient(realm, clientId);
            if (client == null) {
                throw new Exception("Unable to find created client " + clientId + ".");
            }
            return client.getString(KEY_ID);
        }
    }

    public String getClientSecret(String realm, String clientId) throws Exception {
        final String methodName = "getClientSecret";

        JsonObject client = getClient(realm, clientId);
        if (client == null) {
            throw new Exception("Unable to find client " + clientId + ".");
        }

        String id = client.getString(KEY_ID);
        String uri = getAdminRESTEndpoint() + "/" + realm + "/clients/" + id + "/client-secret";
        HttpGet getRequest = buildHttpGetRequest(uri);

        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            Log.info(CLASS, methodName, "Retrieving client secret from " + getRequest.getURI());
            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to retrieve secret for client " + clientId);
                }
                return KeycloakUtils.getJsonObject(KeycloakUtils.getStringResponse(response)).getString("value");
            }
        }
    }

    public String getOidcDiscoveryEndpoint(String realm) {
        return keycloak.getRootHttpsEndpoint() + "/realms/" + realm + "/.well-known/openid-configuration";
    }

    public void createRealm(String realm) throws Exception {
        final String methodName = "createRealm";

        JsonObject body = Json.createObjectBuilder()
                        .add("id", realm)
                        .add("realm", realm)
                        .add("displayName", "Keycloak")
                        .add("displayNameHtml", "<div class=\"kc-logo-text\"><span>Keycloak</span></div>")
                        .add("enabled", "true")
                        .build();

        HttpPost request = new HttpPost(getAdminRESTEndpoint());
        addAdminBearerToken(request);
        request.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

        Log.info(CLASS, methodName, "Requesting to create realm " + realm + " at URL " + request.getURI());
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode()) {
                    throw createResponseException(response, "Unable to create new realm. ");
                }
            }
        }
    }

    public boolean deleteClient(String realm, String id) throws Exception {
        final String methodName = "deleteClient";

        HttpDelete request = buildHttpDeleteRequest(getClientsRestEndpoint(realm) + "/" + id);

        Log.info(CLASS, methodName, "Requesting to delete client at URL " + request.getURI());
        try (CloseableHttpClient httpClient = KeycloakUtils.getInsecureHttpClient()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return HttpStatus.SC_OK == response.getStatusLine().getStatusCode()
                                || HttpStatus.SC_NO_CONTENT == response.getStatusLine().getStatusCode();
            }
        }
    }

    private static Exception createResponseException(CloseableHttpResponse response, String msg) throws Exception {
        String errorMsg = msg + " HTTP status code: " + response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase();

        String contents = KeycloakUtils.getStringResponse(response);
        if (contents != null && !contents.isEmpty()) {
            errorMsg += "\n\n" + contents;
        }

        return new Exception(errorMsg);
    }
}

// Made with Bob
