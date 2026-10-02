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

package net.clydo.eventbus.event;

import lombok.Getter;

/**
 * An optional base class for events whose propagation can be stopped.
 *
 * <p>Any object can be posted to an {@link net.clydo.eventbus.EventBus}; extending this class
 * only adds {@link #stopPropagation()}. Stopping propagation is independent of
 * {@linkplain Cancellable cancellation}: a stopped event is not offered to any further listener,
 * while a canceled event still is, and only signals the poster not to carry out the action.</p>
 *
 * <p>Events are not thread-safe. They should be mutated only by synchronous listeners.</p>
 *
 * @see CancellableEvent
 */
public abstract class Event {

    /**
     * Whether a listener has stopped this event's propagation.
     */
    @Getter
    private boolean propagationStopped;

    /**
     * Prevents listeners after the current one from receiving this event. It stays stopped until
     * {@link #reset()}.
     */
    public void stopPropagation() {
        this.propagationStopped = true;
    }

    /**
     * Clears the dispatch state so this instance can be posted again. Hot paths, such as a render
     * loop posting one event per frame, can reuse a single instance instead of allocating:
     * <pre>{@code
     * this.frameEvent.reset();
     * this.frameEvent.setPartialTick(partialTick);
     * bus.post(this.frameEvent);
     * }</pre>
     *
     * <p>Subclasses that add their own per-dispatch state should override this and call
     * {@code super.reset()}.</p>
     */
    public void reset() {
        this.propagationStopped = false;
    }

}
