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

package net.clydo.eventbus.subscriber;

import lombok.EqualsAndHashCode;

/**
 * The {@code EventSubscriber} class provides a base implementation for an
 * event listener with an associated priority. It implements the {@link EventCaller}
 * interface and allows subclasses to handle events with a specific priority level.
 */
@EqualsAndHashCode
public abstract class EventSubscriber implements EventCaller<Object> {

    /**
     * The priority level of this event subscriber.
     */
    private final int priority;

    /**
     * Constructs an {@code EventSubscriber} with the given priority.
     *
     * @param priority the priority level of this event subscriber
     */
    public EventSubscriber(int priority) {
        this.priority = priority;
    }

    /**
     * Returns the priority of this event subscriber.
     *
     * @return the priority level of this subscriber
     */
    public int priority() {
        return this.priority;
    }
}
