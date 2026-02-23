package io.openliberty.mcp.internal.monitor;

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Meter;

import io.openliberty.mcp.annotations.Tool;

public class McpStats extends Meter implements McpStatsMXBean {
	
	private String toolName;
	private Counter toolCallCount;
	
	public McpStats(String toolName) {
		this.toolName = toolName;
		
		toolCallCount = new Counter();
		toolCallCount.setDescription("Total calls of an MCP tool");

	}

	public String getToolName() {
		return toolName;
	}
	
	public void incrementToolCallCountBy(int i) {
		toolCallCount.incrementBy(i);
	}
	
	@Override
	public long getToolCallCount() {
		return toolCallCount.getCurrentValue();
	}


}
