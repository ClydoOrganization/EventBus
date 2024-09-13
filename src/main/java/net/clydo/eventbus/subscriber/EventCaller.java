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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EventBus.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2024 ClydoNetwork
 */

package net.clydo.eventbus.subscriber;

/**
 * The {@code EventCaller} interface represents a functional interface
 * for calling an event. It defines a single abstract method, {@code call(E event)},
 * which allows an event to be processed.
 *
 * @param <E> the type of event to be processed
 */
@FunctionalInterface
public interface EventCaller<E> {

    /**
     * Processes the given event.
     *
     * @param event the event to be processed
     */
    void call(E event);
}