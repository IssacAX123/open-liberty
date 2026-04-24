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

import java.util.List;

public class ProtectedResourceMetadataJsonBuilder {

    public String toJson(ProtectedResourceMetadata metadata) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"resource\":\"").append(metadata.getResource()).append("\"");

        List<String> authorizationServers = metadata.getAuthorizationServers();
        if (authorizationServers != null && !authorizationServers.isEmpty()) {
            json.append(",\"authorization_servers\":[");
            for (int i = 0; i < authorizationServers.size(); i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append("\"").append(authorizationServers.get(i)).append("\"");
            }
            json.append("]");
        }

        json.append("}");
        return json.toString();
    }
}
