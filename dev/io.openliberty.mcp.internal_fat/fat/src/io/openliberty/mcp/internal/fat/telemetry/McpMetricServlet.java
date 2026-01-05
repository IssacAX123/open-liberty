/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/McpMetricServlet")
public class McpMetricServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    InMemoryMetricReader reader = PullExporterAutoConfigurationCustomizerProvider.exporter;

    @Test
    public void testMcpToolMetrics() {
        Optional<MetricData> toolCountMetricData = getMetricData("mcp.tool.calls.total");
        assertTrue(toolCountMetricData.isPresent());
        Optional<Long> basicToolCount = getToolCallCount(toolCountMetricData.get(), "basicTool");
        Optional<Long> advancedToolCount = getToolCallCount(toolCountMetricData.get(), "advancdTool");

        // Count how many times the io.openliberty.mcp.internal.fat.tool.MetricTest.testToolCallMetrics() FAT test sends requests to a tool
        assertTrue("basicTool count metric not found", basicToolCount.isPresent());
        assertEquals(1, basicToolCount.get().longValue());
        assertTrue("advancdTool count metric not found", advancedToolCount.isPresent());
        assertEquals(2, advancedToolCount.get().longValue());
    }

    private Optional<MetricData> getMetricData(String metricName) {
        return reader.getMcpMetricData()
                     .stream()
                     .filter(metric -> metric.getName().equals(metricName))
                     .findFirst();
    }

    private Optional<Long> getToolCallCount(MetricData metricData, String methodName) {
        return metricData.getLongSumData()
                         .getPoints()
                         .stream()
                         .filter(m -> m.getAttributes()
                                       .get(AttributeKey.stringKey("tool"))
                                       .contains(methodName))
                         .map(LongPointData::getValue)
                         .findFirst();
    }

}
