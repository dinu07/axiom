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

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.interceptor.Tracer;
import org.apache.commons.configuration.Configuration;
import static org.apache.commons.lang.Validate.*;
import static org.axiom.configuration.ExternalConfigurationSourceFactory.*;
import org.axiom.integration.Environment;
import org.axiom.integration.camel.RouteConfigurationScriptEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.rmi.registry.Registry;

/**
 * Provides a managed message channel that can be used to
 * control {@link CamelContext}s. Once activated, the control channel's
 * behavior can be controlled by adding routing instructions via the
 * {@code load} method. Managed {@link CamelContext}s (i.e. those stored
 * as components in the host {@link CamelContext}'s {@link Registry}) can
 * be configured via the {@link ControlChannel#configure} methods.
 * <p>
 * This component is <b>not thread safe</b>. Specifically, there is no
 * guarantee that calls to {@link ControlChannel#activate()} and/or
 * {@link ControlChannel#destroy()} will not stomp on each other,
 * nor are additional control channel and/or managed context route configuration
 * updates synchronized. This component should therefore be considered
 * to be <b>thread-hostile</b> and explicit synchronization used if/when required.
 * </p>
 * <p>
 * An unconfigured channel will do exactly nothing for you. You need to load
 * some configuration for the channel itself first of all, and this is normally
 * done using {@link ControlChannelBootstrapper#bootstrap(ControlChannel)}, so you
 * can look there for further details. 
 * <p>
 * Once configured, the channel remains idle until {@link ControlChannel#activate()} is
 * called. {@link ControlChannel#activate()} returns control to the calling thread
 * once the channel is running.
 * </p>
 * <p>
 * There are several options for stopping the channel. The
 * {@link ControlChannel#destroy()} method abruptly terminates all running services,
 * effectively killing the channel. A more graceful mechanism for shutting down involves
 * waiting for a message to signal that a client has requested shutdown. The default bootstrap
 * scripts create a channel for exactly this purpose, using the uri defined by the
 * {@code axiom.channels.shutdown.uri} system property.
 * </p>
 * <p>
 * A client instructs the channel to shutdown by sending an exchange
 * to the endpoint for this uri, with a header named �signal� set to the value �shutdown.� A
 * consuming thread can then poll for this signal.
 * To simplify this process and do this without having to explicitly use the <i>Camel</i> APIs,
 * the {@link ControlChannel#waitShutdown} method provides this polling facility (with a variation
 * that times out without further action). Before the {@link ControlChannel#waitShutdown} method
 * returns, {@link ControlChannel#destroy()} will be called, effectively terminating the channel.
 * </p>
 * <p>
 * To explicitly send the 'shutdown' signal yourself, call the {@link ControlChannel#sendShutdownSignal()}
 * method, and to optionally combine this with the {@link ControlChannel#waitShutdown} call, the
 * {@link ControlChannel#sendShutdownSignalAndWait} method can be used. 
 * </p>
 * Example:
 * <pre>
 *
 *      CamelContext camelContext;
 *      RouteLoader  routeLoader;
 *
 *      public static void main(String... argv) {
 *          ControlChannel channel = new ControlChannel(camelContext);
 *          channel.load(routeLoader);
 *          channel.activate();
 *
 *          // to wait for a 'shutdown' signal
 *          channel.waitShutdown(long timeout = 10000);
 *
 *          // alternatively, you can send 'shutdown' yourself
 *          channel.sendShutdownSignal();
 *
 *          // optionally you can combine shutting down and waiting
 *          channel.sendShutdownSignalAndWait(); //optional timeout as before
 * 
 *          // to stop all services
 *          channel.destroy();
 *      }
 * </pre>
 * </p>
 */
public class ControlChannel {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final CamelContext hostContext;
    private final Tracer tracer;

    private Configuration config;

    public ControlChannel(final CamelContext hostContext) {
        this(hostContext, getTracer(hostContext));
    }

    public ControlChannel(final CamelContext hostContext, final Tracer tracer) {
        notNull(hostContext, "Camel context cannot be null.");
        notNull(tracer, "Tracer cannot be null.");
        this.tracer = tracer;
        this.hostContext = hostContext;
    }

    private static Tracer getTracer(final CamelContext context) {
        Tracer tracer = Tracer.getTracer(context);
        if (tracer == null) {
            return new Tracer();
        } else {
            return tracer;
        }
    }

    /**
     * Adds a set of routing/configuration rules to the host {@link CamelContext}
     * using the supplied {@link RouteLoader}. 
     * @param loader The loader for the routes you wish to load.
     */
    public void load(final RouteLoader loader) {
        notNull(loader, "Route loader cannot be null.");
        try {
            log.debug("Adding routes to context {}.", hostContext.getName());
            //TODO: consider whether this should call via producer using the 'axiom:host' uri
            /*
            //e.g.,
            hostContext.createProducerTemplate().
                sendBodyAndHeader(Environment.AXIOM_HOST_URI,
                    loader.getBuilder(), "command", "configure");*/
            hostContext.addRoutes(loader.load());
        } catch (Exception e) {
            throw new LifecycleException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Sends new routing configuration to the control channel, which is
     * subsequently processed based on the chosen startup mode (as set by
     * the {@code axiom.bootstrap.startup.mode} system property).
     * @param builder The builder containing the configuration you wish to apply
     */
    public void configure(final RouteBuilder builder) {
        final String channelUri = getConfig().getString(Environment.CONTROL_CHANNEL);
        sendBodyAndHeader(channelUri, builder, "signal", "configure");
    }

    /**
     * Sends new routing configuration to the control channel, which is
     * subsequently processed based on the chosen startup mode (as set by
     * the {@code axiom.bootstrap.startup.mode} system property).
     * @param routeLoader An object which can load the configuration you wish to apply
     */
    public void configure(final RouteLoader routeLoader) {
        configure(routeLoader.getBuilder());
    }

    /**
     * Activates the control channel, which will from now on behave in
     * accordance with the routes you set up in your bootstrap script(s).
     *
     * See {@link ControlChannel#waitShutdown} and {@link ControlChannel#sendShutdownSignal()}
     * for instructions on shutting down an activated channel. 
     */
    public void activate() {
        try {
            log.info("Activating control channel.");
            Configuration config = getRegisteredConfiguration(getContext());
            log.info("Configuring tracer for {}.", getContext());
            TraceBuilder builder = new TraceBuilder(config, tracer);
            getContext().addInterceptStrategy(builder.build());
            getContext().start();
        } catch (Exception e) {
            throw new LifecycleException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Stops all the underlying services, jobs and worker threads.
     * @throws LifecycleException in the face of termination failure(s).
     */
    public void destroy() {
        log.info("Destroying control channel.");
        try {
            getContext().stop();
        } catch (Exception e) {
            throw new LifecycleException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Sends a shutdown signal to the control channel and
     * immediately returns. If you wish to wait for shutdown
     * to complete, you should call one of the {@link ControlChannel#waitShutdown}
     * methods, or one of the {@link ControlChannel#sendShutdownSignalAndWait} methods
     * to combine the signal sending and wait operations.
     */
    public void sendShutdownSignal() {
        final String shutdownChannelUri =
            getConfig().getString(Environment.TERMINATION_CHANNEL);
        log.info("Sending shutdown signal to {}.", shutdownChannelUri);
        sendBodyAndHeader(shutdownChannelUri, null,
            Environment.SIGNAL, Environment.SIG_TERMINATE);
    }

    /**
     * Sends a shutdown signal (using {@link ControlChannel#sendShutdownSignal()}
     * and immediately goes into {@link ControlChannel#waitShutdown()}, which is
     * a blocking call.
     */
    public void sendShutdownSignalAndWait() {
        sendShutdownSignal();
        waitShutdown();
    }

    /**
     * Sends a shutdown signal (using {@link ControlChannel#sendShutdownSignal()}
     * and immediately goes into {@link ControlChannel#waitShutdown(long)}, using
     * the supplied {@code timeout}.
     *
     * @param timeout The timeout to set when waiting for shutdown.
     */
    public void sendShutdownSignalAndWait(final long timeout) {
        sendShutdownSignal();
        waitShutdown(timeout);
    }

    /**
     * Waits for the 'shutdown channel' to receive a 'shutdown' signal.
     * This is a blocking call: the calling thread will wait indefinitely
     * until a 'shutdown' signal arrived. If the shutdown channel is not ready
     * to service consumers, an exception is thrown.
     *
     * @exception LifecycleException Thrown if the shutdown channel is not ready.
     * @exception IllegalStateException Thrown if the shutdown channel has not been started 
     */
    public void waitShutdown() {
        log.info("Entering wait shutdown.");
        PollingConsumer pollingConsumer;
        try {
            pollingConsumer = getTerminationChannel().createPollingConsumer();
        } catch (Exception e) {
            throw new LifecycleException(e.getLocalizedMessage(), e);
        }
        //TODO: throw IllegalStateException if return value is null
        pollingConsumer.receive();
    }

    /**
     * Waits for the 'shutdown channel' to receive a 'shutdown' signal.
     * This blocks the calling thread, but returns {@code false} if the
     * shutdown channel does not receive a signal prior to the timeout being
     * exceeded. If the shutdown channel is not ready to service consumers,
     * an exception is also thrown.
     *
     * @param timeout The number of milliseconds to wait before raising an exception.
     * @exception LifecycleException Thrown if the shutdown channel is not ready
     */
    public /*boolean*/ void waitShutdown(final long timeout) {
        log.info("Entering wait shutdown ({}ms timeout).", timeout);
        try {
            //TODO: return consumer.receive(timeout) != null;
            getTerminationChannel().createPollingConsumer().receive(timeout);
        } catch (Exception e) {
            throw new LifecycleException(e.getLocalizedMessage(), e);
        }
    }

    private Endpoint getTerminationChannel() {
        final String channelUri = getConfig().getString(Environment.TERMINATION_CHANNEL);
        return getContext().getEndpoint(channelUri);
    }

    private void sendBodyAndHeader(final String channelUri, final Object payload,
        final String header, final String headerValue) {
        final ProducerTemplate<Exchange> producer = getContext().createProducerTemplate();
        producer.sendBodyAndHeader(channelUri, payload, header, headerValue);
    }

    /**
     * Gets the configured {@link RouteConfigurationScriptEvaluator}.
     * @return
     */
    public RouteConfigurationScriptEvaluator getRouteScriptEvaluator() {
        return lookup(Environment.ROUTE_SCRIPT_EVALUATOR,
                RouteConfigurationScriptEvaluator.class);
    }

    /**
     * Gets the underlying {@link CamelContext}. It is recommended that
     * you do not interfere with this service unless you *really* know what
     * you're doing.
     * @return The {@link CamelContext} hosting this control channel.
     */
    public CamelContext getContext() {
        return hostContext;
    }

    /**
     * Gets the {@link Tracer} instance attached to the underlying
     * {@link CamelContext}. This can be used to configure and
     * enable/disable tracing dynamically at runtime.
     * @return The {@link Tracer} instance associated with this control channel.
     */
    public Tracer getTracer() {
        return tracer;
    }

    /**
     * Looks up a service in the registry underlying the backing {@link CamelContext},
     * returning the service or null if it could not be found.
     * @param key The registered name of the service
     * @param clazz The expected type of the service instance
     * @param <T>
     * @return A service/object of the requisite type, or {@code null} if no registered
     * instance(s) match the supplied {@code key}.
     */
    public <T> T lookup(final String key, Class<T> clazz) {
        return getContext().getRegistry().lookup(key, clazz);
    }

    public Configuration getConfig() {
        //TODO: consider whether lazy init is really needed here!?
        //NB: configuration instance is a singleton so potential
        //    overwrite stomping due to concurrent access isn't going to cause any issues
        if (config == null) {
            config = getRegisteredConfiguration(hostContext);
            if (config == null) {
                throw new LifecycleException(MessageFormat.format(
                    "Context Registry is incorrectly configured: bean for id {0} is not present.",
                    Environment.CONFIG_BEAN
                ));
            }
        }
        return config;
    }

}
