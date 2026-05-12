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

import java.lang.reflect.Constructor;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.apache.sling.mcp.server.contribs.log.LogSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class StructuredLogBufferAppenderTest {

    @Test
    void appenderSnapshotsFormattedMessageAndThrowable() {
        StructuredLogBufferImpl buffer = new StructuredLogBufferImpl(5);
        StructuredLogBufferAppender appender = new StructuredLogBufferAppender(buffer);

        LoggerContext context = new LoggerContext();
        appender.setContext(context);
        Logger logger = context.getLogger("test.logger");
        RuntimeException failure = new RuntimeException("error");
        LoggingEvent event = new LoggingEvent(getClass().getName(), logger, Level.ERROR, "message", failure, null);
        event.setMDCPropertyMap(java.util.Map.of());
        event.setThreadName("worker-1");

        appender.append(event);

        List<LogSnapshot> logs = buffer.getRecent(null, "TRACE", 10);
        assertEquals(1, logs.size());
        assertEquals("message", logs.get(0).formattedMessage());
        assertEquals("worker-1", logs.get(0).threadName());
        assertEquals("ERROR", logs.get(0).level());
        assertNotNull(logs.get(0).throwableText());
    }

    @Test
    void appenderSkipsInvalidLogLevels() {
        StructuredLogBufferImpl buffer = new StructuredLogBufferImpl(5);
        StructuredLogBufferAppender appender = new StructuredLogBufferAppender(buffer);

        LoggerContext context = new LoggerContext();
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName("invalid.logger");
        event.setThreadName("invalid-thread");
        event.setMessage("ignored");
        event.setLevel(invalidLevel());

        appender.append(event);

        assertEquals(List.of(), buffer.getRecent(null, "TRACE", 10));
    }

    private Level invalidLevel() {
        try {
            Constructor<Level> constructor = Level.class.getDeclaredConstructor(int.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(Integer.MAX_VALUE, "INVALID");
        } catch (ReflectiveOperationException e) {
            fail("Unable to construct invalid log level", e);
            return null;
        }
    }
}
