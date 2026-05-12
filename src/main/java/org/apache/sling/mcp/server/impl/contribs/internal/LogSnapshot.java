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
package org.apache.sling.mcp.server.impl.contribs.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stores only the lightweight, stable parts of a log event so the in-memory buffer
 * does not retain full logging event object graphs.
 */
public record LogSnapshot(
        long timeMillis,
        LogLevel level, // avoid binding to logback or a specific version of slf4j
        String loggerName,
        String threadName,
        String formattedMessage,
        String throwableText,
        Map<String, String> mdc) {

    public static boolean isValidLogLevel(String logLevelName) {
        try {
            LogLevel.valueOf(logLevelName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static List<String> getValidLogLevelNames() {
        return Arrays.stream(LogLevel.values()).map(Enum::toString).toList();
    }

    public static String getHighestLogLevelName() {
        return LogLevel.values()[LogLevel.values().length - 1].toString();
    }

    public LogSnapshot {
        mdc = mdc == null ? Collections.emptyMap() : Collections.unmodifiableMap(mdc);
    }

    enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR;

        boolean isGreaterOrEqual(LogLevel minLevel) {
            return ordinal() >= minLevel.ordinal();
        }

        public boolean isValid(String logLevelName) {
            try {
                LogLevel.valueOf(logLevelName);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}
