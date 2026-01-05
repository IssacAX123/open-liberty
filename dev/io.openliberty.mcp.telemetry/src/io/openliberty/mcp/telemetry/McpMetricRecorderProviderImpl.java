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

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.kernel.service.util.SecureAction;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.metrics.McpMetricRecorder;
import io.openliberty.mcp.metrics.McpMetricRecorderProvider;
import io.openliberty.mcp.telemetry.utils.DummyMcpMetricRecorder;
import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.metrics.Meter;

@Component(configurationPolicy = IGNORE)
public class McpMetricRecorderProviderImpl implements McpMetricRecorderProvider {

    private final static SecureAction secureAction = AccessController.doPrivileged(SecureAction.get());

    /**
     * This lock must be held before reading or writing from the {@link #metricsEnabledCache}
     */
    private final ReentrantReadWriteLock metricsEnabledCacheLock = new ReentrantReadWriteLock();

    /**
     * Map from Classloader to whether metrics are disabled for that classloader
     * <p>
     * Must hold {@link #metricsEnabledCacheLock} before reading or writing
     */
    private final Map<ClassLoader, Boolean> metricsEnabledCache = new WeakHashMap<>();

    private final static String CONFIG_METRICS_ENABLED = "MCP_Metrics_Enabled";

    private final WeakHashMap<Method, McpMetricRecorder> recorders = new WeakHashMap<>();

    /** {@inheritDoc} */
    @Override
    public McpMetricRecorder getMetricRecorder(Method method, Tool tool, AsyncType isAsync) {
        synchronized (recorders) {
            McpMetricRecorder recorder = recorders.get(method);
            if (recorder == null) {
                recorder = createNewRecorder(method, tool, isAsync);
                recorders.put(method, recorder);
            }
            return recorder;
        }
    }

    private McpMetricRecorder createNewRecorder(Method method, Tool tool, AsyncType isAsync) {
        if (isMetricsEnabled(method.getDeclaringClass())) {
            Meter meter = OpenTelemetryAccessor.getOpenTelemetryInfo().getOpenTelemetry().getMeter(OpenTelemetryConstants.INSTRUMENTATION_NAME);
            return new McpMetricRecorderImpl(method.getDeclaringClass().getName() + "."
                                             + method.getName(), meter, tool, isAsync);
        } else {
            return DummyMcpMetricRecorder.get();
        }
    }

    private boolean isMetricsEnabled(Class<?> clazz) {

        ClassLoader cl = secureAction.getClassLoader(clazz);
        Boolean metricsEnabled = null;
        // Get the read lock before checking the cache
        metricsEnabledCacheLock.readLock().lock();
        try {
            metricsEnabled = metricsEnabledCache.get(cl);
            if (metricsEnabled == null) {
                // Classloader is not in the cache, let's add it
                // must release the read lock before acquiring the write lock (upgrading while holding the lock is not allowed)
                metricsEnabledCacheLock.readLock().unlock();
                metricsEnabledCacheLock.writeLock().lock();
                try {
                    // Now we have the write lock, recheck whether the classloader is in the cache
                    metricsEnabled = metricsEnabledCache.get(cl);
                    if (metricsEnabled == null) {
                        Config mpConfig = ConfigProvider.getConfig(cl);
                        metricsEnabled = mpConfig.getOptionalValue(CONFIG_METRICS_ENABLED, Boolean.class).orElse(Boolean.TRUE);
                    }
                    // Downgrade to the read lock (this is allowed)
                    metricsEnabledCacheLock.readLock().lock();
                } finally {
                    metricsEnabledCacheLock.writeLock().unlock();
                }
            }
        } finally {
            metricsEnabledCacheLock.readLock().unlock();
        }

        return metricsEnabled;

    }
}
