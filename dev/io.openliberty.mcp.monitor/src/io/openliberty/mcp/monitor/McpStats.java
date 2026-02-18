package io.openliberty.mcp.monitor;

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Meter;

import io.openliberty.mcp.annotations.Tool;

public class McpStats extends Meter implements McpStatsMXBean {
	
	private String classAndMethod;
	private Tool tool;
	private Counter toolCallCount;
	
	public McpStats(String classAndMethod, Tool tool) {
		this.classAndMethod = classAndMethod;
		this.tool = tool;
		
		toolCallCount = new Counter();
		toolCallCount.setDescription("Total calls of an MCP tool");

	}

	public String getClassAndMethod() {
		return classAndMethod;
	}

	public Tool getTool() {
		return tool;
	}
	
	public void incrementToolCallCountBy(int i) {
		toolCallCount.incrementBy(i);
	}
	
	@Override
	public long getToolCallCount() {
		return toolCallCount.getCurrentValue();
	}


}
