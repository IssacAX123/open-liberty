/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.prompt;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.prompt.basicPromptApp.BasicPrompts;
import io.openliberty.mcp.internal.fat.utils.HttpTestUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class PromptTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "promptTest.war").addPackage(BasicPrompts.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testPromptList() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "prompts/list",
                          "params": {
                            "cursor": "optional-cursor-value"
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/promptTest", request);
        JSONObject jsonResponse = new JSONObject(response);
        String expectedString = """
                                                        {
                            "id": 1,
                            "jsonrpc": "2.0",
                            "result": {
                                "prompts": [
                                    {
                                        "description": "Prompt to return script to echo input",
                                        "arguments": [
                                            {
                                                "name": "input",
                                                "description": "input to echo",
                                                "required": true
                                            },
                                            {
                                                "name": "repeat",
                                                "description": "how many times to repeat echo",
                                                "required": true
                                            }
                                        ],
                                        "name": "echo",
                                        "title": "Echoes the input"
                                    }
                                ]
                            }
                        }
                                                """;

        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, jsonResponse.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testEchoPromptSuccess() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "prompts/get",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello",
                              "repeat": 4
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/promptTest", request);
        String expectedResponseString = """
                        {
                            "id": 2,
                            "jsonrpc": "2.0",
                            "result": {
                                "description": "Prompt to return script to echo input",
                                "messages": [
                                    {
                                        "role": "user",
                                        "content": {
                                            "type": "text",
                                            "text": "write an echo command for input Hello and repeat 4 times"
                                        }
                                    }
                                ]
                            }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testinvalidPrompt() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "prompts/get",
                          "params": {
                            "name": "privateEcho",
                            "arguments": {
                              "input": "Hello",
                              "repeat": 4
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/promptTest", request);
        String expectedResponseString = """
                        {
                            "id": 2,
                            "jsonrpc": "2.0",
                            "error": {
                                "code": -32602,
                                "data": [
                                    "Method privateEcho not found"
                                ],
                                "message": "Invalid params"
                            }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testinvalidParamsPrompt() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "prompts/get",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "other": "Hello",
                              "repeat": 4
                            }
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/promptTest", request);
        String expectedResponseString = """
                        {"error":{"code":-32602,
                        "data":[
                            "args [other] passed but not found in method",
                            "args [input] were expected by the method"
                            ],
                        "message": "Invalid params"},
                        "id":2,
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }
}