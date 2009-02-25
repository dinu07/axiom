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
 *
 *
 */

package org.axiom.service;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertThat;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import static org.apache.commons.io.FileUtils.readFileToString;
import org.apache.camel.builder.RouteBuilder;
import org.jmock.Mockery;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.internal.ExpectationBuilder;
import org.jmock.internal.ExpectationCollector;
import org.jmock.integration.junit4.JMock;
import org.springframework.core.io.ClassPathResource;
import org.axiom.configuration.RouteConfigurationScriptEvaluator;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.IOException;
import java.io.File;

@RunWith(JMock.class)
public class TestBootstrapRouteLoader {

    private Mockery mockery;
    private Configuration config;
    private RouteConfigurationScriptEvaluator evaluator;

    protected static final String TEST_BOOT_SCRIPT = "test-boot.rb";
    protected static final String CP_BOOT_SCRIPT = String.format("classpath:%s", TEST_BOOT_SCRIPT);

    @Before
    public void beforeEach() throws Exception {
        mockery = new Mockery();
        config = mockery.mock(Configuration.class);
        evaluator = mockery.mock(RouteConfigurationScriptEvaluator.class);
    }

    @Test(expected=IllegalArgumentException.class)
    public void itShouldPukeWhenInitializedWithMissingConfiguration() {
        new BootstrapRouteLoader(null, evaluator);
    }

    @Test(expected=IllegalArgumentException.class)
    public void itShouldPukeWhenInitializedWithMissingEvaluator() {
        new BootstrapRouteLoader(config, null);
    }

    @Test
    public void itShouldPullThePathToTheControlChannelBootstrapScript() {
        mockery.checking(new Expectations() {{
            one(config).getString(BootstrapRouteLoader.SCRIPT_URI_PROPERTY_KEY,
                "classpath:default-bootstrap.rb");
            will(returnValue(CP_BOOT_SCRIPT));
            allowing(evaluator);
        }});
        load();
    }

    @Test(expected=LifecycleException.class)
    public void itShouldPukeIfTheSuppliedFileDoesNotExitAtTheGivenUri() {
        mockery.checking(new Expectations() {{
            one(config).getString(with(any(String.class)), with(any(String.class)));
            will(returnValue("no-such-file.rb"));
            allowing(evaluator);
        }});
        load();
    }

    @Test
    public void itShouldPassTheLoadedScriptToTheScriptEvaluator() throws IOException {
        final File file = new ClassPathResource(TEST_BOOT_SCRIPT).getFile();
        final String bootstrapCode = readFileToString(file);

        mockery.checking(new Expectations() {{
            allowing(config).getString(with(any(String.class)), with(any(String.class)));
            will(returnValue(CP_BOOT_SCRIPT));

            one(evaluator).configure(bootstrapCode);
        }});
        load();
    }

    private RouteBuilder load() {
        return new BootstrapRouteLoader(config, evaluator).load();
    }

    @Test
    public void itShouldReturnTheRouteBuilderGeneratedByTheEvaluator() {
        final RouteBuilder routeBuilder = new RouteBuilder() {
            @Override public void configure() throws Exception {}
        };

        mockery.checking(new Expectations() {{
            allowing(config);
            will(returnValue(CP_BOOT_SCRIPT));

            allowing(evaluator).configure(with(any(String.class)));
            will(returnValue(routeBuilder));
        }});

        assertThat(load(), instanceOf(RouteBuilder.class));
    }
}
