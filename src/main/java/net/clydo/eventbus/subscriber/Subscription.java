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

package net.clydo.eventbus.subscriber;

import net.clydo.clytil.Validates;
import net.clydo.clytil.iface.CloseableScope;
import net.clydo.eventbus.internal.CompositeSubscription;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * A handle to one or more listeners registered on an {@link net.clydo.eventbus.EventBus}.
 *
 * <p>A subscription stays active until it is {@linkplain #unsubscribe() unsubscribed},
 * reaches its {@linkplain SubscriptionOptions#maxInvocations() invocation limit}, or (for
 * {@linkplain net.clydo.eventbus.EventBus#registerWeak(Object) weak} registrations) its listener
 * object is garbage collected. Once {@link #unsubscribe()} returns, the listener is not invoked by
 * any dispatch that starts afterward.</p>
 *
 * <p>Being a {@link CloseableScope}, a subscription can be scoped with try-with-resources:</p>
 * <pre>{@code
 * try (var subscription = bus.subscribe(PlayerJoinEvent.class, this::onJoin)) {
 *     ...
 * }
 * }</pre>
 *
 * <p>Subscriptions combine into one handle with {@link #and(Subscription)} and {@link #all}, and
 * {@link #empty()} is a convenient starting value for fields:</p>
 * <pre>{@code
 * private Subscription subscriptions = Subscription.empty();
 *
 * void enable() {
 *     this.subscriptions = bus.subscribe(JoinEvent.class, this::onJoin)
 *             .and(bus.subscribe(QuitEvent.class, this::onQuit));
 * }
 *
 * void disable() {
 *     this.subscriptions.unsubscribe();
 *     this.subscriptions = Subscription.empty();
 * }
 * }</pre>
 */
public interface Subscription extends CloseableScope {

    /**
     * Returns a subscription that is never active and whose {@link #unsubscribe()} does nothing.
     *
     * @return the empty subscription
     */
    static @NotNull Subscription empty() {
        return CompositeSubscription.EMPTY;
    }

    /**
     * Combines several subscriptions into one that is active while any of them is, and
     * unsubscribes all of them together.
     *
     * @param subscriptions the subscriptions
     * @return a subscription covering all of them; {@link #empty()} if there are none
     */
    static @NotNull Subscription all(
            @NotNull final Subscription @NotNull ... subscriptions
    ) {
        Validates.require(subscriptions, "subscriptions");

        return CompositeSubscription.of(Arrays.asList(subscriptions));
    }

    /**
     * Combines a collection of subscriptions into one, such as handles gathered in a loop.
     *
     * @param subscriptions the subscriptions
     * @return a subscription covering all of them; {@link #empty()} if there are none
     * @see #all(Subscription...)
     */
    static @NotNull Subscription all(
            @NotNull final Collection<? extends Subscription> subscriptions
    ) {
        return CompositeSubscription.of(subscriptions);
    }

    /**
     * Returns whether the listener behind this subscription can still receive events.
     *
     * @return {@code true} until the subscription is removed
     */
    boolean isActive();

    /**
     * Removes the listener from its bus. Calling this more than once has no effect.
     */
    void unsubscribe();

    /**
     * Combines this subscription with another into one handle. Unlike
     * {@link CloseableScope#closeThen(CloseableScope)}, the result is still a {@code Subscription},
     * so it keeps {@link #isActive()}.
     *
     * @param other the subscription to add
     * @return a subscription covering both
     */
    default @NotNull Subscription and(
            @NotNull final Subscription other
    ) {
        return all(this, other);
    }

    /**
     * Same as {@link #unsubscribe()}.
     */
    @Override
    default void close() {
        this.unsubscribe();
    }

}
