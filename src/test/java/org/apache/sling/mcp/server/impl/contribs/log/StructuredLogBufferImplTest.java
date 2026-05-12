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
package org.apache.sling.mcp.server.impl.contribs.log;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.sling.mcp.server.contribs.log.LogSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredLogBufferImplTest {

    @Test
    void keepsOnlyNewestEntriesWithinCapacity() {
        StructuredLogBufferImpl buffer = new StructuredLogBufferImpl(2);

        buffer.append(snapshot(1L, "INFO", "first"));
        buffer.append(snapshot(2L, "INFO", "second"));
        buffer.append(snapshot(3L, "INFO", "third"));

        List<LogSnapshot> logs = buffer.getRecent(null, "TRACE", 10);
        assertEquals(
                List.of("third", "second"),
                logs.stream().map(LogSnapshot::formattedMessage).toList());
    }

    @Test
    void filtersByLevelAndRegex() {
        StructuredLogBufferImpl buffer = new StructuredLogBufferImpl(10);

        buffer.append(snapshot(1L, "DEBUG", "debug trace"));
        buffer.append(snapshot(2L, "INFO", "first user ok"));
        buffer.append(snapshot(3L, "ERROR", "first user failure"));

        List<LogSnapshot> logs = buffer.getRecent(Pattern.compile("first", Pattern.CASE_INSENSITIVE), "INFO", 10);

        assertEquals(
                List.of("first user failure", "first user ok"),
                logs.stream().map(LogSnapshot::formattedMessage).toList());
    }

    @Test
    void exposesSupportedLogLevels() {
        StructuredLogBufferImpl buffer = new StructuredLogBufferImpl(10);

        assertTrue(buffer.isValidLogLevel("INFO"));
        assertFalse(buffer.isValidLogLevel("info"));
        assertEquals(List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR"), buffer.getValidLogLevelNames());
        assertEquals("ERROR", buffer.getHighestLogLevelName());
    }

    private LogSnapshot snapshot(long timeMillis, String level, String message) {
        return new LogSnapshot(timeMillis, level, "logger", "thread", message, null, Map.of());
    }
}
