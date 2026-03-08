package io.openliberty.mcp.internal.monitor;

import com.ibm.websphere.monitor.meters.StatisticsReading;

public interface McpStatsMXBean {
	
	public long getToolCallCount();

	StatisticsReading getToolCallDurationReading();
}
