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
import lombok.Setter;

/**
 * A base class for events that support both {@linkplain Cancellable cancellation} and
 * {@linkplain Event#stopPropagation() propagation stopping}.
 */
@Getter
@Setter
public abstract class CancellableEvent extends Event implements Cancellable {

    /**
     * Whether the action this event announces has been vetoed.
     */
    private boolean cancelled;

    /**
     * Clears both the propagation state and the cancellation, so this instance can be posted again.
     */
    @Override
    public void reset() {
        super.reset();
        this.cancelled = false;
    }

}
