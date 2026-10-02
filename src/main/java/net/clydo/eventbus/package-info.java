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

/**
 * A small, fast, type-safe publish/subscribe event bus.
 *
 * <p>{@link net.clydo.eventbus.EventBus} is the entry point. It is configured with an
 * {@link net.clydo.eventbus.EventErrorHandler} and an optional {@link net.clydo.eventbus.EventTracer}.
 *
 * <ul>
 *   <li>{@link net.clydo.eventbus.event} &mdash; the event model: base classes, cancellation and keys.</li>
 *   <li>{@link net.clydo.eventbus.subscriber} &mdash; subscribing: listeners, annotations, options and
 *   subscription handles.</li>
 * </ul>
 */
package net.clydo.eventbus;
