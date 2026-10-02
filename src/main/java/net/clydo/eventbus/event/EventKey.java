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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.clydo.clytil.Primitives;
import net.clydo.clytil.Validates;
import org.jetbrains.annotations.NotNull;

/**
 * A typed channel for events that are not identified by their class.
 *
 * <p>Use a key when the payload type alone is ambiguous, for example several channels that all
 * carry a {@code String}. Keys are compared by identity, so declare them once as constants:</p>
 * <pre>{@code
 * public static final EventKey<String> CHAT = EventKey.of("chat", String.class);
 *
 * bus.subscribe(CHAT, message -> ...);
 * bus.post(CHAT, "hello");
 * }</pre>
 *
 * <p>Keyed dispatch is exact: listeners subscribed to a key receive only what is posted to that
 * key, never class-based events, and vice versa.</p>
 *
 * @param <E> the payload type
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventKey<E> {

    /**
     * A human-readable name used in diagnostics.
     */
    private final @NotNull String name;

    /**
     * The payload type.
     */
    private final @NotNull Class<E> type;

    /**
     * Creates a new key. Two calls never return equal keys.
     *
     * @param name a human-readable name used in diagnostics
     * @param type the payload type
     * @param <E>  the payload type
     * @return a new key
     */
    public static <E> @NotNull EventKey<E> of(
            @NotNull final String name,
            @NotNull final Class<E> type
    ) {
        Validates.require(name, "name");
        Validates.require(type, "type");
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Event payloads are objects; use " + Primitives.wrap(type).getName() + " instead of " + type);
        }

        return new EventKey<>(name, type);
    }

    @Override
    public String toString() {
        return "EventKey[" + this.name + ": " + this.type.getName() + "]";
    }

}
