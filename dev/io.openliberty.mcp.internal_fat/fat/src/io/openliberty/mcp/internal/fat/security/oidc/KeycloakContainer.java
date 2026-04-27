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

import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.Duration;

import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.SimpleLogConsumer;
import componenttest.topology.impl.LibertyServer;

/**
 * A local {@link Testcontainers} implementation for the Keycloak IDP used by MCP FATs.
 */
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {
    private static final Class<?> CLASS = KeycloakContainer.class;

    private static final String IMAGE_NAME = "jboss/keycloak";
    private static final String IMAGE_VERSION = "16.1.1";

    private static final int HTTP_PORT = 8080;
    private static final int HTTPS_PORT = 8443;

    static final String DEFAULT_REALM = "master";
    public static final String TEST_REALM = "TestRealm";

    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASS = "password";

    private final KeycloakAdmin keycloakAdmin;

    public KeycloakContainer() {
        this(IMAGE_NAME + ":" + IMAGE_VERSION);
    }

    public KeycloakContainer(String imageName) {
        super(imageName);

        withEnv("KEYCLOAK_USER", ADMIN_USER);
        withEnv("KEYCLOAK_PASSWORD", ADMIN_PASS);
        withEnv("DB_VENDOR", "h2");

        withLogConsumer(new SimpleLogConsumer(CLASS, "KEYCLOAK"));
        withExposedPorts(HTTPS_PORT, HTTP_PORT);

        WaitAllStrategy strategy = new WaitAllStrategy(WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT);
        strategy.withStartupTimeout(Duration.ofMinutes(2));
        strategy.withStrategy(Wait.forListeningPort());
        strategy.withStrategy(Wait.forHttp("/").forPort(HTTP_PORT).forStatusCode(200));
        waitingFor(strategy);

        keycloakAdmin = new KeycloakAdmin(this);
    }

    @Override
    public void start() {
        Log.info(CLASS, "start", "Starting " + CLASS.getName() + " testcontainer...");
        try {
            super.start();
            Log.info(CLASS, "start", CLASS.getName() + " testcontainer started.");
        } catch (RuntimeException e) {
            Log.error(CLASS, "start", e, CLASS.getName() + " testcontainer failed to start.");
            throw e;
        }

        long end = System.currentTimeMillis() + 60000;
        while (true) {
            try {
                keycloakAdmin.createRealm(TEST_REALM);
                break;
            } catch (Exception e) {
                if (System.currentTimeMillis() > end) {
                    Log.error(CLASS, "start", e, CLASS.getName() + " failed to create realm " + TEST_REALM + ".");
                    throw new RuntimeException("Failed to create realm " + TEST_REALM, e);
                }
                Log.error(CLASS, "start", e, CLASS.getName() + " failed to create realm " + TEST_REALM + ". Retrying in 5 seconds...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    // Ignore.
                }
            }
        }
    }

    public Integer getRemoteHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    public Integer getRemoteHttpsPort() {
        return getMappedPort(HTTPS_PORT);
    }

    public KeycloakAdmin getKeycloakAdmin() {
        return keycloakAdmin;
    }

    public String getRootHttpEndpoint() {
        return "http://" + getHost() + ":" + getRemoteHttpPort() + "/auth";
    }

    public String getRootHttpsEndpoint() {
        return "https://" + getHost() + ":" + getRemoteHttpsPort() + "/auth";
    }

    public void createTrustFromKeycloak(String password, LibertyServer... servers) throws Exception {
        try {
            Certificate[] certs = HttpUtils.getServerCertificates(getRootHttpsEndpoint());

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setEntry("keycloak", new KeyStore.TrustedCertificateEntry(certs[0]), null);

            for (LibertyServer server : servers) {
                try (FileOutputStream output = new FileOutputStream(server.getServerRoot() + "/resources/security/truststore.p12")) {
                    ks.store(output, password.toCharArray());
                }
            }
        } catch (Exception e) {
            throw new Exception("Failed to generate truststore with Keycloak certificate.", e);
        }
    }
}

// Made with Bob
