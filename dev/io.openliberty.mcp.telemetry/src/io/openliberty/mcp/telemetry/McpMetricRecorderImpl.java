/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.telemetry;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.metrics.McpMetricRecorder;
import io.openliberty.mcp.metrics.McpMetricRecorderProvider.AsyncType;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

public class McpMetricRecorderImpl implements McpMetricRecorder {

    private static final AttributeKey<String> TOOL = AttributeKey.stringKey("tool");

    // Every metric uses this tag to identify the method it's reporting metrics for
    private final Attributes toolAttribute;

    private final LongCounter mcpToolCallsTotal;

    public McpMetricRecorderImpl(String classAndMethod, Meter meter, Tool tool, AsyncType isAsync) {
        /*
         * Register all of the metrics required for this method and store them in fields
         */
        toolAttribute = Attributes.builder().put(TOOL, classAndMethod).build();
        if (tool != null) {
            mcpToolCallsTotal = meter.counterBuilder("mcp.tool.calls.total").setDescription("Total calls of an MCP tool").build();
        } else {
            mcpToolCallsTotal = null;
        }

    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public void incrementToolCallCount() {
        if (mcpToolCallsTotal != null) {
            /*
             * The "tool" attribute key is used to tag the mcp.tool.calls.total metric with each tool's identifier (classAndMethod),
             * Each tool call adds to the mcp.tool.calls.total counter with its tool name as the "tool" attribute value
             * This allows a breakdown in metrics per tool
             */
            mcpToolCallsTotal.add(1, toolAttribute);
        }
    }

}
