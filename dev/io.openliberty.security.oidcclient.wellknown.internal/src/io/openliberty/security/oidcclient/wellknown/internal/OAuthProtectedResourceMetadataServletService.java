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

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

import io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadata;

@Component(name = "io.openliberty.security.oidcclient.wellknown.internal.OAuthProtectedResourceMetadataServletService", service = {}, property = { "service.vendor=IBM" })
public class OAuthProtectedResourceMetadataServletService extends OAuthProtectedResourceMetadataServlet {

    private static final long serialVersionUID = 1L;

    private static final ConcurrentServiceReferenceSet<OidcClientConfig> oidcClientConfigRef = new ConcurrentServiceReferenceSet<OidcClientConfig>("oidcClientConfig");

    @Reference(name = "oidcClientConfig", service = OidcClientConfig.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setOidcClientConfig(ServiceReference<OidcClientConfig> reference) {
        oidcClientConfigRef.addReference(reference);
    }

    protected void unsetOidcClientConfig(ServiceReference<OidcClientConfig> reference) {
        oidcClientConfigRef.removeReference(reference);
    }

    protected void activate(ComponentContext cc) {
        oidcClientConfigRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        oidcClientConfigRef.deactivate(cc);
    }

    @Override
    protected ProtectedResourceMetadata resolveMetadata(String protectedResourcePath) {
        // Task 5: Fail closed - no config-derived path matching
        // Task 6 will implement security-owned authFilter derivation
        return null;
    }
}

// Made with Bob
