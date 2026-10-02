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

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.val;
import net.clydo.eventbus.subscriber.EventListener;
import net.clydo.eventbus.subscriber.Subscribe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Invokes one {@link Subscribe} method on a listener object, which is held either strongly or
 * weakly. A weakly held listener that has been collected is skipped; its {@link EventSubscriber}
 * then removes itself.
 */
@Accessors(fluent = true)
final class MethodListener implements EventListener<Object> {

    /**
     * The metadata of the invoked method.
     */
    @Getter
    private final @NotNull ListenerMethod method;

    private final @NotNull MethodHandle invoker;

    private final @Nullable Object strongListener;

    private final @Nullable WeakReference<Object> weakListener;

    MethodListener(
            @NotNull final ListenerMethod method,
            @NotNull final Object listener,
            @Nullable final ReferenceQueue<Object> weakQueue
    ) {
        this.method = method;
        this.invoker = method.invoker();
        this.strongListener = weakQueue == null ? listener : null;
        this.weakListener = weakQueue == null ? null : new WeakReference<>(listener, weakQueue);
    }

    /**
     * Returns the listener object.
     *
     * @return the listener object, or {@code null} if it was weakly held and has been collected
     */
    @Nullable Object listener() {
        return this.weakListener == null ? this.strongListener : this.weakListener.get();
    }

    /**
     * Returns whether the listener object is held weakly.
     *
     * @return {@code true} for {@link net.clydo.eventbus.EventBus#registerWeak(Object)} registrations
     */
    boolean weak() {
        return this.weakListener != null;
    }

    @SneakyThrows
    @Override
    public void onEvent(
            @NotNull final Object event
    ) {
        val listener = this.listener();
        if (listener != null) {
            this.invoker.invokeExact(listener, event);
        }
    }

    @Override
    public String toString() {
        return this.weak() ? this.method.description() + " (weak)" : this.method.description();
    }

}
