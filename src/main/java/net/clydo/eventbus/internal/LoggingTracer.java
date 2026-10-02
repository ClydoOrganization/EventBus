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

import lombok.CustomLog;
import net.clydo.eventbus.EventTracer;
import net.clydo.eventbus.subscriber.Subscription;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link EventTracer#logging()} implementation.
 *
 * <p>Not part of the public API; it may change without notice.</p>
 */
@ApiStatus.Internal
@CustomLog
public enum LoggingTracer implements EventTracer {

    /**
     * The singleton tracer.
     */
    INSTANCE;

    @Override
    public void onPost(
            @NotNull final Object event,
            final int subscriberCount
    ) {
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Posting {0} to {1} subscriber(s)", event, subscriberCount);
        }
    }

    @Override
    public void onDelivered(
            @NotNull final Object event,
            @NotNull final Subscription subscription,
            final long durationNanos
    ) {
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "{0} handled {1} in {2} ns", subscription, event, durationNanos);
        }
    }

}
