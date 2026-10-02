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

import org.jetbrains.annotations.NotNull;

/**
 * The {@code EventListener} interface is the functional interface for handling events posted to
 * an {@link net.clydo.eventbus.EventBus}. Lambdas and method references are subscribed as listeners,
 * with their priority and other settings given by {@link SubscriptionOptions}.
 *
 * <p>Exceptions thrown by a listener never reach the poster directly; they are passed to the
 * bus's {@link net.clydo.eventbus.EventErrorHandler}, which decides whether dispatch continues.</p>
 *
 * @param <E> the type of event to be processed
 */
@FunctionalInterface
public interface EventListener<E> {

    /**
     * Processes the given event.
     *
     * @param event the event to be processed
     * @throws Exception if processing fails
     */
    void onEvent(
            @NotNull final E event
    ) throws Exception;

}
