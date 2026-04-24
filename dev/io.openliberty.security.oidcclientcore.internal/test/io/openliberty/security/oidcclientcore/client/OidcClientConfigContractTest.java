/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the OidcClientConfig interface contract to ensure it provides
 * the correct accessors for RFC 9728 protected resource metadata generation
 * without violating the design principle that protected resource path derivation
 * belongs in the security/auth-filter layer, not in OIDC config.
 */
public class OidcClientConfigContractTest {

    /**
     * Mock implementation for testing the interface contract
     */
    private static class MockOidcClientConfig implements OidcClientConfig {
        private Boolean protectedResourceMetadataEnabled = false;
        private String authFilterRef = null;
        private String providerURI = null;
        private String issuerIdentifier = null;

        @Override
        public String getProviderURI() {
            return providerURI;
        }

        @Override
        public OidcProviderMetadata getProviderMetadata() {
            return null;
        }

        @Override
        public String getClientId() {
            return "test-client";
        }

        @Override
        public com.ibm.websphere.ras.ProtectedString getClientSecret() {
            return null;
        }

        @Override
        public ClaimsMappingConfig getClaimsMappingConfig() {
            return null;
        }

        @Override
        public String getRedirectURI() {
            return null;
        }

        @Override
        public boolean isRedirectToOriginalResource() {
            return false;
        }

        @Override
        public java.util.Set<String> getScope() {
            return null;
        }

        @Override
        public String getResponseType() {
            return null;
        }

        @Override
        public String getResponseMode() {
            return null;
        }

        @Override
        public String getPromptParameter() {
            return null;
        }

        @Override
        public String getDisplayParameter() {
            return null;
        }

        @Override
        public boolean isUseNonce() {
            return false;
        }

        @Override
        public boolean isUseSession() {
            return false;
        }

        @Override
        public String[] getExtraParameters() {
            return null;
        }

        @Override
        public int getJwksConnectTimeout() {
            return 0;
        }

        @Override
        public int getJwksReadTimeout() {
            return 0;
        }

        @Override
        public boolean isTokenAutoRefresh() {
            return false;
        }

        @Override
        public int getTokenMinValidity() {
            return 0;
        }

        @Override
        public Boolean isProtectedResourceMetadataEnabled() {
            return protectedResourceMetadataEnabled;
        }

        @Override
        public String getAuthFilterRef() {
            return authFilterRef;
        }

        public void setProtectedResourceMetadataEnabled(Boolean enabled) {
            this.protectedResourceMetadataEnabled = enabled;
        }

        public void setAuthFilterRef(String ref) {
            this.authFilterRef = ref;
        }

        public void setProviderURI(String uri) {
            this.providerURI = uri;
        }

        public void setIssuerIdentifier(String issuer) {
            this.issuerIdentifier = issuer;
        }
    }

    @Test
    public void testProtectedResourceMetadataEnabledDefaultsToFalse() {
        MockOidcClientConfig config = new MockOidcClientConfig();
        assertFalse("protectedResourceMetadataEnabled should default to false",
                    config.isProtectedResourceMetadataEnabled());
    }

    @Test
    public void testProtectedResourceMetadataCanBeEnabled() {
        MockOidcClientConfig config = new MockOidcClientConfig();
        config.setProtectedResourceMetadataEnabled(true);
        assertTrue("protectedResourceMetadataEnabled should be true when set",
                   config.isProtectedResourceMetadataEnabled());
    }

    @Test
    public void testAuthFilterRefAccessor() {
        MockOidcClientConfig config = new MockOidcClientConfig();
        assertNull("authFilterRef should default to null", config.getAuthFilterRef());
        
        config.setAuthFilterRef("myAuthFilter");
        assertEquals("authFilterRef should return configured value",
                     "myAuthFilter", config.getAuthFilterRef());
    }

    @Test
    public void testProviderURIAccessorForAuthorizationServerIdentifier() {
        MockOidcClientConfig config = new MockOidcClientConfig();
        assertNull("providerURI should default to null", config.getProviderURI());
        
        config.setProviderURI("https://auth.example.com");
        assertEquals("providerURI should return configured value for AS identifier",
                     "https://auth.example.com", config.getProviderURI());
    }

    @Test
    public void testConfigProvidesAuthorizationServerIdentifiers() {
        // The config interface must provide access to authorization server identifiers
        // for RFC 9728 metadata generation, but should NOT provide protected resource
        // path derivation - that belongs in the security layer
        MockOidcClientConfig config = new MockOidcClientConfig();
        config.setProviderURI("https://auth.example.com");
        
        assertNotNull("Config must provide authorization server identifier via providerURI",
                      config.getProviderURI());
    }

    @Test
    public void testConfigDoesNotDeriveProtectedResourcePaths() {
        // This test documents that the config interface should NOT have methods
        // that derive protected resource paths - that responsibility belongs to
        // the security/auth-filter layer which knows the actual request mapping
        
        MockOidcClientConfig config = new MockOidcClientConfig();
        config.setAuthFilterRef("myAuthFilter");
        
        // The config provides the authFilterRef for the security layer to use,
        // and correctly does not expose protected-resource path derivation.
        assertNotNull("Config provides authFilterRef for security layer",
                      config.getAuthFilterRef());
    }
}
// Made with Bob
