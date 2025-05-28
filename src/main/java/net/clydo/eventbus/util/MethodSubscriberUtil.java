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

package net.clydo.eventbus.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Primitives;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.clydo.eventbus.event.SubscribeEvent;
import net.clydo.eventbus.subscriber.EventSubscriber;
import net.clydo.eventbus.subscriber.impl.MethodSubscriber;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * The {@code MethodSubscriberUtil} class provides utility methods for creating
 * {@link MethodSubscriber} instances based on methods annotated with {@link SubscribeEvent}.
 */
@UtilityClass
public class MethodSubscriberUtil {

    /**
     * Creates a {@link Multimap} of event subscribers from the specified listener object.
     * It scans the listener's methods for those annotated with {@link SubscribeEvent} and
     * creates {@link MethodSubscriber} instances accordingly.
     *
     * <p>The created {@code Multimap} maps event types (classes) to their corresponding
     * {@code MethodSubscriber} instances.</p>
     *
     * @param listener the object containing methods to be analyzed and subscribed
     * @return a {@link Multimap} mapping event types to {@code MethodSubscriber} instances,
     * or {@code null} if the listener is {@code null}
     */
    public Multimap<Class<?>, EventSubscriber> createMethodSubscribers(Object listener) {
        if (listener == null) {
            return null;
        }

        val clazz = listener.getClass();
        val methodsInListener = HashMultimap.<Class<?>, EventSubscriber>create();

        for (val method : clazz.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                continue;
            }

            val annotation = method.getAnnotation(SubscribeEvent.class);
            if (annotation == null) {
                continue;
            }

            val parameterTypes = method.getParameterTypes();
            validateMethod(method, parameterTypes);

            val subscriber = new MethodSubscriber(annotation.priority(), method, listener);
            methodsInListener.put(parameterTypes[0], subscriber);
        }
        return methodsInListener;
    }

    /**
     * Validates the method annotated with {@link SubscribeEvent}. Ensures that the method
     * has exactly one parameter and that the parameter type is not a primitive type.
     *
     * @param method         the method to be validated
     * @param parameterTypes the parameter types of the method
     * @throws IllegalArgumentException if the method does not meet the required criteria
     */
    private void validateMethod(@NotNull Method method, Class<?> @NotNull [] parameterTypes) {
        Preconditions.checkArgument(parameterTypes.length == 1,
                "Subscriber method '%s' is incorrectly annotated with @SubscribeEvent. Expected 1 parameter, but found %d. "
                        + "Ensure the method has exactly one parameter representing the event type.",
                method.getName(), parameterTypes.length);

        val paramType = parameterTypes[0];
        Preconditions.checkArgument(!paramType.isPrimitive(),
                "Subscriber method '%s' cannot accept primitive types. Found primitive type '%s'. "
                        + "Consider changing the parameter type to '%s' (the wrapper class for the primitive).",
                method.getName(), paramType.getSimpleName(), Primitives.wrap(paramType).getSimpleName());
    }
}
