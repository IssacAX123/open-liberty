/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.types;

/**
 *
 */
public enum Role {
    USER("user"),
    ASSISTANT("assistant");

    private String role;

    /**
     * @param string
     */
    Role(String role) {
        this.role = role;
    }

    /**
     * @return the role
     */
    public String getRole() {
        return role;
    }

}
