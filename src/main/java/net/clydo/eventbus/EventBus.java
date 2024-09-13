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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EventBus.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2024 ClydoNetwork
 */

package net.clydo.eventbus;

import lombok.experimental.UtilityClass;
import lombok.val;
import net.clydo.eventbus.event.Event;
import net.clydo.eventbus.event.SubscribeEvent;
import net.clydo.eventbus.subscriber.EventSubscriber;
import net.clydo.eventbus.subscriber.impl.MethodSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The {@code EventBus} class is a utility class that provides an interface for
 * managing and dispatching events. It handles registration, unregistration, and
 * subscription of listeners and subscribers.
 *
 * <p>This class uses a {@link SubscriberRegistry} to maintain the list of subscribers
 * and handle event dispatching. It provides methods to register and unregister listeners,
 * subscribe and unsubscribe to specific events, and invoke subscribers with events.</p>
 */
@UtilityClass
public class EventBus {

    /**
     * The registry used for managing subscribers and event dispatching.
     */
    private final SubscriberRegistry REGISTRY = new SubscriberRegistry();

    /**
     * Registers a listener object with the event bus. The listener's methods annotated with
     * {@link SubscribeEvent} are used to create {@link MethodSubscriber} instances and
     * subscribe them to the relevant event types.
     *
     * @param listener the listener object to be registered
     */
    public void register(Object listener) {
        REGISTRY.register(listener);
    }

    /**
     * Unregisters a listener object from the event bus. The listener's methods annotated with
     * {@link SubscribeEvent} are used to remove {@link MethodSubscriber} instances
     * from the relevant event types.
     *
     * @param listener the listener object to be unregistered
     */
    public void unregister(Object listener) {
        REGISTRY.unregister(listener);
    }

    /**
     * Registers multiple listener objects with the event bus.
     *
     * @param listeners the listener objects to be registered
     */
    public void registerAll(Object... listeners) {
        if (listeners != null) {
            for (Object listener : listeners) {
                REGISTRY.register(listener);
            }
        }
    }

    /**
     * Unregisters multiple listener objects from the event bus.
     *
     * @param listeners the listener objects to be unregistered
     */
    public void unregisterAll(Object... listeners) {
        if (listeners != null) {
            for (Object listener : listeners) {
                REGISTRY.unregister(listener);
            }
        }
    }

    /**
     * Subscribes a {@link EventSubscriber} instance to a specific event type.
     *
     * @param eventType  the type of event to subscribe to
     * @param subscriber the subscriber to be added
     * @param <E>        the type of event
     */
    public <E> void subscribe(Class<E> eventType, EventSubscriber subscriber) {
        REGISTRY.subscribe(eventType, subscriber);
    }

    /**
     * Unsubscribes a {@link EventSubscriber} instance from a specific event type.
     *
     * @param eventType  the type of event to unsubscribe from
     * @param subscriber the subscriber to be removed
     * @param <E>        the type of event
     */
    public <E> void unsubscribe(Class<E> eventType, EventSubscriber subscriber) {
        REGISTRY.unsubscribe(eventType, subscriber);
    }

    /**
     * Subscribes multiple {@link EventSubscriber} instances to a specific event type.
     *
     * @param eventType   the type of event to subscribe to
     * @param subscribers the subscribers to be added
     * @param <E>         the type of event
     */
    public <E> void subscribeAll(Class<E> eventType, EventSubscriber... subscribers) {
        REGISTRY.subscribeAll(eventType, List.of(subscribers));
    }

    /**
     * Unsubscribes multiple {@link EventSubscriber} instances from a specific event type.
     *
     * @param eventType   the type of event to unsubscribe from
     * @param subscribers the subscribers to be removed
     * @param <E>         the type of event
     */
    public <E> void unsubscribeAll(Class<E> eventType, EventSubscriber... subscribers) {
        REGISTRY.unsubscribeAll(eventType, List.of(subscribers));
    }

    /**
     * Dispatches an event to all subscribers registered for the event's class.
     * Calls the {@link EventSubscriber#call(Object)} method on each subscriber
     * and checks if the event was cancelled.
     *
     * @param rawEvent the event to be dispatched
     * @return {@code true} if the event was cancelled, {@code false} otherwise
     */
    public boolean call(@NotNull Object rawEvent) {
        val iterator = REGISTRY.getSubscribers(rawEvent.getClass());
        if (iterator == null) {
            return false;
        }

        while (iterator.hasNext()) {
            val subscriber = iterator.next();
            if (subscriber == null) {
                continue;
            }

            subscriber.call(rawEvent);
        }

        return (rawEvent instanceof Event event && event.cancelled());
    }

    /**
     * Checks if there are any subscribers registered for a specific event class.
     *
     * @param clazz the class of the event to check
     * @param <E>   the type of event
     * @return {@code true} if there are subscribers registered, {@code false} otherwise
     */
    public <E> boolean hasSub(@NotNull Class<E> clazz) {
        return REGISTRY.hasSubscriber(clazz);
    }
}
