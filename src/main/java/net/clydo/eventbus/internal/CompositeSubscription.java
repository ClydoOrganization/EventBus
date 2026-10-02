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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.clydo.clytil.Validates;
import net.clydo.eventbus.subscriber.Subscription;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Several subscriptions handled as one, created by {@link Subscription#all(Subscription...)},
 * {@link Subscription#and(Subscription)} and registrations of listener objects with several
 * {@link net.clydo.eventbus.subscriber.Subscribe} methods.
 *
 * <p>Composites are flat: combining composites copies their parts instead of nesting them, so
 * chaining {@code and} in a loop never builds a deep structure.</p>
 *
 * <p>Not part of the public API; it may change without notice.</p>
 */
@ApiStatus.Internal
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompositeSubscription implements Subscription {

    /**
     * The subscription with no parts, returned by {@link Subscription#empty()}.
     */
    public static final Subscription EMPTY = new CompositeSubscription(new Subscription[0]);

    private final @NotNull Subscription @NotNull [] subscriptions;

    /**
     * Combines subscriptions, flattening nested composites and dropping empty ones.
     *
     * @param subscriptions the subscriptions
     * @return {@link #EMPTY}, the only remaining subscription, or a flat composite
     */
    public static @NotNull Subscription of(
            @NotNull final Collection<? extends Subscription> subscriptions
    ) {
        // Object overload on purpose: Validates' Collection overload also rejects empty collections, which are valid here
        Validates.require((Object) subscriptions, "subscriptions");

        val parts = new ArrayList<Subscription>(subscriptions.size());
        for (val subscription : subscriptions) {
            Validates.require(subscription, "subscription");

            if (subscription instanceof CompositeSubscription composite) {
                parts.addAll(Arrays.asList(composite.subscriptions));
            } else {
                parts.add(subscription);
            }
        }

        return switch (parts.size()) {
            case 0 -> EMPTY;
            case 1 -> parts.get(0);
            default -> new CompositeSubscription(parts.toArray(Subscription[]::new));
        };
    }

    @Override
    public boolean isActive() {
        for (val subscription : this.subscriptions) {
            if (subscription.isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void unsubscribe() {
        for (val subscription : this.subscriptions) {
            subscription.unsubscribe();
        }
    }

    @Override
    public boolean closesNothing() {
        return this.subscriptions.length == 0;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.subscriptions);
    }

}
