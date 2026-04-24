/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProtectedResourceMetadataJsonBuilderTest {

    @Test
    public void includesAuthorizationServersWhenPresent() {
        ProtectedResourceMetadata metadata = ProtectedResourceMetadata.builder()
                        .resource("https://example.com/inventory/api")
                        .protectedResourcePath("/inventory/api")
                        .enabled(true)
                        .authorizationServer("https://authz.example.test")
                        .build();

        String json = new ProtectedResourceMetadataJsonBuilder().toJson(metadata);

        assertTrue(json.contains("\"resource\":\"https://example.com/inventory/api\""));
        assertTrue(json.contains("\"authorization_servers\":[\"https://authz.example.test\"]"));
    }

    @Test
    public void omitsAuthorizationServersWhenAbsent() {
        ProtectedResourceMetadata metadata = ProtectedResourceMetadata.builder()
                        .resource("https://example.com/inventory/api")
                        .protectedResourcePath("/inventory/api")
                        .enabled(true)
                        .build();

        String json = new ProtectedResourceMetadataJsonBuilder().toJson(metadata);

        assertTrue(json.contains("\"resource\":\"https://example.com/inventory/api\""));
        assertFalse(json.contains("authorization_servers"));
    }
}

// Made with Bob
