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

import net.clydo.eventbus.subscriber.Subscribe;
import net.clydo.eventbus.subscriber.SubscriptionOptions;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;

/**
 * Validated metadata for one {@link Subscribe} method, shared by every instance of its class.
 *
 * @param eventType   the method's parameter type
 * @param invoker     the method, typed {@code (Object listener, Object event)void}
 * @param options     the options declared by the annotation
 * @param description {@code ClassName#method(EventType)}, for diagnostics
 * @see ListenerMethods
 */
record ListenerMethod(
        @NotNull Class<?> eventType,
        @NotNull MethodHandle invoker,
        @NotNull SubscriptionOptions options,
        @NotNull String description
) {

}
