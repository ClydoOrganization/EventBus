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

package net.clydo.eventbus.internal;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;
import net.clydo.eventbus.EventErrorHandler;
import net.clydo.eventbus.EventTracer;
import net.clydo.eventbus.event.Cancellable;
import net.clydo.eventbus.event.Event;
import net.clydo.eventbus.event.EventKey;
import net.clydo.eventbus.subscriber.EventListener;
import net.clydo.eventbus.subscriber.Subscription;
import net.clydo.eventbus.subscriber.SubscriptionOptions;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * The engine behind one {@link net.clydo.eventbus.EventBus}: it creates subscribers, keeps them
 * in a {@link SubscriberRegistry} and runs the dispatch loop. The bus validates arguments and
 * delegates here.
 *
 * <p>Not part of the public API; it may change without notice.</p>
 */
@ApiStatus.Internal
@Accessors(fluent = true)
public final class EventDispatcher {

    /**
     * Awaiters run last so they observe the final state of the event.
     */
    private static final SubscriptionOptions AWAIT_OPTIONS = SubscriptionOptions.once().withPriority(Integer.MIN_VALUE);

    /**
     * Runs asynchronous listeners and asynchronous posts.
     */
    @Getter
    final @NotNull Executor executor;

    final @NotNull EventErrorHandler errorHandler;

    final @Nullable EventTracer tracer;

    final @NotNull SubscriberRegistry registry;

    /**
     * Creates a dispatcher.
     *
     * @param executor                 runs asynchronous listeners and posts
     * @param errorHandler             handles listener exceptions
     * @param tracer                   observes every dispatch, or {@code null}
     * @param listenerWarningThreshold warns once a topic has more subscribers than this; {@code 0} disables it
     */
    public EventDispatcher(
            @NotNull final Executor executor,
            @NotNull final EventErrorHandler errorHandler,
            @Nullable final EventTracer tracer,
            final int listenerWarningThreshold
    ) {
        this.executor = executor;
        this.errorHandler = errorHandler;
        this.tracer = tracer;
        this.registry = new SubscriberRegistry(listenerWarningThreshold);
    }

    /**
     * Subscribes a listener to a topic.
     *
     * @param topic    an event {@link Class} or an {@link EventKey}
     * @param options  the subscription options
     * @param filter   selects the events passed to the listener, or {@code null} for all
     * @param listener the listener
     * @return the subscription
     */
    @SuppressWarnings("unchecked")
    public @NotNull Subscription subscribe(
            @NotNull final Object topic,
            @NotNull final SubscriptionOptions options,
            @Nullable final Predicate<?> filter,
            @NotNull final EventListener<?> listener
    ) {
        val subscriber = new EventSubscriber(
                this,
                topic,
                options,
                (Predicate<Object>) filter,
                (EventListener<Object>) listener,
                this.registry.nextSequence()
        );
        this.registry.add(subscriber);
        return subscriber;
    }

    /**
     * Subscribes every {@link net.clydo.eventbus.subscriber.Subscribe} method of a listener object.
     *
     * @param listener the listener object
     * @param weak     whether to hold the listener object weakly
     * @return a subscription covering all the object's methods
     * @throws IllegalArgumentException if the object has no annotated methods, or one is malformed
     */
    public @NotNull Subscription register(
            @NotNull final Object listener,
            final boolean weak
    ) {
        val listeners = ListenerMethods.createMethodListeners(listener, weak ? this.registry.collectedListeners() : null);
        val subscribers = new EventSubscriber[listeners.size()];
        for (int i = 0; i < subscribers.length; i++) {
            val methodListener = listeners.get(i);
            subscribers[i] = new EventSubscriber(
                    this,
                    methodListener.method().eventType(),
                    methodListener.method().options(),
                    null,
                    methodListener,
                    this.registry.nextSequence()
            );
        }

        this.registry.add(subscribers);
        return Subscription.all(subscribers);
    }

    /**
     * Unsubscribes every annotated method registered for a listener object, matched by identity.
     *
     * @param listener the listener object
     * @return whether anything was unsubscribed
     */
    public boolean unregister(
            @NotNull final Object listener
    ) {
        return this.registry.removeIf(subscriber -> subscriber.owner() == listener);
    }

    /**
     * Unsubscribes a listener from a topic, matched by identity.
     *
     * @param topic    the topic the listener was subscribed to
     * @param listener the listener
     * @return whether anything was unsubscribed
     */
    public boolean unsubscribe(
            @NotNull final Object topic,
            @NotNull final EventListener<?> listener
    ) {
        return this.registry.removeIf(topic, subscriber -> subscriber.listener == listener);
    }

    /**
     * Returns a future completed with the next event of a topic that matches a filter.
     *
     * @param topic  an event {@link Class} or an {@link EventKey}
     * @param filter selects the event, or {@code null} for the next one
     * @param <E>    the event type
     * @return a future that unsubscribes when it completes or is canceled
     */
    public <E> @NotNull CompletableFuture<E> await(
            @NotNull final Object topic,
            @Nullable final Predicate<? super E> filter
    ) {
        val future = new CompletableFuture<E>();
        final EventListener<E> completer = future::complete;
        val subscription = this.subscribe(topic, AWAIT_OPTIONS, filter, completer);
        future.whenComplete((event, failure) -> subscription.unsubscribe());
        return future;
    }

    /**
     * Dispatches an event to the listeners of its class and supertypes.
     *
     * @param event the event
     */
    public void post(
            @NotNull final Object event
    ) {
        val route = this.registry.route(event.getClass());
        this.dispatch(event, route.subscribers(), route.traits());
    }

    /**
     * Dispatches a payload to the listeners of a key.
     *
     * @param key   the key
     * @param event the payload
     */
    public void post(
            @NotNull final EventKey<?> key,
            @NotNull final Object event
    ) {
        val subscribers = this.registry.subscribersOf(key);
        this.dispatch(event, subscribers, subscribers.length == 0 ? 0 : SubscriberRegistry.traitsOf(event.getClass()));
    }

    /**
     * Returns how many listeners an event of a class would be offered to, counting its supertypes.
     *
     * @param type the event class
     * @return the subscriber count
     */
    public int subscriberCount(
            @NotNull final Class<?> type
    ) {
        return this.registry.route(type).subscribers().length;
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
        return this.registry.subscribersOf(key).length;
    }

    private void dispatch(
            @NotNull final Object event,
            @NotNull final EventSubscriber @NotNull [] subscribers,
            final int traits
    ) {
        if (this.tracer != null) {
            this.tracer.onPost(event, subscribers.length);
        }

        // Casts from precomputed traits instead of instanceof, see SubscriberRegistry#TRAITS
        val stoppable = (traits & SubscriberRegistry.STOPPABLE) != 0 ? (Event) event : null;
        val cancellable = (traits & SubscriberRegistry.CANCELLABLE) != 0 ? (Cancellable) event : null;

        for (val subscriber : subscribers) {
            if (stoppable != null && stoppable.isPropagationStopped()) {
                return;
            }
            if (subscriber.ignoreCancelled && cancellable != null && cancellable.isCancelled()) {
                continue;
            }

            subscriber.deliver(event);
        }
    }

}
