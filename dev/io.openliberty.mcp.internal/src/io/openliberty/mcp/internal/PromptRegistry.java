/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.openliberty.mcp.annotations.Prompt;
import jakarta.enterprise.inject.spi.CDI;

public class PromptRegistry {

    private static PromptRegistry staticInstance = null;

    public static PromptRegistry get() {
        if (staticInstance != null) {
            return staticInstance;
        }
        return CDI.current().select(McpCdiExtension.class).get().getPromptRegistry();
    }

    /**
     * For unit testing only
     *
     * @param toolRegistry
     */
    public static void set(PromptRegistry promptRegistry) {
        staticInstance = promptRegistry;
    }

    private Map<String, PromptMetadata> prompt = new HashMap<>();

    public PromptMetadata getPrompt(String name) {
        PromptMetadata result = prompt.get(name);
        return result;
    }

    public void addPrompt(PromptMetadata tool) {
        String name = tool.annotation().name();
        if (name.equals(Prompt.ELEMENT_NAME)) {
            name = tool.method().getJavaMember().getName();
        }
        prompt.put(name, tool);
    }

    public boolean hasPrompts() {
        return !prompt.isEmpty();
    }

    public Collection<PromptMetadata> getAllPrompts() {
        return new ArrayList<>(prompt.values());
    }

}
