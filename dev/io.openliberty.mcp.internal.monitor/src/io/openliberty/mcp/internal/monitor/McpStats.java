package io.openliberty.mcp.internal.monitor;

import com.ibm.websphere.monitor.meters.StatisticsMeter;
import com.ibm.websphere.monitor.meters.StatisticsReading;
import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Meter;

import io.openliberty.mcp.annotations.Tool;

public class McpStats extends Meter implements McpStatsMXBean {
	
	private String toolName;
	private Counter toolCallCount;
	private StatisticsMeter toolCallRunDuration;
	
	public McpStats(McpStatAttributes mcpStatAttributes) {
		this.toolName = mcpStatAttributes.getMcpMethodName();
		
		toolCallCount = new Counter();
		toolCallCount.setDescription("Total calls of an MCP tool");
		
		toolCallRunDuration = new StatisticsMeter();
		toolCallRunDuration.setDescription("Duration of tool call operations");
		toolCallRunDuration.setUnit("seconds");

	}

	public String getToolName() {
		return toolName;
	}
	
	public void incrementToolCallCountBy(int i) {
		toolCallCount.incrementBy(i);
	}
	
	public void addToolTimeStat(long time) {
		toolCallRunDuration.addDataPoint(time);
	}
	
	@Override
	public long getToolCallCount() {
		return toolCallCount.getCurrentValue();
	}
	
	@Override
	public StatisticsReading getToolCallDurationReading() {
		return toolCallRunDuration.getReading();
	}


}
