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

package net.clydo.eventbus.subscriber.impl;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.val;
import net.clydo.eventbus.exception.InvokeEventException;
import net.clydo.eventbus.subscriber.EventSubscriber;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The {@code MethodSubscriber} class is an implementation of {@link EventSubscriber}
 * that uses reflection to invoke a method on a specified listener object when an event occurs.
 * It provides functionality to handle events by calling methods dynamically.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class MethodSubscriber extends EventSubscriber {

    /**
     * Logger for logging errors during method invocation.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodSubscriber.class);

    /**
     * The object on which the method will be invoked.
     */
    private final Object listener;

    /**
     * The method to be invoked on the listener object.
     */
    private final Method method;

    /**
     * Constructs a {@code MethodSubscriber} with the specified priority, method, and listener.
     *
     * @param priority the priority level of this subscriber
     * @param method   the method to be invoked when an event occurs
     * @param listener the object on which the method will be invoked
     * @throws NullPointerException if {@code method} or {@code listener} is {@code null}
     */
    public MethodSubscriber(int priority, @NotNull Method method, @NotNull Object listener) {
        super(priority);
        this.listener = Preconditions.checkNotNull(listener, "Listener must not be null");
        this.method = method;
        this.method.setAccessible(true);
    }

    /**
     * Invokes the configured method on the listener object with the given event.
     * Handles exceptions that may occur during method invocation and logs errors.
     *
     * @param event the event to be passed to the method
     */
    @Override
    public void call(Object event) {
        try {
            try {
                this.method.invoke(this.listener, Preconditions.checkNotNull(event, "Event must not be null"));
            } catch (IllegalArgumentException e) {
                throw new InvokeEventException("Invalid argument passed to method: " + method, e);
            } catch (IllegalAccessException e) {
                throw new InvokeEventException("Method access failure: Unable to access method '" + method + "'", e);
            } catch (InvocationTargetException e) {
                val cause = e.getCause();
                if (cause instanceof Error error) {
                    throw new InvokeEventException("Error occurred during method invocation: " + method, error);
                }
                throw new InvokeEventException("Exception thrown by method: " + method, cause);
            }
        } catch (InvokeEventException e) {
            LOGGER.error("Failed to invoke method: {}", method, e);
        }
    }
}