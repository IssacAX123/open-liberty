package io.openliberty.mcp.monitor;

import io.openliberty.mcp.annotations.Tool;

public record McpStatsMonitor (String classAndMethod, Tool tool) {

	public void recordToolCall() {
	}

}

