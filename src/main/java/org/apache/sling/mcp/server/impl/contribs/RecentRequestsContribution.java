/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.mcp.server.impl.contribs;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.apache.sling.engine.RequestInfo;
import org.apache.sling.engine.RequestInfoProvider;
import org.apache.sling.mcp.server.spi.McpServerContribution;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class RecentRequestsContribution implements McpServerContribution {

    private final RequestInfoProvider requestInfoProvider;

    @Activate
    public RecentRequestsContribution(@Reference RequestInfoProvider requestInfoProvider) {
        this.requestInfoProvider = requestInfoProvider;
    }

    private String describe(RequestInfo ri) {
        return "Id: " + ri.getId() + "\n" + "Method: "
                + ri.getMethod() + "\n" + "Path: " + ri.getPath() + "\n" + "User id: " + ri.getUserId() + "\n"
                + ":\n" + ri.getLog();
    }

    @Override
    public List<SyncToolSpecification> getSyncToolSpecification() {

        var schema = Map.<String, Object>of(
                "type",
                "object",
                "properties",
                Map.of(
                        "path",
                        Map.of(
                                "type",
                                "string",
                                "description",
                                "Optional regex pattern to filter requests by path. If not provided, all recent requests are returned.")),
                "additionalProperties",
                false);

        return List.of(new SyncToolSpecification(
                Tool.builder("recent-requests", schema)
                        .description(
                                "Returns all recent requests matching the specified path regex. "
                                        + "Contains information about method, path, user id and a verbose log of internal operations, "
                                        + "including authentication, resource resolution, script resolution, nested scripts/servlets and filters.")
                        .build(),
                (exchange, request) -> {
                    String pathRegex = (String) request.arguments().get("path");

                    Pattern pattern = null;
                    if (pathRegex != null && !pathRegex.isEmpty()) {
                        try {
                            pattern = Pattern.compile(pathRegex, Pattern.CASE_INSENSITIVE);
                        } catch (PatternSyntaxException e) {
                            return CallToolResult.builder()
                                    .addTextContent("Invalid regex pattern: " + e.getMessage())
                                    .isError(true)
                                    .build();
                        }
                    }

                    final Pattern finalPattern = pattern;
                    String allRequests = StreamSupport.stream(
                                    requestInfoProvider.getRequestInfos().spliterator(), false)
                            .filter(ri -> finalPattern == null
                                    || finalPattern.matcher(ri.getPath()).find())
                            .map(this::describe)
                            .collect(Collectors.joining("\n\n" + "-".repeat(20) + "\n\n"));

                    return CallToolResult.builder().addTextContent(allRequests).build();
                }));
    }
}
