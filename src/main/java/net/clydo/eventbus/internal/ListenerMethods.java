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

import lombok.experimental.UtilityClass;
import lombok.val;
import net.clydo.clytil.Primitives;
import net.clydo.eventbus.subscriber.Subscribe;
import net.clydo.eventbus.subscriber.SubscriptionOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates {@link MethodListener}s from the {@link Subscribe} methods of listener objects.
 *
 * <p>Reflection happens only here, once per listener class: each annotated method is validated
 * and turned into a {@link MethodHandle} adapted to {@code (Object, Object)void}, so every later
 * invocation is a plain {@code invokeExact}.</p>
 */
@UtilityClass
class ListenerMethods {

    private final MethodType INVOKER_TYPE = MethodType.methodType(void.class, Object.class, Object.class);

    /**
     * Cached per listener class. A {@link ClassValue} lives with the class itself, so it never
     * keeps an unloaded listener class alive.
     */
    private final ClassValue<List<ListenerMethod>> METHODS = new ClassValue<>() {
        @Override
        protected @NotNull @Unmodifiable List<ListenerMethod> computeValue(
                @NotNull final Class<?> type
        ) {
            return scan(type);
        }
    };

    /**
     * Creates one {@link MethodListener} per {@link Subscribe} method of a listener object,
     * including inherited methods.
     *
     * @param listener  the listener object
     * @param weakQueue where collected listeners are reported, or {@code null} to hold the listener strongly
     * @return the method listeners, in a stable order
     * @throws IllegalArgumentException if the listener has no {@link Subscribe} methods, or one is malformed
     */
    @NotNull List<MethodListener> createMethodListeners(
            @NotNull final Object listener,
            @Nullable final ReferenceQueue<Object> weakQueue
    ) {
        val methods = METHODS.get(listener.getClass());
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(listener.getClass().getName() + " has no @Subscribe methods");
        }

        val listeners = new ArrayList<MethodListener>(methods.size());
        for (val method : methods) {
            listeners.add(new MethodListener(method, listener, weakQueue));
        }
        return listeners;
    }

    private @NotNull @Unmodifiable List<ListenerMethod> scan(
            @NotNull final Class<?> type
    ) {
        val methods = new ArrayList<ListenerMethod>();
        val overridable = new HashSet<String>();

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            val declared = current.getDeclaredMethods();
            // getDeclaredMethods() has no specified order; sort so equal priorities dispatch identically on every run
            Arrays.sort(declared, Comparator.comparing(Method::getName).thenComparing(Method::toGenericString));

            for (val method : declared) {
                if (method.isSynthetic()) {
                    continue;
                }

                val annotation = method.getAnnotation(Subscribe.class);
                if (annotation == null) {
                    continue;
                }

                // An annotated override is registered at the subclass; its super method must not be registered again
                val isPrivate = Modifier.isPrivate(method.getModifiers());
                if (!isPrivate && !overridable.add(method.getName() + Arrays.toString(method.getParameterTypes()))) {
                    continue;
                }

                methods.add(createListenerMethod(method, annotation));
            }
        }
        return List.copyOf(methods);
    }

    private @NotNull ListenerMethod createListenerMethod(
            @NotNull final Method method,
            @NotNull final Subscribe annotation
    ) {
        val description = describe(method);
        validateMethod(method, description);

        final MethodHandle invoker;
        try {
            method.trySetAccessible();
            invoker = MethodHandles.lookup().unreflect(method).asType(INVOKER_TYPE);
        } catch (final IllegalAccessException exception) {
            throw new IllegalArgumentException("@Subscribe method " + description + " is not accessible; open its package to the event bus", exception);
        }

        val options = new SubscriptionOptions(
                annotation.priority(),
                annotation.mode(),
                SubscriptionOptions.UNLIMITED,
                annotation.ignoreCancelled()
        );
        return new ListenerMethod(method.getParameterTypes()[0], invoker, options, description);
    }

    /**
     * Ensures a {@link Subscribe} method is an instance method returning {@code void} with exactly
     * one non-primitive parameter.
     *
     * @param method      the method to validate
     * @param description the method's description, for error messages
     * @throws IllegalArgumentException if the method does not meet these criteria
     */
    private void validateMethod(
            @NotNull final Method method,
            @NotNull final String description
    ) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw invalid(description, "must not be static");
        }
        if (method.getParameterCount() != 1) {
            throw invalid(description, "must declare exactly one parameter, the event, but declares " + method.getParameterCount());
        }
        if (method.getReturnType() != void.class) {
            throw invalid(description, "must return void");
        }

        val eventType = method.getParameterTypes()[0];
        if (eventType.isPrimitive()) {
            throw invalid(description, "cannot take a primitive event; use " + Primitives.wrap(eventType).getSimpleName());
        }
    }

    private @NotNull String describe(
            @NotNull final Method method
    ) {
        return method.getDeclaringClass().getName()
                + "#" + method.getName()
                + "(" + Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ")) + ")";
    }

    private @NotNull IllegalArgumentException invalid(
            @NotNull final String description,
            @NotNull final String problem
    ) {
        return new IllegalArgumentException("@Subscribe method " + description + " " + problem);
    }

}
