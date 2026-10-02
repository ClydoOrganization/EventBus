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

import net.clydo.eventbus.internal.DefaultErrorHandlers;
import net.clydo.eventbus.subscriber.Subscription;
import org.jetbrains.annotations.NotNull;

/**
 * Decides what happens when a listener (or its filter) throws.
 *
 * <p>The handler runs on the thread that invoked the listener. Returning normally lets dispatch
 * continue with the next listener; throwing aborts the dispatch and propagates out of
 * {@link EventBus#post(Object)} (or completes the future of {@link EventBus#postAsync(Object)}
 * exceptionally). {@link Error}s thrown by listeners are never passed here; they always propagate.</p>
 */
@FunctionalInterface
public interface EventErrorHandler {

    /**
     * Handles a listener failure.
     *
     * @param exception    what the listener threw
     * @param event        the event being dispatched
     * @param subscription the failing listener's subscription, which may be unsubscribed here
     */
    void handle(
            @NotNull final Exception exception,
            @NotNull final Object event,
            @NotNull final Subscription subscription
    );

    /**
     * Logs the failure through {@link System.Logger} and continues with the next listener.
     * This is the default.
     *
     * @return the logging handler
     */
    static @NotNull EventErrorHandler logging() {
        return DefaultErrorHandlers.LOGGING;
    }

    /**
     * Aborts the dispatch by throwing an {@link EventDispatchException}
     * that wraps the failure.
     *
     * @return the rethrowing handler
     */
    static @NotNull EventErrorHandler rethrowing() {
        return DefaultErrorHandlers.RETHROWING;
    }

}
