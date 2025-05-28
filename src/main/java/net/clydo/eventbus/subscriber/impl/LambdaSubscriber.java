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
 * Copyright (C) 2024-2025 ClydoNetwork
 */

package net.clydo.eventbus.subscriber.impl;

import lombok.EqualsAndHashCode;
import net.clydo.eventbus.subscriber.EventCaller;
import net.clydo.eventbus.subscriber.EventSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The {@code LambdaSubscriber} class is an implementation of {@link EventSubscriber}
 * that uses a lambda or method reference to handle events. It allows specifying an
 * event handler with a generic type and a priority level.
 *
 * @param <E> the type of event that this subscriber handles
 */
@EqualsAndHashCode(callSuper = true)
public class LambdaSubscriber<E> extends EventSubscriber {

    /**
     * The event handler that processes events.
     */
    @NotNull
    private final EventCaller<E> caller;

    /**
     * Constructs a {@code LambdaSubscriber} with the specified priority and event handler.
     *
     * @param priority the priority level of this subscriber
     * @param caller   the event handler that processes events
     * @throws NullPointerException if {@code caller} is {@code null}
     */
    public LambdaSubscriber(int priority, @NotNull EventCaller<E> caller) {
        super(priority);
        this.caller = Objects.requireNonNull(caller, "Caller must not be null");
    }

    /**
     * Calls the event handler with the given event. The event is cast to the
     * appropriate type {@code E} before being processed by the {@code caller}.
     *
     * @param event the event to be processed
     */
    @SuppressWarnings("unchecked")
    @Override
    public void call(Object event) {
        this.caller.call((E) event);
    }
}
