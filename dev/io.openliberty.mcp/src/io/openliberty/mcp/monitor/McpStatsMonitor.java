package io.openliberty.mcp.monitor;

public class McpStatsMonitor {
    private final String toolName;
    private long toolDuration;

    /**
     * @param toolName
     * @param toolDuration
     */
    public McpStatsMonitor(String toolName, long toolDuration) {
        super();
        this.toolName = toolName;
        this.toolDuration = toolDuration;
    }

    /**
     * @param toolName
     */
    public McpStatsMonitor(String toolName) {
        super();
        this.toolName = toolName;
    }

    /**
     * @return the toolDuration
     */
    public long getToolDuration() {
        return toolDuration;
    }

    /**
     * @param toolDuration the toolDuration to set
     */
    public void setToolDuration(long toolDuration) {
        this.toolDuration = toolDuration;
    }

    /**
     * @return the toolName
     */
    public String getToolName() {
        return toolName;
    }

    public void recordToolCall() {}

}
