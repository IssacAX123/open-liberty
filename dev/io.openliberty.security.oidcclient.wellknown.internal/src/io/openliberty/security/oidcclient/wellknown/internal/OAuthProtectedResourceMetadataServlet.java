/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclient.wellknown.internal;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadata;
import io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadataJsonBuilder;

public class OAuthProtectedResourceMetadataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final ProtectedResourceMetadataJsonBuilder jsonBuilder = new ProtectedResourceMetadataJsonBuilder();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String protectedResourcePath = toProtectedResourcePath(request.getPathInfo());
        ProtectedResourceMetadata metadata = resolveMetadata(request, protectedResourcePath);

        if (metadata == null || !metadata.isEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonBuilder.toJson(metadata));
    }

    protected String toProtectedResourcePath(String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
            return "/";
        }
        if (pathInfo.startsWith("/")) {
            return pathInfo;
        }
        return "/" + pathInfo;
    }

    protected ProtectedResourceMetadata resolveMetadata(HttpServletRequest request, String protectedResourcePath) {
        return null;
    }
}

// Made with Bob
