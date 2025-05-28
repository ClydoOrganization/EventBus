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

/**
 * The {@code Event} class represents a basic event that can be canceled.
 * It determines if the event is cancelable based on the presence of the
 * {@link Cancelable} annotation on the class.
 */
public class Event {

    /**
     * Indicates whether this event can be canceled.
     */
    private final boolean cancelable;

    /**
     * Tracks whether this event has been canceled.
     */
    private boolean cancelled;

    /**
     * Constructs a new {@code Event}. The cancelable status is determined
     * by checking for the presence of the {@link Cancelable} annotation on the class.
     */
    public Event() {
        this.cancelable = this.getClass().isAnnotationPresent(Cancelable.class);
    }

    /**
     * Cancels the event if it is cancelable.
     * Sets the {@code cancelled} flag to {@code true}.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Resume the event by setting the {@code cancelled} flag to {@code false}.
     * This allows the event to proceed even if it was previously canceled.
     */
    public void resume() {
        this.cancelled = false;
    }

    /**
     * Determines whether the event is cancelable.
     *
     * @return {@code true} if the event can be canceled, {@code false} otherwise.
     */
    public boolean cancelable() {
        return this.cancelable;
    }

    /**
     * Checks whether the event has been canceled. This will return {@code true}
     * only if the event is both cancelable and has been canceled.
     *
     * @return {@code true} if the event has been canceled, {@code false} otherwise.
     */
    public boolean cancelled() {
        return this.cancelable() && this.cancelled;
    }
}
