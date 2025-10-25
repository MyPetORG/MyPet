/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util.sentry.marshaller.gson.bindings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import io.sentry.event.interfaces.StackTraceInterface;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;

public class ExceptionInterfaceBinding implements InterfaceBinding<ExceptionInterface> {

    private static final String TYPE_PARAMETER = "type";
    private static final String VALUE_PARAMETER = "value";
    private static final String MODULE_PARAMETER = "module";
    private static final String STACKTRACE_PARAMETER = "stacktrace";
    private final InterfaceBinding<StackTraceInterface> stackTraceInterfaceBinding;

    public ExceptionInterfaceBinding(InterfaceBinding<StackTraceInterface> stackTraceInterfaceBinding) {
        this.stackTraceInterfaceBinding = stackTraceInterfaceBinding;
    }

    public JsonElement writeInterface(ExceptionInterface exceptionInterface) throws IOException {
        JsonArray generator = new JsonArray();
        Deque<SentryException> exceptions = exceptionInterface.getExceptions();
        Iterator iterator = exceptions.descendingIterator();
        while (iterator.hasNext()) {
            generator.add(this.writeException((SentryException) iterator.next()));
        }
        return generator;
    }

    private JsonObject writeException(SentryException sentryException) throws IOException {
        JsonObject generator = new JsonObject();
        generator.addProperty(TYPE_PARAMETER, sentryException.getExceptionClassName());
        generator.addProperty(VALUE_PARAMETER, sentryException.getExceptionMessage());
        generator.addProperty(MODULE_PARAMETER, sentryException.getExceptionPackageName());
        generator.add(STACKTRACE_PARAMETER, this.stackTraceInterfaceBinding.writeInterface(sentryException.getStackTraceInterface()));
        return generator;
    }
}