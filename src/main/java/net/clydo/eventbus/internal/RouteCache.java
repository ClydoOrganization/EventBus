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

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.function.Predicate;

/**
 * Maps posted event classes to their {@link Route}s, holding the classes only weakly.
 *
 * <p>A bus may live for the whole application while the event classes posted to it come from
 * plugins or other unloadable class loaders. Keying a regular map by {@link Class} would pin those
 * loaders forever, even for classes nobody listens to, so this table keeps its keys in
 * {@link WeakReference}s and drops cleared entries whenever it is rebuilt.</p>
 *
 * <p>Reads are lock-free and allocation-free: one volatile read, then linear probing over an
 * immutable open-addressing table. Writes copy the table and must be serialized by the caller.</p>
 */
final class RouteCache {

    private static final int INITIAL_CAPACITY = 16;

    private volatile Entry @NotNull [] table = new Entry[INITIAL_CAPACITY];

    /**
     * Number of entries in {@link #table}, including cleared ones. Guarded by the caller's lock.
     */
    private int size;

    /**
     * Returns the cached route of an event class.
     *
     * @param type the event class
     * @return the route, or {@code null} if it is not cached
     */
    @Nullable Route get(
            @NotNull final Class<?> type
    ) {
        val table = this.table;
        val mask = table.length - 1;
        int index = indexOf(type, mask);

        Entry entry;
        while ((entry = table[index]) != null) {
            if (entry.get() == type) {
                return entry.route;
            }
            index = (index + 1) & mask;
        }
        return null;
    }

    /**
     * Caches the route of an event class that is not cached yet. Requires the caller's lock.
     *
     * @param type  the event class
     * @param route its route
     */
    void put(
            @NotNull final Class<?> type,
            @NotNull final Route route
    ) {
        // Keep the load factor at or below one half so probe sequences stay short
        if ((this.size + 1) * 2 > this.table.length) {
            this.rebuild(null, this.size + 1);
        }

        val table = this.table.clone();
        insert(table, new Entry(type, route));
        this.table = table;
        this.size++;
    }

    /**
     * Drops the routes of every event class that matches a condition, along with the routes of
     * collected classes. Requires the caller's lock.
     *
     * @param condition selects the event classes to drop
     */
    void removeIf(
            @NotNull final Predicate<Class<?>> condition
    ) {
        for (val entry : this.table) {
            if (entry == null) {
                continue;
            }

            val type = entry.get();
            if (type == null || condition.test(type)) {
                this.rebuild(condition, this.size);
                return;
            }
        }
    }

    /**
     * Drops every route. Requires the caller's lock.
     */
    void clear() {
        this.table = new Entry[INITIAL_CAPACITY];
        this.size = 0;
    }

    /**
     * Copies the live entries that do not match {@code condition} into a table sized for
     * {@code expected} entries.
     */
    private void rebuild(
            @Nullable final Predicate<Class<?>> condition,
            final int expected
    ) {
        int capacity = INITIAL_CAPACITY;
        while (capacity < expected * 2) {
            capacity <<= 1;
        }

        val table = new Entry[capacity];
        int size = 0;
        for (val entry : this.table) {
            if (entry == null) {
                continue;
            }

            val type = entry.get();
            if (type == null || (condition != null && condition.test(type))) {
                continue;
            }

            insert(table, entry);
            size++;
        }

        this.table = table;
        this.size = size;
    }

    private static void insert(
            final Entry @NotNull [] table,
            @NotNull final Entry entry
    ) {
        val mask = table.length - 1;
        int index = entry.hash & mask;
        while (table[index] != null) {
            index = (index + 1) & mask;
        }
        table[index] = entry;
    }

    private static int indexOf(
            @NotNull final Class<?> type,
            final int mask
    ) {
        return spread(System.identityHashCode(type)) & mask;
    }

    private static int spread(
            final int hash
    ) {
        return hash ^ (hash >>> 16);
    }

    /**
     * A weakly keyed table entry. The route never references its key class, so a cached route
     * cannot keep the class alive.
     */
    private static final class Entry extends WeakReference<Class<?>> {

        private final int hash;

        private final @NotNull Route route;

        private Entry(
                @NotNull final Class<?> type,
                @NotNull final Route route
        ) {
            super(type);
            this.hash = spread(System.identityHashCode(type));
            this.route = route;
        }

    }

}
