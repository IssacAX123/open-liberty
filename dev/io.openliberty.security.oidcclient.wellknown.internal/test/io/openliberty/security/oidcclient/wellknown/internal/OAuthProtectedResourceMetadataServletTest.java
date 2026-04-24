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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadata;

public class OAuthProtectedResourceMetadataServletTest {

    private final JUnit4Mockery context = new JUnit4Mockery();

    @Test
    public void returnsMetadataJsonForKnownEnabledProtectedResource() throws Exception {
        ProtectedResourceMetadata metadata = ProtectedResourceMetadata.builder()
                        .resource("https://example.com/inventory/api")
                        .protectedResourcePath("/inventory/api")
                        .enabled(true)
                        .authorizationServer("https://issuer.example.com")
                        .build();

        TestServlet servlet = new TestServlet(Collections.singletonMap("/inventory/api", metadata));

        HttpServletRequest request = context.mock(HttpServletRequest.class);
        HttpServletResponse response = context.mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        PrintWriter writer = new PrintWriter(body);

        context.checking(new Expectations() {
            {
                oneOf(request).getPathInfo();
                will(returnValue("/inventory/api"));

                oneOf(response).setContentType("application/json");
                oneOf(response).setCharacterEncoding("UTF-8");
                oneOf(response).getWriter();
                will(returnValue(writer));
            }
        });

        servlet.doGet(request, response);

        writer.flush();
        assertTrue(body.toString().contains("\"resource\":\"https://example.com/inventory/api\""));
        assertTrue(body.toString().contains("\"authorization_servers\":[\"https://issuer.example.com\"]"));
    }

    @Test
    public void returnsNotFoundWhenProtectedResourceIsUnknown() throws Exception {
        TestServlet servlet = new TestServlet(Collections.<String, ProtectedResourceMetadata>emptyMap());

        HttpServletRequest request = context.mock(HttpServletRequest.class, "unknownRequest");
        HttpServletResponse response = context.mock(HttpServletResponse.class, "unknownResponse");

        context.checking(new Expectations() {
            {
                oneOf(request).getPathInfo();
                will(returnValue("/unknown/resource"));

                oneOf(response).sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        });

        servlet.doGet(request, response);
    }

    @Test
    public void returnsNotFoundWhenMetadataIsDisabled() throws Exception {
        ProtectedResourceMetadata metadata = ProtectedResourceMetadata.builder()
                        .resource("https://example.com/inventory/api")
                        .protectedResourcePath("/inventory/api")
                        .enabled(false)
                        .build();

        TestServlet servlet = new TestServlet(Collections.singletonMap("/inventory/api", metadata));

        HttpServletRequest request = context.mock(HttpServletRequest.class, "disabledRequest");
        HttpServletResponse response = context.mock(HttpServletResponse.class, "disabledResponse");

        context.checking(new Expectations() {
            {
                oneOf(request).getPathInfo();
                will(returnValue("/inventory/api"));

                oneOf(response).sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        });

        servlet.doGet(request, response);
    }

    @Test
    public void normalizesMissingLeadingSlashBeforeLookup() throws Exception {
        ProtectedResourceMetadata metadata = ProtectedResourceMetadata.builder()
                        .resource("https://example.com/inventory/api")
                        .protectedResourcePath("/inventory/api")
                        .enabled(true)
                        .build();

        TestServlet servlet = new TestServlet(Collections.singletonMap("/inventory/api", metadata));

        assertEquals("/inventory/api", servlet.toProtectedResourcePath("inventory/api"));
        assertEquals("/inventory/api", servlet.toProtectedResourcePath("/inventory/api"));
    }

    private static class TestServlet extends OAuthProtectedResourceMetadataServlet {
        private final java.util.Map<String, ProtectedResourceMetadata> metadataByPath;

        private TestServlet(java.util.Map<String, ProtectedResourceMetadata> metadataByPath) {
            this.metadataByPath = metadataByPath;
        }

        @Override
        protected ProtectedResourceMetadata resolveMetadata(String protectedResourcePath) {
            return metadataByPath.get(protectedResourcePath);
        }
    }
}

// Made with Bob
