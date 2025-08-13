/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import io.openliberty.mcp.annotations.Prompt;
import io.openliberty.mcp.annotations.PromptArg;
import io.openliberty.mcp.internal.Literals;
import io.openliberty.mcp.internal.PromptDescription;
import io.openliberty.mcp.internal.PromptMetadata;
import io.openliberty.mcp.internal.PromptMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.PromptRegistry;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class MCPServerPromptsListTest {

    PromptRegistry registry = new PromptRegistry();

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        PromptRegistry.set(registry);

        //Weather Tool
        Prompt weatherTool = Literals.prompt("get_weather", "Weather Information Provider", "Get current weather information for a location");
        PromptArg weatherToolArgLocation = Literals.promptArg("location", "Location of place you want the weather", true);
        PromptArg weatherToolArgTemperature = Literals.promptArg("temperature", "Temperature of place you want weather", true);
        PromptArg weatherToolArgHumidity = Literals.promptArg("humidity", "Humidity of place you want weather", false);

        Map<String, ArgumentMetadata> arguments = Map.of("location", new ArgumentMetadata(String.class, 0, weatherToolArgLocation),
                                                         "temperature", new ArgumentMetadata(double.class, 1, weatherToolArgTemperature),
                                                         "humidity", new ArgumentMetadata(int.class, 2, weatherToolArgHumidity));
        registry.addPrompt(new PromptMetadata(weatherTool, null, null, arguments));

        // Addition Tool
        Prompt additionTool = Literals.prompt("Addition Calculator", "The Calculator Addition Tool", "Can add two floating point numbers");
        PromptArg additionToolArgNumber1 = Literals.promptArg("number1", "Initaial number", true);
        PromptArg additionToolArgNumber2 = Literals.promptArg("number2", "How much you want to increase number1", true);
        Map<String, ArgumentMetadata> arguments2 = Map.of("number1", new ArgumentMetadata(double.class, 0, additionToolArgNumber1),
                                                          "number2", new ArgumentMetadata(double.class, 1, additionToolArgNumber2));
        registry.addPrompt(new PromptMetadata(additionTool, null, null, arguments2));

    }

    @Test
    public void testJSONSerialization() throws Exception {

        Jsonb jsonb = JsonbBuilder.create();

        List<PromptDescription> response = new LinkedList<>();

        if (registry.hasPrompts()) {
            for (PromptMetadata pmd : registry.getAllPrompts()) {
                response.add(new PromptDescription(pmd));
            }
            jsonb.toJson(response);
        }

        String responseString = jsonb.toJson(response);
        String expectedString = """
                        [
                            {
                                "description": "Get current weather information for a location",
                                "arguments": [
                                        {
                                            "name": "location",
                                            "description": "Location of place you want the weather",
                                            "required": true
                                        },
                                        {
                                            "name":"temperature",
                                            "description": "Temperature of place you want weather",
                                            "required": true
                                        },
                                        {
                                            "name":"humidity",
                                            "description": "Humidity of place you want weather",
                                            "required": false
                                        }

                                ],
                                "name": "get_weather",
                                "title": "Weather Information Provider"
                            },
                            {
                                "description": "Can add two floating point numbers",
                                "arguments": [

                                        {
                                            "name":"number1",
                                            "description": "Initaial number",
                                            "required": true
                                        },
                                        {
                                            "name":"number2",
                                            "description": "How much you want to increase number1",
                                            "required": true
                                        }

                                ],
                                "name": "Addition Calculator",
                                "title": "The Calculator Addition Tool"
                            }
                        ]
                        """;

        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, responseString, false);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

}
