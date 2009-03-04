/*
 * Copyright (c) 2009, Tim Watson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *     * Neither the name of the author nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.axiom.service;

import org.apache.camel.processor.interceptor.Tracer;
import org.apache.camel.processor.LoggingLevel;
import org.apache.commons.configuration.Configuration;
import static org.apache.commons.lang.Validate.notNull;

public class TraceBuilder {

    protected static final String TRACE_INTERCEPTORS_KEY = "axiom.core.configuration.trace.include.interceptors";
    protected static final String TRACE_EXCEPTIONS_KEY = "axiom.core.configuration.trace.include.exceptions";
    protected static final String TRACE_ENABLED_KEY = "axiom.core.configuration.trace.enabled";
    protected static final String TRACE_LEVEL_KEY = "axiom.core.configuration.trace.logLevel";

    private final Configuration config;
    private final Tracer tracer;

    //TODO: move these out into a resource bundle
    protected static final String MISSING_CONFIG_MSG = "Configuration instance cannot be null.";
    protected static final String MISSING_TRACER_MSG = "Tracer instance cannot be null" ;

    public TraceBuilder(final Configuration config, final Tracer tracer) {
        notNull(config, MISSING_CONFIG_MSG);
        notNull(tracer, MISSING_TRACER_MSG);
        this.config = config;
        this.tracer = tracer;
    }

    public TraceBuilder(final Configuration config) {
        this(config, new Tracer());
    }

    public Tracer build() {
        tracer.setLogLevel(getTraceLevel());
        tracer.setTraceInterceptors(config.getBoolean(TraceBuilder.TRACE_INTERCEPTORS_KEY));
        tracer.setTraceExceptions(config.getBoolean(TraceBuilder.TRACE_EXCEPTIONS_KEY));
        return tracer;
    }

    private LoggingLevel getTraceLevel() {
        final String level = config.getString(TRACE_LEVEL_KEY).toUpperCase();
        return LoggingLevel.valueOf(level);
    }
}
