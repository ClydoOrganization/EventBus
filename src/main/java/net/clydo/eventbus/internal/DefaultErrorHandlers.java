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
import net.clydo.eventbus.EventDispatchException;
import net.clydo.eventbus.EventErrorHandler;
import net.clydo.eventbus.subscriber.Subscription;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in {@link EventErrorHandler}s, returned by {@link EventErrorHandler#logging()} and
 * {@link EventErrorHandler#rethrowing()}.
 *
 * <p>Not part of the public API; it may change without notice.</p>
 */
@ApiStatus.Internal
@CustomLog
public enum DefaultErrorHandlers implements EventErrorHandler {

    /**
     * Logs the failure and continues with the next listener.
     */
    LOGGING {
        @Override
        public void handle(
                @NotNull final Exception exception,
                @NotNull final Object event,
                @NotNull final Subscription subscription
        ) {
            LOGGER.log(System.Logger.Level.ERROR, () -> describe(event, subscription), exception);
        }
    },

    /**
     * Aborts the dispatch with an {@link EventDispatchException}.
     */
    RETHROWING {
        @Override
        public void handle(
                @NotNull final Exception exception,
                @NotNull final Object event,
                @NotNull final Subscription subscription
        ) {
            throw new EventDispatchException(describe(event, subscription), exception);
        }
    };

    private static @NotNull String describe(
            @NotNull final Object event,
            @NotNull final Subscription subscription
    ) {
        return subscription + " failed to handle " + event.getClass().getName();
    }

}
