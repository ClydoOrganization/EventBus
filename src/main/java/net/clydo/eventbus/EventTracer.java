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

import net.clydo.eventbus.internal.LoggingTracer;
import net.clydo.eventbus.subscriber.Subscription;
import org.jetbrains.annotations.NotNull;

/**
 * Observes every dispatch on a bus, for debugging and profiling.
 *
 * <p>A bus without a tracer pays nothing for this hook. Callbacks run inline on the dispatching
 * (or, for {@link net.clydo.eventbus.subscriber.DispatchMode#ASYNC} listeners, the executor) thread, so they should be cheap
 * and must not throw.</p>
 */
public interface EventTracer {

    /**
     * Called once per {@code post}, before any listener runs. A count of zero means the event
     * has no listeners (a "dead" event).
     *
     * @param event           the posted event
     * @param subscriberCount how many subscriptions the event will be offered to
     */
    default void onPost(
            @NotNull final Object event,
            final int subscriberCount
    ) {
    }

    /**
     * Called after a listener has handled an event, whether it succeeded or not.
     *
     * @param event         the event
     * @param subscription  the listener's subscription
     * @param durationNanos how long the listener ran, including error handling
     */
    default void onDelivered(
            @NotNull final Object event,
            @NotNull final Subscription subscription,
            final long durationNanos
    ) {
    }

    /**
     * Logs posts and deliveries through {@link System.Logger} at {@code DEBUG} level.
     *
     * @return the logging tracer
     */
    static @NotNull EventTracer logging() {
        return LoggingTracer.INSTANCE;
    }

}
