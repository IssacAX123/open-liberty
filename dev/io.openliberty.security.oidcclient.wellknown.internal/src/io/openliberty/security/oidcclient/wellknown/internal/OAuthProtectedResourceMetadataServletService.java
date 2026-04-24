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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.security.openidconnect.client.internal.OidcClientImpl;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;

import io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadata;

@Component(name = "io.openliberty.security.oidcclient.wellknown.internal.OAuthProtectedResourceMetadataServletService", service = {}, property = { "service.vendor=IBM" })
public class OAuthProtectedResourceMetadataServletService extends OAuthProtectedResourceMetadataServlet {

    private static final long serialVersionUID = 1L;

    private OidcClientImpl oidcClientImpl;

    @Reference(name = "oidcClientImpl", service = OidcClientImpl.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY)
    protected void setOidcClientImpl(ServiceReference<OidcClientImpl> reference) {
        oidcClientImpl = reference.getBundle().getBundleContext().getService(reference);
    }

    protected void unsetOidcClientImpl(ServiceReference<OidcClientImpl> reference) {
        oidcClientImpl = null;
    }

    protected void activate(ComponentContext cc) {
        // No-op: OidcClientImpl manages its own lifecycle
    }

    protected void deactivate(ComponentContext cc) {
        // No-op: OidcClientImpl manages its own lifecycle
    }

    @Override
    protected ProtectedResourceMetadata resolveMetadata(HttpServletRequest request, String protectedResourcePath) {
        if (oidcClientImpl == null) {
            return null;
        }

        // Create a wrapper request with the protected resource path for authFilter matching
        HttpServletRequest wrappedRequest = new ProtectedResourcePathRequest(request, protectedResourcePath);
        
        // Reuse OidcClientImpl's config matching logic
        OidcClientConfig matchingConfig = oidcClientImpl.findConfigForProtectedResourceMetadata(wrappedRequest);
        if (matchingConfig == null) {
            return null;
        }

        // Build absolute URI for resource field per RFC 9728
        String absoluteResourceUri = buildAbsoluteUri(request, protectedResourcePath);

        // Build and return metadata
        return ProtectedResourceMetadata.builder()
                .resource(absoluteResourceUri)
                .protectedResourcePath(protectedResourcePath)
                .enabled(true)
                .authorizationServer(matchingConfig.getIssuerIdentifier())
                .build();
    }

    /**
     * Build an absolute URI from the request's scheme, host, port, and the protected resource path.
     */
    private String buildAbsoluteUri(HttpServletRequest request, String path) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        
        StringBuilder uri = new StringBuilder();
        uri.append(scheme).append("://").append(host);
        
        // Only include port if it's not the default for the scheme
        if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
            uri.append(":").append(port);
        }
        
        uri.append(path);
        return uri.toString();
    }

    /**
     * Request wrapper that overrides getRequestURI to return the protected resource path.
     * This allows authFilter matching based on the actual protected resource path.
     */
    private static class ProtectedResourcePathRequest extends HttpServletRequestWrapper {
        private final String protectedResourcePath;

        public ProtectedResourcePathRequest(HttpServletRequest request, String protectedResourcePath) {
            super(request);
            this.protectedResourcePath = protectedResourcePath;
        }

        @Override
        public String getRequestURI() {
            return protectedResourcePath;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append(getScheme()).append("://").append(getServerName());
            int port = getServerPort();
            if ((getScheme().equals("http") && port != 80) || (getScheme().equals("https") && port != 443)) {
                url.append(":").append(port);
            }
            url.append(protectedResourcePath);
            return url;
        }
    }
}

// Made with Bob
