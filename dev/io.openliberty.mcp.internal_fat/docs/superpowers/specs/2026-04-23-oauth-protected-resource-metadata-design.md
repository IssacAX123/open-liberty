# OAuth 2.0 Protected Resource Metadata for OIDC-Protected MCP Resources

**Date:** 2026-04-23  
**Author:** AI Assistant  
**Status:** Revised After Review  
**Scope:** Design for general RFC 9728 support owned by the OIDC client and security features and consumed by MCP

## Executive Summary

This design defines how Open Liberty should support OAuth 2.0 Protected Resource Metadata as specified by RFC 9728 for resources protected through the `oidcConnectClient` feature, with MCP as an important consumer of that capability.

The goal is not to add an MCP-specific OAuth metadata subsystem. Instead, the implementation should treat RFC 9728 support as a general protected-resource capability owned by the OIDC client and security areas. MCP will consume that capability by returning `401 Unauthorized` responses with a `WWW-Authenticate` challenge that includes a `resource_metadata` parameter when the underlying security layer can derive the correct metadata URL.

This work is explicitly limited to Liberty acting as a protected resource that uses a remote authorization server through `oidcConnectClient`. It does not include Liberty acting as an authorization server, and it does not include OAuth 2.0 Authorization Server Metadata support from RFC 8414.

The protected resource metadata endpoint must be manually enabled through configuration. The capability is opt-in rather than automatically exposed for every `oidcConnectClient` configuration.

## Problem Statement

When Liberty acts as an MCP server protected by OAuth/OIDC, the MCP client benefits from being able to discover OAuth protected resource metadata through the standard RFC 9728 mechanism:

1. Return a `401` response with a `WWW-Authenticate` header that includes a `resource_metadata` attribute.
2. Serve a protected resource metadata JSON document from the RFC 9728 well-known location derived from the protected resource path.

For example, if the protected MCP endpoint is:

`https://example.com/MyApplication/mcp`

then the metadata document must be served from:

`https://example.com/.well-known/oauth-protected-resource/MyApplication/mcp`

However, the solution must not be designed as an MCP-only feature. The protected resource metadata, including the published well-known document and the challenge metadata pointer, must be derived from general `oidcConnectClient` configuration and the relevant authentication filter mapping, not from MCP endpoint configuration.

It is also important not to put responsibility in MCP for deriving the protected resource URL or the `resource_metadata` URL. MCP does not own the `authFilter` configuration and may not have enough information to derive the correct protected resource mapping. That derivation belongs in the security feature that already knows how the request was matched to the relevant authentication filter and OIDC client configuration.

## Scope

### In Scope

- RFC 9728 protected resource metadata support for resources protected through `oidcConnectClient`
- A new well-known metadata publishing bundle in the OIDC client feature area
- Derivation of protected resource metadata from the relevant authentication filter configuration and associated `oidcConnectClient` configuration
- Inclusion of `resource_metadata` in bearer challenges for unauthorized MCP requests when the security layer can derive it
- Explicit opt-in configuration to enable protected resource metadata publication for a given protected resource

### Out of Scope

- Liberty acting as the authorization server
- RFC 8414 authorization server metadata
- Any design that requires new MCP-specific OAuth configuration as the source of truth
- Requiring MCP to compute the protected resource path or metadata URL itself
- Generalizing this work to the older `oauth-2.0` feature instead of `oidcConnectClient`

## Design Principles

1. **OIDC and security ownership**  
   RFC 9728 support belongs to the OIDC client and security areas because those features already own remote authorization-server integration, authentication filter mapping, and bearer challenge generation.

2. **Configuration truthfulness**  
   The metadata document must contain only values that Liberty can truthfully derive from the protected resource mapping and `oidcConnectClient` configuration.

3. **No MCP-specific source of truth**  
   MCP may consume the capability, but it must not become the place where OAuth protected resource metadata is configured or derived.

4. **Security-owned URL derivation**  
   The protected resource URL and `resource_metadata` URL must be derived by the security side using `authFilter` knowledge, not by MCP.

5. **Manual enablement**  
   Resource metadata publication must be explicitly enabled by configuration. Existing deployments should not begin exposing RFC 9728 metadata automatically.

6. **Well-bounded components**  
   The solution should be split into a small number of focused components with clear ownership boundaries.

## Recommended Approach

Three implementation shapes were considered:

1. An MCP-local implementation
2. A general `oidcConnectClient`-owned metadata service with security integration points
3. A hybrid registration model where MCP explicitly registers protected resources with an OIDC-owned service

The recommended design is the second approach: a general `oidcConnectClient`-owned metadata capability with the security feature deriving and attaching the `resource_metadata` URL, and MCP simply benefiting from standard unauthorized challenge behavior.

This approach is preferred because it matches the ownership boundary implied by the existing Liberty features, keeps the protected-resource metadata logic reusable for non-MCP consumers, avoids duplicating OAuth/OIDC logic inside the MCP feature, and avoids forcing MCP to guess protected resource URLs it does not own.

## Architecture Overview

The implementation is divided into four focused units.

### 1. Protected Resource Metadata Configuration and Resolver

This component lives in the OIDC client area and answers a simple question:

> For a given protected request path and associated OIDC client configuration, what RFC 9728 metadata can Liberty truthfully publish?

Its responsibilities are:

- identify whether protected resource metadata publication is enabled for the matched protected resource
- resolve the associated `oidcConnectClient` configuration
- hold the RFC 9728 publication settings that are intentionally enabled
- derive the canonical protected resource identifier
- identify optional values such as `authorization_servers` when available from configuration
- refuse to manufacture values that are not clearly derivable

This component does not decide which request path was actually protected. It consumes that information from the security side.

### 2. Security-Owned Protected Resource Path and Metadata URL Derivation

The security layer is responsible for deriving the actual protected resource path and the resulting `resource_metadata` URL for the current request.

Its responsibilities are:

- determine which `authFilter` matched the current request
- determine the protected resource path associated with that match
- derive the well-known RFC 9728 metadata URL from that protected resource path
- pass the derived metadata URL into the bearer challenge path when metadata publication is enabled and resolvable

This is a core boundary in the design: MCP does not compute the metadata URL because it does not own the `authFilter` mapping and may not have enough information to do so correctly.

### 3. Well-Known Metadata Publisher Bundle

A new bundle belonging to the OIDC client feature serves the metadata documents.

This bundle must declare:

`Web-ContextPath: /.well-known/oauth-protected-resource/`

Its servlet handles `GET` requests below that path, maps the remaining request path back to the protected resource path, asks the metadata resolver for the RFC 9728 document for that protected resource, and returns RFC 9728 JSON.

For example:

- request to `/.well-known/oauth-protected-resource/mcp` resolves metadata for `/mcp`
- request to `/.well-known/oauth-protected-resource/MyApplication/mcp` resolves metadata for `/MyApplication/mcp`

If the path does not correspond to a protected resource with enabled and derivable metadata, the endpoint should not invent a response. It should return `404 Not Found`.

### 4. Bearer Challenge Augmentation

Unauthorized request handling should continue to use Liberty’s established bearer challenge behavior. The new behavior is additive: when the unauthorized request maps to a protected resource with enabled RFC 9728 metadata, the `WWW-Authenticate` challenge includes a `resource_metadata` attribute.

This part of the design should reuse existing OAuth challenge-building logic where possible, including the established handling around `com.ibm.ws.security.oauth20.util.OAuth20ProviderUtils.handleOAuthChallenge()`, rather than creating a parallel challenge formatter.

The important result is that the bearer challenge points the client at the correct RFC 9728 document for that protected resource, and that this pointer is derived by the security side using the actual matched protection configuration.

## End-to-End Flow

The expected end-to-end flow is:

1. A client calls an MCP endpoint protected through `oidcConnectClient`.
2. Liberty determines that the request is unauthorized and produces a bearer challenge.
3. The security challenge path determines which `authFilter` and protected resource mapping applied to the request.
4. If RFC 9728 publication is enabled and resolvable for that protected resource, the `WWW-Authenticate` header includes:

   `resource_metadata="https://host/.well-known/oauth-protected-resource/<resource-path>"`

5. The client performs `GET` on that well-known URI.
6. The well-known metadata servlet resolves the protected resource from the path.
7. The servlet returns an RFC 9728 protected resource metadata JSON document.

This flow is valid regardless of whether a given MCP protocol version requires the client to consume `resource_metadata`. Sending the `401` challenge with `resource_metadata` is still valid even for older MCP protocol versions that do not require the client to handle it.

## Metadata Derivation Rules

### Protected Resource Path

The protected resource path must be derived from the relevant authentication filter mapping, not from MCP endpoint configuration.

This is a critical design constraint because the solution must work for general `oidcConnectClient` consumers, not only for MCP.

### Metadata Document Location

The document location follows RFC 9728 well-known derivation rules:

- protected resource: `https://example.com/MyApplication/mcp`
- metadata document: `https://example.com/.well-known/oauth-protected-resource/MyApplication/mcp`

The suffix after `/.well-known/oauth-protected-resource/` corresponds to the protected resource path without the leading slash.

### Manual Enablement

Protected resource metadata publication should be explicitly enabled through configuration. The intent is:

- a protected resource without explicit enablement continues to behave exactly as it does today
- no RFC 9728 well-known document is served unless the feature is enabled for that protected resource
- no `resource_metadata` parameter is attached unless the feature is enabled and the metadata URL can be derived

The exact configuration surface should remain in the `oidcConnectClient` area rather than MCP.

### Metadata Fields

The implementation should include only fields that can be correctly derived.

#### Required RFC 9728 fields

At minimum, the design assumes support for the required RFC 9728 metadata fields that Liberty can derive for the protected resource document.

#### Optional `authorization_servers`

The `authorization_servers` field is optional in this design and should be included when Liberty can identify the relevant authorization server identifiers from `oidcConnectClient` configuration.

If Liberty cannot identify those values confidently, it should not invent or guess them.

#### Additional Optional Fields

Additional RFC 9728 fields may be included only when they can be truthfully sourced from configuration that belongs in the OIDC client area.

This design does not require inventing new MCP-specific configuration for optional metadata fields.

## MCP-Specific Behavior

MCP consumes the general protected-resource metadata capability with the following rules:

- unauthorized MCP requests may return `401` with a bearer challenge that includes `resource_metadata` when the request is backed by an OIDC-protected resource with enabled and derivable metadata
- missing credentials and invalid credentials should both surface the metadata pointer when the normal unauthorized bearer challenge path runs
- authorization failures should include the metadata pointer only if the existing Liberty challenge behavior legitimately returns a bearer challenge for that case
- MCP should not compute the protected resource URL or metadata URL itself

MCP is therefore a consumer of the general security/OIDC capability, not the owner of protocol gating or URL derivation logic.

## Error Handling

### Well-Known Endpoint Errors

- If a requested well-known path does not match any known protected resource, return `404 Not Found`.
- If the path maps to a protected resource but metadata publication is not enabled, return `404 Not Found`.
- If the path maps to a protected resource but Liberty cannot derive valid RFC 9728 metadata, prefer not exposing metadata rather than returning misleading content.
- Error responses must not expose internal server configuration details.

### Configuration and Derivation Errors

- Log server-side diagnostics when metadata resolution fails or configuration is inconsistent.
- Do not emit metadata fields whose values are ambiguous or unverifiable.
- Prefer omission over fabrication for optional values.
- If enablement is configured but required metadata inputs are missing, fail closed by not exposing the metadata endpoint or challenge parameter.

### Challenge Behavior Errors

- If the request is unauthorized but there is no reliable RFC 9728 metadata for that protected resource, preserve the normal unauthorized response behavior without adding a misleading `resource_metadata` parameter.

## Testing Strategy

### Unit Testing

Unit coverage should live primarily with the OIDC-side and security-side implementation and cover:

- protected resource path resolution from authentication filter mappings
- association between protected resources and `oidcConnectClient` configuration
- explicit enablement behavior
- RFC 9728 JSON generation
- well-known URI derivation
- bearer challenge augmentation with `resource_metadata`
- omission behavior when metadata cannot be derived

### Integration and FAT Testing

Integration and FAT coverage should verify:

1. an unauthorized MCP request returns `401` and a `WWW-Authenticate` header containing `resource_metadata` when metadata publication is enabled
2. the well-known endpoint returns RFC 9728 JSON for an OIDC-protected MCP resource when metadata publication is enabled
3. a protected resource without metadata enablement does not advertise `resource_metadata`
4. paths without matching protected-resource metadata return `404`
5. multiple protected resources with different authentication filter and OIDC mappings resolve independently

For FAT coverage, Liberty should be exercised against an external Keycloak authorization server started with Testcontainers. Keycloak supplies the runtime `providerURI` / issuer metadata used by `oidcConnectClient`, while Liberty remains only the protected resource / MCP server under test.

Existing MCP FAT patterns around unauthorized responses, such as those exercised through helpers like `AuthHelper`, should be extended rather than replaced.

## Ownership and Packaging

### OIDC Client Area

The following responsibilities belong in the OIDC client area:

- protected resource metadata configuration
- metadata resolution from enabled OIDC client settings
- metadata JSON generation
- well-known publishing servlet

### Security Area

The following responsibilities belong in the security area:

- determining which protected resource mapping matched the request
- deriving the `resource_metadata` URL for the current request
- challenge augmentation support for bearer metadata pointers

### MCP Area

The MCP area should be responsible only for:

- remaining compatible with the standard unauthorized bearer challenge behavior
- exercising the behavior through FAT coverage

### New Bundle Requirement

The well-known metadata servlet should be packaged in a new bundle belonging to the OIDC client feature, and that bundle must declare:

`Web-ContextPath: /.well-known/oauth-protected-resource/`

This keeps the endpoint ownership aligned with the configuration and metadata logic it serves.

## Backward Compatibility

This design is intentionally narrow and backward-compatible:

- existing MCP deployments without enabled OIDC protected-resource metadata continue to work unchanged
- no Liberty authorization-server capability is introduced
- no new MCP-specific OAuth configuration becomes mandatory
- older and newer MCP protocol versions can still receive the same valid `401` challenge format; client support expectations are outside this server-side design
- resource metadata publication is opt-in, so existing deployments do not begin exposing new well-known endpoints automatically

## Future Work

Potential future work, intentionally excluded from this design, includes:

- Liberty acting as an authorization server for MCP deployments
- RFC 8414 authorization server metadata publication
- broader non-MCP documentation and examples for RFC 9728 once the core general capability exists

## Summary

This design keeps RFC 9728 support in the correct ownership boundary: the `oidcConnectClient` and security feature areas. It introduces explicit opt-in protected-resource metadata publication, security-owned derivation of the protected resource and metadata URL from `authFilter` knowledge, a new well-known metadata publisher bundle, and additive bearer challenge augmentation. MCP then consumes that capability through standard unauthorized responses without owning the derivation logic.

That structure satisfies the MCP requirements you outlined while remaining reusable, configuration-truthful, manually enabled, and aligned with Open Liberty feature boundaries.