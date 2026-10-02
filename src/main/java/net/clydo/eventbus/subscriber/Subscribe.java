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

import java.lang.annotation.*;

/**
 * Marks a method as an event listener for {@link net.clydo.eventbus.EventBus#register(Object)}.
 *
 * <p>The method must be non-static, return {@code void}, and declare exactly one non-primitive
 * parameter: the event type it listens to. It receives instances of that type and of all its
 * subtypes. It may have any visibility and may throw checked exceptions, which are passed to the
 * bus's {@link net.clydo.eventbus.EventErrorHandler}.</p>
 *
 * <p>Annotated methods inherited from superclasses are registered too. Overriding an annotated
 * method does not register it twice.</p>
 *
 * <pre>{@code
 * @Subscribe(priority = EventPriority.HIGH, ignoreCancelled = true)
 * void onChat(ChatEvent event) { ... }
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {

    /**
     * Listeners with a higher priority run first.
     *
     * @return the priority, {@link EventPriority#NORMAL} by default
     * @see EventPriority
     */
    int priority() default EventPriority.NORMAL;

    /**
     * Whether the method runs on the dispatching thread or on the bus executor.
     *
     * @return the dispatch mode, {@link DispatchMode#SYNC} by default
     */
    DispatchMode mode() default DispatchMode.SYNC;

    /**
     * Whether to skip events that are already canceled when this listener's turn comes.
     *
     * @return {@code true} to skip canceled events
     * @see net.clydo.eventbus.event.Cancellable
     */
    boolean ignoreCancelled() default false;

}
