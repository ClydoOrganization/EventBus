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

import org.jetbrains.annotations.NotNull;

/**
 * The cached dispatch plan for one event class.
 *
 * @param subscribers the merged subscribers of the class's hierarchy, in dispatch order
 * @param traits      the class's {@link SubscriberRegistry#STOPPABLE} and {@link SubscriberRegistry#CANCELLABLE} bits
 */
record Route(
        @NotNull EventSubscriber @NotNull [] subscribers,
        int traits
) {

}
