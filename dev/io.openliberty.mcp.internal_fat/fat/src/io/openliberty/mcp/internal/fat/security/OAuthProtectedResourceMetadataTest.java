/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.security;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.jcache.internal.fat.docker.KeycloakContainer.TEST_REALM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.OpenidConnectClient;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.jcache.internal.fat.docker.KeycloakContainer;
import io.openliberty.mcp.internal.fat.tool.securityApps.AdminsRoleTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.McpClient.RawResponse;

@RunWith(FATRunner.class)
public class OAuthProtectedResourceMetadataTest {

    private static final String TRUSTSTORE_PASSWORD = "trustPassword";
    private static final String ENABLED_CLIENT_ID = "mcp-client";
    private static final String DISABLED_CLIENT_ID = "mcp-client-disabled";
    private static final String ENABLED_APP_NAME = "adminsRoleTools";
    private static final String DISABLED_APP_NAME = "disabledMetadataTest";
    private static final String ENABLED_METADATA_SUFFIX = "/.well-known/oauth-protected-resource/adminsRoleTools/mcp";
    private static final String DISABLED_METADATA_PATH = "/.well-known/oauth-protected-resource/disabledMetadataTest/mcp";
    private static final String ENABLED_METADATA_RESOURCE = "https://localhost/adminsRoleTools/mcp";

    @Server("mcp-server-auth")
    public static LibertyServer server;

    private static KeycloakContainer keycloak;
    private static String enabledClientRegistrationId;
    private static String disabledClientRegistrationId;

    @Rule
    public McpClient client = new McpClient(server, "/adminsRoleTools");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive enabledWar = ShrinkWrap.create(WebArchive.class, ENABLED_APP_NAME + ".war").addClass(AdminsRoleTools.class);
        ShrinkHelper.exportDropinAppToServer(server, enabledWar, SERVER_ONLY);

        WebArchive disabledWar = ShrinkWrap.create(WebArchive.class, DISABLED_APP_NAME + ".war").addClass(AdminsRoleTools.class);
        ShrinkHelper.exportDropinAppToServer(server, disabledWar, SERVER_ONLY);

        keycloak = new KeycloakContainer();
        keycloak.start();
        keycloak.createTrustFromKeycloak(TRUSTSTORE_PASSWORD, server);

        enabledClientRegistrationId = keycloak.getKeycloakAdmin().registerOAuth20Client(server, ENABLED_CLIENT_ID, TEST_REALM, "mcpProtectedResourceMetadataClient");
        disabledClientRegistrationId = keycloak.getKeycloakAdmin().registerOAuth20Client(server, DISABLED_CLIENT_ID, TEST_REALM, "mcpProtectedResourceMetadataDisabledClient");
        String clientSecret = keycloak.getKeycloakAdmin().getClientSecret(TEST_REALM, ENABLED_CLIENT_ID);
        String disabledClientSecret = keycloak.getKeycloakAdmin().getClientSecret(TEST_REALM, DISABLED_CLIENT_ID);
        String discoveryEndpoint = keycloak.getKeycloakAdmin().getOidcDiscoveryEndpoint(TEST_REALM);

        ServerConfiguration config = server.getServerConfiguration().clone();
        OpenidConnectClient enabledClient = config.getOpenidConnectClients().get(0);
        enabledClient.setClientSecret(clientSecret);
        enabledClient.setDiscoveryEndpointUrl(discoveryEndpoint);

        OpenidConnectClient disabledClient = config.getOpenidConnectClients().get(1);
        disabledClient.setClientSecret(disabledClientSecret);
        disabledClient.setDiscoveryEndpointUrl(discoveryEndpoint);

        server.updateServerConfiguration(config);
        server.startServer();
        server.waitForStringInLog("CWWKS0008I.*");
        server.waitForStringInLog("CWWKF0011I.*");
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(ENABLED_APP_NAME));
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        } finally {
            try {
                if (keycloak != null && disabledClientRegistrationId != null) {
                    keycloak.getKeycloakAdmin().deleteClient(TEST_REALM, disabledClientRegistrationId);
                }
            } finally {
                try {
                    if (keycloak != null && enabledClientRegistrationId != null) {
                        keycloak.getKeycloakAdmin().deleteClient(TEST_REALM, enabledClientRegistrationId);
                    }
                } finally {
                    if (keycloak != null) {
                        keycloak.stop();
                    }
                }
            }
        }
    }

    @Test
    public void unauthorizedRequestIncludesResourceMetadataHeaderWhenEnabled() throws Exception {
        String request = AuthHelper.buildUnauthorizedEchoRequest();
        RawResponse response = client.callMcpRaw(request, 401);
        String header = response.getWwwAuthenticateHeader();

        assertNotNull(header);
        AuthHelper.assertResourceMetadataHeaderPresent(header, ENABLED_METADATA_SUFFIX);
    }

    @Test
    public void unauthorizedRequestOmitsResourceMetadataHeaderWhenDisabled() throws Exception {
        String request = AuthHelper.buildUnauthorizedEchoRequestForPath("/disabledMetadataTest");
        RawResponse response = new McpClient(server, "/disabledMetadataTest").callMcpRaw(request, 401);
        String header = response.getWwwAuthenticateHeader();

        assertNotNull(header);
        AuthHelper.assertResourceMetadataHeaderAbsent(header);
    }

    @Test
    public void wellKnownEndpointReturnsProtectedResourceMetadataWhenEnabled() throws Exception {
        RawResponse response = client.getRawConnection(ENABLED_METADATA_SUFFIX, 200);

        JSONAssert.assertEquals("""
            {
              "resource":"ENABLED_RESOURCE",
              "authorization_servers":["KEYCLOAK_PROVIDER_URI"]
            }
            """.replace("ENABLED_RESOURCE", ENABLED_METADATA_RESOURCE)
                .replace("KEYCLOAK_PROVIDER_URI", keycloak.getRootHttpsEndpoint() + "/realms/" + TEST_REALM),
                response.getBody(),
                false);
    }

    @Test
    public void wellKnownEndpointReturns404WhenMetadataDisabled() throws Exception {
        RawResponse response = client.getRawConnection(DISABLED_METADATA_PATH, 404);
        assertEquals(404, response.getResponseCode());
    }
}

// Made with Bob
