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

import lombok.val;
import net.clydo.eventbus.subscriber.DispatchMode;
import net.clydo.eventbus.subscriber.EventListener;
import net.clydo.eventbus.subscriber.Subscription;
import net.clydo.eventbus.subscriber.SubscriptionOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * A listener registered on a bus, together with its dispatch settings. This is the element type
 * of the {@link SubscriberRegistry}'s copy-on-write arrays, so its hot fields are plain finals.
 */
final class EventSubscriber implements Subscription {

    final @NotNull EventDispatcher dispatcher;
    final @NotNull Object topic;
    final @NotNull EventListener<Object> listener;
    final @Nullable Predicate<Object> filter;
    final int priority;
    final long sequence;
    final boolean async;
    final boolean ignoreCancelled;
    final boolean weak;

    /**
     * Invocations left, or {@code null} for unlimited subscriptions, which are the common case and
     * so carry no counter. Claimed atomically so concurrent dispatches never exceed the limit.
     */
    private final @Nullable AtomicInteger remaining;

    /**
     * Whether no option applies, so delivery is a plain call. True for most subscribers, and it
     * keeps their per-event cost to one volatile read and one branch.
     */
    final boolean direct;

    /**
     * Written only under the registry lock, read lock-free by dispatchers.
     */
    volatile boolean active = true;


    EventSubscriber(
            @NotNull final EventDispatcher dispatcher,
            @NotNull final Object topic,
            @NotNull final SubscriptionOptions options,
            @Nullable final Predicate<Object> filter,
            @NotNull final EventListener<Object> listener,
            final long sequence
    ) {
        this.dispatcher = dispatcher;
        this.topic = topic;
        this.listener = listener;
        this.filter = filter;
        this.priority = options.priority();
        this.sequence = sequence;
        this.async = options.mode() == DispatchMode.ASYNC;
        this.ignoreCancelled = options.ignoreCancelled();
        this.remaining = options.maxInvocations() == SubscriptionOptions.UNLIMITED ? null : new AtomicInteger(options.maxInvocations());
        this.weak = listener instanceof MethodListener method && method.weak();
        this.direct = !this.async && this.remaining == null && !this.weak && filter == null && dispatcher.tracer == null;
    }

    /**
     * Offers an event to this listener: applies the filter and invocation limit, then invokes the
     * listener inline or hands it to the executor.
     *
     * @param event the event
     */
    void deliver(
            @NotNull final Object event
    ) {
        if (!this.active) {
            return;
        }

        if (this.direct) {
            this.invokeListener(event);
        } else {
            this.deliverWithOptions(event);
        }
    }

    /**
     * Returns the object whose {@link net.clydo.eventbus.subscriber.Subscribe} method this
     * subscriber invokes.
     *
     * @return the listener object, or {@code null} for functional listeners and collected weak ones
     */
    @Nullable Object owner() {
        return this.listener instanceof MethodListener method ? method.listener() : null;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void unsubscribe() {
        this.dispatcher.registry.remove(this);
    }

    @Override
    public String toString() {
        val topicName = this.topic instanceof Class<?> type ? type.getName() : this.topic.toString();
        return "Subscription[" + topicName + " -> " + this.listener + ", priority=" + this.priority + "]";
    }

    private void deliverWithOptions(
            @NotNull final Object event
    ) {
        if (this.weak && this.owner() == null) {
            this.unsubscribe();
            return;
        }

        if (this.filter != null) {
            try {
                if (!this.filter.test(event)) {
                    return;
                }
            } catch (final Exception exception) {
                this.dispatcher.errorHandler.handle(exception, event, this);
                return;
            }
        }

        if (this.remaining != null && !this.claimInvocation(this.remaining)) {
            return;
        }

        if (this.async) {
            this.dispatcher.executor.execute(() -> this.invoke(event));
        } else {
            this.invoke(event);
        }
    }

    private void invoke(
            @NotNull final Object event
    ) {
        val tracer = this.dispatcher.tracer;
        if (tracer == null) {
            this.invokeListener(event);
            return;
        }

        val start = System.nanoTime();
        try {
            this.invokeListener(event);
        } finally {
            tracer.onDelivered(event, this, System.nanoTime() - start);
        }
    }

    private void invokeListener(
            @NotNull final Object event
    ) {
        try {
            this.listener.onEvent(event);
        } catch (final Exception exception) {
            this.dispatcher.errorHandler.handle(exception, event, this);
        }
    }

    /**
     * Takes one invocation from the counter, never letting it drop below zero, and unsubscribes
     * once the last one is taken.
     *
     * @param remaining this subscriber's invocation counter
     * @return whether an invocation was available
     */
    private boolean claimInvocation(
            @NotNull final AtomicInteger remaining
    ) {
        val left = remaining.getAndUpdate(count -> count > 0 ? count - 1 : 0);
        if (left <= 0) {
            return false;
        }

        if (left == 1) {
            this.unsubscribe();
        }
        return true;
    }

}
