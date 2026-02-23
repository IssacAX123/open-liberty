package io.openliberty.mcp.internal.monitor;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeAtReturn;
import com.ibm.websphere.monitor.annotation.ProbeSite;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.annotation.This;
import com.ibm.websphere.monitor.meters.MeterCollection;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.monitor.McpStatsMonitor;



@Monitor(group = "MCP")
public class McpMonitor {

	 // mcpCountByName is a MeterCollection that will hold the McpStats MXBean for each MCP server
	@PublishedMetric
	public MeterCollection<McpStats> mcpCountByName = new MeterCollection<McpStats>("MCP", this);
	
	
	/**
	 * {@link McpStatsMonitor} has empty methods that the MCP implementation will call 
	 * when it wants to update a metric. This @Probe annotation watches for when that method is called, and when it is,
	 * we handle the actual metric logic. We have a map mcpCountByName to store all metrics, so first we get the object
	 * in the @ProbeSite argument that was invoked, and use its fields to identify the tool that was called. We get that
	 * tool's metric data from the {@code mcpCountByName} map, which returns a {@link McpStats} 
	 * that contains the implementation to count and store the metrics for a MCP tool. 
	 * 
	 *  Once we get the correct {@link McpStats} we call the relevant method to update the metrics for it 
	 * to count and store the metrics
	 * 
	 * @param mcpStats
	 */
	@ProbeAtReturn
	@ProbeSite(clazz = "io.openliberty.mcp.monitor.McpStatsMonitor", method = "recordToolCall")
	public void atMcpToolCalled(@This Object mcpStats) {
		McpStatsMonitor stats = (McpStatsMonitor) mcpStats;
		getMcpServerMetrics(stats.toolName()).incrementToolCallCountBy(1);
	}
	
	private synchronized McpStats getMcpServerMetrics(String toolName) {
		McpStats mcpStats = mcpCountByName.get(toolName);
		if (mcpStats == null) {
			mcpStats = new McpStats(toolName);
			mcpCountByName.put(toolName, mcpStats);
		}
		return mcpStats;
	}
	
}
