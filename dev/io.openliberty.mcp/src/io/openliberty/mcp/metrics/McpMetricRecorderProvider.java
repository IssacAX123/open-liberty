/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.metrics;

import java.lang.reflect.Method;

import io.openliberty.mcp.annotations.Tool;

public interface McpMetricRecorderProvider {
    /**
     * Service which can create a {@link McpMetricRecorder} for a given method and fault tolerance policy
     */

    static enum AsyncType {
        ASYNC,
        SYNC
    }

    /**
     * Get a metric recorder for invocations of the given method
     */
    public McpMetricRecorder getMetricRecorder(Method method, Tool tool,
                                               AsyncType isAsync);

}
