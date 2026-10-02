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

/**
 * An event that announces an action its listeners can veto.
 *
 * <p>Cancelling does not stop dispatch: later listeners still receive the event (unless they
 * subscribed with {@code ignoreCancelled}) and may {@linkplain #resume() resume} it. The poster
 * reads the final state after {@code post} returns:</p>
 * <pre>{@code
 * if (!bus.post(new BlockBreakEvent(block)).isCancelled()) {
 *     block.destroy();
 * }
 * }</pre>
 *
 * <p>Implement this interface on any event type, even one that does not extend {@link Event},
 * or extend {@link CancellableEvent}.</p>
 *
 * @see Event#stopPropagation()
 */
public interface Cancellable {

    /**
     * Returns whether the action this event announces has been vetoed.
     *
     * @return {@code true} if canceled
     */
    boolean isCancelled();

    /**
     * Vetoes or restores the action this event announces.
     *
     * @param cancelled {@code true} to cancel
     */
    void setCancelled(
            final boolean cancelled
    );

    /**
     * Vetoes the action this event announces. Same as {@code setCancelled(true)}.
     */
    default void cancel() {
        this.setCancelled(true);
    }

    /**
     * Restores an action that was vetoed. Same as {@code setCancelled(false)}.
     */
    default void resume() {
        this.setCancelled(false);
    }

}
