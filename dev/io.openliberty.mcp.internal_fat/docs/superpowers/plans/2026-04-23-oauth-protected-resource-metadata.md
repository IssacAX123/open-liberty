# OAuth 2.0 Protected Resource Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add RFC 9728 protected resource metadata support for resources protected by `oidcConnectClient`, with explicit opt-in configuration, security-owned derivation of the protected resource and metadata URL from `authFilter` knowledge, and MCP exercising the behavior through unauthorized challenge and well-known endpoint FAT coverage.

**Architecture:** Implement configuration, metadata resolution, and JSON generation in the OIDC client area; publish metadata from a new well-known servlet bundle mounted at `/.well-known/oauth-protected-resource/`; and augment existing unauthorized bearer challenge handling in the security layer to add `resource_metadata` only when the security side can derive the correct URL for an enabled protected resource. MCP does not compute the URL and does not gate this behavior by protocol version.

**Tech Stack:** Java, OSGi Declarative Services, Servlet, Liberty security/webcontainer internals, OIDC client core, FAT/JUnit/component tests.

---

## File Structure

### Existing files to modify

- Modify: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/OidcMetadataService.java`
- Modify: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/client/OidcClientConfig.java`
- Modify: `../../../open-liberty/dev/com.ibm.ws.webcontainer.security/src/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReply.java`
- Modify: `../../../open-liberty/dev/fattest.simplicity/src/com/ibm/websphere/simplicity/config/OpenidConnectClient.java`
- Modify: security/auth-filter-aware runtime files discovered in Task 1
- Modify: `fat/src/io/openliberty/mcp/internal/fat/security/AuthHelper.java`
- Modify: `publish/servers/mcp-server-auth/server.xml`
- Modify: `publish/servers/mcp-server-async-auth/server.xml`

### New files likely to create in the OIDC client core bundle

- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadata.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataResolver.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataJsonBuilder.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataResolverTest.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataJsonBuilderTest.java`

### New files likely to create in the well-known publisher bundle

- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/bnd.bnd`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/build.gradle`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/src/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServlet.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/src/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServletService.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/test/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServletTest.java`

### New/updated FAT coverage

- Create: `fat/src/io/openliberty/mcp/internal/fat/security/OAuthProtectedResourceMetadataTest.java`
- Possibly create: `fat/src/io/openliberty/mcp/internal/fat/utils/HttpHeaderAsserts.java`

If the actual bundle names differ after local code inspection, keep the responsibility split exactly the same and adjust only the path names.

---

## Task 1: Confirm the exact OIDC, security, and auth-filter extension points

**Files:**
- Modify: `docs/superpowers/plans/2026-04-23-oauth-protected-resource-metadata.md`
- Inspect: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/bnd.bnd`
- Inspect: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/OidcMetadataService.java`
- Inspect: `../../../open-liberty/dev/com.ibm.ws.webcontainer.security/src/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReply.java`
- Inspect: security/auth-filter-aware classes that know which `authFilter` matched the current request

- [ ] **Step 1: Read the OIDC client core bundle metadata**
```bash
type ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/bnd.bnd
```
Expected: the bundle is the right home for opt-in config, metadata model, resolver, and JSON generation.

- [ ] **Step 2: Read the current unauthorized bearer challenge implementation**
```bash
type ../../../open-liberty/dev/com.ibm.ws.webcontainer.security/src/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReply.java
```
Expected: confirmation of where `WWW-Authenticate` is finalized and where a security-owned `resource_metadata` parameter can be appended.

- [ ] **Step 3: Locate the runtime that knows which `authFilter` matched the request**
```bash
findstr /s /n /i "authFilter isAccepted processRequest requestUrl" ../../../open-liberty/dev/com.ibm.ws.security*/*.java ../../../open-liberty/dev/io.openliberty.security*/*.java
```
Expected: identify the real extension point that can derive the protected resource path and metadata URL from `authFilter` knowledge.

- [ ] **Step 4: Update this plan inline with the exact extension point paths if they differ**
```markdown
- Replace any guessed bundle path with the actual bundle path.
- Replace any guessed security integration target with the actual target.
```

- [ ] **Step 5: Commit**
```bash
git add docs/superpowers/plans/2026-04-23-oauth-protected-resource-metadata.md
git commit -m "docs: lock down RFC9728 implementation file targets"
```

---

## Task 2: Add the failing OIDC-core unit tests for metadata resolution, enablement, and JSON generation

**Files:**
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataResolverTest.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataJsonBuilderTest.java`
- Modify: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/OidcMetadataService.java`

- [ ] **Step 1: Write the failing resolver test**
```java
package io.openliberty.security.oidcclientcore.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProtectedResourceMetadataResolverTest {

    @Test
    public void resolvesWellKnownPathFromProtectedResourcePath() {
        ProtectedResourceMetadataResolver resolver = new ProtectedResourceMetadataResolver();
        String metadataPath = resolver.toWellKnownPath("/MyApplication/mcp");
        assertEquals("/.well-known/oauth-protected-resource/MyApplication/mcp", metadataPath);
    }

    @Test
    public void stripsLeadingSlashWhenBuildingWellKnownSuffix() {
        ProtectedResourceMetadataResolver resolver = new ProtectedResourceMetadataResolver();
        assertEquals("mcp", resolver.toWellKnownSuffix("/mcp"));
    }

    @Test
    public void disabledMetadataIsNotPublished() {
        ProtectedResourceMetadata metadata = ProtectedResourceMetadata.builder()
                .resource("https://example.com/mcp")
                .protectedResourcePath("/mcp")
                .enabled(false)
                .build();
        assertFalse(metadata.isEnabled());
    }
}
```

- [ ] **Step 2: Write the failing JSON builder test**
```java
package io.openliberty.security.oidcclientcore.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProtectedResourceMetadataJsonBuilderTest {

    @Test
    public void includesAuthorizationServersWhenPresent() {
        ProtectedResourceMetadata metadata = ProtectedResourceMetadata.builder()
                .resource("https://example.com/mcp")
                .protectedResourcePath("/mcp")
                .enabled(true)
                .authorizationServer("https://authorization-server.example.test")
                .build();

        String json = new ProtectedResourceMetadataJsonBuilder().toJson(metadata);

        assertTrue(json.contains("\"resource\":\"https://example.com/mcp\""));
        assertTrue(json.contains("\"authorization_servers\":[\"https://authorization-server.example.test\"]"));
    }

    @Test
    public void omitsAuthorizationServersWhenAbsent() {
        ProtectedResourceMetadata metadata = ProtectedResourceMetadata.builder()
                .resource("https://example.com/mcp")
                .protectedResourcePath("/mcp")
                .enabled(true)
                .build();

        String json = new ProtectedResourceMetadataJsonBuilder().toJson(metadata);

        assertTrue(json.contains("\"resource\":\"https://example.com/mcp\""));
        assertFalse(json.contains("authorization_servers"));
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**
```bash
cd ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal && gradlew test --tests "io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadataResolverTest" --tests "io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadataJsonBuilderTest"
```
Expected: FAIL with class-not-found or symbol-not-found errors for the new resolver/model/builder types.

- [ ] **Step 4: Commit**
```bash
git add ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataResolverTest.java ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataJsonBuilderTest.java
git commit -m "test: add failing OIDC protected resource metadata unit tests"
```

---

## Task 3: Implement the OIDC-core metadata model and JSON builder

**Files:**
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadata.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataJsonBuilder.java`
- Test: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataJsonBuilderTest.java`

- [ ] **Step 1: Implement the metadata model**
```java
package io.openliberty.security.oidcclientcore.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ProtectedResourceMetadata {

    private final String resource;
    private final String protectedResourcePath;
    private final boolean enabled;
    private final List<String> authorizationServers;

    public ProtectedResourceMetadata(String resource, String protectedResourcePath, boolean enabled, List<String> authorizationServers) {
        this.resource = resource;
        this.protectedResourcePath = protectedResourcePath;
        this.enabled = enabled;
        this.authorizationServers = Collections.unmodifiableList(new ArrayList<>(authorizationServers));
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

    public Optional<List<String>> getAuthorizationServers() {
        return authorizationServers.isEmpty() ? Optional.empty() : Optional.of(authorizationServers);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resource;
        private String protectedResourcePath;
        private boolean enabled;
        private final List<String> authorizationServers = new ArrayList<>();

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
            this.authorizationServers.add(authorizationServer);
            return this;
        }

        public ProtectedResourceMetadata build() {
            return new ProtectedResourceMetadata(resource, protectedResourcePath, enabled, authorizationServers);
        }
    }
}
```

- [ ] **Step 2: Implement the JSON builder**
```java
package io.openliberty.security.oidcclientcore.config;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ProtectedResourceMetadataJsonBuilder {

    public String toJson(ProtectedResourceMetadata metadata) {
        JSONObject json = new JSONObject();
        json.put("resource", metadata.getResource());

        // RFC 9728 optional field listing external authorization server identifiers.
        // Liberty remains the protected resource and does not act as the authorization server.
        metadata.getAuthorizationServers().ifPresent(servers -> {
            JSONArray array = new JSONArray();
            array.addAll(servers);
            json.put("authorization_servers", array);
        });

        return json.serialize();
    }
}
```

- [ ] **Step 3: Run the JSON builder test**
```bash
cd ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal && gradlew test --tests "io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadataJsonBuilderTest"
```
Expected: PASS.

- [ ] **Step 4: Commit**
```bash
git add ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadata.java ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataJsonBuilder.java
git commit -m "feat: add OIDC protected resource metadata model"
```

---

## Task 4: Implement protected resource path to well-known path resolution

**Files:**
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataResolver.java`
- Modify: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/OidcMetadataService.java`
- Test: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataResolverTest.java`

- [ ] **Step 1: Implement the resolver with path helpers**
```java
package io.openliberty.security.oidcclientcore.config;

public class ProtectedResourceMetadataResolver {

    static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource/";

    public String toWellKnownPath(String protectedResourcePath) {
        return WELL_KNOWN_PREFIX + toWellKnownSuffix(protectedResourcePath);
    }

    public String toWellKnownSuffix(String protectedResourcePath) {
        if (protectedResourcePath == null || protectedResourcePath.isBlank() || "/".equals(protectedResourcePath)) {
            return "";
        }
        return protectedResourcePath.startsWith("/") ? protectedResourcePath.substring(1) : protectedResourcePath;
    }

    public String toProtectedResourcePath(String wellKnownSuffix) {
        if (wellKnownSuffix == null || wellKnownSuffix.isBlank()) {
            return "/";
        }
        return wellKnownSuffix.startsWith("/") ? wellKnownSuffix : "/" + wellKnownSuffix;
    }
}
```

- [ ] **Step 2: Add service-level accessors in `OidcMetadataService`**
```java
public ProtectedResourceMetadataResolver getProtectedResourceMetadataResolver() {
    return new ProtectedResourceMetadataResolver();
}
```

```java
public String getProtectedResourceMetadataPath(String protectedResourcePath) {
    return getProtectedResourceMetadataResolver().toWellKnownPath(protectedResourcePath);
}
```

- [ ] **Step 3: Run the resolver tests**
```bash
cd ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal && gradlew test --tests "io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadataResolverTest"
```
Expected: PASS.

- [ ] **Step 4: Commit**
```bash
git add ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataResolver.java ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/OidcMetadataService.java
git commit -m "feat: add protected resource metadata path resolver"
```

---

## Task 5: Add opt-in config plumbing for protected resource metadata

**Files:**
- Modify: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/client/OidcClientConfig.java`
- Modify: `../../../open-liberty/dev/fattest.simplicity/src/com/ibm/websphere/simplicity/config/OpenidConnectClient.java`
- Possibly modify: OIDC metatype/config backing classes found during Task 1
- Test: existing config tests in the OIDC client area

- [ ] **Step 1: Write the failing config test or extend an existing one**
```java
@Test
public void openidConnectClientConfigExposesProtectedResourceMetadataEnablement() {
    OpenidConnectClient client = new OpenidConnectClient();
    client.setAuthFilterRef("myAuthFilter");
    client.setResource("https://example.com/mcp");
    client.setProtectedResourceMetadataEnabled(Boolean.TRUE);

    assertEquals("myAuthFilter", client.getAuthFilterRef());
    assertEquals("https://example.com/mcp", client.getResource());
    assertTrue(client.getProtectedResourceMetadataEnabled());
}
```

- [ ] **Step 2: If the OIDC runtime config lacks needed accessors, add them**
```java
public interface OidcClientConfig {
    String getAuthFilterRef();
    String getResource();
    Boolean isProtectedResourceMetadataEnabled();
    String getAuthorizationEndpointUrl();
    String getProviderURI();
}
```
Use the actual style of the existing file; add only methods that are truly missing.

- [ ] **Step 3: Add the opt-in property on `openidConnectClient`**
```java
@XmlAttribute(name = "protectedResourceMetadataEnabled")
public void setProtectedResourceMetadataEnabled(Boolean protectedResourceMetadataEnabled) {
    this.protectedResourceMetadataEnabled = protectedResourceMetadataEnabled;
}
```
Keep any new property in the `openidConnectClient` area rather than MCP.

- [ ] **Step 4: Run the relevant unit/config tests**
```bash
cd ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal && gradlew test --tests "*Oidc*Config*"
```
Expected: PASS after the accessors/config plumbing is complete.

- [ ] **Step 5: Commit**
```bash
git add ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/client/OidcClientConfig.java ../../../open-liberty/dev/fattest.simplicity/src/com/ibm/websphere/simplicity/config/OpenidConnectClient.java
git commit -m "feat: add opt-in OIDC protected resource metadata config"
```

---

## Task 6: Add OIDC service logic to resolve metadata only for enabled resources

**Files:**
- Modify: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/OidcMetadataService.java`
- Modify: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/MetadataUtils.java`
- Test: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataResolverTest.java`

- [ ] **Step 1: Write the failing service-level test for an enabled protected resource**
```java
@Test
public void buildsProtectedResourceMetadataFromEnabledClientConfig() {
    OidcMetadataService service = new OidcMetadataService();
    OidcClientConfig config = stubClientConfig(
            "myClient",
            "myAuthFilter",
            "https://example.com/mcp",
            Boolean.TRUE,
            "https://authorization-server.example.test/authorize",
            "https://authorization-server.example.test");

    ProtectedResourceMetadata metadata = service.buildProtectedResourceMetadata("/mcp", config);

    assertTrue(metadata.isEnabled());
    assertEquals("https://example.com/mcp", metadata.getResource());
    assertTrue(metadata.getAuthorizationServers().isPresent());
    assertEquals("https://authorization-server.example.test", metadata.getAuthorizationServers().get().get(0));
}
```

- [ ] **Step 2: Write the failing service-level test for a disabled protected resource**
```java
@Test
public void disabledProtectedResourceMetadataReturnsNullOrEmptyResult() {
    OidcMetadataService service = new OidcMetadataService();
    OidcClientConfig config = stubClientConfig(
            "myClient",
            "myAuthFilter",
            "https://example.com/mcp",
            Boolean.FALSE,
            "https://authorization-server.example.test/authorize",
            "https://authorization-server.example.test");

    ProtectedResourceMetadata metadata = service.buildProtectedResourceMetadata("/mcp", config);

    assertFalse(metadata.isEnabled());
}
```

- [ ] **Step 3: Implement the service method**
```java
public ProtectedResourceMetadata buildProtectedResourceMetadata(String protectedResourcePath, OidcClientConfig oidcClientConfig) {
    boolean enabled = Boolean.TRUE.equals(oidcClientConfig.isProtectedResourceMetadataEnabled());

    ProtectedResourceMetadata.Builder builder = ProtectedResourceMetadata.builder()
            .protectedResourcePath(protectedResourcePath)
            .enabled(enabled)
            .resource(resolveProtectedResourceIdentifier(protectedResourcePath, oidcClientConfig));

    if (enabled) {
        String authorizationServer = resolveAuthorizationServerIdentifier(oidcClientConfig);
        if (authorizationServer != null && !authorizationServer.isBlank()) {
            builder.authorizationServer(authorizationServer);
        }
    }

    return builder.build();
}
```

- [ ] **Step 4: Run the targeted tests**
```bash
cd ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal && gradlew test --tests "io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadataResolverTest" --tests "io.openliberty.security.oidcclientcore.config.OidcMetadataServiceTest"
```
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/OidcMetadataService.java ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/src/io/openliberty/security/oidcclientcore/config/MetadataUtils.java
git commit -m "feat: resolve protected resource metadata only for enabled OIDC resources"
```

---

## Task 7: Add the failing servlet tests for the well-known publisher bundle

**Files:**
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/test/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServletTest.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/src/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServlet.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/bnd.bnd`

- [ ] **Step 1: Write the failing servlet test**
```java
package io.openliberty.security.oidcclient.wellknown.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OAuthProtectedResourceMetadataServletTest {

    @Test
    public void mapsWellKnownSuffixBackToProtectedPath() {
        OAuthProtectedResourceMetadataServlet servlet = new OAuthProtectedResourceMetadataServlet();
        assertEquals("/MyApplication/mcp", servlet.toProtectedResourcePath("MyApplication/mcp"));
    }

    @Test
    public void usesApplicationJsonContentType() {
        OAuthProtectedResourceMetadataServlet servlet = new OAuthProtectedResourceMetadataServlet();
        assertEquals("application/json", servlet.getResponseContentType());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**
```bash
cd ../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal && gradlew test --tests "io.openliberty.security.oidcclient.wellknown.internal.OAuthProtectedResourceMetadataServletTest"
```
Expected: FAIL because the new bundle and servlet do not exist yet.

- [ ] **Step 3: Commit**
```bash
git add ../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/test/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServletTest.java
git commit -m "test: add failing well-known metadata servlet tests"
```

---

## Task 8: Implement the new well-known publisher bundle and servlet

**Files:**
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/bnd.bnd`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/build.gradle`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/src/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServlet.java`
- Create: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/src/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServletService.java`
- Test: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/test/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServletTest.java`

- [ ] **Step 1: Create the bundle metadata**
```properties
Bundle-Name: OAuth Protected Resource Metadata Well-Known Publisher
Bundle-SymbolicName: io.openliberty.security.oidcclient.wellknown.internal
Web-ContextPath: /.well-known/oauth-protected-resource
-privatepackages: io.openliberty.security.oidcclient.wellknown.internal
Import-Package: javax.servlet,javax.servlet.http,org.osgi.service.component.annotations,io.openliberty.security.oidcclientcore.config,*
```

- [ ] **Step 2: Implement the servlet**
```java
package io.openliberty.security.oidcclient.wellknown.internal;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadata;
import io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadataJsonBuilder;
import io.openliberty.security.oidcclientcore.config.ProtectedResourceMetadataResolver;

public class OAuthProtectedResourceMetadataServlet extends HttpServlet {

    private final ProtectedResourceMetadataResolver resolver = new ProtectedResourceMetadataResolver();
    private final ProtectedResourceMetadataJsonBuilder jsonBuilder = new ProtectedResourceMetadataJsonBuilder();

    String toProtectedResourcePath(String suffix) {
        return resolver.toProtectedResourcePath(suffix);
    }

    String getResponseContentType() {
        return "application/json";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String suffix = request.getPathInfo() == null ? "" : request.getPathInfo();
        String protectedResourcePath = toProtectedResourcePath(suffix);
        ProtectedResourceMetadata metadata = lookupMetadata(protectedResourcePath);

        if (metadata == null || !metadata.isEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(getResponseContentType());
        response.getWriter().write(jsonBuilder.toJson(metadata));
    }

    ProtectedResourceMetadata lookupMetadata(String protectedResourcePath) {
        return null;
    }
}
```

- [ ] **Step 3: Replace the stub lookup with OSGi-backed service wiring**
```java
@Component(service = OAuthProtectedResourceMetadataServletService.class)
public class OAuthProtectedResourceMetadataServletService {

    @Reference
    OidcMetadataService oidcMetadataService;

    public ProtectedResourceMetadata getMetadata(String protectedResourcePath) {
        return oidcMetadataService.resolveProtectedResourceMetadata(protectedResourcePath);
    }
}
```

- [ ] **Step 4: Wire the servlet to the service and re-run tests**
```bash
cd ../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal && gradlew test --tests "io.openliberty.security.oidcclient.wellknown.internal.OAuthProtectedResourceMetadataServletTest"
```
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add ../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal
git commit -m "feat: publish RFC9728 metadata from well-known OIDC servlet"
```

---

## Task 9: Add the failing security-side challenge augmentation tests

**Files:**
- Modify: `../../../open-liberty/dev/com.ibm.ws.webcontainer.security/src/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReply.java`
- Create or modify: `../../../open-liberty/dev/com.ibm.ws.webcontainer.security/test/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReplyTest.java`
- Modify: actual auth-filter-aware request-context file discovered in Task 1

- [ ] **Step 1: Write the failing challenge augmentation test**
```java
@Test
public void preservesBearerRealmAndAppendsResourceMetadata() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader("WWW-Authenticate", "Bearer realm=\"oauth\"");

    OAuthChallengeReply reply = new OAuthChallengeReply("unauthorized");
    reply.appendResourceMetadata(response, "https://example.com/.well-known/oauth-protected-resource/mcp");

    assertEquals(
            "Bearer realm=\"oauth\", resource_metadata=\"https://example.com/.well-known/oauth-protected-resource/mcp\"",
            response.getHeader("WWW-Authenticate"));
}
```

- [ ] **Step 2: Run the test to verify it fails**
```bash
cd ../../../open-liberty/dev/com.ibm.ws.webcontainer.security && gradlew test --tests "com.ibm.ws.webcontainer.security.internal.OAuthChallengeReplyTest"
```
Expected: FAIL because `appendResourceMetadata` does not exist yet.

- [ ] **Step 3: Commit**
```bash
git add ../../../open-liberty/dev/com.ibm.ws.webcontainer.security/test/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReplyTest.java
git commit -m "test: add failing OAuth challenge resource metadata test"
```

---

## Task 10: Implement security-owned bearer challenge augmentation for `resource_metadata`

**Files:**
- Modify: `../../../open-liberty/dev/com.ibm.ws.webcontainer.security/src/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReply.java`
- Modify: the upstream security code path that knows the matched request URI and `authFilter`
- Test: `../../../open-liberty/dev/com.ibm.ws.webcontainer.security/test/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReplyTest.java`

- [ ] **Step 1: Add a helper that appends the RFC 9728 parameter**
```java
void appendResourceMetadata(HttpServletResponse rsp, String resourceMetadataUri) {
    if (resourceMetadataUri == null || resourceMetadataUri.isBlank()) {
        return;
    }

    String wwwAuthenticate = rsp.getHeader(AUTHENTICATE_HDR);
    if (wwwAuthenticate == null || wwwAuthenticate.isBlank()) {
        wwwAuthenticate = "Bearer realm=\"oauth\"";
    }

    if (!wwwAuthenticate.contains("resource_metadata=")) {
        wwwAuthenticate = wwwAuthenticate + ", resource_metadata=\"" + resourceMetadataUri + "\"";
    }

    rsp.setHeader(AUTHENTICATE_HDR, wwwAuthenticate);
}
```

- [ ] **Step 2: Derive the metadata URL from the security-owned request context**
```java
String protectedResourcePath = matchedProtectedResourceContext.getProtectedResourcePath();
boolean metadataEnabled = matchedProtectedResourceContext.isProtectedResourceMetadataEnabled();
String resourceMetadataUri = metadataEnabled
        ? matchedProtectedResourceContext.getProtectedResourceMetadataUri()
        : null;

appendResourceMetadata(rsp, resourceMetadataUri);
```
Use the actual request-context mechanism discovered in Task 1. Do not create global static state, and do not ask MCP to compute the URL.

- [ ] **Step 3: Run the webcontainer/security tests**
```bash
cd ../../../open-liberty/dev/com.ibm.ws.webcontainer.security && gradlew test --tests "com.ibm.ws.webcontainer.security.internal.OAuthChallengeReplyTest"
```
Expected: PASS.

- [ ] **Step 4: Commit**
```bash
git add ../../../open-liberty/dev/com.ibm.ws.webcontainer.security/src/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReply.java
git commit -m "feat: append RFC9728 resource metadata to bearer challenges"
```

---

## Task 11: Add MCP FAT fixtures for an enabled and a disabled OIDC-protected resource

**Files:**
- Modify: `publish/servers/mcp-server-auth/server.xml`
- Modify: `publish/servers/mcp-server-async-auth/server.xml`
- Possibly create: test app/resources under `fat/src/io/openliberty/mcp/internal/fat/security/`
- Modify: `fat/src/io/openliberty/mcp/internal/fat/security/AuthHelper.java`
- Possibly create/modify Keycloak Testcontainers support under the FAT test infrastructure

- [ ] **Step 1: Stand up an external Keycloak authorization server with Testcontainers for FAT**
```java
public final class KeycloakTestContainerSupport {
    static KeycloakContainer startKeycloak() {
        return new KeycloakContainer("quay.io/keycloak/keycloak:latest")
                .withRealmImportFile("keycloak/mcp-realm.json");
    }

    static String getProviderUri(KeycloakContainer keycloak) {
        return keycloak.getAuthServerUrl() + "/realms/mcp";
    }
}
```

Notes:
- Use Keycloak only as an external authorization server for the FAT environment.
- Liberty remains the protected resource / MCP server under test.
- Feed the discovered Keycloak issuer/provider URI into the Liberty server config before boot.

- [ ] **Step 2: Update the auth server configuration to use `openidConnectClient` + `authFilter` + opt-in enablement**
```xml
<featureManager>
    <feature>servlet-6.0</feature>
    <feature>cdi-4.0</feature>
    <feature>mcpServer-1.0</feature>
    <feature>appSecurity-5.0</feature>
    <feature>openidConnectClient-1.0</feature>
    <feature>monitor-1.0</feature>
</featureManager>

<authFilter id="mcpAuthFilterEnabled">
    <requestUrl id="mcpPathEnabled" urlPattern="/cancellationTest" matchType="contains" />
</authFilter>

<authFilter id="mcpAuthFilterDisabled">
    <requestUrl id="mcpPathDisabled" urlPattern="/disabledMetadataTest" matchType="contains" />
</authFilter>

<openidConnectClient id="mcpOidcEnabled"
                     authFilterRef="mcpAuthFilterEnabled"
                     resource="https://localhost:${default.https.port}/cancellationTest"
                     providerURI="${keycloakProviderUri}"
                     protectedResourceMetadataEnabled="true" />

<openidConnectClient id="mcpOidcDisabled"
                     authFilterRef="mcpAuthFilterDisabled"
                     resource="https://localhost:${default.https.port}/disabledMetadataTest"
                     providerURI="${keycloakProviderUri}"
                     protectedResourceMetadataEnabled="false" />
```

- [ ] **Step 3: Add helper assertions for the `WWW-Authenticate` header**
```java
static void assertResourceMetadataHeaderPresent(String headerValue, String expectedSuffix) {
    assertTrue(headerValue.contains("Bearer"));
    assertTrue(headerValue.contains("resource_metadata=\""));
    assertTrue(headerValue.contains(expectedSuffix));
}

static void assertResourceMetadataHeaderAbsent(String headerValue) {
    assertTrue(headerValue.contains("Bearer"));
    assertFalse(headerValue.contains("resource_metadata=\""));
}
```

- [ ] **Step 4: Run the existing auth-related FAT tests to verify the fixture compiles and boots with Keycloak**
```bash
gradlew test --tests "io.openliberty.mcp.internal.fat.security.*"
```
Expected: the FAT environment boots Liberty against the Keycloak Testcontainer; existing auth tests still pass or fail only where new assertions are not yet added.

- [ ] **Step 5: Commit**
```bash
git add publish/servers/mcp-server-auth/server.xml publish/servers/mcp-server-async-auth/server.xml fat/src/io/openliberty/mcp/internal/fat/security/AuthHelper.java
git commit -m "test: add Keycloak-backed OIDC MCP FAT fixtures"
```

---

## Task 12: Add the failing MCP FAT tests for RFC 9728 behavior

**Files:**
- Create: `fat/src/io/openliberty/mcp/internal/fat/security/OAuthProtectedResourceMetadataTest.java`
- Test fixtures: `publish/servers/mcp-server-auth/server.xml`, `publish/servers/mcp-server-async-auth/server.xml`

- [ ] **Step 1: Write the failing unauthorized-challenge FAT test for enabled metadata**
```java
@Test
public void unauthorizedRequestIncludesResourceMetadataHeaderWhenEnabled() throws Exception {
    String request = AuthHelper.buildUnauthorizedEchoRequest();
    HttpURLConnection connection = client.callMcpRaw(request, 401);

    String header = connection.getHeaderField("WWW-Authenticate");

    assertNotNull(header);
    AuthHelper.assertResourceMetadataHeaderPresent(
            header,
            "/.well-known/oauth-protected-resource/cancellationTest");
}
```

- [ ] **Step 2: Write the failing unauthorized-challenge FAT test for disabled metadata**
```java
@Test
public void unauthorizedRequestOmitsResourceMetadataHeaderWhenDisabled() throws Exception {
    String request = AuthHelper.buildUnauthorizedEchoRequestForPath("/disabledMetadataTest");
    HttpURLConnection connection = client.callMcpRaw(request, 401);

    String header = connection.getHeaderField("WWW-Authenticate");

    assertNotNull(header);
    AuthHelper.assertResourceMetadataHeaderAbsent(header);
}
```

- [ ] **Step 3: Write the failing well-known endpoint FAT test**
```java
@Test
public void wellKnownEndpointReturnsProtectedResourceMetadataWhenEnabled() throws Exception {
    String response = client.getRaw("/.well-known/oauth-protected-resource/cancellationTest");

    JSONAssert.assertEquals("""
        {
          "resource":"https://localhost/cancellationTest",
          "authorization_servers":["%s"]
        }
        """.formatted(KeycloakTestContainerSupport.getProviderUri(keycloak)), response, false);
}
```

- [ ] **Step 4: Write the failing disabled-well-known FAT test**
```java
@Test
public void wellKnownEndpointReturns404WhenMetadataDisabled() throws Exception {
    HttpURLConnection connection = client.getRawConnection("/.well-known/oauth-protected-resource/disabledMetadataTest");
    assertEquals(404, connection.getResponseCode());
}
```

- [ ] **Step 5: Run the FAT tests to verify they fail**
```bash
gradlew test --tests "io.openliberty.mcp.internal.fat.security.OAuthProtectedResourceMetadataTest"
```
Expected: FAIL because header augmentation and the well-known endpoint are not wired yet.

- [ ] **Step 6: Commit**
```bash
git add fat/src/io/openliberty/mcp/internal/fat/security/OAuthProtectedResourceMetadataTest.java
git commit -m "test: add failing MCP RFC9728 FAT coverage"
```

---

## Task 13: Remove protocol-version-specific assumptions from testing and wiring

**Files:**
- Modify: `fat/src/io/openliberty/mcp/internal/fat/security/OAuthProtectedResourceMetadataTest.java`
- Modify: any runtime draft code that introduced protocol-version gating
- Modify: `docs/superpowers/specs/2026-04-23-oauth-protected-resource-metadata-design.md`
- Modify: `docs/superpowers/plans/2026-04-23-oauth-protected-resource-metadata.md`

- [ ] **Step 1: Ensure there is no MCP protocol-version gate in the runtime design**
```java
// No shouldAdvertiseProtectedResourceMetadata(protocolVersion) gate should remain.
// The challenge parameter is controlled by security derivation + enablement only.
```

- [ ] **Step 2: Remove or avoid protocol-version-negative FAT assertions**
```java
// Do not add a test asserting that 2025-03-26 suppresses resource_metadata.
// A valid 401 with resource_metadata remains acceptable even if the client is not required to consume it.
```

- [ ] **Step 3: Run the MCP FAT test suite after removing the assumption**
```bash
gradlew test --tests "io.openliberty.mcp.internal.fat.security.OAuthProtectedResourceMetadataTest"
```
Expected: PASS once implementation is complete.

- [ ] **Step 4: Commit**
```bash
git add fat/src/io/openliberty/mcp/internal/fat/security/OAuthProtectedResourceMetadataTest.java docs/superpowers/specs/2026-04-23-oauth-protected-resource-metadata-design.md docs/superpowers/plans/2026-04-23-oauth-protected-resource-metadata.md
git commit -m "docs: remove MCP protocol-version gating assumption"
```

---

## Task 14: Run the full targeted verification matrix

**Files:**
- Test: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataResolverTest.java`
- Test: `../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal/test/io/openliberty/security/oidcclientcore/config/ProtectedResourceMetadataJsonBuilderTest.java`
- Test: `../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal/test/io/openliberty/security/oidcclient/wellknown/internal/OAuthProtectedResourceMetadataServletTest.java`
- Test: `../../../open-liberty/dev/com.ibm.ws.webcontainer.security/test/com/ibm/ws/webcontainer/security/internal/OAuthChallengeReplyTest.java`
- Test: `fat/src/io/openliberty/mcp/internal/fat/security/OAuthProtectedResourceMetadataTest.java`

- [ ] **Step 1: Run OIDC core unit tests**
```bash
cd ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal && gradlew test --tests "io.openliberty.security.oidcclientcore.config.*"
```
Expected: PASS.

- [ ] **Step 2: Run well-known servlet unit tests**
```bash
cd ../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal && gradlew test --tests "io.openliberty.security.oidcclient.wellknown.internal.*"
```
Expected: PASS.

- [ ] **Step 3: Run webcontainer/security challenge tests**
```bash
cd ../../../open-liberty/dev/com.ibm.ws.webcontainer.security && gradlew test --tests "com.ibm.ws.webcontainer.security.internal.OAuthChallengeReplyTest"
```
Expected: PASS.

- [ ] **Step 4: Run MCP RFC 9728 FAT coverage**
```bash
cd c:/Users/IssacAbraham/Documents/repos/open-liberty/dev/io.openliberty.mcp.internal_fat && gradlew test --tests "io.openliberty.mcp.internal.fat.security.OAuthProtectedResourceMetadataTest"
```
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add -A
git commit -m "test: verify RFC9728 protected resource metadata end to end"
```

---

## Task 15: Update docs and handoff notes

**Files:**
- Modify: `docs/superpowers/specs/2026-04-23-oauth-protected-resource-metadata-design.md`
- Modify: `docs/superpowers/plans/2026-04-23-oauth-protected-resource-metadata.md`
- Possibly modify: product docs discovered during implementation

- [ ] **Step 1: Add any exact implementation deltas discovered during coding**
```markdown
## Implementation Notes
- actual OIDC runtime bundle used:
- actual security auth-filter integration point used:
- actual well-known publisher bundle used:
- actual FAT server fixture path:
```

- [ ] **Step 2: Add final verification commands to the plan**
```markdown
- `cd ../../../open-liberty/dev/io.openliberty.security.oidcclientcore.internal && gradlew test --tests "io.openliberty.security.oidcclientcore.config.*"`
- `cd ../../../open-liberty/dev/io.openliberty.security.oidcclient.wellknown.internal && gradlew test --tests "io.openliberty.security.oidcclient.wellknown.internal.*"`
- `cd ../../../open-liberty/dev/com.ibm.ws.webcontainer.security && gradlew test --tests "com.ibm.ws.webcontainer.security.internal.OAuthChallengeReplyTest"`
- `cd c:/Users/IssacAbraham/Documents/repos/open-liberty/dev/io.openliberty.mcp.internal_fat && gradlew test --tests "io.openliberty.mcp.internal.fat.security.OAuthProtectedResourceMetadataTest"`
```

- [ ] **Step 3: Commit**
```bash
git add docs/superpowers/specs/2026-04-23-oauth-protected-resource-metadata-design.md docs/superpowers/plans/2026-04-23-oauth-protected-resource-metadata.md
git commit -m "docs: finalize RFC9728 implementation handoff"
```

---

## Self-Review Notes

Spec coverage check:
- RFC 9728 support remains implemented in the OIDC and security areas, not MCP-specific config.
- Well-known document publication is covered through a new OIDC-owned servlet bundle.
- `resource_metadata` challenge augmentation is covered in security/webcontainer integration.
- Manual enablement is covered explicitly.
- Security-owned `authFilter`-based URL derivation is covered explicitly.
- FAT coverage validates enabled headers, disabled omission, well-known JSON, and 404 behavior.

Placeholder scan:
- Remaining placeholders are limited to “actual file discovered in Task 1” only where this workspace did not contain the runtime file. Resolve those before implementation begins.
- If the new bundle path differs from `io.openliberty.security.oidcclient.wellknown.internal`, update the plan first and then execute.

Type consistency:
- Keep `ProtectedResourceMetadata`, `ProtectedResourceMetadataResolver`, and `ProtectedResourceMetadataJsonBuilder` names consistent across all tasks unless the first implementation step establishes the real in-repo naming convention.