io.openliberty.mcpServer-1.0-mpTelemetry-2.0.feature
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mcpServer-1.0-mpTelemetry-2.0
IBM-Provision-Capability: \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mcpServer-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpTelemetry-2.0)(osgi.identity=io.openliberty.mpTelemetry-2.1)))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.mcp.telemetry
kind=ga
edition=core