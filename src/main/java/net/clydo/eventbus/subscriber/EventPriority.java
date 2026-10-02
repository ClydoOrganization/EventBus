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

import lombok.experimental.UtilityClass;

/**
 * Well-known listener priorities.
 *
 * <p>Priorities are plain {@code int}s so any value in between can be used. Listeners with a
 * higher priority run first; listeners with equal priority run in the order they were
 * subscribed, which makes dispatch order fully deterministic.</p>
 *
 * <p>The constants are declared {@code static} explicitly (rather than left to {@code @UtilityClass})
 * because annotations such as {@link Subscribe#priority()} reference them, and tools that do not run
 * Lombok, like Javadoc, must still see compile-time constants.</p>
 */
@UtilityClass
public class EventPriority {

    /**
     * Runs after every other standard priority.
     */
    public static final int LOWEST = -200;

    /**
     * Runs after {@link #NORMAL}.
     */
    public static final int LOW = -100;

    /**
     * The default priority.
     */
    public static final int NORMAL = 0;

    /**
     * Runs before {@link #NORMAL}.
     */
    public static final int HIGH = 100;

    /**
     * Runs before every other standard priority.
     */
    public static final int HIGHEST = 200;

}
