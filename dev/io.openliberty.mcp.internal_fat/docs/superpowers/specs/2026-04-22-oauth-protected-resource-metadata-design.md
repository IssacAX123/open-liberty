# OAuth 2.0 Protected Resource Metadata (RFC 9728) Implementation for Open Liberty MCP Server

**Date:** 2026-04-22  
**Author:** AI Assistant  
**Status:** Design Review  
**Target Feature:** MCP Server OAuth Integration

## Executive Summary

This design document specifies the implementation of OAuth 2.0 Protected Resource Metadata (RFC 9728) support for Open Liberty's MCP (Model Context Protocol) server feature. The implementation enables MCP servers to advertise their OAuth 2.0 authorization requirements through standardized metadata documents and WWW-Authenticate headers, allowing MCP clients to discover and interact with remote authorization servers.

## Background

### MCP Requirements

The MCP specification requires that Liberty MCP servers:

1. Return 401 responses with a `resource_metadata` attribute pointing to the protected resource metadata document
2. Serve a protected resource metadata JSON document compliant with RFC 9728
3. Include the optional `authorization_servers` parameter containing authorization server identifiers
4. Serve metadata from a well-known URI derived from the MCP server path

**Well-Known URI Derivation:**
- MCP server path: `https://example.com/MyApplication/mcp`
- Well-known path: `https://example.com/.well-known/oauth-protected-resource/MyApplication/mcp`

### MCP Protocol Version Support

- **Supported:** 2025-06-18, 2025-11-25 (and newer versions)
- **Not Supported:** 2025-03-26 (assumes MCP server is also the authorization server)

The implementation will only apply RFC 9728 metadata for supported protocol versions.

### Existing Infrastructure

Open Liberty already provides:
- `com.ibm.ws.security.oauth20.util.OAuth20ProviderUtils.handleOAuthChallenge()` - Builds WWW-Authenticate headers for 401 responses from OIDC clients
- OAuth 2.0 provider configuration infrastructure
- MCP server implementation with security integration

## Design Decisions

### Key Decisions from Brainstorming

1. **Configuration Model:** Hybrid approach - support both static configuration (server.xml) with fallback to dynamic generation based on detected OAuth providers
2. **401 Response Behavior:** Return 401 with resource_metadata for both authentication and authorization failures to guide clients to the authorization server
3. **Multiple MCP Instances:** Support multiple MCP servers with different paths, each with its own well-known endpoint
4. **Caching Strategy:** No caching - generate metadata document fresh on each request to ensure current configuration

### Implementation Approach

**Selected: Servlet Filter-Based Implementation**

This approach provides:
- Clean separation of concerns
- Minimal changes to existing MCP server code
- Easy to enable/disable via configuration
- Alignment with Liberty servlet patterns
- Good balance of modularity and maintainability

## Architecture

### Component Overview

The implementation consists of four main components:

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Request                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              OAuth Challenge Filter                          │
│  - Intercepts MCP requests                                   │
│  - Detects auth failures (401/403)                          │
│  - Generates WWW-Authenticate header                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │   Authenticated?      │
         └───────┬───────────────┘
                 │ No
                 ▼
┌─────────────────────────────────────────────────────────────┐
│  Return 401 + WWW-Authenticate                               │
│  (includes resource_metadata URI)                            │
└─────────────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│  Client → GET /.well-known/oauth-protected-resource/{path}  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         Well-Known Metadata Servlet                          │
│  - Handles /.well-known/oauth-protected-resource/**        │
│  - Extracts MCP path from URI                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         Metadata Configuration Service                       │
│  - Reads server.xml configuration                           │
│  - Discovers OAuth providers                                │
│  - Provides hybrid configuration                            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Metadata Generator                              │
│  - Builds RFC 9728-compliant JSON                           │
│  - Includes required + optional parameters                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         Returns RFC 9728 JSON Document                       │
└─────────────────────────────────────────────────────────────┘
```

## Component Specifications

### 1. OAuth Challenge Filter

**Class:** `io.openliberty.mcp.internal.oauth.McpOAuthChallengeFilter`

**Responsibilities:**
- Intercept all MCP server requests (pattern: `/mcp/*` or configured path)
- Detect authentication/authorization failures (401/403 from downstream)
- Generate RFC 9728-compliant WWW-Authenticate header
- Add `resource_metadata` parameter pointing to well-known URI

**Key Methods:**

```java
public class McpOAuthChallengeFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        // Wrap response to capture status code
        // Continue filter chain
        // If 401/403, modify response with WWW-Authenticate header
    }
    
    private String buildWWWAuthenticateHeader(String mcpPath, 
                                              boolean isAuthorizationFailure) {
        // Build Bearer challenge with resource_metadata
        // Include error/error_description for authorization failures
    }
    
    private String deriveWellKnownUri(HttpServletRequest request, 
                                      String mcpPath) {
        // Construct /.well-known/oauth-protected-resource/{mcpPath}
    }
}
```

**Filter Configuration:**
- URL Pattern: Matches configured MCP server paths
- Filter Order: After authentication filters, before MCP request processing
- Enabled: When OAuth metadata configuration is present

### 2. Well-Known Metadata Servlet

**Class:** `io.openliberty.mcp.internal.oauth.OAuthProtectedResourceMetadataServlet`

**Responsibilities:**
- Handle GET requests to `/.well-known/oauth-protected-resource/**`
- Extract MCP path from request URI
- Delegate to Metadata Generator
- Return JSON with proper content-type (`application/json`)

**URL Mapping Examples:**
- `/.well-known/oauth-protected-resource/mcp` → MCP at `/mcp`
- `/.well-known/oauth-protected-resource/app1/mcp` → MCP at `/app1/mcp`
- `/.well-known/oauth-protected-resource/MyApplication/mcp` → MCP at `/MyApplication/mcp`

**Key Methods:**

```java
@WebServlet(urlPatterns = "/.well-known/oauth-protected-resource/*")
public class OAuthProtectedResourceMetadataServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, 
                        HttpServletResponse response) 
            throws ServletException, IOException {
        // Extract MCP path from request URI
        // Validate MCP path exists
        // Generate metadata document
        // Return JSON response
    }
    
    private String extractMcpPath(String requestUri) {
        // Parse URI to extract MCP path
    }
}
```

### 3. Metadata Configuration Service

**Class:** `io.openliberty.mcp.internal.oauth.McpOAuthMetadataConfigService`

**Responsibilities:**
- Read OAuth configuration from server.xml (`<mcpServer>` element)
- Discover OAuth providers from Liberty's OAuth feature
- Provide hybrid configuration (static overrides dynamic)
- Support per-MCP-instance configuration

**Configuration Schema (server.xml):**

```xml
<server>
    <featureManager>
        <feature>mcpServer-1.0</feature>
        <feature>oauth-2.0</feature>
    </featureManager>
    
    <mcpServer id="myMcpServer" path="/mcp">
        <oauthMetadata>
            <!-- Required: Authorization server(s) -->
            <authorizationServer>https://auth.example.com</authorizationServer>
            <authorizationServer>https://backup-auth.example.com</authorizationServer>
            
            <!-- Optional: Override resource identifier -->
            <resource>https://api.example.com/mcp</resource>
            
            <!-- Optional: Bearer methods supported -->
            <bearerMethod>header</bearerMethod>
            <bearerMethod>body</bearerMethod>
            
            <!-- Optional: Resource documentation URI -->
            <resourceDocumentation>https://example.com/mcp/docs</resourceDocumentation>
        </oauthMetadata>
    </mcpServer>
    
    <!-- OAuth provider configuration (for dynamic discovery) -->
    <oauthProvider id="OAuthProvider">
        <localStore>
            <client name="client01" secret="secret" 
                    displayname="Client 01" enabled="true" />
        </localStore>
    </oauthProvider>
</server>
```

**Key Methods:**

```java
@Component(service = McpOAuthMetadataConfigService.class)
public class McpOAuthMetadataConfigService {
    
    public OAuthMetadataConfig getConfiguration(String mcpPath) {
        // Get static configuration from server.xml
        // Get dynamic configuration from OAuth providers
        // Merge with static overriding dynamic
    }
    
    private OAuthMetadataConfig getStaticConfiguration(String mcpPath) {
        // Read from server.xml
    }
    
    private List<String> discoverAuthorizationServers() {
        // Query Liberty OAuth provider configuration
    }
}
```

### 4. Metadata Generator

**Class:** `io.openliberty.mcp.internal.oauth.OAuthMetadataGenerator`

**Responsibilities:**
- Build RFC 9728-compliant JSON documents
- Include required parameters (`resource`, `authorization_servers`)
- Include optional parameters when configured
- Validate generated metadata against RFC 9728

**Key Methods:**

```java
public class OAuthMetadataGenerator {
    
    public String generateMetadata(String mcpPath, 
                                   OAuthMetadataConfig config) {
        // Build JSON document
        // Include required fields
        // Include optional fields if configured
        // Validate against RFC 9728
    }
    
    private JsonObject buildMetadataJson(String resource, 
                                        List<String> authServers,
                                        OAuthMetadataConfig config) {
        // Construct JSON object
    }
}
```

## RFC 9728 Compliance

### Protected Resource Metadata Document Structure

**Minimum Required Fields:**

```json
{
  "resource": "https://example.com/mcp",
  "authorization_servers": [
    "https://auth.example.com"
  ]
}
```

**Full Example with Optional Fields:**

```json
{
  "resource": "https://example.com/MyApplication/mcp",
  "authorization_servers": [
    "https://auth.example.com",
    "https://backup-auth.example.com"
  ],
  "bearer_methods_supported": ["header", "body"],
  "resource_documentation": "https://example.com/mcp/docs",
  "resource_signing_alg_values_supported": ["RS256", "ES256"]
}
```

### WWW-Authenticate Header Format

**For Unauthenticated Requests (401):**

```
WWW-Authenticate: Bearer realm="MCP Server", 
                  resource_metadata="https://example.com/.well-known/oauth-protected-resource/mcp"
```

**For Authorization Failures (401 with error):**

```
WWW-Authenticate: Bearer realm="MCP Server",
                  error="insufficient_scope",
                  error_description="The request requires higher privileges",
                  resource_metadata="https://example.com/.well-known/oauth-protected-resource/mcp"
```

**Error Codes:**
- `invalid_token` - Token is malformed, expired, or invalid
- `insufficient_scope` - Token lacks required scope
- `invalid_request` - Request is malformed

### Field Descriptions

| Field | Required | Description |
|-------|----------|-------------|
| `resource` | Yes | URI identifying the protected resource (MCP server) |
| `authorization_servers` | Yes | Array of authorization server identifiers |
| `bearer_methods_supported` | No | Methods for presenting bearer tokens (header, body, query) |
| `resource_documentation` | No | URI for human-readable documentation |
| `resource_signing_alg_values_supported` | No | JWS signing algorithms supported |

## Error Handling

### Configuration Errors

| Error Scenario | Behavior |
|----------------|----------|
| No OAuth providers configured | Return 500, log error message |
| Invalid authorization server URI | Log warning, exclude from metadata |
| Malformed server.xml | Fail gracefully, use dynamic discovery only |
| Missing required fields | Use sensible defaults, log warning |

### Runtime Errors

| Error Scenario | HTTP Status | Behavior |
|----------------|-------------|----------|
| Well-known endpoint for non-existent MCP path | 404 | Return not found |
| Metadata generation fails | 500 | Return generic error, log details |
| OAuth provider discovery fails | 200 | Use static configuration only |
| Invalid request format | 400 | Return bad request |

### Security Considerations

1. **Information Disclosure:** Don't expose internal server details in error messages
2. **Logging:** Log detailed errors server-side for debugging
3. **Rate Limiting:** Consider rate limiting well-known endpoint to prevent DoS
4. **Validation:** Validate all configuration inputs
5. **HTTPS:** Recommend HTTPS for production deployments

## Testing Strategy

### Unit Tests

**Test Coverage:**
- Metadata generator with various configurations
- WWW-Authenticate header formatting
- Well-known URI derivation logic
- Configuration service with static/dynamic scenarios
- Error handling for all error scenarios
- JSON validation against RFC 9728

**Test Classes:**
- `OAuthMetadataGeneratorTest`
- `McpOAuthChallengeFilterTest`
- `OAuthProtectedResourceMetadataServletTest`
- `McpOAuthMetadataConfigServiceTest`

### Integration Tests (FAT)

**Test Scenarios:**
1. End-to-end 401 response with resource_metadata
2. Well-known endpoint returns valid RFC 9728 JSON
3. Multiple MCP instances with different OAuth configs
4. Protocol version compatibility (2025-06-18, 2025-11-25)
5. Hybrid configuration (static overrides dynamic)
6. Error scenarios (missing config, invalid URIs)
7. Security integration with existing auth mechanisms

**Test Servers:**
- `mcp-server-oauth-metadata` - Basic OAuth metadata setup
- `mcp-server-oauth-multi` - Multiple MCP instances
- `mcp-server-oauth-static` - Static configuration only
- `mcp-server-oauth-dynamic` - Dynamic discovery only
- `mcp-server-oauth-errors` - Error handling scenarios

**Test Classes:**
- `OAuthProtectedResourceMetadataTest`
- `OAuthChallengeFilterTest`
- `MultiInstanceOAuthMetadataTest`
- `OAuthMetadataErrorHandlingTest`

### Conformance Testing

**RFC 9728 Compliance:**
- Validate metadata document structure
- Verify required fields present
- Test optional fields when configured
- Validate JSON schema
- Test WWW-Authenticate header format

## Implementation Phases

### Phase 1: Core Infrastructure (Week 1-2)
- Implement Metadata Generator
- Implement Configuration Service
- Unit tests for core components
- Basic server.xml schema

### Phase 2: Servlet Components (Week 2-3)
- Implement Well-Known Metadata Servlet
- Implement OAuth Challenge Filter
- Filter registration and ordering
- Unit tests for servlet components

### Phase 3: Integration & Testing (Week 3-4)
- Integration tests (FAT)
- Error handling refinement
- Documentation
- Performance testing

### Phase 4: Review & Polish (Week 4-5)
- Code review
- Security review
- Documentation review
- Final testing

## Backwards Compatibility

### Compatibility Guarantees

1. **Opt-in Feature:** OAuth metadata is only enabled when explicitly configured
2. **Existing MCP Servers:** Continue working unchanged without OAuth configuration
3. **No Breaking Changes:** No modifications to existing MCP APIs
4. **Security Integration:** Compatible with existing security configurations
5. **Protocol Versions:** Only applies to supported MCP protocol versions

### Migration Path

**For Existing Deployments:**

1. Update to new Liberty version with RFC 9728 support
2. Add OAuth provider configuration (if not already present)
3. Add `<oauthMetadata>` element to `<mcpServer>` configuration
4. Test with MCP clients supporting 2025-06-18+ protocol
5. Monitor logs for any configuration warnings

**Example Migration:**

```xml
<!-- Before -->
<mcpServer id="myMcpServer" path="/mcp">
    <!-- No OAuth metadata -->
</mcpServer>

<!-- After -->
<mcpServer id="myMcpServer" path="/mcp">
    <oauthMetadata>
        <authorizationServer>https://auth.example.com</authorizationServer>
    </oauthMetadata>
</mcpServer>
```

## Performance Considerations

### Expected Performance Impact

1. **Filter Overhead:** Minimal - only processes 401/403 responses
2. **Metadata Generation:** Lightweight JSON generation, no caching needed
3. **Configuration Lookup:** OSGi service lookup, cached by framework
4. **Well-Known Endpoint:** Simple servlet, no database queries

### Optimization Strategies

1. **Lazy Initialization:** Initialize components only when needed
2. **Efficient JSON Generation:** Use streaming JSON APIs
3. **Minimal String Operations:** Avoid unnecessary string concatenation
4. **Filter Ordering:** Place after authentication to minimize processing

### Performance Testing

- Load test well-known endpoint (target: <10ms response time)
- Measure filter overhead (target: <1ms additional latency)
- Test with multiple concurrent MCP instances
- Monitor memory usage with various configurations

## Monitoring & Observability

### Logging

**Log Levels:**
- **INFO:** Configuration loaded, OAuth metadata enabled
- **WARNING:** Invalid configuration, fallback to defaults
- **ERROR:** Metadata generation failures, configuration errors
- **DEBUG:** Detailed request/response information

**Log Messages:**
```
CWMCP0100I: OAuth Protected Resource Metadata enabled for MCP server at path: {0}
CWMCP0101I: Authorization servers configured: {0}
CWMCP0102W: Invalid authorization server URI: {0}, excluding from metadata
CWMCP0103E: Failed to generate OAuth metadata for MCP path: {0}
CWMCP0104W: No OAuth providers configured, using static configuration only
```

### Metrics

**Recommended Metrics:**
- Well-known endpoint request count
- Well-known endpoint response time
- 401 responses with resource_metadata count
- Configuration errors count
- Metadata generation failures count

## Documentation Requirements

### User Documentation

1. **Feature Overview:** Introduction to RFC 9728 support
2. **Configuration Guide:** How to configure OAuth metadata
3. **Examples:** Common configuration scenarios
4. **Troubleshooting:** Common issues and solutions
5. **Security Best Practices:** Recommendations for production

### Developer Documentation

1. **Architecture Overview:** Component interactions
2. **API Documentation:** Public APIs and SPIs
3. **Extension Points:** How to customize behavior
4. **Testing Guide:** How to test OAuth metadata
5. **Debugging Guide:** How to debug issues

## Security Review Checklist

- [ ] Input validation for all configuration parameters
- [ ] No sensitive information in error messages
- [ ] Proper error handling for all failure scenarios
- [ ] Rate limiting considerations documented
- [ ] HTTPS recommendations documented
- [ ] Authorization checks for well-known endpoint (public access)
- [ ] Logging doesn't expose sensitive data
- [ ] Configuration validation prevents injection attacks

## Open Questions

None - all design decisions have been made through the brainstorming process.

## References

1. [RFC 9728 - OAuth 2.0 Protected Resource Metadata](https://datatracker.ietf.org/doc/html/rfc9728)
2. [MCP Specification - Authorization](https://spec.modelcontextprotocol.io/specification/2025-11-25/authorization/)
3. [RFC 6750 - OAuth 2.0 Bearer Token Usage](https://datatracker.ietf.org/doc/html/rfc6750)
4. [RFC 8414 - OAuth 2.0 Authorization Server Metadata](https://datatracker.ietf.org/doc/html/rfc8414)

## Appendix A: Example Configurations

### Minimal Configuration

```xml
<mcpServer id="myMcpServer" path="/mcp">
    <oauthMetadata>
        <authorizationServer>https://auth.example.com</authorizationServer>
    </oauthMetadata>
</mcpServer>
```

### Full Configuration

```xml
<mcpServer id="myMcpServer" path="/mcp">
    <oauthMetadata>
        <authorizationServer>https://auth.example.com</authorizationServer>
        <authorizationServer>https://backup-auth.example.com</authorizationServer>
        <resource>https://api.example.com/mcp</resource>
        <bearerMethod>header</bearerMethod>
        <bearerMethod>body</bearerMethod>
        <resourceDocumentation>https://example.com/mcp/docs</resourceDocumentation>
    </oauthMetadata>
</mcpServer>
```

### Multiple MCP Instances

```xml
<mcpServer id="publicMcp" path="/public/mcp">
    <oauthMetadata>
        <authorizationServer>https://public-auth.example.com</authorizationServer>
    </oauthMetadata>
</mcpServer>

<mcpServer id="internalMcp" path="/internal/mcp">
    <oauthMetadata>
        <authorizationServer>https://internal-auth.example.com</authorizationServer>
        <resource>https://internal-api.example.com/mcp</resource>
    </oauthMetadata>
</mcpServer>
```

## Appendix B: HTTP Request/Response Examples

### Example 1: Unauthenticated Request

**Request:**
```http
POST /mcp HTTP/1.1
Host: example.com
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "id": 1
}
```

**Response:**
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="MCP Server", resource_metadata="https://example.com/.well-known/oauth-protected-resource/mcp"
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "error": {
    "code": -32001,
    "message": "Unauthorized"
  },
  "id": 1
}
```

### Example 2: Metadata Document Request

**Request:**
```http
GET /.well-known/oauth-protected-resource/mcp HTTP/1.1
Host: example.com
Accept: application/json
```

**Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json
Cache-Control: no-cache

{
  "resource": "https://example.com/mcp",
  "authorization_servers": [
    "https://auth.example.com"
  ],
  "bearer_methods_supported": ["header"],
  "resource_documentation": "https://example.com/mcp/docs"
}
```

### Example 3: Authorization Failure

**Request:**
```http
POST /mcp HTTP/1.1
Host: example.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "admin-tool"
  },
  "id": 2
}
```

**Response:**
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="MCP Server", error="insufficient_scope", error_description="The request requires higher privileges", resource_metadata="https://example.com/.well-known/oauth-protected-resource/mcp"
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "error": {
    "code": -32002,
    "message": "Insufficient scope"
  },
  "id": 2
}
```

## Conclusion

This design provides a comprehensive, RFC 9728-compliant implementation of OAuth 2.0 Protected Resource Metadata for Open Liberty's MCP server feature. The servlet filter-based approach offers clean separation of concerns, minimal impact on existing code, and flexibility for future enhancements. The hybrid configuration model (static + dynamic) provides both explicit control and automatic discovery, meeting the needs of various deployment scenarios.