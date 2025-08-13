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
import java.util.List;
import java.util.Map;

import io.openliberty.mcp.annotations.PromptArg;
import io.openliberty.mcp.internal.PromptMetadata.ArgumentMetadata;
import jakarta.json.bind.annotation.JsonbProperty;

// TODO "cursor": "optional-cursor-value" (pagination after Tech Exchange)
// TODO build object of objects (we will probably build a schema generator for this) so we could delete the InputSchema object
// TODO method parameter descriptions needs to be defined in the tool annotation

public class PromptDescription {

    private final String name;
    private final String title;
    private final String description;
    private final List<ArgumentObject> arguments = new ArrayList<>();

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<ArgumentObject> getArguments() {
        return arguments;
    }

    public PromptDescription(PromptMetadata promptMetadata) {

        this.name = promptMetadata.annotation().name();
        this.title = promptMetadata.annotation().title();
        this.description = promptMetadata.annotation().description();

        Map<String, ArgumentMetadata> argumentMap = promptMetadata.arguments();
        PromptArg pInfo;
        for (String key : argumentMap.keySet()) {
            pInfo = argumentMap.get(key).pInfo();
            arguments.add(new ArgumentObject(key, pInfo.description(), pInfo.required()));
        }
    }

    public record ArgumentObject(@JsonbProperty("name") String name, @JsonbProperty("description") String description, @JsonbProperty("required") boolean required) {

//        public String getName() {
//            return name;
//        }
//
//        public String getDescription() {
//            return description;
//        }
//
//        public boolean getRequired() {
//            return required;
//        }

    }
}