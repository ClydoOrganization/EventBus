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

package net.clydo.eventbus;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import lombok.val;
import net.clydo.eventbus.event.SubscribeEvent;
import net.clydo.eventbus.subscriber.EventSubscriber;
import net.clydo.eventbus.subscriber.impl.MethodSubscriber;
import net.clydo.eventbus.util.MethodSubscriberUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The {@code SubscriberRegistry} class manages event subscribers, allowing
 * for the registration, unregistration, and management of listeners that
 * handle specific types of events.
 *
 * <p>Subscribers are organized in a concurrent map where the key is the
 * event type and the value is a list of {@link EventSubscriber} instances
 * sorted by priority.</p>
 */
public class SubscriberRegistry {

    /**
     * Logger for logging errors and information related to subscriber management.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberRegistry.class);

    /**
     * A map of event types to lists of subscribers.
     */
    private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<EventSubscriber>> subscribersMap = Maps.newConcurrentMap();

    /**
     * A list of registered classes to prevent duplicate registrations.
     */
    private final List<Class<?>> registeredClasses = new ArrayList<>();

    /**
     * Registers the provided listener object. The listener's methods annotated with
     * {@link SubscribeEvent} are used to create {@link MethodSubscriber} instances
     * which are then subscribed to the relevant event types.
     *
     * @param listener the listener object to be registered
     */
    public void register(Object listener) {
        Objects.requireNonNull(listener, "You must provide a non-null listener");

        val clazz = listener.getClass();
        if (this.registeredClasses.contains(clazz)) {
            LOGGER.debug("You are already registered to {}", clazz);
            return;
        }
        this.registeredClasses.add(clazz);

        val methodSubscribers = MethodSubscriberUtil.createMethodSubscribers(listener);
        if (methodSubscribers != null) {
            for (val entry : methodSubscribers.asMap().entrySet()) {
                this.subscribeAll(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Unregisters the provided listener object. The listener's methods annotated with
     * {@link SubscribeEvent} are used to remove {@link MethodSubscriber} instances
     * from the relevant event types.
     *
     * @param listener the listener object to be unregistered
     */
    public void unregister(Object listener) {
        Objects.requireNonNull(listener, "You must provide a non-null listener");

        val clazz = listener.getClass();
        this.registeredClasses.remove(clazz);

        val methodSubscribers = MethodSubscriberUtil.createMethodSubscribers(listener);
        if (methodSubscribers != null) {
            for (val entry : methodSubscribers.asMap().entrySet()) {
                this.unsubscribeAll(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Subscribes a collection of {@link EventSubscriber} instances to the specified event type.
     *
     * @param eventType        the type of event to subscribe to
     * @param subscribersToAdd the subscribers to be added
     */
    public <E> void subscribeAll(Class<E> eventType, Collection<EventSubscriber> subscribersToAdd) {
        var subscribers = this.subscribersMap.get(eventType);

        if (subscribers == null) {
            val newSet = new CopyOnWriteArrayList<EventSubscriber>();
            subscribers = MoreObjects.firstNonNull(this.subscribersMap.putIfAbsent(eventType, newSet), newSet);
        }

        subscribers.addAll(subscribersToAdd);
        subscribers.sort(Comparator.comparingInt(EventSubscriber::priority));
    }

    /**
     * Unsubscribes a collection of {@link EventSubscriber} instances from the specified event type.
     *
     * @param eventType           the type of event to unsubscribe from
     * @param subscribersToRemove the subscribers to be removed
     */
    public void unsubscribeAll(Class<?> eventType, Collection<EventSubscriber> subscribersToRemove) {
        val subscribers = this.subscribersMap.get(eventType);

        if (subscribers == null || !subscribers.removeAll(subscribersToRemove)) {
            // Missing event subscriber for an annotated method. Is listener registered?
            return;
        }

        if (subscribers.isEmpty()) {
            this.subscribersMap.remove(eventType);
        }
    }

    /**
     * Subscribes a single {@link EventSubscriber} instance to the specified event type.
     *
     * @param eventType  the type of event to subscribe to
     * @param subscriber the subscriber to be added
     */
    public <E> void subscribe(Class<E> eventType, EventSubscriber subscriber) {
        var subscribers = this.subscribersMap.get(eventType);
        if (subscribers == null) {
            val newSet = new CopyOnWriteArrayList<EventSubscriber>();
            subscribers = MoreObjects.firstNonNull(this.subscribersMap.putIfAbsent(eventType, newSet), newSet);
        }

        subscribers.add(subscriber);
        subscribers.sort(Comparator.comparingInt(EventSubscriber::priority));
    }

    /**
     * Unsubscribes a single {@link EventSubscriber} instance from the specified event type.
     *
     * @param eventType  the type of event to unsubscribe from
     * @param subscriber the subscriber to be removed
     */
    public void unsubscribe(Class<?> eventType, EventSubscriber subscriber) {
        val subscribers = this.subscribersMap.get(eventType);

        if (subscribers == null || !subscribers.remove(subscriber)) {
            // Missing event subscriber for an annotated method. Is listener registered?
            return;
        }

        if (subscribers.isEmpty()) {
            this.subscribersMap.remove(eventType);
        }
    }

    /**
     * Retrieves an iterator over the subscribers for a specified event class.
     *
     * @param eventClass the class of the event to get subscribers for
     * @return an iterator over the event subscribers, or {@code null} if no subscribers are registered
     */
    public <E> Iterator<EventSubscriber> getSubscribers(@NotNull Class<E> eventClass) {
        val subscribers = this.subscribersMap.get(eventClass);
        return subscribers == null || subscribers.isEmpty() ? null : subscribers.iterator();
    }

    /**
     * Checks if there are any subscribers registered for a specified event class.
     *
     * @param eventClass the class of the event to check
     * @return {@code true} if there are subscribers registered, {@code false} otherwise
     */
    public <E> boolean hasSubscriber(@NotNull Class<E> eventClass) {
        val subscribers = this.subscribersMap.get(eventClass);
        return subscribers != null && !subscribers.isEmpty();
    }
}
