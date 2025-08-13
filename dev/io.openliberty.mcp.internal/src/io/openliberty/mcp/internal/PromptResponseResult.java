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

import io.openliberty.mcp.types.PromptCallResult;

/**
 *
 */
public class PromptResponseResult {
    private String description;
    private List<Message> messages = new ArrayList<>();

    public PromptResponseResult(String description, @SuppressWarnings("restriction") List<PromptCallResult> prompts) {
        this.description = description;
        for (@SuppressWarnings("restriction")
        PromptCallResult pcr : prompts) {
            switch (pcr.type()) {
                case "text" -> messages.add(new Message(pcr.result(), pcr.role().getRole()));
            }
        }
    }

    public PromptResponseResult(String description, @SuppressWarnings("restriction") PromptCallResult prompt) {
        this.description = description;
        switch (prompt.type()) {
            case "text" -> messages.add(new Message(prompt.result(), prompt.role().getRole()));
        }
    }

    @SuppressWarnings({ "unchecked", "restriction" })
    public static PromptResponseResult createFrom(String description, Object promptsObject) {
        if (promptsObject instanceof List<?>) {
            List<PromptCallResult> promptsList = (List<PromptCallResult>) promptsObject;
            return new PromptResponseResult(description, promptsList);
        } else if (promptsObject instanceof PromptCallResult) {
            return new PromptResponseResult(description, (PromptCallResult) promptsObject);
        } else {
            throw new RuntimeException("Invalid result for prompt method");
        }
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the messages
     */
    public List<Message> getMessages() {
        return messages;
    }

    public static class Message {
        private String role;
        private Content content;

        public Message(Object result, String role) {
            this.role = role;
            this.content = new TextContent(result);
        }

        public Content getContent() {
            return content;
        }

        public String getRole() {
            return role;
        }
    }

    public static abstract class Content {
        private final String type;

        public Content(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class TextContent extends Content {

        public Object getText() {
            return text;
        }

        private final Object text;

        public TextContent(Object text) {
            super("text");
            this.text = text;
        }

    }
}
