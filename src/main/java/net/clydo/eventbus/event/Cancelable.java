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

package net.clydo.eventbus.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code Cancelable} annotation indicates that an event can be canceled.
 * Classes annotated with {@code Cancelable} are treated as events that
 * allow cancellation by invoking methods such as {@code cancel()}.
 *
 * <p>This annotation should be applied to classes that are intended to
 * represent cancelable events, allowing the framework to determine
 * whether an event can be canceled.</p>
 *
 * <p>The retention policy is {@link RetentionPolicy#RUNTIME}, meaning that
 * the annotation is available at runtime for reflection, and it can only
 * be applied to types (classes or interfaces).</p>
 *
 * @see Event
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cancelable {

}
