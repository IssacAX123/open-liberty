/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.prompt.basicPromptApp;

import io.openliberty.mcp.annotations.Prompt;
import io.openliberty.mcp.annotations.PromptArg;
import io.openliberty.mcp.types.PromptCallResult;
import io.openliberty.mcp.types.Role;
import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class BasicPrompts {

    @Prompt(name = "echo", title = "Echoes the input", description = "Prompt to return script to echo input")
    public PromptCallResult echo(@PromptArg(name = "input", description = "input to echo") String input,
                                 @PromptArg(name = "repeat", description = "how many times to repeat echo") int repeat) {
        return new PromptCallResult(Role.USER, "text", "write an echo command for input " + input + " and repeat " + repeat + " times");
    }
}
