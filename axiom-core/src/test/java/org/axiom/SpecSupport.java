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

import jdave.ExpectationFailedException;
import jdave.IContract;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.Transformer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ObjectUtils;
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
        allowing(registry).lookup(with(any(String.class)),
            with(equal(Configuration.class)));
        will(returnValue(config));
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

    public static Object setTo(final Object object) { return object; }

    public static Transformer property(final String propertyName) {
        return new BeanToPropertyValueTransformer(propertyName);
    }
}
