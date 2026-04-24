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

public class ProtectedResourceMetadataResolver {

    private static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource";

    public String toWellKnownPath(String protectedResourcePath) {
        return WELL_KNOWN_PREFIX + "/" + toWellKnownSuffix(protectedResourcePath);
    }

    public String toWellKnownSuffix(String protectedResourcePath) {
        if (protectedResourcePath == null || protectedResourcePath.isEmpty()) {
            return "";
        }
        if (protectedResourcePath.startsWith("/")) {
            return protectedResourcePath.substring(1);
        }
        return protectedResourcePath;
    }
}

// Made with Bob
