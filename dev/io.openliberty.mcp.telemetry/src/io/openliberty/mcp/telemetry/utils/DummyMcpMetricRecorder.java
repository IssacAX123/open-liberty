/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.telemetry.utils;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.mcp.metrics.McpMetricRecorder;

@Trivial
public final class DummyMcpMetricRecorder implements McpMetricRecorder {

    private static final DummyMcpMetricRecorder instance = new DummyMcpMetricRecorder();

    public static DummyMcpMetricRecorder get() {
        return instance;
    }

    // Use instance instead
    private DummyMcpMetricRecorder() {}

    /** {@inheritDoc} */
    @Override
    public void incrementToolCallCount() {}

}