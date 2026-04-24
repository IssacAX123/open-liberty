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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ProtectedResourceMetadataResolverTest {

    @Test
    public void resolvesWellKnownPathFromProtectedResourcePath() {
        ProtectedResourceMetadataResolver resolver = new ProtectedResourceMetadataResolver();

        String metadataPath = resolver.toWellKnownPath("/inventory/api");

        assertEquals("/.well-known/oauth-protected-resource/inventory/api", metadataPath);
    }

    @Test
    public void stripsLeadingSlashWhenBuildingWellKnownSuffix() {
        ProtectedResourceMetadataResolver resolver = new ProtectedResourceMetadataResolver();

        assertEquals("inventory/api", resolver.toWellKnownSuffix("/inventory/api"));
    }

    @Test
    public void disabledMetadataIsNotPublished() {
        ProtectedResourceMetadata metadata = ProtectedResourceMetadata.builder()
                        .resource("https://example.com/inventory/api")
                        .protectedResourcePath("/inventory/api")
                        .enabled(false)
                        .build();

        assertFalse(metadata.isEnabled());
    }
}
