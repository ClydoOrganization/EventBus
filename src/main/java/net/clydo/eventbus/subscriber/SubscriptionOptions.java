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

import lombok.With;
import net.clydo.clytil.Validates;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable settings for a single subscription.
 *
 * <p>Start from {@link #defaults()} or {@link #once()} and adjust with the {@code with*}
 * methods:</p>
 * <pre>{@code
 * bus.subscribe(ChatEvent.class,
 *         SubscriptionOptions.defaults().withPriority(EventPriority.HIGH).withIgnoreCancelled(true),
 *         event -> ...);
 * }</pre>
 *
 * @param priority        listeners with a higher priority run first, see {@link EventPriority}
 * @param mode            whether the listener runs on the dispatching thread or on the bus executor
 * @param maxInvocations  how many times the listener runs before it unsubscribes itself, or
 *                        {@link #UNLIMITED}; events rejected by a filter do not count
 * @param ignoreCancelled whether to skip events that are already canceled when this listener's turn comes
 */
@With
public record SubscriptionOptions(
        int priority,
        @NotNull DispatchMode mode,
        int maxInvocations,
        boolean ignoreCancelled
) {

    /**
     * The {@link #maxInvocations()} value for subscriptions that never expire.
     */
    public static final int UNLIMITED = 0;

    private static final SubscriptionOptions DEFAULTS = new SubscriptionOptions(
            EventPriority.NORMAL,
            DispatchMode.SYNC,
            UNLIMITED,
            false
    );

    private static final SubscriptionOptions ONCE = DEFAULTS.withMaxInvocations(1);

    /**
     * Creates validated options.
     *
     * @param priority        listeners with a higher priority run first
     * @param mode            whether the listener runs on the dispatching thread or on the bus executor
     * @param maxInvocations  invocations before the subscription ends, or {@link #UNLIMITED}
     * @param ignoreCancelled whether to skip events that are already canceled
     * @throws NullPointerException     if {@code mode} is {@code null}
     * @throws IllegalArgumentException if {@code maxInvocations} is negative
     */
    public SubscriptionOptions {
        Validates.require(mode, "mode");
        Validates.requireNonNegative(maxInvocations, "maxInvocations");
    }

    /**
     * Normal priority, synchronous, unlimited, receives canceled events.
     *
     * @return the default options
     */
    public static @NotNull SubscriptionOptions defaults() {
        return DEFAULTS;
    }

    /**
     * The {@linkplain #defaults() defaults}, limited to a single invocation.
     *
     * @return options for a one-shot subscription
     */
    public static @NotNull SubscriptionOptions once() {
        return ONCE;
    }

}
