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

import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;
import net.clydo.eventbus.event.Cancellable;
import net.clydo.eventbus.event.Event;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.ReferenceQueue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Stores the subscribers of one {@link EventDispatcher} and resolves who receives a posted event.
 *
 * <p>Each topic ({@link Class} or {@link net.clydo.eventbus.event.EventKey}) maps to an immutable
 * array of subscribers in dispatch order. Writers replace arrays under a lock; readers never lock.
 * For class-based posts, the merged array of the event class's whole hierarchy is cached as a
 * {@link Route}, so steady-state dispatch costs one lookup.</p>
 *
 * <p>Nothing here pins classes or listeners beyond their subscriptions: routes are cached by weak
 * class keys, and weakly registered listeners are purged as soon as they are collected.</p>
 */
@CustomLog
@Accessors(fluent = true)
final class SubscriberRegistry {

    /**
     * {@link Route#traits()} bit for events extending {@link Event}.
     */
    static final int STOPPABLE = 1;

    /**
     * {@link Route#traits()} bit for events implementing {@link Cancellable}.
     */
    static final int CANCELLABLE = 2;

    /**
     * Dispatch order: higher priority first, then subscription order.
     */
    private static final Comparator<EventSubscriber> ORDER = (first, second) -> first.priority != second.priority
            ? Integer.compare(second.priority, first.priority)
            : Long.compare(first.sequence, second.sequence);

    private static final EventSubscriber[] NO_SUBSCRIBERS = new EventSubscriber[0];

    /**
     * Every class and interface an event class is assignable to, including itself. The values only
     * reference classes the key class already references, so this cache never pins a class.
     */
    private static final ClassValue<Class<?>[]> HIERARCHIES = new ClassValue<>() {
        @Override
        protected Class<?>[] computeValue(
                @NotNull final Class<?> type
        ) {
            return flattenHierarchy(type);
        }
    };

    /**
     * {@link #STOPPABLE} and {@link #CANCELLABLE} bits per class. Resolving them once matters: on
     * JDK 17 a failing {@code instanceof SomeInterface} costs tens of nanoseconds.
     */
    private static final ClassValue<Integer> TRAITS = new ClassValue<>() {
        @Override
        protected Integer computeValue(
                @NotNull final Class<?> type
        ) {
            return (Event.class.isAssignableFrom(type) ? STOPPABLE : 0)
                    | (Cancellable.class.isAssignableFrom(type) ? CANCELLABLE : 0);
        }
    };

    private final int warningThreshold;

    private final Object lock = new Object();

    private final AtomicLong sequence = new AtomicLong();

    /**
     * Topic to its own subscribers, in dispatch order. Arrays are never mutated.
     */
    private final ConcurrentHashMap<Object, EventSubscriber[]> topics = new ConcurrentHashMap<>();

    /**
     * Posted class to the merged subscribers of its whole hierarchy. Written under {@link #lock}.
     */
    private final RouteCache routes = new RouteCache();

    /**
     * Receives the weak references of weakly registered listeners once they are collected.
     */
    @Getter
    private final ReferenceQueue<Object> collectedListeners = new ReferenceQueue<>();

    /**
     * Topics already reported as possible leaks. Guarded by {@link #lock}.
     */
    private final Set<Object> warnedTopics = new HashSet<>();

    /**
     * Creates an empty registry.
     *
     * @param warningThreshold logs a warning once a topic has more subscribers than this; {@code 0} disables it
     */
    SubscriberRegistry(
            final int warningThreshold
    ) {
        this.warningThreshold = warningThreshold;
    }

    /**
     * Returns the {@link Route#traits()} of an event class.
     *
     * @param type the event class
     * @return the {@link #STOPPABLE} and {@link #CANCELLABLE} bits
     */
    static int traitsOf(
            @NotNull final Class<?> type
    ) {
        return TRAITS.get(type);
    }

    /**
     * Returns the next subscription sequence number, which breaks priority ties.
     *
     * @return a number greater than every previous one
     */
    long nextSequence() {
        return this.sequence.getAndIncrement();
    }

    /**
     * Returns the subscribers an event of a class is delivered to, merged across its hierarchy.
     *
     * @param type the event class
     * @return the route
     */
    @NotNull Route route(
            @NotNull final Class<?> type
    ) {
        val cached = this.routes.get(type);
        return cached != null ? cached : this.resolve(type);
    }

    /**
     * Returns the subscribers of exactly one topic.
     *
     * @param topic the topic
     * @return the subscribers, in dispatch order
     */
    @NotNull EventSubscriber @NotNull [] subscribersOf(
            @NotNull final Object topic
    ) {
        val subscribers = this.topics.get(topic);
        return subscribers == null ? NO_SUBSCRIBERS : subscribers;
    }

    /**
     * Adds subscribers, all under one lock acquisition.
     *
     * @param subscribers the subscribers
     */
    void add(
            @NotNull final EventSubscriber @NotNull ... subscribers
    ) {
        synchronized (this.lock) {
            this.purgeCollected();
            for (val subscriber : subscribers) {
                this.insert(subscriber);
            }
        }
    }

    /**
     * Deactivates a subscriber and removes it from its topic. Does nothing if it was already removed.
     *
     * @param subscriber the subscriber
     */
    void remove(
            @NotNull final EventSubscriber subscriber
    ) {
        synchronized (this.lock) {
            this.purgeCollected();
            this.delete(subscriber);
        }
    }

    /**
     * Removes every subscriber of a topic that matches a condition.
     *
     * @param topic     the topic
     * @param condition selects the subscribers to remove
     * @return whether anything was removed
     */
    boolean removeIf(
            @NotNull final Object topic,
            @NotNull final Predicate<EventSubscriber> condition
    ) {
        synchronized (this.lock) {
            this.purgeCollected();
            return this.deleteAll(this.matching(Arrays.asList(this.subscribersOf(topic)), condition));
        }
    }

    /**
     * Removes every subscriber, of any topic, that matches a condition.
     *
     * @param condition selects the subscribers to remove
     * @return whether anything was removed
     */
    boolean removeIf(
            @NotNull final Predicate<EventSubscriber> condition
    ) {
        synchronized (this.lock) {
            this.purgeCollected();
            return this.deleteAll(this.matching(this.all(), condition));
        }
    }

    private @NotNull Route resolve(
            @NotNull final Class<?> type
    ) {
        synchronized (this.lock) {
            val cached = this.routes.get(type);
            if (cached != null) {
                return cached;
            }

            val route = new Route(this.collect(type), traitsOf(type));
            this.routes.put(type, route);
            return route;
        }
    }

    /**
     * Removes the subscribers of weakly registered listeners that were collected, so their classes
     * can unload even if their events are never posted again. Requires {@link #lock}.
     */
    private void purgeCollected() {
        if (this.collectedListeners.poll() == null) {
            return;
        }
        while (this.collectedListeners.poll() != null) {
            // drain: one scan below handles every collected listener
        }

        this.deleteAll(this.matching(this.all(), subscriber -> subscriber.weak && subscriber.owner() == null));
    }

    private @NotNull List<EventSubscriber> all() {
        val all = new ArrayList<EventSubscriber>();
        for (val subscribers : this.topics.values()) {
            Collections.addAll(all, subscribers);
        }
        return all;
    }

    private @NotNull List<EventSubscriber> matching(
            @NotNull final List<EventSubscriber> subscribers,
            @NotNull final Predicate<EventSubscriber> condition
    ) {
        val matches = new ArrayList<EventSubscriber>();
        for (val subscriber : subscribers) {
            if (condition.test(subscriber)) {
                matches.add(subscriber);
            }
        }
        return matches;
    }

    /**
     * Requires {@link #lock}.
     */
    private boolean deleteAll(
            @NotNull final List<EventSubscriber> subscribers
    ) {
        for (val subscriber : subscribers) {
            this.delete(subscriber);
        }
        return !subscribers.isEmpty();
    }

    /**
     * Requires {@link #lock}.
     */
    private void delete(
            @NotNull final EventSubscriber subscriber
    ) {
        if (!subscriber.active) {
            return;
        }
        subscriber.active = false;

        val topic = subscriber.topic;
        val current = this.topics.get(topic);
        val index = current == null ? -1 : indexOf(current, subscriber);
        if (index < 0) {
            return;
        }

        if (current.length == 1) {
            this.topics.remove(topic);
            this.warnedTopics.remove(topic);
        } else {
            val updated = new EventSubscriber[current.length - 1];
            System.arraycopy(current, 0, updated, 0, index);
            System.arraycopy(current, index + 1, updated, index, updated.length - index);
            this.topics.put(topic, updated);
        }
        this.invalidate(topic);
    }

    /**
     * Requires {@link #lock}.
     */
    private void insert(
            @NotNull final EventSubscriber subscriber
    ) {
        val current = this.subscribersOf(subscriber.topic);
        // Sequences are unique, so the search never finds an equal element and always yields the insertion point
        val index = -Arrays.binarySearch(current, subscriber, ORDER) - 1;

        val updated = new EventSubscriber[current.length + 1];
        System.arraycopy(current, 0, updated, 0, index);
        updated[index] = subscriber;
        System.arraycopy(current, index, updated, index + 1, current.length - index);

        this.topics.put(subscriber.topic, updated);
        this.invalidate(subscriber.topic);
        this.warnIfLeaking(subscriber.topic, updated.length);
    }

    /**
     * Drops cached routes that include a topic. Requires {@link #lock}.
     */
    private void invalidate(
            @NotNull final Object topic
    ) {
        if (topic == Object.class) {
            this.routes.clear();
        } else if (topic instanceof Class<?> type) {
            this.routes.removeIf(type::isAssignableFrom);
        }
    }

    private void warnIfLeaking(
            @NotNull final Object topic,
            final int count
    ) {
        if (this.warningThreshold == 0 || count <= this.warningThreshold || !this.warnedTopics.add(topic)) {
            return;
        }

        LOGGER.log(
                System.Logger.Level.WARNING,
                "{0} has {1} subscriptions, more than the warning threshold of {2}; listeners may be leaking",
                topic instanceof Class<?> type ? type.getName() : topic,
                count,
                this.warningThreshold
        );
    }

    private @NotNull EventSubscriber @NotNull [] collect(
            @NotNull final Class<?> type
    ) {
        EventSubscriber[] merged = null;
        for (val superType : HIERARCHIES.get(type)) {
            val subscribers = this.topics.get(superType);
            if (subscribers != null) {
                merged = merged == null ? subscribers : merge(merged, subscribers);
            }
        }
        return merged == null ? NO_SUBSCRIBERS : merged;
    }

    private static int indexOf(
            @NotNull final EventSubscriber @NotNull [] subscribers,
            @NotNull final EventSubscriber subscriber
    ) {
        for (int i = 0; i < subscribers.length; i++) {
            if (subscribers[i] == subscriber) {
                return i;
            }
        }
        return -1;
    }

    private static @NotNull EventSubscriber @NotNull [] merge(
            @NotNull final EventSubscriber @NotNull [] first,
            @NotNull final EventSubscriber @NotNull [] second
    ) {
        val merged = new EventSubscriber[first.length + second.length];
        int i = 0;
        int j = 0;
        int k = 0;
        while (i < first.length && j < second.length) {
            merged[k++] = ORDER.compare(first[i], second[j]) <= 0 ? first[i++] : second[j++];
        }
        System.arraycopy(first, i, merged, k, first.length - i);
        System.arraycopy(second, j, merged, k + first.length - i, second.length - j);
        return merged;
    }

    private static @NotNull Class<?> @NotNull [] flattenHierarchy(
            @NotNull final Class<?> type
    ) {
        val types = new LinkedHashSet<Class<?>>();
        val pending = new ArrayDeque<Class<?>>();
        pending.add(type);

        while (!pending.isEmpty()) {
            val current = pending.poll();
            if (!types.add(current)) {
                continue;
            }

            val superclass = current.getSuperclass();
            if (superclass != null) {
                pending.add(superclass);
            }
            Collections.addAll(pending, current.getInterfaces());
        }
        return types.toArray(Class<?>[]::new);
    }

}
