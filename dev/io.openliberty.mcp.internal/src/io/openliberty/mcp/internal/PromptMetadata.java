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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import io.openliberty.mcp.annotations.Prompt;
import io.openliberty.mcp.annotations.PromptArg;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;

/**
 *
 */
public record PromptMetadata(Prompt annotation, Bean<?> bean, AnnotatedMethod<?> method, Map<String, ArgumentMetadata> arguments) {

    public record ArgumentMetadata(Type type, int index, PromptArg pInfo) {}

    public PromptMetadata(Prompt annotation, Bean<?> bean, AnnotatedMethod<?> method) {
        this(annotation, bean, method, getArgumentMap(method));
    }

    private static Map<String, ArgumentMetadata> getArgumentMap(AnnotatedMethod<?> method) {
        Map<String, ArgumentMetadata> result = new HashMap<>();
        for (AnnotatedParameter<?> p : method.getParameters()) {
            PromptArg pInfo = p.getAnnotation(PromptArg.class);
            if (pInfo != null) {
                ArgumentMetadata pData = new ArgumentMetadata(p.getBaseType(), p.getPosition(), pInfo);
                result.put(pInfo.name(), pData);
            }
        }
        return result;
    }
}
