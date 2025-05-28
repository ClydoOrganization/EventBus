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

import lombok.experimental.UtilityClass;

/**
 * The {@code StandardPriorities} class defines standard priority levels
 * that can be used to categorize the importance of various tasks or events.
 *
 * <p>Each priority level is represented by an integer, where higher values
 * indicate higher priority. These constants can be used to compare and
 * set priority levels in a consistent manner.</p>
 *
 * <p>This class is marked as a utility class, meaning it should not be instantiated.</p>
 *
 * <ul>
 *   <li>{@code LOWEST}: Represents the lowest priority (-2).</li>
 *   <li>{@code LOW}: Represents a low priority (-1).</li>
 *   <li>{@code NORMAL}: Represents a normal priority (0).</li>
 *   <li>{@code HIGH}: Represents a high priority (1).</li>
 *   <li>{@code HIGHEST}: Represents the highest priority (2).</li>
 * </ul>
 */
@SuppressWarnings("unused")
@UtilityClass
public class StandardPriorities {

    /**
     * The lowest possible priority.
     */
    public final int LOWEST = -2;

    /**
     * A low priority.
     */
    public final int LOW = -1;

    /**
     * The default or normal priority.
     */
    public final int NORMAL = 0;

    /**
     * A high priority.
     */
    public final int HIGH = 1;

    /**
     * The highest possible priority.
     */
    public final int HIGHEST = 2;
}
