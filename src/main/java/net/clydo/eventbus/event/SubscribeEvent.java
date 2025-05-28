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

package net.clydo.eventbus.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code SubscribeEvent} annotation is used to mark methods as event listeners.
 * Methods annotated with {@code SubscribeEvent} will be automatically invoked
 * when the corresponding event is fired within the event-handling system.
 *
 * <p>This annotation also allows setting a priority level, which determines
 * the order in which listeners are called for the same event. Methods with
 * higher priority are called before those with lower priority.</p>
 *
 * <p>The priority defaults to {@link StandardPriorities#NORMAL}, but can be
 * customized by specifying a different priority level. See {@link StandardPriorities}
 * for available priority levels.</p>
 *
 * <p>This annotation is retained at runtime and can only be applied to methods.</p>
 *
 * @see StandardPriorities
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribeEvent {

    /**
     * Defines the priority of the event listener.
     * Listeners with higher priority are executed before those with lower priority.
     *
     * @return the priority of the event listener, defaulting to {@link StandardPriorities#NORMAL}
     */
    int priority() default 0;
}
