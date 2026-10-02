/*
 * This file is part of EventBus.
 *
 * EventBus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * EventBus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EventBus. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2024-2026 ClydoNetwork
 */

package net.clydo.eventbus;

import lombok.val;
import net.clydo.clytil.Nulls;
import net.clydo.clytil.Primitives;
import net.clydo.clytil.Validates;
import net.clydo.eventbus.event.Cancellable;
import net.clydo.eventbus.event.Event;
import net.clydo.eventbus.event.EventKey;
import net.clydo.eventbus.internal.EventDispatcher;
import net.clydo.eventbus.subscriber.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;

/**
 * A type-safe, thread-safe publish/subscribe event bus.
 *
 * <h2>Dispatch</h2>
 * An event posted with {@link #post(Object)} is offered to every listener subscribed to its class
 * or to any of its superclasses and interfaces, so subscribing to {@code Object.class} observes
 * every class-based event. Listeners run in descending {@linkplain EventPriority priority}, and in
 * subscription order within a priority. The merged, sorted listener array of each posted class is
 * cached, so steady-state dispatch costs one map lookup plus one array walk, regardless of how deep
 * the event hierarchy is.
 *
 * <h2>Cancellation and propagation</h2>
 * A {@link Cancellable} event keeps being dispatched after it is canceled; listeners that do not
 * want canceled events subscribe with {@code ignoreCancelled}. An {@link Event} whose
 * {@link Event#stopPropagation()} was called is not offered to any further listener.
 *
 * <h2>Concurrency</h2>
 * Posting is lock-free. Subscribing and unsubscribing take a short lock and copy the affected
 * listener array, so they are safe from any thread, including from inside a listener. A dispatch
 * that is already running is not affected by concurrent subscriptions, but a listener that is
 * unsubscribed is never invoked afterward.
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * EventBus bus = EventBus.builder()
 *         .executor(myExecutor)
 *         .errorHandler(EventErrorHandler.rethrowing())
 *         .tracer(EventTracer.logging())
 *         .listenerWarningThreshold(64)
 *         .build();
 * }</pre>
 */
public final class EventBus {

    private final @NotNull EventDispatcher dispatcher;

    /**
     * Creates a bus with the default configuration: the common {@link ForkJoinPool} for
     * {@linkplain DispatchMode#ASYNC asynchronous} work, the {@linkplain EventErrorHandler#logging()
     * logging} error handler, no tracer and no leak warnings.
     */
    public EventBus() {
        this(null, null, null, 0);
    }

    /**
     * Creates a configured bus. Use {@link #builder()}.
     *
     * @param executor                 runs {@link DispatchMode#ASYNC} listeners and {@link #postAsync(Object)};
     *                                 defaults to {@link ForkJoinPool#commonPool()}
     * @param errorHandler             handles listener exceptions; defaults to {@link EventErrorHandler#logging()}
     * @param tracer                   observes every dispatch; none by default
     * @param listenerWarningThreshold logs a warning once a single event type or key has more subscriptions than this,
     *                                 which usually means listeners are being leaked; {@code 0} (the default) disables it
     */
    @lombok.Builder(builderClassName = "Builder")
    private EventBus(
            @Nullable final Executor executor,
            @Nullable final EventErrorHandler errorHandler,
            @Nullable final EventTracer tracer,
            final int listenerWarningThreshold
    ) {
        this.dispatcher = new EventDispatcher(
                Nulls.or(executor, ForkJoinPool.commonPool()),
                Nulls.or(errorHandler, EventErrorHandler.logging()),
                tracer,
                Validates.requireNonNegative(listenerWarningThreshold, "listenerWarningThreshold")
        );
    }

    /**
     * Returns the shared, application-wide bus with the default configuration. Prefer passing
     * your own instances around; this exists for code that has no better place to get one.
     *
     * @return the global bus
     */
    public static @NotNull EventBus global() {
        return Global.INSTANCE;
    }

    // ---------------------------------------------------------------------------------------------
    // Annotated listeners
    // ---------------------------------------------------------------------------------------------

    /**
     * Subscribes every {@link Subscribe} method of a listener object, including inherited ones.
     * The bus holds the object strongly until it is unsubscribed.
     *
     * <p>Registering the same object twice subscribes its methods twice.</p>
     *
     * @param listener the listener object
     * @return a subscription covering all the object's methods
     * @throws IllegalArgumentException if the object has no {@link Subscribe} methods, or one is malformed
     */
    public @NotNull Subscription register(
            @NotNull final Object listener
    ) {
        return this.dispatcher.register(Validates.require(listener, "listener"), false);
    }

    /**
     * Like {@link #register(Object)}, but holds the listener object weakly: once it is garbage
     * collected, its subscriptions remove themselves the next time they are offered an event.
     *
     * @param listener the listener object
     * @return a subscription covering all the object's methods
     * @throws IllegalArgumentException if the object has no {@link Subscribe} methods, or one is malformed
     */
    public @NotNull Subscription registerWeak(
            @NotNull final Object listener
    ) {
        return this.dispatcher.register(Validates.require(listener, "listener"), true);
    }

    /**
     * Registers several listener objects. Either all of them are registered, or, if one is
     * rejected, none are.
     *
     * @param listeners the listener objects
     * @return a subscription covering all of them
     * @throws IllegalArgumentException if any object has no {@link Subscribe} methods, or one is malformed
     */
    public @NotNull Subscription registerAll(
            @NotNull final Object @NotNull ... listeners
    ) {
        Validates.require(listeners, "listeners");

        val subscriptions = new Subscription[listeners.length];
        try {
            for (int i = 0; i < listeners.length; i++) {
                subscriptions[i] = this.register(listeners[i]);
            }
        } catch (final RuntimeException exception) {
            for (val subscription : subscriptions) {
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            }
            throw exception;
        }
        return Subscription.all(subscriptions);
    }

    /**
     * Unsubscribes every {@link Subscribe} method registered for a listener object (matched by
     * identity). Closing the {@link Subscription} returned by {@link #register(Object)} is faster
     * when it is at hand.
     *
     * @param listener the listener object
     * @return whether anything was unsubscribed
     */
    public boolean unregister(
            @NotNull final Object listener
    ) {
        Validates.require(listener, "listener");

        return this.dispatcher.unregister(listener);
    }

    /**
     * Unregisters several listener objects.
     *
     * @param listeners the listener objects
     * @return whether anything was unsubscribed
     */
    public boolean unregisterAll(
            @NotNull final Object @NotNull ... listeners
    ) {
        Validates.require(listeners, "listeners");

        boolean changed = false;
        for (val listener : listeners) {
            changed |= this.unregister(listener);
        }
        return changed;
    }

    // ---------------------------------------------------------------------------------------------
    // Functional listeners
    // ---------------------------------------------------------------------------------------------

    /**
     * Subscribes a listener to an event type and all of its subtypes, with default options.
     *
     * @param type     the event type
     * @param listener the listener
     * @param <E>      the event type
     * @return the subscription
     */
    public <E> @NotNull Subscription subscribe(
            @NotNull final Class<E> type,
            @NotNull final EventListener<? super E> listener
    ) {
        return this.subscribe(type, SubscriptionOptions.defaults(), listener);
    }

    /**
     * Subscribes a listener to an event type and all of its subtypes.
     *
     * @param type     the event type
     * @param options  the subscription options
     * @param listener the listener
     * @param <E>      the event type
     * @return the subscription
     */
    public <E> @NotNull Subscription subscribe(
            @NotNull final Class<E> type,
            @NotNull final SubscriptionOptions options,
            @NotNull final EventListener<? super E> listener
    ) {
        return this.add(requireEventType(type), options, null, listener);
    }

    /**
     * Subscribes a listener to the events of a type (and its subtypes) that match a filter.
     * Rejected events do not count towards {@link SubscriptionOptions#maxInvocations()}.
     *
     * @param type     the event type
     * @param options  the subscription options
     * @param filter   selects the events passed to the listener
     * @param listener the listener
     * @param <E>      the event type
     * @return the subscription
     */
    public <E> @NotNull Subscription subscribe(
            @NotNull final Class<E> type,
            @NotNull final SubscriptionOptions options,
            @NotNull final Predicate<? super E> filter,
            @NotNull final EventListener<? super E> listener
    ) {
        return this.add(requireEventType(type), options, Validates.require(filter, "filter"), listener);
    }

    /**
     * Subscribes a listener to a key, with default options.
     *
     * @param key      the key
     * @param listener the listener
     * @param <E>      the payload type
     * @return the subscription
     */
    public <E> @NotNull Subscription subscribe(
            @NotNull final EventKey<E> key,
            @NotNull final EventListener<? super E> listener
    ) {
        return this.subscribe(key, SubscriptionOptions.defaults(), listener);
    }

    /**
     * Subscribes a listener to a key.
     *
     * @param key      the key
     * @param options  the subscription options
     * @param listener the listener
     * @param <E>      the payload type
     * @return the subscription
     */
    public <E> @NotNull Subscription subscribe(
            @NotNull final EventKey<E> key,
            @NotNull final SubscriptionOptions options,
            @NotNull final EventListener<? super E> listener
    ) {
        return this.add(Validates.require(key, "key"), options, null, listener);
    }

    /**
     * Subscribes a listener to the payloads posted to a key that match a filter.
     * Rejected payloads do not count towards {@link SubscriptionOptions#maxInvocations()}.
     *
     * @param key      the key
     * @param options  the subscription options
     * @param filter   selects the payloads passed to the listener
     * @param listener the listener
     * @param <E>      the payload type
     * @return the subscription
     */
    public <E> @NotNull Subscription subscribe(
            @NotNull final EventKey<E> key,
            @NotNull final SubscriptionOptions options,
            @NotNull final Predicate<? super E> filter,
            @NotNull final EventListener<? super E> listener
    ) {
        return this.add(Validates.require(key, "key"), options, Validates.require(filter, "filter"), listener);
    }

    /**
     * Subscribes several listeners to an event type, with default options, in the given order.
     *
     * @param type      the event type
     * @param listeners the listeners
     * @param <E>       the event type
     * @return a subscription covering all of them
     */
    @SafeVarargs
    public final <E> @NotNull Subscription subscribeAll(
            @NotNull final Class<E> type,
            @NotNull final EventListener<? super E> @NotNull ... listeners
    ) {
        requireEventType(type);
        Validates.require(listeners, "listeners");

        val subscriptions = new Subscription[listeners.length];
        for (int i = 0; i < listeners.length; i++) {
            subscriptions[i] = this.subscribe(type, listeners[i]);
        }
        return Subscription.all(subscriptions);
    }

    /**
     * Unsubscribes a listener from an event type, matched by identity. Unsubscribing through the
     * {@link Subscription} returned by {@code subscribe} is preferred when it is at hand.
     *
     * @param type     the event type the listener was subscribed to
     * @param listener the listener
     * @return whether anything was unsubscribed
     */
    public boolean unsubscribe(
            @NotNull final Class<?> type,
            @NotNull final EventListener<?> listener
    ) {
        Validates.require(type, "type");
        Validates.require(listener, "listener");

        return this.dispatcher.unsubscribe(type, listener);
    }

    /**
     * Unsubscribes a listener from a key, matched by identity.
     *
     * @param key      the key the listener was subscribed to
     * @param listener the listener
     * @return whether anything was unsubscribed
     */
    public boolean unsubscribe(
            @NotNull final EventKey<?> key,
            @NotNull final EventListener<?> listener
    ) {
        Validates.require(key, "key");
        Validates.require(listener, "listener");

        return this.dispatcher.unsubscribe(key, listener);
    }

    /**
     * Unsubscribes several listeners from an event type, matched by identity.
     *
     * @param type      the event type the listeners were subscribed to
     * @param listeners the listeners
     * @return whether anything was unsubscribed
     */
    public boolean unsubscribeAll(
            @NotNull final Class<?> type,
            @NotNull final EventListener<?> @NotNull ... listeners
    ) {
        Validates.require(listeners, "listeners");

        boolean changed = false;
        for (val listener : listeners) {
            changed |= this.unsubscribe(type, listener);
        }
        return changed;
    }

    // ---------------------------------------------------------------------------------------------
    // Awaiting
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a future completed with the next event of a type (or subtype).
     *
     * <p>The future is completed on the posting thread after all other listeners have run, so it
     * sees the event's final state; use the {@code *Async} stages to continue elsewhere. Cancelling
     * the future, or letting it time out with {@link CompletableFuture#orTimeout}, unsubscribes it.</p>
     *
     * @param type the event type
     * @param <E>  the event type
     * @return a future of the next event
     */
    public <E> @NotNull CompletableFuture<E> await(
            @NotNull final Class<E> type
    ) {
        return this.dispatcher.await(requireEventType(type), null);
    }

    /**
     * Returns a future completed with the next event of a type (or subtype) that matches a filter.
     *
     * @param type   the event type
     * @param filter selects the event
     * @param <E>    the event type
     * @return a future of the next matching event
     * @see #await(Class)
     */
    public <E> @NotNull CompletableFuture<E> await(
            @NotNull final Class<E> type,
            @NotNull final Predicate<? super E> filter
    ) {
        return this.dispatcher.await(requireEventType(type), Validates.require(filter, "filter"));
    }

    /**
     * Returns a future completed with the next payload posted to a key.
     *
     * @param key the key
     * @param <E> the payload type
     * @return a future of the next payload
     * @see #await(Class)
     */
    public <E> @NotNull CompletableFuture<E> await(
            @NotNull final EventKey<E> key
    ) {
        return this.dispatcher.await(Validates.require(key, "key"), null);
    }

    /**
     * Returns a future completed with the next payload posted to a key that matches a filter.
     *
     * @param key    the key
     * @param filter selects the payload
     * @param <E>    the payload type
     * @return a future of the next matching payload
     * @see #await(Class)
     */
    public <E> @NotNull CompletableFuture<E> await(
            @NotNull final EventKey<E> key,
            @NotNull final Predicate<? super E> filter
    ) {
        return this.dispatcher.await(Validates.require(key, "key"), Validates.require(filter, "filter"));
    }

    // ---------------------------------------------------------------------------------------------
    // Posting
    // ---------------------------------------------------------------------------------------------

    /**
     * Dispatches an event, on the calling thread, to every listener of its class and supertypes.
     *
     * @param event the event
     * @param <E>   the event type
     * @return the same event, so its final state can be read inline
     */
    public <E> @NotNull E post(
            @NotNull final E event
    ) {
        Validates.require(event, "event");

        this.dispatcher.post(event);
        return event;
    }

    /**
     * Dispatches a payload, on the calling thread, to every listener of a key.
     *
     * @param key   the key
     * @param event the payload
     * @param <E>   the payload type
     * @return the same payload
     */
    public <E> @NotNull E post(
            @NotNull final EventKey<E> key,
            @NotNull final E event
    ) {
        Validates.require(key, "key");
        Validates.require(event, "event");

        this.dispatcher.post(key, event);
        return event;
    }

    /**
     * Like {@link #post(Object)}, but dispatches on the bus executor.
     *
     * @param event the event
     * @param <E>   the event type
     * @return a future completed with the event once every synchronous listener has run
     */
    public <E> @NotNull CompletableFuture<E> postAsync(
            @NotNull final E event
    ) {
        Validates.require(event, "event");

        return CompletableFuture.supplyAsync(() -> this.post(event), this.dispatcher.executor());
    }

    /**
     * Like {@link #post(EventKey, Object)}, but dispatches on the bus executor.
     *
     * @param key   the key
     * @param event the payload
     * @param <E>   the payload type
     * @return a future completed with the payload once every synchronous listener has run
     */
    public <E> @NotNull CompletableFuture<E> postAsync(
            @NotNull final EventKey<E> key,
            @NotNull final E event
    ) {
        Validates.require(key, "key");
        Validates.require(event, "event");

        return CompletableFuture.supplyAsync(() -> this.post(key, event), this.dispatcher.executor());
    }

    // ---------------------------------------------------------------------------------------------
    // Introspection
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns whether posting an event of a type would reach any listener, counting listeners of
     * its supertypes. Use it to skip building expensive events nobody listens to.
     *
     * @param type the event type
     * @return {@code true} if at least one listener would be offered the event
     */
    public boolean hasSubscribers(
            @NotNull final Class<?> type
    ) {
        return this.subscriberCount(type) != 0;
    }

    /**
     * Returns whether any listener is subscribed to a key.
     *
     * @param key the key
     * @return {@code true} if at least one listener is subscribed
     */
    public boolean hasSubscribers(
            @NotNull final EventKey<?> key
    ) {
        return this.subscriberCount(key) != 0;
    }

    /**
     * Returns how many listeners an event of a type would be offered to, counting listeners of
     * its supertypes (before filters are applied).
     *
     * @param type the event type
     * @return the subscriber count
     */
    public int subscriberCount(
            @NotNull final Class<?> type
    ) {
        return this.dispatcher.subscriberCount(Validates.require(type, "type"));
    }

    /**
     * Returns how many listeners are subscribed to a key.
     *
     * @param key the key
     * @return the subscriber count
     */
    public int subscriberCount(
            @NotNull final EventKey<?> key
    ) {
        return this.dispatcher.subscriberCount(Validates.require(key, "key"));
    }

    // ---------------------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------------------

    private @NotNull Subscription add(
            @NotNull final Object topic,
            @NotNull final SubscriptionOptions options,
            @Nullable final Predicate<?> filter,
            @NotNull final EventListener<?> listener
    ) {
        Validates.require(options, "options");
        Validates.require(listener, "listener");

        return this.dispatcher.subscribe(topic, options, filter, listener);
    }

    private static <T> @NotNull Class<T> requireEventType(
            @NotNull final Class<T> type
    ) {
        Validates.require(type, "type");
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Events are objects; use " + Primitives.wrap(type).getName() + " instead of " + type);
        }
        return type;
    }

    /**
     * Lazily creates the {@link #global()} bus on first use.
     */
    private static final class Global {

        private static final EventBus INSTANCE = new EventBus();

    }

}
