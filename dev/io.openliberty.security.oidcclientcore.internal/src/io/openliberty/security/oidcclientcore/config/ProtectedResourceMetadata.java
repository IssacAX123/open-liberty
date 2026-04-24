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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProtectedResourceMetadata {

    private final String resource;
    private final String protectedResourcePath;
    private final boolean enabled;
    private final List<String> authorizationServers;

    private ProtectedResourceMetadata(Builder builder) {
        this.resource = builder.resource;
        this.protectedResourcePath = builder.protectedResourcePath;
        this.enabled = builder.enabled;
        this.authorizationServers = Collections.unmodifiableList(new ArrayList<String>(builder.authorizationServers));
    }

    public String getResource() {
        return resource;
    }

    public String getProtectedResourcePath() {
        return protectedResourcePath;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getAuthorizationServers() {
        return authorizationServers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resource;
        private String protectedResourcePath;
        private boolean enabled;
        private List<String> authorizationServers = new ArrayList<String>();

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder protectedResourcePath(String protectedResourcePath) {
            this.protectedResourcePath = protectedResourcePath;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder authorizationServer(String authorizationServer) {
            if (authorizationServer != null) {
                this.authorizationServers.add(authorizationServer);
            }
            return this;
        }

        public ProtectedResourceMetadata build() {
            return new ProtectedResourceMetadata(this);
        }
    }
}

// Made with Bob
