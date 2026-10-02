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

/**
 * Where a listener is invoked.
 */
public enum DispatchMode {

    /**
     * On the dispatching thread, in priority order. The listener can cancel the event or
     * stop its propagation, and later listeners observe the result.
     */
    SYNC,

    /**
     * On the bus's {@link java.util.concurrent.Executor}. The listener is handed the event at
     * its priority position, but runs concurrently with the rest of the dispatch, so it cannot
     * influence later listeners and must treat the event as read-only.
     */
    ASYNC

}
