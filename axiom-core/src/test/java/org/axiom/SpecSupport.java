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

package org.axiom;

import jdave.*;
import org.apache.camel.*;
import org.apache.camel.spi.Registry;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.Transformer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ObjectUtils;
import org.axiom.integration.Environment;
import static org.axiom.util.CollectionUtils.*;
import org.axiom.util.Operation;
import org.hamcrest.*;
import org.jmock.Expectations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.text.MessageFormat.*;

@SuppressWarnings({"SuspiciousToArrayCall"})
public abstract class SpecSupport extends Expectations {

    private final static Logger log = LoggerFactory.getLogger(SpecSupport.class);

    protected SpecSupport justIgnore(final Object... things) {
        for (final Object o : things) allowing(o);
        return this;
    }

    protected void stubConfiguration(final CamelContext context,
            final Registry registry, final Configuration config) {
        allowing(context).getRegistry();
        will(returnValue(registry));
        allowing(registry).lookup(Environment.CONFIG_BEAN, Configuration.class);
        will(returnValue(config));
    }

    public static Matcher<Expression> shouldEvaluateExchangeAndReturn(final Exchange input, final Object expectedValue) {
        return new TypeSafeMatcher<Expression>() {
            @SuppressWarnings({"unchecked"})
            @Override public boolean matchesSafely(final Expression expression) {
                final Expression nillableExpr = (Expression<Exchange>) ObjectUtils.defaultIfNull(
                    expression,
                    new Expression<Exchange>() {
                        @Override public Object evaluate(final Exchange exchange) {
                            return false;
                        }
                    }
                );
                return ObjectUtils.equals(nillableExpr.evaluate(input), expectedValue);
            }

            @Override public void describeTo(final Description description) {
                description.appendText(format("ExpressionMatcher[matching=%s, for=%s]",
                    expectedValue, input));
            }
        };
    }

    public static IContract propertyValueContract(final String propertyName, final Object expectedValue) {
        return new IContract() {
            @Override
            public void isSatisfied(final Object obj) throws ExpectationFailedException {
                BeanToPropertyValueTransformer transformer =
                    new BeanToPropertyValueTransformer(propertyName);
                if (!ObjectUtils.equals(transformer.transform(obj), expectedValue)) {
                    throw new ExpectationFailedException(
                        format("Expected {0} equal to {1} but was {2}.",
                            propertyName, expectedValue, obj));
                }
                log.debug("Object {} satisfied propertyValueContract[propertyName={}, expectedValue={}].",
                    new Object[] {obj, propertyName, expectedValue});
            }
        };
    }

    public static Block repeat(final Block block, final Integer n) {
        return new Block() {
            @Override public void run() throws Throwable {
                map(range(0, n - 1), new Operation<Integer>() {
                    @Override public void apply(final Integer _) {
                        try { block.run(); }
                        catch (Throwable throwable) { throw new RuntimeException(throwable); }
                    }
                });
            }
        };
    }

    /**
     * Syntactic sugar that just returns its input.
     * @param number
     * @return
     */
    public static Integer times(final Integer number) {
        // oh my how I'd like to write `alias times is' here :(
        return is(number);
    }

    /**
     * Syntactic sugar that just returns its input.
     * @param input The input to return
     * @param <T>
     * @return {@code input}.
     */
    public static <T> T is(final T input) { return input; }

    public static Transformer property(final String propertyName) {
        return new BeanToPropertyValueTransformer(propertyName);
    }
}
