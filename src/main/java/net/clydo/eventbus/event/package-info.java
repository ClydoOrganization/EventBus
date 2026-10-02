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
 * The event model.
 *
 * <p>Any object can be posted as an event. {@link net.clydo.eventbus.event.Event} adds propagation
 * stopping, {@link net.clydo.eventbus.event.Cancellable} adds cancellation, and
 * {@link net.clydo.eventbus.event.CancellableEvent} combines both. {@link net.clydo.eventbus.event.EventKey}
 * identifies events that are not distinguished by their class.</p>
 */
package net.clydo.eventbus.event;
